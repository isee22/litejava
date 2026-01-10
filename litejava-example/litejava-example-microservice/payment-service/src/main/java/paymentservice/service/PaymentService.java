package paymentservice.service;

import common.BizException;
import common.Err;
import common.event.PaymentRefundedEvent;
import common.event.PaymentSuccessEvent;
import litejava.plugins.mq.MqPlugin;
import paymentservice.G;
import paymentservice.model.Payment;
import paymentservice.model.Refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付服务
 * 
 * @author LiteJava
 */
public class PaymentService {
    
    public static List<Payment> findAll() {
        return G.paymentMapper.findAll();
    }
    
    public static Payment findByPaymentNo(String paymentNo) {
        return G.paymentMapper.findByPaymentNo(paymentNo);
    }
    
    public static Payment findByOrderNo(String orderNo) {
        return G.paymentMapper.findByOrderNo(orderNo);
    }
    
    public static List<Payment> findByUserId(Long userId) {
        return G.paymentMapper.findByUserId(userId);
    }

    /**
     * 创建支付单
     */
    public static Payment create(String orderNo, Long userId, BigDecimal amount, String channel) {
        Payment existing = G.paymentMapper.findByOrderNo(orderNo);
        if (existing != null && existing.status == Payment.STATUS_SUCCESS) BizException.error(Err.PAYMENT_FAILED, "订单已支付");
        
        Payment payment = new Payment();
        payment.paymentNo = "PAY" + System.currentTimeMillis();
        payment.orderNo = orderNo;
        payment.userId = userId;
        payment.amount = amount;
        payment.channel = channel;
        payment.status = Payment.STATUS_PENDING;
        
        G.paymentMapper.insert(payment);
        return payment;
    }
    
    /**
     * 发起支付（分布式锁防重复）
     */
    public static Payment pay(String paymentNo) {
        return G.redis().withLock("lock:payment:pay:" + paymentNo, 30, () -> {
            Payment payment = G.paymentMapper.findByPaymentNo(paymentNo);
            if (payment == null) BizException.error(Err.PAYMENT_NOT_FOUND, "支付单不存在");
            if (payment.status != Payment.STATUS_PENDING) BizException.error(Err.PAYMENT_FAILED, "支付单状态无效");
            
            payment.status = Payment.STATUS_PROCESSING;
            G.paymentMapper.updateStatus(payment);
            
            // 模拟支付成功
            payment.status = Payment.STATUS_SUCCESS;
            payment.paidAt = LocalDateTime.now();
            G.paymentMapper.updateStatus(payment);
            
            // 发送支付成功消息（异步通知订单服务）
            publishPaymentSuccess(payment);
            
            return payment;
        });
    }
    
    /**
     * 支付回调（分布式锁保证幂等）
     */
    public static Payment callback(String paymentNo, boolean success) {
        return G.redis().withLock("lock:payment:callback:" + paymentNo, 30, () -> {
            Payment payment = G.paymentMapper.findByPaymentNo(paymentNo);
            if (payment == null) BizException.error(Err.PAYMENT_NOT_FOUND, "支付单不存在");
            
            if (payment.status == Payment.STATUS_SUCCESS || payment.status == Payment.STATUS_FAILED) {
                return payment;
            }
            
            if (success) {
                payment.status = Payment.STATUS_SUCCESS;
                payment.paidAt = LocalDateTime.now();
                // 发送支付成功消息
                publishPaymentSuccess(payment);
            } else {
                payment.status = Payment.STATUS_FAILED;
            }
            
            G.paymentMapper.updateStatus(payment);
            return payment;
        });
    }
    
    /**
     * 申请退款（分布式锁防重复）
     */
    public static Refund refund(String paymentNo, BigDecimal amount, String reason) {
        return G.redis().withLock("lock:payment:refund:" + paymentNo, 30, () -> {
            Payment payment = G.paymentMapper.findByPaymentNo(paymentNo);
            if (payment == null) BizException.error(Err.PAYMENT_NOT_FOUND, "支付单不存在");
            if (payment.status != Payment.STATUS_SUCCESS) BizException.error(Err.REFUND_FAILED, "支付单未成功，无法退款");
            if (amount.compareTo(payment.amount) > 0) BizException.error(Err.REFUND_AMOUNT_ERROR, "退款金额超过支付金额");
            
            Refund refund = new Refund();
            refund.refundNo = "REF" + System.currentTimeMillis();
            refund.paymentNo = paymentNo;
            refund.amount = amount;
            refund.reason = reason;
            refund.status = 0;
            
            G.refundMapper.insert(refund);
            
            refund.status = 2;
            G.refundMapper.updateStatus(refund.refundNo, 2);
            
            payment.status = Payment.STATUS_REFUNDED;
            G.paymentMapper.updateStatus(payment);
            
            // 发送退款成功消息
            MqPlugin mq = G.app.getPlugin(MqPlugin.class);
            if (mq != null && mq.isConnected()) {
                mq.publishJson("payment.refunded", new PaymentRefundedEvent(
                    payment.orderNo, paymentNo, refund.refundNo, amount
                ));
            } else {
                G.app.log.warn("[MQ] 消息队列未连接，退款消息未发送: " + paymentNo);
            }
            
            return refund;
        });
    }
    
    /**
     * 发布支付成功消息
     * 优先使用 MQ，MQ 不可用时同步调用订单服务
     */
    private static void publishPaymentSuccess(Payment payment) {
        MqPlugin mq = G.app.getPlugin(MqPlugin.class);
        if (mq != null && mq.isConnected()) {
            mq.publishJson("payment.success", new PaymentSuccessEvent(
                payment.orderNo, payment.paymentNo, payment.userId, payment.amount
            ));
            G.app.log.info("[MQ] 发送支付成功消息: " + payment.orderNo);
        } else {
            // MQ 不可用，同步调用订单服务更新状态
            G.app.log.warn("[MQ] 消息队列未连接，使用同步调用更新订单状态");
            try {
                litejava.plugins.http.RpcClient rpc = G.app.getPlugin(litejava.plugins.http.RpcClient.class);
                if (rpc != null) {
                    java.util.Map<String, Object> body = new java.util.HashMap<>();
                    body.put("orderNo", payment.orderNo);
                    body.put("status", 2); // 已支付
                    rpc.call("order-service", "/order/updateStatus", body);
                    G.app.log.info("[RPC] 同步更新订单状态成功: " + payment.orderNo);
                }
            } catch (Exception e) {
                G.app.log.error("[RPC] 同步更新订单状态失败: " + e.getMessage());
            }
        }
    }
}
