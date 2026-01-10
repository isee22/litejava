# API Gateway (Consul 版)

API 网关，所有请求的统一入口。使用 Consul 进行服务发现。

## 功能

- 路由转发 - 根据路径前缀路由到后端服务
- JWT 认证 - Token 验证和用户信息提取
- 限流 - 基于 Resilience4j 的限流保护
- 熔断 - 服务不可用时快速失败
- 灰度发布 - 按版本/用户分流
- 链路追踪 - 生成 TraceId/SpanId
- CORS - 跨域支持

## 技术栈

- LiteJava 框架
- Consul (服务发现)
- JWT (认证)
- Resilience4j (限流/熔断)
- OkHttp (HTTP 客户端)

## 路由规则

| 路径前缀 | 目标服务 |
|----------|----------|
| /user/* | user-service:8081 |
| /product/* | product-service:8083 |
| /order/* | order-service:8082 |
| /auth/* | auth-service:8085 |
| /payment/* | payment-service:8084 |

## 中间件链

```
请求 → RecoveryPlugin → CorsPlugin → TracingPlugin 
     → RateLimiterPlugin → GrayReleaseFilter 
     → AuthFilter → ProxyFilter → 后端服务
```

## 配置

```yaml
server:
  port: 8080

jwt:
  secret: "your-secret-key"

publicPaths:           # 无需认证的路径
  - /auth/login
  - /auth/register
  - /product/list

consul:
  host: consul
  port: 8500
```

## 启动

```bash
# 本地
java -jar target/gateway-1.0.0-jdk8-shaded.jar

# Docker
docker-compose up -d gateway
```

## 健康检查

```bash
curl http://localhost:8080/health
```
