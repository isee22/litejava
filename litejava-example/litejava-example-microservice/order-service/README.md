# Order Service

订单服务，负责订单管理和订单流程处理。

## 功能

- 订单列表/详情查询
- 创建订单 (调用 Product Service 扣减库存)
- 取消订单 (调用 Product Service 恢复库存)
- 订单状态更新

## 技术栈

- LiteJava 框架
- H2 内存数据库 (可切换 MySQL)
- Consul (服务注册/发现)
- RpcClient (服务间调用)
- HikariCP (连接池)

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /order/list | 订单列表 |
| POST | /order/detail | 订单详情 |
| POST | /order/create | 创建订单 |
| POST | /order/cancel | 取消订单 |
| POST | /order/pay | 支付订单 |
| GET | /health | 健康检查 |

## 数据模型

```java
public class Order {
    public Long id;
    public String orderNo;      // 订单号
    public Long userId;
    public BigDecimal totalAmount;
    public Integer status;      // 0:待支付 1:已支付 2:已发货 3:已完成 4:已取消
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}

public class OrderItem {
    public Long id;
    public Long orderId;
    public Long productId;
    public String productName;
    public BigDecimal price;
    public Integer quantity;
}
```

## 订单状态流转

```
创建订单 → 待支付(0) → 已支付(1) → 已发货(2) → 已完成(3)
                ↓
            已取消(4)
```

## 服务间调用

创建订单时调用 Product Service 扣减库存：

```java
RpcClient rpc = app.getPlugin(RpcClient.class);
Map<String, Object> body = Map.of("id", productId, "quantity", quantity);
Map<String, Object> result = rpc.call("product-service", "/product/stock/decrease", body);
```

## 配置

```yaml
server:
  port: 8082

service:
  name: order-service
  host: auto

database:
  url: jdbc:h2:mem:orderdb
  username: sa
  password: 

consul:
  host: consul
  port: 8500
```

## 启动

```bash
# 本地
java -jar target/order-service-1.0.0-jdk8-shaded.jar

# Docker
docker-compose up -d order-service
```

## 依赖服务

- Product Service (库存扣减)
- User Service (用户信息)
- Consul (服务注册/发现)
