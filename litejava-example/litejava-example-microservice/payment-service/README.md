# Payment Service

支付服务，负责支付处理和支付状态管理。

## 功能

- 创建支付单
- 支付回调处理
- 支付状态查询
- 退款处理

## 技术栈

- LiteJava 框架
- H2 内存数据库
- Consul (服务注册)

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /payment/create | 创建支付单 |
| POST | /payment/callback | 支付回调 |
| POST | /payment/query | 查询支付状态 |
| POST | /payment/refund | 申请退款 |
| GET | /health | 健康检查 |

## 支付流程

```
1. Order Service 调用创建支付单
2. 返回支付参数 (模拟)
3. 用户完成支付
4. 支付回调更新状态
5. 通知 Order Service 更新订单状态
```

## 数据模型

```java
public class Payment {
    public Long id;
    public String paymentNo;    // 支付单号
    public Long orderId;
    public String orderNo;
    public BigDecimal amount;
    public Integer status;      // 0:待支付 1:已支付 2:已退款
    public String payType;      // alipay/wechat
    public LocalDateTime createdAt;
    public LocalDateTime paidAt;
}
```

## 配置

```yaml
server:
  port: 8084

service:
  name: payment-service
  host: auto
```

## 启动

```bash
java -jar target/payment-service-1.0.0-jdk8-shaded.jar
```

## 注意

当前为模拟实现，生产环境需对接真实支付渠道 (支付宝/微信)。
