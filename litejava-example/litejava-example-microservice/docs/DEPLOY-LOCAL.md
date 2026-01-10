# 本地部署指南

本地开发环境部署，不使用 Docker。

## 环境要求

- JDK 9+ (推荐 JDK 11/17)
- Maven 3.6+
- Node.js 18+ (前端)
- Consul (服务发现)
- Redis (可选，缓存)

## 安装依赖

### 1. 安装 Consul

**Windows (Chocolatey)**:
```bash
choco install consul
```

**macOS (Homebrew)**:
```bash
brew install consul
```

**Linux**:
```bash
# 下载
wget https://releases.hashicorp.com/consul/1.15.4/consul_1.15.4_linux_amd64.zip
unzip consul_1.15.4_linux_amd64.zip
sudo mv consul /usr/local/bin/
```

### 2. 安装 Redis (可选)

**Windows**:
```bash
# 使用 WSL 或下载 Windows 版本
# https://github.com/microsoftarchive/redis/releases
```

**macOS**:
```bash
brew install redis
brew services start redis
```

**Linux**:
```bash
sudo apt install redis-server
sudo systemctl start redis
```

## 启动服务

### 1. 启动 Consul

```bash
# 开发模式 (数据不持久化)
consul agent -dev

# 或指定数据目录
consul agent -dev -data-dir=/tmp/consul
```

验证: http://localhost:8500

### 2. 构建项目

```bash
cd litejava-example/litejava-example-microservice

# 构建所有模块
mvn clean install -DskipTests
```

### 3. 启动后端服务

每个服务在单独的终端窗口启动：

**User Service (8081)**:
```bash
cd user-service
java -jar target/user-service-1.0.0-jdk8-shaded.jar
```

**Product Service (8083)**:
```bash
cd product-service
java -jar target/product-service-1.0.0-jdk8-shaded.jar
```

**Order Service (8082)**:
```bash
cd order-service
java -jar target/order-service-1.0.0-jdk8-shaded.jar
```

**Gateway (8080)**:
```bash
cd gateway
java -jar target/gateway-1.0.0-jdk8-shaded.jar
```

### 4. 启动前端 (可选)

```bash
cd vue-frontend
npm install
npm run dev
```

访问: http://localhost:5173

## 配置文件

### 本地配置

每个服务的 `application.yml` 默认配置适用于本地开发：

```yaml
server:
  port: 8081

service:
  name: user-service
  host: localhost    # 本地用 localhost

consul:
  host: localhost
  port: 8500
```

### 修改配置

如果需要修改配置，编辑 `src/main/resources/application.yml`，然后重新打包：

```bash
mvn package -DskipTests -pl user-service
```

## 服务启动顺序

推荐按以下顺序启动：

```
1. Consul (服务发现)
2. Redis (可选，缓存)
3. User Service
4. Product Service
5. Order Service
6. Auth Service (可选)
7. Gateway
8. Frontend (可选)
```

## 验证部署

### 检查服务注册

```bash
# 查看 Consul 服务列表
curl http://localhost:8500/v1/agent/services
```

### 测试 API

```bash
# 健康检查
curl http://localhost:8080/health

# 商品列表 (POST)
curl -X POST http://localhost:8080/product/list \
  -H "Content-Type: application/json" \
  -d '{}'

# 用户列表 (需要登录)
curl -X POST http://localhost:8080/user/list \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{}'
```

## IDE 开发

### IntelliJ IDEA

1. 导入 Maven 项目
2. 右键服务模块 → Run 'xxxApp.main()'
3. 或配置 Run Configuration

### VS Code

1. 安装 Java Extension Pack
2. 打开项目文件夹
3. 在 Java Projects 面板运行 main 方法

## 调试

### 远程调试

启动时添加调试参数：

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar target/user-service-1.0.0-jdk8-shaded.jar
```

然后在 IDE 中连接 `localhost:5005`。

### 日志级别

修改 `logback.xml` 或启动参数：

```bash
java -Dlogging.level.root=DEBUG -jar xxx.jar
```

## 常见问题

### 端口被占用

```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <pid> /F

# Linux/macOS
lsof -i :8080
kill -9 <pid>
```

### Consul 连接失败

确保 Consul 正在运行：
```bash
consul members
```

### 服务注册失败

检查 `application.yml` 中的配置：
- `service.host` 应为 `localhost`
- `consul.host` 应为 `localhost`

### 数据库连接失败

本地默认使用 H2 内存数据库，无需额外配置。如需 MySQL：

1. 安装 MySQL
2. 创建数据库
3. 修改 `application.yml`:

```yaml
database:
  url: jdbc:mysql://localhost:3306/litejava
  username: root
  password: your_password
```

## 快速启动脚本

### Windows (start-local.cmd)

```batch
@echo off
echo Starting Consul...
start "Consul" consul agent -dev

timeout /t 5

echo Starting Services...
start "User Service" cmd /c "cd user-service && java -jar target\user-service-1.0.0-jdk8-shaded.jar"
start "Product Service" cmd /c "cd product-service && java -jar target\product-service-1.0.0-jdk8-shaded.jar"
start "Order Service" cmd /c "cd order-service && java -jar target\order-service-1.0.0-jdk8-shaded.jar"

timeout /t 10

start "Gateway" cmd /c "cd gateway && java -jar target\gateway-1.0.0-jdk8-shaded.jar"

echo All services started!
echo Gateway: http://localhost:8080
echo Consul: http://localhost:8500
```

### Linux/macOS (start-local.sh)

```bash
#!/bin/bash

echo "Starting Consul..."
consul agent -dev &
sleep 5

echo "Starting Services..."
cd user-service && java -jar target/user-service-1.0.0-jdk8-shaded.jar &
cd ../product-service && java -jar target/product-service-1.0.0-jdk8-shaded.jar &
cd ../order-service && java -jar target/order-service-1.0.0-jdk8-shaded.jar &

sleep 10

cd ../gateway && java -jar target/gateway-1.0.0-jdk8-shaded.jar &

echo "All services started!"
echo "Gateway: http://localhost:8080"
echo "Consul: http://localhost:8500"
```
