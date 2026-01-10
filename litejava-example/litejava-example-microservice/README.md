# LiteJava 微服务示例

基于 LiteJava 框架的完整微服务架构示例，展示如何用 LiteJava 构建生产级微服务系统。

## 项目结构

```
litejava-example-microservice/
├── common/              # 公共模块 (异常、工具类)
├── gateway/             # API 网关 (Consul 版)
├── gateway-k8s/         # API 网关 (K8s 版)
├── user-service/        # 用户服务
├── product-service/     # 商品服务
├── order-service/       # 订单服务
├── auth-service/        # 认证服务
├── payment-service/     # 支付服务
├── notification-service/# 通知服务
├── search-service/      # 搜索服务
├── vue-frontend/        # Vue 前端
├── k8s/                 # K8s 部署配置
└── docs/                # 文档
    ├── ARCHITECTURE.md  # 架构说明
    ├── DEPLOY-DOCKER.md # Docker 部署指南
    └── DEPLOY-LOCAL.md  # 本地部署指南
```

## 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | LiteJava (Java 版 Gin) |
| 服务发现 | Consul |
| 网关 | 自研 Gateway |
| 数据库 | H2 (内存) / MySQL |
| 缓存 | Redis |
| 消息队列 | RabbitMQ |
| 链路追踪 | Zipkin |
| 日志聚合 | ELK (Elasticsearch + Logstash + Kibana) |
| 前端 | Vue 3 + Vite |

## 快速开始

### Docker 部署 (推荐)

```bash
# 1. 构建所有服务
mvn clean package -DskipTests

# 2. 启动基础设施
docker-compose up -d consul redis rabbitmq

# 3. 启动所有服务
docker-compose up -d

# 4. 访问
# 前端: http://localhost:3000
# 网关: http://localhost:8080
# Consul: http://localhost:8500
```

### 本地开发

```bash
# 1. 启动 Consul
consul agent -dev

# 2. 依次启动服务
cd user-service && mvn exec:java
cd product-service && mvn exec:java
cd order-service && mvn exec:java
cd gateway && mvn exec:java
```

## 文档

- [架构说明](docs/ARCHITECTURE.md) - 详细架构设计
- [Docker 部署](docs/DEPLOY-DOCKER.md) - Docker/Docker Compose 部署
- [本地部署](docs/DEPLOY-LOCAL.md) - 本地开发环境部署
- [K8s 部署](k8s/README.md) - Kubernetes 部署

## 服务端口

| 服务 | 端口 |
|------|------|
| Gateway | 8080 |
| User Service | 8081 |
| Order Service | 8082 |
| Product Service | 8083 |
| Payment Service | 8084 |
| Auth Service | 8085 |
| Notification Service | 8086 |
| Search Service | 8087 |
| Frontend | 3000 |
