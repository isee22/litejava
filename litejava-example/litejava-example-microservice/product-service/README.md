# Product Service

商品服务，负责商品管理、库存管理和分类管理。

## 功能

- 商品列表/详情/搜索
- 商品创建/更新/删除
- 库存增减
- 分类管理

## 技术栈

- LiteJava 框架
- H2 内存数据库 (可切换 MySQL)
- Consul (服务注册)
- HikariCP (连接池)

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /product/list | 商品列表 |
| POST | /product/detail | 商品详情 |
| POST | /product/search | 商品搜索 |
| POST | /product/create | 创建商品 |
| POST | /product/update | 更新商品 |
| POST | /product/delete | 删除商品 |
| POST | /product/stock/decrease | 扣减库存 |
| POST | /product/stock/increase | 增加库存 |
| POST | /category/list | 分类列表 |
| POST | /category/create | 创建分类 |
| GET | /health | 健康检查 |

## 数据模型

```java
public class Product {
    public Long id;
    public String name;
    public Long categoryId;
    public BigDecimal price;
    public Integer stock;
    public String description;
    public String imageUrl;
    public Integer status;      // 1:上架 0:下架
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}

public class Category {
    public Long id;
    public String name;
    public Long parentId;
}
```

## 配置

```yaml
server:
  port: 8083

service:
  name: product-service
  host: auto

database:
  url: jdbc:h2:mem:productdb
  username: sa
  password: 
```

## 启动

```bash
# 本地
java -jar target/product-service-1.0.0-jdk8-shaded.jar

# Docker (支持扩展)
docker-compose up -d --scale product-service=3
```

## 被调用方

- Order Service (下单时扣减库存)
- Gateway (商品查询)
