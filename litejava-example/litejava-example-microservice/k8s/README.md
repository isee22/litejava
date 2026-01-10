# Kubernetes 部署指南

## 前置条件

- Docker Desktop with Kubernetes enabled
- 或 minikube / kind
- kubectl 命令行工具

## 与 Docker Compose 的区别

| 功能 | Docker Compose | Kubernetes |
|------|---------------|------------|
| 服务发现 | Consul | K8s DNS (内置) |
| 负载均衡 | Gateway 轮询 | K8s Service (自动) |
| 扩缩容 | `--scale` | `kubectl scale` / HPA |
| 健康检查 | Consul HTTP | liveness/readiness probe |
| 配置管理 | 配置文件 | ConfigMap / Secret |
| 网关 | gateway (Consul版) | gateway-k8s (K8s版) |

## 架构

```
                    ┌─────────────────┐
                    │   LoadBalancer  │
                    │   (port 8080)   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │   Gateway-K8s   │
                    │  (无 Consul 依赖) │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
┌───────▼───────┐   ┌───────▼───────┐   ┌───────▼───────┐
│ product-svc   │   │  user-svc     │   │  order-svc    │
│  (3 replicas) │   │  (2 replicas) │   │  (2 replicas) │
└───────────────┘   └───────────────┘   └───────────────┘
```

## 快速部署

### Windows

```bash
cd k8s
deploy.cmd
```

### Linux/macOS

```bash
cd k8s
chmod +x deploy.sh
./deploy.sh
```

### 手动部署

```bash
# 1. 构建项目
cd ..
mvn clean package -DskipTests -pl gateway-k8s,product-service,user-service,order-service -am

# 2. 构建 Docker 镜像
docker build -t litejava/gateway-k8s:latest gateway-k8s
docker build -t litejava/product-service:latest product-service
docker build -t litejava/user-service:latest user-service
docker build -t litejava/order-service:latest order-service

# 3. 创建命名空间
kubectl apply -f k8s/namespace.yaml

# 4. 部署服务
kubectl apply -f k8s/
```

## 验证部署

```bash
# 查看 Pod 状态
kubectl get pods -n litejava

# 查看服务
kubectl get svc -n litejava

# 获取 Gateway 地址
kubectl get svc gateway -n litejava

# 测试健康检查
curl http://localhost:8080/health
```

## 常用命令

### 扩缩容

```bash
# 扩展到 5 个实例
kubectl scale deployment product-service --replicas=5 -n litejava

# 缩减到 1 个实例
kubectl scale deployment product-service --replicas=1 -n litejava
```

### 日志查看

```bash
# 查看 Gateway 日志
kubectl logs -f deployment/gateway -n litejava

# 查看特定 Pod 日志
kubectl logs -f <pod-name> -n litejava
```

### 进入容器

```bash
kubectl exec -it <pod-name> -n litejava -- sh
```

### 清理

```bash
# 删除所有资源
kubectl delete namespace litejava
```

## 路由配置

Gateway-K8s 通过 K8s Service DNS 访问后端服务：

```yaml
gateway:
  routes:
    /product: http://product-service:8083
    /user: http://user-service:8081
    /order: http://order-service:8082
```

K8s 会自动将 `product-service` 解析为对应 Service 的 ClusterIP，并负载均衡到后端 Pod。

## 配置文件说明

| 文件 | 说明 |
|------|------|
| namespace.yaml | 创建 litejava 命名空间 |
| gateway.yaml | Gateway Deployment + Service |
| product-service.yaml | Product Service Deployment + Service |
| user-service.yaml | User Service Deployment + Service |
| order-service.yaml | Order Service Deployment + Service |

## 生产环境建议

### 使用 Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: litejava-ingress
  namespace: litejava
spec:
  rules:
  - host: api.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: gateway
            port:
              number: 8080
```

### 配置 HPA (自动扩缩容)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: product-service-hpa
  namespace: litejava
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: product-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### 使用 ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: gateway-config
  namespace: litejava
data:
  application.yml: |
    server:
      port: 8080
    gateway:
      routes:
        /product: http://product-service:8083
```
