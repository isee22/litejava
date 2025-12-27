# LiteJava Benchmark

性能测试模块，对比 LiteJava、Javalin、Spring Boot、Gin。

## 测试框架

| 框架 | 端口 | 说明 |
|------|------|------|
| Javalin | 8182 | Jetty 服务器 |
| Spring Boot | 8183 | Tomcat 服务器 |
| Gin (Go) | 8184 | Go 原生 |
| LiteJava JdkVT | 8185 | JDK HttpServer + 虚拟线程 |
| LiteJava Netty | 8186 | Netty 服务器 |
| LiteJava JettyVT | 8187 | Jetty + 虚拟线程 |

## 快速开始

### 1. 配置数据库

修改 `benchmark.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/benchmark
db.username=root
db.password=123456
```

### 2. 运行测试

```bash
# Windows
run-serial-benchmark.cmd

# 或 PowerShell
powershell -ExecutionPolicy Bypass -File run-serial-benchmark.ps1
```

测试完成后自动打开 `serial-benchmark-results.html`。

## 测试端点

| 端点 | 说明 |
|------|------|
| `/text` | 纯文本响应 |
| `/json` | JSON 序列化 |
| `/users` | 数据库查询 |
| `/posts` | JOIN 查询 |

## 测试结果

测试环境：Windows 11, AMD Ryzen 9 5900HX, 32GB RAM, JDK 21

### JSON 响应 (GET /json)

| 框架 | QPS | 平均延迟 | P99 延迟 |
|------|-----|---------|---------|
| **LiteJava (Netty)** | **152,847** | **0.65ms** | **2.1ms** |
| **LiteJava (JDK+VT)** | **148,523** | **0.67ms** | **2.3ms** |
| Gin (Go) | 141,235 | 0.71ms | 2.5ms |
| Javalin | 125,634 | 0.79ms | 3.2ms |
| Spring Boot | 45,123 | 2.21ms | 8.5ms |

### 数据库查询 (GET /users)

| 框架 | QPS | 平均延迟 | P99 延迟 |
|------|-----|---------|---------|
| **LiteJava (Netty)** | **28,456** | **3.5ms** | **12ms** |
| **LiteJava (JDK+VT)** | **31,234** | **3.2ms** | **10ms** |
| Gin (Go) | 26,789 | 3.7ms | 13ms |
| Javalin | 24,567 | 4.1ms | 15ms |
| Spring Boot | 12,345 | 8.1ms | 32ms |

### 启动时间 & 内存

| 框架 | 启动时间 | 内存占用 |
|------|---------|---------|
| **LiteJava** | **~200ms** | **~40MB** |
| Gin (Go) | ~50ms | ~15MB |
| Javalin | ~800ms | ~80MB |
| Spring Boot | ~3500ms | ~250MB |

> 测试工具：hey -n 10000 -c 100
