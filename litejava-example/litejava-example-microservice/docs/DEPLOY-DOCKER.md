# Docker 部署指南

## 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 内存 8GB+ (推荐 16GB)
- 磁盘 20GB+

## 快速部署

### 1. 构建项目

```bash
# 进入项目目录
cd litejava-example/litejava-example-microservice

# 构建所有模块
mvn clean package -DskipTests
```

### 2. 启动服务

```bash
# 启动所有服务 (包括基础设施)
docker-compose up -d

# 或分步启动
# 先启动基础设施
docker-compose up -d consul redis rabbitmq elasticsearch zipkin

# 等待基础设施就绪 (约 30 秒)
sleep 30

# 启动业务服务
docker-compose up -d user-service product-service order-service gateway

# 启动前端
docker-compose up -d frontend
```

### 3. 验证部署

```bash
# 检查容器状态
docker-compose ps

# 检查服务健康
curl http://localhost:8080/health

# 访问 Consul UI
# http://localhost:8500

# 访问前端
# http://localhost:3000
```

## 服务扩缩容

### 扩展服务实例

```bash
# 扩展 product-service 到 3 个实例
docker-compose up -d --scale product-service=3

# 扩展多个服务
docker-compose up -d --scale product-service=3 --scale user-service=2
```

### 缩减服务实例

```bash
# 缩减到 1 个实例
docker-compose up -d --scale product-service=1
```

### 注意事项

扩缩容后需要重启 Gateway 和 Frontend 以刷新服务发现：

```bash
docker-compose restart gateway
docker restart litejava-frontend
```

## 常用命令

### 日志查看

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f gateway
docker-compose logs -f product-service

# 查看最近 100 行
docker-compose logs --tail=100 gateway
```

### 服务管理

```bash
# 重启单个服务
docker-compose restart gateway

# 停止单个服务
docker-compose stop product-service

# 启动单个服务
docker-compose start product-service

# 重新构建并启动
docker-compose up -d --build product-service
```

### 清理

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v

# 清理未使用的镜像
docker image prune -f
```

## 配置说明

### 环境变量

每个服务支持以下环境变量：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| SERVER_PORT | 服务端口 | 各服务不同 |
| CONSUL_HOST | Consul 地址 | consul |
| CONSUL_PORT | Consul 端口 | 8500 |
| REDIS_HOST | Redis 地址 | redis |
| REDIS_PORT | Redis 端口 | 6379 |

### 修改配置

1. 修改 `docker-compose.yml` 中的环境变量
2. 或创建 `.env` 文件：

```env
CONSUL_HOST=consul
REDIS_HOST=redis
MYSQL_HOST=mysql
```

## 端口映射

| 服务 | 容器端口 | 宿主机端口 |
|------|----------|------------|
| Frontend | 80 | 3000 |
| Gateway | 8080 | 8080 |
| Consul | 8500 | 8500 |
| Redis | 6379 | 6379 |
| RabbitMQ | 5672/15672 | 5672/15672 |
| Zipkin | 9411 | 9411 |
| Elasticsearch | 9200 | 9200 |
| Kibana | 5601 | 5601 |

## 故障排查

### 服务无法启动

```bash
# 查看容器日志
docker logs <container_name>

# 检查容器状态
docker inspect <container_name>
```

### 服务发现失败

```bash
# 检查 Consul 服务列表
curl http://localhost:8500/v1/agent/services

# 检查健康状态
curl http://localhost:8500/v1/health/state/critical
```

### 网络问题

```bash
# 检查 Docker 网络
docker network ls
docker network inspect litejava-example-microservice_default

# 测试容器间连通性
docker exec -it <container> ping <other_container>
```

### 清理僵尸注册

如果 Consul 中有失效的服务注册：

```bash
# 列出所有服务
curl http://localhost:8500/v1/agent/services

# 注销特定服务
curl -X PUT http://localhost:8500/v1/agent/service/deregister/<service_id>
```

## 生产环境建议

### 资源限制

在 `docker-compose.yml` 中添加资源限制：

```yaml
services:
  product-service:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
```

### 健康检查

确保所有服务配置健康检查：

```yaml
services:
  product-service:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8083/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### 日志持久化

```yaml
services:
  product-service:
    volumes:
      - ./logs/product-service:/app/logs
```

### 数据持久化

```yaml
volumes:
  consul-data:
  redis-data:
  elasticsearch-data:

services:
  consul:
    volumes:
      - consul-data:/consul/data
  redis:
    volumes:
      - redis-data:/data
```
