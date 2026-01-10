# 微服务架构说明

## 整体架构

```
                                    ┌─────────────────┐
                                    │   Vue Frontend  │
                                    │   (port 3000)   │
                                    └────────┬────────┘
                                             │
                                    ┌────────▼────────┐
                                    │     Nginx       │
                                    │  (反向代理)      │
                                    └────────┬────────┘
                                             │
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API Gateway (8080)                              │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │  Auth   │ │  CORS   │ │ Tracing │ │  Rate   │ │  Gray   │ │  Proxy  │   │
│  │ Filter  │ │ Plugin  │ │ Plugin  │ │ Limiter │ │ Release │ │ Filter  │   │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                             │
              ┌──────────────────────────────┼──────────────────────────────┐
              │                              │                              │
     ┌────────▼────────┐           ┌────────▼────────┐           ┌────────▼────────┐
     │  User Service   │           │ Product Service │           │  Order Service  │
     │    (8081)       │           │    (8083)       │           │    (8082)       │
     └────────┬────────┘           └────────┬────────┘           └────────┬────────┘
              │                              │                              │
              └──────────────────────────────┼──────────────────────────────┘
                                             │
┌─────────────────────────────────────────────────────────────────────────────┐
│                            基础设施层                                        │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │ Consul  │ │  Redis  │ │RabbitMQ │ │ Zipkin  │ │   ELK   │ │  MySQL  │   │
│  │ (8500)  │ │ (6379)  │ │ (5672)  │ │ (9411)  │ │ (5601)  │ │ (3306)  │   │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 核心组件

### 1. API Gateway (网关)

网关是所有请求的入口，负责：

| 功能 | 实现 | 说明 |
|------|------|------|
| 路由转发 | ProxyFilter | 根据路径前缀路由到后端服务 |
| 认证鉴权 | AuthFilter | JWT Token 验证 |
| 限流 | RateLimiterPlugin | 基于 Resilience4j |
| 熔断 | CircuitBreaker | 服务不可用时快速失败 |
| 灰度发布 | GrayReleaseFilter | 按用户/版本分流 |
| 链路追踪 | TracingPlugin | 生成 TraceId/SpanId |
| 跨域 | CorsPlugin | CORS 支持 |

**路由规则**：
```
/user/*    → user-service:8081
/product/* → product-service:8083
/order/*   → order-service:8082
/auth/*    → auth-service:8085
/payment/* → payment-service:8084
```

### 2. 服务发现 (Consul)

```
┌─────────────────────────────────────────────────────────────┐
│                        Consul                                │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                  Service Registry                    │    │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐         │    │
│  │  │user-svc   │ │product-svc│ │order-svc  │  ...    │    │
│  │  │172.18.0.11│ │172.18.0.12│ │172.18.0.13│         │    │
│  │  │:8081      │ │:8083      │ │:8082      │         │    │
│  │  └───────────┘ └───────────┘ └───────────┘         │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                  Health Check                        │    │
│  │  HTTP GET /health 每 10 秒检查一次                   │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

**服务注册流程**：
1. 服务启动时，ConsulPlugin 自动检测容器 IP
2. 向 Consul 注册服务 (服务名、IP、端口)
3. 配置健康检查端点 `/health`
4. Consul 定期检查服务健康状态

**服务发现流程**：
1. Gateway 从 Consul 获取服务列表
2. 根据路径前缀找到目标服务
3. 轮询选择健康实例
4. 转发请求

### 3. 负载均衡

```
                    Gateway
                       │
          ┌────────────┼────────────┐
          │            │            │
          ▼            ▼            ▼
    ┌──────────┐ ┌──────────┐ ┌──────────┐
    │product-1 │ │product-2 │ │product-3 │
    │172.18.0.11│ │172.18.0.19│ │172.18.0.20│
    └──────────┘ └──────────┘ └──────────┘
```

**负载均衡策略**：
- 默认：轮询 (Round Robin)
- 灰度：按版本/用户分流

### 4. 熔断器

```java
CircuitBreaker circuitBreaker = new CircuitBreaker();

// 执行带熔断保护的调用
String result = circuitBreaker.execute(
    "product-service",           // 服务名
    () -> doProxy(ctx, url),     // 正常调用
    () -> "{\"code\":503,...}"   // 降级响应
);
```

**熔断状态**：
- CLOSED: 正常状态，请求正常转发
- OPEN: 熔断状态，直接返回降级响应
- HALF_OPEN: 半开状态，允许部分请求探测

## 数据流

### 请求处理流程

```
1. 用户请求 → Nginx → Gateway
2. Gateway 中间件链:
   RecoveryPlugin → CorsPlugin → TracingPlugin → RateLimiter
   → AuthFilter → ProxyFilter
3. ProxyFilter:
   - 解析路径前缀 (/product/list → product-service)
   - 从 Consul 获取服务实例
   - 熔断器包装请求
   - 转发到后端服务
4. 后端服务处理请求，返回响应
5. Gateway 返回响应给用户
```

### 服务间调用

```
Order Service                    Product Service
     │                                │
     │  POST /product/stock/decrease  │
     │ ─────────────────────────────► │
     │                                │
     │  {"code":0,"msg":"success"}    │
     │ ◄───────────────────────────── │
```

使用 RpcClient 进行服务间调用：
```java
RpcClient rpc = app.getPlugin(RpcClient.class);
Map<String, Object> result = rpc.call("product-service", "/product/stock/decrease", body);
```

## 技术选型理由

### 为什么用 Consul 而不是 Nacos/Eureka？

| 对比项 | Consul | Nacos | Eureka |
|--------|--------|-------|--------|
| 一致性 | CP (强一致) | AP/CP 可选 | AP |
| 健康检查 | 主动检查 | 心跳 | 心跳 |
| KV 存储 | ✅ 内置 | ✅ 内置 | ❌ |
| 多数据中心 | ✅ | ✅ | ❌ |
| 依赖 | 单二进制 | Java | Java |

选择 Consul：轻量、功能完整、Go 实现性能好。

### 为什么自研 Gateway 而不是用 Kong/APISIX？

1. **轻量**: 自研 Gateway 启动 <500ms，内存 <100MB
2. **可控**: 完全掌控路由、认证、限流逻辑
3. **一致**: 与业务服务使用相同技术栈 (LiteJava)
4. **学习**: 展示如何用 LiteJava 构建网关

## 扩展性设计

### 水平扩展

```bash
# Docker Compose 扩展
docker-compose up -d --scale product-service=5

# K8s 扩展
kubectl scale deployment product-service --replicas=5 -n litejava
```

### 新增服务

1. 创建新服务模块
2. 注册到 Consul
3. Gateway 自动发现并路由

### 灰度发布

```yaml
# 配置灰度规则
gray:
  rules:
    - service: product-service
      version: v2
      weight: 10        # 10% 流量到 v2
      users: [1001, 1002]  # 指定用户走 v2
```
