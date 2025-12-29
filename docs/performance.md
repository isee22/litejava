# 性能调优指南

## 基准数据

LiteJava 默认配置下的性能参考：

| 指标 | 数值 |
|------|------|
| 启动时间 | < 500ms |
| 内存占用 | 30-80MB |
| QPS (Hello World) | 50,000+ |
| 延迟 P99 | < 5ms |

## 服务器选择

### JDK HttpServer（默认）

```java
app.use(new HttpServerPlugin());
```

- 零依赖，启动最快
- 适合中小流量场景
- QPS: ~30,000

### Netty

```java
app.use(new NettyServerPlugin());
```

- 高性能，适合高并发
- 内存占用稍高
- QPS: ~80,000

### Undertow

```java
app.use(new UndertowServerPlugin());
```

- 性能优秀，Red Hat 出品
- 适合生产环境
- QPS: ~70,000

## 连接池配置

### HikariCP

```yaml
hikari:
  maximumPoolSize: 20      # 最大连接数
  minimumIdle: 5           # 最小空闲连接
  connectionTimeout: 30000 # 连接超时(ms)
  idleTimeout: 600000      # 空闲超时(ms)
  maxLifetime: 1800000     # 连接最大生命周期(ms)
```

**调优建议**：

```
最大连接数 = (核心数 * 2) + 有效磁盘数
```

- 4 核服务器：10-15 连接
- 8 核服务器：20-25 连接

## JSON 性能

### Jackson（默认）

```java
app.use(new JacksonPlugin());
```

- 功能全面，性能优秀
- 适合大多数场景

### 优化配置

```java
JacksonPlugin jackson = new JacksonPlugin();
jackson.configure(mapper -> {
    // 禁用不需要的特性
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
});
app.use(jackson);
```

## 缓存策略

### 内存缓存

```java
// 简单场景用 ConcurrentHashMap
Map<String, Object> cache = new ConcurrentHashMap<>();

// 带过期的缓存用 Caffeine
Cache<String, Object> cache = Caffeine.newBuilder()
    .maximumSize(10000)
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .build();
```

### Redis 缓存

```yaml
redis:
  host: localhost
  port: 6379
  timeout: 3000
  pool:
    maxTotal: 50
    maxIdle: 10
```

### 缓存模式

```java
// Cache-Aside 模式
public User getUser(long id) {
    String key = "user:" + id;
    String cached = redis.get(key);
    if (cached != null) {
        return json.parse(cached, User.class);
    }
    
    User user = userMapper.findById(id);
    if (user != null) {
        redis.set(key, json.stringify(user), 3600);
    }
    return user;
}
```

## 数据库优化

### 避免 N+1 查询

```java
// ❌ N+1 问题
List<Order> orders = orderMapper.list();
for (Order order : orders) {
    User user = userMapper.findById(order.userid);  // N 次查询
    order.user = user;
}

// ✅ 批量查询
List<Order> orders = orderMapper.list();
Set<Long> userIds = orders.stream().map(o -> o.userid).collect(toSet());
Map<Long, User> userMap = userMapper.findByIds(userIds)
    .stream().collect(toMap(u -> u.id, u -> u));
```

### 分页优化

```java
// ❌ 深分页性能差
SELECT * FROM orders LIMIT 100000, 20;

// ✅ 游标分页
SELECT * FROM orders WHERE id > #{lastId} LIMIT 20;
```

### 索引优化

```sql
-- 常用查询字段加索引
CREATE INDEX idx_user_username ON user(username);
CREATE INDEX idx_order_userid ON orders(userid);

-- 复合索引
CREATE INDEX idx_order_user_status ON orders(userid, status);
```

## 异步处理

### 耗时操作异步化

```java
ExecutorService executor = Executors.newFixedThreadPool(10);

void sendNotification(Context ctx) {
    Map<String, Object> body = ctx.bindJSON();
    
    // 异步发送通知
    executor.submit(() -> {
        notificationService.send(body);
    });
    
    // 立即返回
    ctx.ok();
}
```

### 批量操作

```java
// ❌ 逐条插入
for (User user : users) {
    userMapper.insert(user);
}

// ✅ 批量插入
userMapper.batchInsert(users);
```

## JVM 调优

### 启动参数

```bash
java -Xms256m -Xmx512m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -jar app.jar
```

### 内存配置

| 场景 | Xms | Xmx |
|------|-----|-----|
| 开发环境 | 128m | 256m |
| 小型服务 | 256m | 512m |
| 中型服务 | 512m | 1g |
| 大型服务 | 1g | 2g |

### GC 选择

| GC | 适用场景 |
|----|----------|
| G1GC | 通用，推荐 |
| ZGC | 低延迟要求 (JDK 11+) |
| Shenandoah | 低延迟要求 (JDK 12+) |

## 监控指标

### 关键指标

- QPS / TPS
- 响应时间 (P50, P95, P99)
- 错误率
- 连接池使用率
- GC 频率和耗时
- 内存使用率

### 健康检查

```java
app.use(new HealthPlugin());
// GET /health 返回服务状态
```

## 压测工具

### wrk

```bash
wrk -t12 -c400 -d30s http://localhost:8080/api/users
```

### ab

```bash
ab -n 10000 -c 100 http://localhost:8080/api/users
```

### hey

```bash
hey -n 10000 -c 100 http://localhost:8080/api/users
```

## 常见问题

### 1. 启动慢

- 检查是否有阻塞的初始化操作
- 数据库连接是否正常
- 减少启动时的预加载

### 2. 内存泄漏

- 检查缓存是否有上限
- 检查连接是否正确关闭
- 使用 jmap/jstat 分析

### 3. 响应慢

- 检查数据库查询是否有慢 SQL
- 检查是否有 N+1 问题
- 检查外部服务调用超时设置

### 4. QPS 上不去

- 增加连接池大小
- 检查是否有锁竞争
- 考虑换用 Netty/Undertow
