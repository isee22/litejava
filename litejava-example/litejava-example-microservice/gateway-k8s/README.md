# API Gateway (K8s 版)

Kubernetes 环境专用网关，使用 K8s Service DNS 进行服务发现，无需 Consul。

## 与 Consul 版的区别

| 对比项 | gateway (Consul) | gateway-k8s |
|--------|------------------|-------------|
| 服务发现 | Consul API | K8s Service DNS |
| 路由配置 | 动态从 Consul 获取 | 静态配置文件 |
| 负载均衡 | Gateway 轮询 | K8s Service 自动 |
| 灰度发布 | GrayReleaseFilter | K8s Ingress |
| 依赖 | Consul | 无 |

## 技术栈

- LiteJava 框架
- K8s Service DNS (服务发现)
- JWT (认证)
- Resilience4j (限流/熔断)
- OkHttp (HTTP 客户端)

## 路由配置

```yaml
gateway:
  routes:
    /product: http://product-service:8083
    /user: http://user-service:8081
    /order: http://order-service:8082
```

K8s 会自动将 `product-service` 解析为对应 Service 的 ClusterIP。

## 部署

```bash
# 构建镜像
docker build -t litejava/gateway-k8s:latest .

# 部署到 K8s
kubectl apply -f ../k8s/gateway.yaml
```

## 本地测试

本地测试时需要修改路由配置为实际地址：

```yaml
gateway:
  routes:
    /product: http://localhost:63357  # Docker 端口映射
```

## 健康检查

K8s 使用 liveness/readiness probe：

```yaml
livenessProbe:
  httpGet:
    path: /health
    port: 8080
readinessProbe:
  httpGet:
    path: /health/ready
    port: 8080
```
