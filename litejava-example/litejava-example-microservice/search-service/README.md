# Search Service

搜索服务，提供全文搜索能力。

## 功能

- 商品搜索
- 搜索建议
- 热门搜索
- 搜索历史

## 技术栈

- LiteJava 框架
- Elasticsearch (全文搜索)
- H2 内存数据库 (搜索历史)

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /search/product | 商品搜索 |
| POST | /search/suggest | 搜索建议 |
| GET | /search/hot | 热门搜索 |
| POST | /search/history | 搜索历史 |
| GET | /health | 健康检查 |

## 搜索参数

```json
{
  "keyword": "iPhone",
  "categoryId": 1,
  "minPrice": 1000,
  "maxPrice": 10000,
  "sort": "price_asc",
  "page": 1,
  "size": 20
}
```

## 配置

```yaml
server:
  port: 8087

service:
  name: search-service
  host: auto

elasticsearch:
  host: elasticsearch
  port: 9200
```

## 启动

```bash
java -jar target/search-service-1.0.0-jdk8-shaded.jar
```

## 数据同步

商品数据通过以下方式同步到 Elasticsearch：
1. Product Service 创建/更新商品时发送消息
2. Search Service 消费消息更新索引
3. 或定时全量同步
