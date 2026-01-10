package paymentservice.controller;

import common.BizException;
import common.Err;
import common.vo.ListResult;
import litejava.Context;
import litejava.Routes;
import paymentservice.model.Payment;
import paymentservice.model.Refund;
import paymentservice.service.PaymentService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class PaymentController {
    
    public static Routes routes() {
        return new Routes()
            .post("/payment/list", PaymentController::list)
            .post("/payment/detail", PaymentController::detail)
            .post("/payment/create", PaymentController::create)
            .post("/payment/pay", PaymentController::pay)
            .post("/payment/callback", PaymentController::callback)
            .post("/payment/refund", PaymentController::refund)
            .post("/payment/user", PaymentController::listByUser)
            .end();
    }
    
    static void list(Context ctx) {
        List<Payment> payments = PaymentService.findAll();
        ctx.ok(ListResult.of(payments));
    }
    
    static void detail(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String paymentNo = (String) body.get("paymentNo");
        if (paymentNo == null || paymentNo.isEmpty()) BizException.paramRequired("paymentNo");
        
        Payment payment = PaymentService.findByPaymentNo(paymentNo);
        if (payment == null) BizException.error(Err.PAYMENT_NOT_FOUND, "支付单不存在");
        ctx.ok(payment);
    }

    static void create(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String orderNo = (String) body.get("orderNo");
        if (orderNo == null || body.get("amount") == null) BizException.paramInvalid("orderNo, amount 不能为空");
        
        // 优先从 header 获取 userId（Gateway 传递），其次从 body 获取
        String userIdStr = ctx.header("X-User-Id");
        Long userId;
        if (userIdStr != null && !userIdStr.isEmpty()) {
            userId = Long.parseLong(userIdStr);
        } else if (body.get("userId") != null) {
            userId = ((Number) body.get("userId")).longValue();
        } else {
            ctx.fail(401, "未登录");
            return;
        }
        
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String channel = (String) body.get("channel");
        Payment payment = PaymentService.create(orderNo, userId, amount, channel);
        ctx.ok(payment);
    }
    
    static void pay(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String paymentNo = (String) body.get("paymentNo");
        if (paymentNo == null || paymentNo.isEmpty()) BizException.paramRequired("paymentNo");
        
        Payment payment = PaymentService.pay(paymentNo);
        ctx.ok(payment);
    }
    
    static void callback(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String paymentNo = (String) body.get("paymentNo");
        if (paymentNo == null || paymentNo.isEmpty()) BizException.paramRequired("paymentNo");
        
        boolean success = Boolean.TRUE.equals(body.get("success"));
        Payment payment = PaymentService.callback(paymentNo, success);
        ctx.ok(payment);
    }
    
    static void refund(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String paymentNo = (String) body.get("paymentNo");
        if (paymentNo == null || body.get("amount") == null) BizException.paramInvalid("paymentNo, amount 不能为空");
        
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String reason = (String) body.get("reason");
        Refund refund = PaymentService.refund(paymentNo, amount, reason);
        ctx.ok(refund);
    }
    
    static void listByUser(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("userId") == null) BizException.paramRequired("userId");
        
        Long userId = ((Number) body.get("userId")).longValue();
        List<Payment> payments = PaymentService.findByUserId(userId);
        ctx.ok(ListResult.of(payments));
    }
}
