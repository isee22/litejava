# Notification Service

通知服务，负责发送各类通知消息。

## 功能

- 短信通知 (模拟)
- 邮件通知 (模拟)
- 站内消息
- 消息模板管理

## 技术栈

- LiteJava 框架
- RabbitMQ (消息队列)
- H2 内存数据库

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /notification/send | 发送通知 |
| POST | /notification/sms | 发送短信 |
| POST | /notification/email | 发送邮件 |
| POST | /notification/list | 通知列表 |
| GET | /health | 健康检查 |

## 消息类型

```java
public enum NotificationType {
    SMS,        // 短信
    EMAIL,      // 邮件
    PUSH,       // 推送
    INTERNAL    // 站内信
}
```

## 配置

```yaml
server:
  port: 8086

service:
  name: notification-service
  host: auto

rabbitmq:
  host: rabbitmq
  port: 5672
```

## 启动

```bash
java -jar target/notification-service-1.0.0-jdk8-shaded.jar
```

## 调用方

- Order Service (订单状态变更通知)
- Payment Service (支付成功通知)
