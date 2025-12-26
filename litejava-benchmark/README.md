# LiteJava Benchmark

性能测试模块，对比 LiteJava、Javalin、Spring Boot。

## 测试项目

| 框架 | 端口 | 启动类 |
|------|------|--------|
| LiteJava | 8081 | `benchmark.LiteJavaServer` |
| Javalin | 8082 | `benchmark.JavalinServer` |
| Spring Boot | 8083 | `benchmark.SpringBootServer` |

## 测试端点

| 端点 | 说明 | 测试场景 |
|------|------|----------|
| `GET /` | 首页 HTML | 静态内容渲染 |
| `GET /text` | 纯文本 | 最小响应开销 |
| `GET /json` | JSON 响应 | JSON 序列化性能 |
| `GET /users` | 用户列表 | 数据库查询 + 分页 |
| `GET /users/:id` | 用户详情 | 单条查询 + 关联查询 |
| `GET /posts` | 文章列表 | JOIN 查询 + 分页 |

## 运行方式

### 1. 构建

```bash
mvn clean package -pl litejava-benchmark -am -DskipTests
```

### 2. 启动服务器

分别在三个终端启动：

```bash
java -jar litejava-benchmark/target/litejava-benchmark-1.0.0-SNAPSHOT.jar benchmark.LiteJavaServer
java -jar litejava-benchmark/target/litejava-benchmark-1.0.0-SNAPSHOT.jar benchmark.JavalinServer
java -jar litejava-benchmark/target/litejava-benchmark-1.0.0-SNAPSHOT.jar benchmark.SpringBootServer
```

或者用 maven:
```bash
mvn exec:java -pl litejava-benchmark -Dexec.mainClass=benchmark.LiteJavaServer
mvn exec:java -pl litejava-benchmark -Dexec.mainClass=benchmark.JavalinServer
mvn exec:java -pl litejava-benchmark -Dexec.mainClass=benchmark.SpringBootServer
```

### 3. 压测

Linux/macOS (需要 wrk):
```bash
chmod +x litejava-benchmark/scripts/benchmark.sh
./litejava-benchmark/scripts/benchmark.sh
```

Windows (需要 hey):
```bash
litejava-benchmark\scripts\benchmark.bat
```

## 测试指标

- 启动时间
- QPS (每秒请求数)
- 延迟 (P50/P99)
- 内存占用

## 数据库

使用 MySQL 数据库 + HikariCP 连接池。

### 准备工作

1. 创建数据库：
```sql
CREATE DATABASE benchmark DEFAULT CHARACTER SET utf8mb4;
```

2. 修改连接配置（`BenchmarkDb.java`）：
```java
public static final String JDBC_URL = "jdbc:mysql://localhost:3306/benchmark?useSSL=false&serverTimezone=UTC";
public static final String USER = "root";
public static final String PASSWORD = "root";
```

### 测试数据
- 100 条用户数据
- 500 条文章数据
- 首次启动时自动初始化
