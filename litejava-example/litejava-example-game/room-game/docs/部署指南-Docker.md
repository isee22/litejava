# Docker 部署指南

本文档描述如何使用 Docker 部署 Room Game 服务。

## 前置条件

- Docker 20.10+
- Docker Compose 2.0+

## 快速启动

### 1. 编译项目

```bash
cd room-game
mvn clean package -DskipTests
```

### 2. 构建镜像

```bash
# 构建所有服务镜像
docker-compose build
```

### 3. 启动服务

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 查看服务状态
docker-compose ps
```

### 4. 停止服务

```bash
docker-compose down
```

## docker-compose.yml

```yaml
version: '3.8'

services:
  registry:
    build: ./registry-server
    ports:
      - "8000:8000"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 10s
      timeout: 5s
      retries: 3

  gateway:
    build: ./gateway-server
    ports:
      - "7100:7100"
      - "7101:7101"
    environment:
      - REGISTRY_URL=http://registry:8000
      - ADVERTISE_HOST=localhost
      - ADVERTISE_WS_PORT=7100
      - ADVERTISE_HTTP_PORT=7101
    depends_on:
      registry:
        condition: service_healthy

  lobby:
    build: ./lobby-server
    ports:
      - "8100:8100"
      - "8101:8101"
    environment:
      - REGISTRY_URL=http://registry:8000
      - ADVERTISE_HOST=localhost
      - ADVERTISE_WS_PORT=8100
      - ADVERTISE_HTTP_PORT=8101
    depends_on:
      registry:
        condition: service_healthy

  match:
    build: ./match-server
    ports:
      - "8200:8200"
      - "8201:8201"
    environment:
      - REGISTRY_URL=http://registry:8000
      - ADVERTISE_HOST=localhost
      - ADVERTISE_WS_PORT=8200
      - ADVERTISE_HTTP_PORT=8201
    depends_on:
      - registry
      - lobby

  doudizhu:
    build: ./game-doudizhu
    ports:
      - "9100:9100"
      - "9101:9101"
    environment:
      - REGISTRY_URL=http://registry:8000
      - ADVERTISE_HOST=localhost
      - ADVERTISE_WS_PORT=9100
      - ADVERTISE_HTTP_PORT=9101
    depends_on:
      - registry

  gobang:
    build: ./game-gobang
    ports:
      - "9200:9200"
      - "9201:9201"
    environment:
      - REGISTRY_URL=http://registry:8000
      - ADVERTISE_HOST=localhost
      - ADVERTISE_WS_PORT=9200
      - ADVERTISE_HTTP_PORT=9201
    depends_on:
      - registry

  mahjong:
    build: ./game-mahjong
    ports:
      - "9300:9300"
      - "9301:9301"
    environment:
      - REGISTRY_URL=http://registry:8000
      - ADVERTISE_HOST=localhost
      - ADVERTISE_WS_PORT=9300
      - ADVERTISE_HTTP_PORT=9301
    depends_on:
      - registry
```

## Dockerfile 示例

```dockerfile
# game-doudizhu/Dockerfile
FROM openjdk:8-jre-alpine

WORKDIR /app
COPY target/game-doudizhu.jar /app/

EXPOSE 9100 9101

ENV REGISTRY_URL=http://registry:8000

CMD ["java", "-jar", "game-doudizhu.jar"]
```

## 环境变量

| 变量 | 说明 | 示例 |
|------|------|------|
| REGISTRY_URL | 注册中心地址 | http://registry:8000 |
| ADVERTISE_HOST | 外部可访问IP | localhost |
| ADVERTISE_WS_PORT | 外部WS端口 | 9100 |
| ADVERTISE_HTTP_PORT | 外部HTTP端口 | 9101 |
| SERVICE_ID | 服务ID (可选) | doudizhu-1 |

## 扩容游戏服务器

```bash
# 扩展斗地主服务器到 3 个实例
docker-compose up -d --scale doudizhu=3

# 注意：扩容时需要修改端口映射，使用动态端口
```

**动态端口配置:**

```yaml
doudizhu:
  build: ./game-doudizhu
  ports:
    - "9100-9199:9100"  # 端口范围
  environment:
    - REGISTRY_URL=http://registry:8000
    # 不设置 ADVERTISE_HOST，让服务自动获取容器IP
```

## 生产环境配置

### 使用外部 MySQL

```yaml
services:
  mysql:
    image: mysql:5.7
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: room_game
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3306:3306"

  lobby:
    environment:
      - DATABASE_URL=jdbc:mysql://mysql:3306/room_game
      - DATABASE_USER=root
      - DATABASE_PASSWORD=root123

volumes:
  mysql_data:
```

### 使用 Redis 缓存

```yaml
services:
  redis:
    image: redis:6-alpine
    ports:
      - "6379:6379"

  lobby:
    environment:
      - REDIS_HOST=redis
      - REDIS_PORT=6379
```

## 健康检查

```bash
# 检查所有服务
curl http://localhost:8000/services

# 检查单个服务
curl http://localhost:7101/health
curl http://localhost:8101/health
curl http://localhost:9101/health
```

## 日志管理

```bash
# 查看所有日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f doudizhu

# 查看最近 100 行
docker-compose logs --tail=100 gateway
```

## 常见问题

### Q: 服务无法互相访问
确保使用 Docker 网络内的服务名 (如 `registry`) 而不是 `localhost`。

### Q: 端口冲突
修改 docker-compose.yml 中的端口映射，或停止占用端口的本地服务。

### Q: 容器启动顺序问题
使用 `depends_on` 和 `healthcheck` 确保依赖服务先启动。
