# Service 导向架构 - 一个文件一个服务

## 1. 核心理念

**每个 Service 文件 = 一个独立微服务**

```java
// UserService.java → 启动后就是 user-service
// OrderService.java → 启动后就是 order-service
// InventoryService.java → 启动后就是 inventory-service

// 通过 Nacos 自动发现和调用
```

---

## 2. 项目结构

```
my-app/
├── pom.xml
├── src/main/java/
│   ├── user/                        # 用户服务
│   │   ├── UserService.java         # 服务入口
│   │   ├── UserController.java
│   │   ├── UserDAO.java
│   │   └── entity/
│   │       └── User.java
│   │
│   ├── order/                       # 订单服务
│   │   ├── OrderService.java        # 服务入口
│   │   ├── OrderController.java
│   │   ├── OrderDAO.java
│   │   └── entity/
│   │       └── Order.java
│   │
│   ├── inventory/                   # 库存服务
│   │   ├── InventoryService.java    # 服务入口
│   │   ├── InventoryController.java
│   │   ├── InventoryDAO.java
│   │   └── entity/
│   │       └── Inventory.java
│   │
│   ├── common/                      # 公共代码
│   │   ├── vo/                      # 通用 VO
│   │   ├── exception/               # 通用异常
│   │   └── util/                    # 工具类
│   │
│   └── Bootstrap.java               # 启动器
│
└── config/
    └── application.yml
```

**关键点**：
- ✅ 单项目，所有服务在一起
- ✅ 每个服务一个文件夹，职责清晰
- ✅ 服务内部自包含（Controller/DAO/Entity）
- ✅ 启动时指定加载哪个服务
- ✅ 服务之间通过 Nacos + RPC 调用

---

## 3. Service 定义

### 3.1 用户服务

```java
// user/UserService.java
@Service("user-service")  // 服务名
public class UserService {
    private UserController controller;
    
    // 初始化（框架自动调用）
    public void init(App app) {
        this.controller = new UserController(app);
        controller.registerRoutes(app);
    }
}

// user/UserController.java
public class UserController {
    private UserDAO userDAO;
    
    public UserController(App app) {
        this.userDAO = new UserDAO(app);
    }
    
    public void registerRoutes(App app) {
        app.get("/user/:id", this::getUser);
        app.post("/user/balance/check", this::checkBalance);
        app.post("/user/balance/deduct", this::deductBalance);
    }
    
    private void getUser(Context ctx) {
        Long userId = ctx.pathLong("id");
        User user = userDAO.findById(userId);
        if (user == null) {
            return ctx.fail(404, "用户不存在");
        }
        ctx.ok(user);
    }
    
    private void checkBalance(Context ctx) {
        Long userId = ctx.bodyLong("userId");
        BigDecimal amount = ctx.bodyDecimal("amount");
        
        User user = userDAO.findById(userId);
        boolean sufficient = user.balance.compareTo(amount) >= 0;
        ctx.ok(Map.of("sufficient", sufficient));
    }
    
    private void deductBalance(Context ctx) {
        Long userId = ctx.bodyLong("userId");
        BigDecimal amount = ctx.bodyDecimal("amount");
        
        User user = userDAO.findById(userId);
        if (user.balance.compareTo(amount) < 0) {
            return ctx.fail(400, "余额不足");
        }
        
        user.balance = user.balance.subtract(amount);
        userDAO.update(user);
        
        ctx.ok(Map.of("success", true));
    }
}

// user/UserDAO.java
public class UserDAO {
    private DatabasePlugin db;
    private RedisCachePlugin cache;
    
    public UserDAO(App app) {
        this.db = app.getPlugin(DatabasePlugin.class);
        this.cache = app.getPlugin(RedisCachePlugin.class);
    }
    
    public User findById(Long id) {
        String key = "user:" + id;
        User user = cache.get(key, User.class);
        if (user != null) {
            return user;
        }
        
        user = db.queryOne("SELECT * FROM user WHERE id = ?", User.class, id);
        if (user != null) {
            cache.set(key, user, 3600);
        }
        return user;
    }
    
    public void update(User user) {
        db.update("UPDATE user SET balance = ? WHERE id = ?", user.balance, user.id);
        cache.del("user:" + user.id);
    }
}

// user/entity/User.java
public class User {
    public Long id;
    public String username;
    public BigDecimal balance;
    public Date createTime;
}
```

---

### 3.2 订单服务（调用其他服务）

```java
// order/OrderService.java
@Service("order-service")
public class OrderService {
    private OrderController controller;
    
    public void init(App app) {
        this.controller = new OrderController(app);
        controller.registerRoutes(app);
    }
}

// order/OrderController.java
public class OrderController {
    private OrderDAO orderDAO;
    private RpcClient rpc;
    
    public OrderController(App app) {
        this.orderDAO = new OrderDAO(app);
        this.rpc = app.getPlugin(RpcClient.class);
    }
    
    public void registerRoutes(App app) {
        app.post("/order/create", this::createOrder);
        app.get("/order/:id", this::getOrder);
    }
    
    private void createOrder(Context ctx) {
        Long userId = ctx.bodyLong("userId");
        Long skuId = ctx.bodyLong("skuId");
        int quantity = ctx.bodyInt("quantity");
        BigDecimal amount = ctx.bodyDecimal("amount");
        
        // 1. 检查用户余额（RPC 调用）
        Map<String, Object> balanceResp = rpc.post("user-service", "/user/balance/check",
            Map.of("userId", userId, "amount", amount));
        if (!(boolean) balanceResp.get("sufficient")) {
            return ctx.fail(400, "余额不足");
        }
        
        // 2. 扣减库存（RPC 调用）
        Map<String, Object> stockResp = rpc.post("inventory-service", "/inventory/deduct", 
            Map.of("skuId", skuId, "quantity", quantity));
        if (!(boolean) stockResp.get("success")) {
            return ctx.fail(400, "库存不足");
        }
        
        // 3. 扣减余额（RPC 调用）
        Map<String, Object> deductResp = rpc.post("user-service", "/user/balance/deduct",
            Map.of("userId", userId, "amount", amount));
        if (!(boolean) deductResp.get("success")) {
            // 回滚库存
            rpc.post("inventory-service", "/inventory/release",
                Map.of("skuId", skuId, "quantity", quantity));
            return ctx.fail(400, "扣款失败");
        }
        
        // 4. 创建订单
        Order order = new Order();
        order.userId = userId;
        order.skuId = skuId;
        order.quantity = quantity;
        order.amount = amount;
        orderDAO.insert(order);
        
        ctx.ok(order);
    }
    
    private void getOrder(Context ctx) {
        Long orderId = ctx.pathLong("id");
        Order order = orderDAO.findById(orderId);
        if (order == null) {
            return ctx.fail(404, "订单不存在");
        }
        ctx.ok(order);
    }
}

// order/OrderDAO.java
public class OrderDAO {
    private DatabasePlugin db;
    
    public OrderDAO(App app) {
        this.db = app.getPlugin(DatabasePlugin.class);
    }
    
    public void insert(Order order) {
        db.insert("INSERT INTO `order` (user_id, sku_id, quantity, amount) VALUES (?, ?, ?, ?)",
            order.userId, order.skuId, order.quantity, order.amount);
    }
    
    public Order findById(Long id) {
        return db.queryOne("SELECT * FROM `order` WHERE id = ?", Order.class, id);
    }
}

// order/entity/Order.java
public class Order {
    public Long id;
    public Long userId;
    public Long skuId;
    public int quantity;
    public BigDecimal amount;
    public Date createTime;
}
```

---

### 3.3 库存服务

```java
// inventory/InventoryService.java
@Service("inventory-service")
public class InventoryService {
    
    public void init(App app) {
        InventoryController controller = new InventoryController(app);
        controller.init(app);
        
        app.log.info("库存服务启动成功");
    }
}

// inventory/InventoryController.java
public class InventoryController {
    private InventoryDAO inventoryDAO;
    
    public InventoryController(App app) {
        this.inventoryDAO = new InventoryDAO(app);
    }
    
    public void init(App app) {
        app.post("/inventory/deduct", this::deduct);
        app.post("/inventory/release", this::release);
        app.get("/inventory/:id", this::getInventory);
    }
    
    private void deduct(Context ctx) {
        Long skuId = ctx.bodyLong("skuId");
        int quantity = ctx.bodyInt("quantity");
        
        Inventory inv = inventoryDAO.findById(skuId);
        if (inv == null) {
            return ctx.fail(404, "商品不存在");
        }
        if (inv.stock < quantity) {
            return ctx.fail(400, "库存不足");
        }
        
        inv.stock -= quantity;
        inventoryDAO.update(inv);
        
        ctx.ok(Map.of("success", true));
    }
    
    private void release(Context ctx) {
        Long skuId = ctx.bodyLong("skuId");
        int quantity = ctx.bodyInt("quantity");
        
        Inventory inv = inventoryDAO.findById(skuId);
        if (inv == null) {
            return ctx.fail(404, "商品不存在");
        }
        
        inv.stock += quantity;
        inventoryDAO.update(inv);
        
        ctx.ok(Map.of("success", true));
    }
    
    private void getInventory(Context ctx) {
        Long skuId = ctx.pathLong("id");
        Inventory inv = inventoryDAO.findById(skuId);
        if (inv == null) {
            return ctx.fail(404, "商品不存在");
        }
        ctx.ok(inv);
    }
}

// inventory/InventoryDAO.java
public class InventoryDAO {
    private RedisCachePlugin cache;
    private InventoryMapper mapper;
    
    public InventoryDAO(App app) {
        this.cache = app.getPlugin(RedisCachePlugin.class);
        this.mapper = app.getPlugin(MyBatisPlugin.class).getMapper(InventoryMapper.class);
    }
    
    public Inventory findById(Long skuId) {
        String key = "inventory:" + skuId;
        Inventory inv = cache.get(key, Inventory.class);
        if (inv != null) {
            return inv;
        }
        
        inv = mapper.selectById(skuId);
        if (inv != null) {
            cache.set(key, inv, 3600);
        }
        return inv;
    }
    
    public void update(Inventory inv) {
        mapper.updateById(inv);
        cache.del("inventory:" + inv.skuId);
    }
}

// inventory/entity/Inventory.java
public class Inventory {
    public Long skuId;
    public String skuName;
    public int stock;
    public Date updateTime;
}
```

---

## 4. 启动器

```java
// Bootstrap.java
public class Bootstrap {
    public static void main(String[] args) {
        // 读取配置：启动哪个服务
        String serviceName = System.getProperty("service", "all");
        int port = Integer.parseInt(System.getProperty("port", "8080"));
        
        App app = LiteJava.create();
        
        // 注册 Nacos
        NacosPlugin nacos = new NacosPlugin("localhost:8848");
        app.use(nacos);
        
        // 注册 RPC 客户端
        RpcClient rpc = new RpcClient(nacos);
        app.use(rpc);
        
        // 加载服务
        if ("all".equals(serviceName)) {
            // 开发模式：加载所有服务
            loadService(app, UserService.class);
            loadService(app, OrderService.class);
            loadService(app, InventoryService.class);
        } else {
            // 生产模式：只加载指定服务
            Class<?> serviceClass = findServiceClass(serviceName);
            loadService(app, serviceClass);
            nacos.register(serviceName, "localhost", port);
        }
        
        app.run(port);
    }
    
    private static void loadService(App app, Class<?> serviceClass) {
        try {
            Object service = serviceClass.newInstance();
            Method initMethod = serviceClass.getMethod("init", App.class);
            initMethod.invoke(service, app);
            
            Service annotation = serviceClass.getAnnotation(Service.class);
            app.log.info("服务加载成功: {}", annotation.value());
        } catch (Exception e) {
            throw new LiteJavaException("服务加载失败: " + serviceClass.getName(), e);
        }
    }
    
    private static Class<?> findServiceClass(String serviceName) {
        // 根据服务名查找对应的类
        try {
            switch (serviceName) {
                case "user-service":
                    return Class.forName("user.UserService");
                case "order-service":
                    return Class.forName("order.OrderService");
                case "inventory-service":
                    return Class.forName("inventory.InventoryService");
                default:
                    throw new IllegalArgumentException("未知服务: " + serviceName);
            }
        } catch (ClassNotFoundException e) {
            throw new LiteJavaException("服务类不存在: " + serviceName, e);
        }
    }
}
```

---

## 5. RPC 客户端（简化版）

```java
// plugin/RpcClient.java
public class RpcClient extends Plugin {
    private NacosPlugin nacos;
    private OkHttpClient httpClient;
    
    public RpcClient(NacosPlugin nacos) {
        this.nacos = nacos;
        this.httpClient = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    }
    
    // GET 请求
    public Map<String, Object> get(String serviceName, String path) {
        return call(serviceName, "GET", path, null);
    }
    
    // POST 请求
    public Map<String, Object> post(String serviceName, String path, Map<String, Object> body) {
        return call(serviceName, "POST", path, body);
    }
    
    // 通用调用
    private Map<String, Object> call(String serviceName, String method, String path, Map<String, Object> body) {
        try {
            // 1. 从 Nacos 获取服务实例
            ServiceInstance instance = nacos.getInstance(serviceName);
            String url = "http://" + instance.ip + ":" + instance.port + path;
            
            // 2. 构造请求
            Request.Builder builder = new Request.Builder().url(url);
            if ("POST".equals(method) && body != null) {
                String json = app.getPlugin(JsonPlugin.class).toJson(body);
                builder.post(RequestBody.create(
                    MediaType.parse("application/json"),
                    json
                ));
            }
            
            // 3. 发送请求
            Response response = httpClient.newCall(builder.build()).execute();
            String respBody = response.body().string();
            
            // 4. 解析响应
            return app.getPlugin(JsonPlugin.class).fromJson(respBody, Map.class);
            
        } catch (Exception e) {
            app.log.error("RPC 调用失败: {} {}", serviceName, path, e);
            return null;
        }
    }
}
```

---

## 6. 启动方式

### 6.1 开发模式（单进程）

```bash
# 启动所有服务（本地调试）
java -Dservice=all -jar my-app.jar

# 访问：
# http://localhost:8080/user/1
# http://localhost:8080/order/create
# http://localhost:8080/inventory/deduct
```

---

### 6.2 生产模式（多进程）

```bash
# 启动 Nacos
docker run -d -p 8848:8848 nacos/nacos-server

# 启动用户服务
java -Dservice=user-service -Dport=8001 -jar my-app.jar

# 启动订单服务
java -Dservice=order-service -Dport=8002 -jar my-app.jar

# 启动库存服务
java -Dservice=inventory-service -Dport=8003 -jar my-app.jar
```

**自动服务发现**：
- OrderService 调用 `rpc.post("user-service", ...)` 
- RpcClient 自动从 Nacos 获取 user-service 的地址
- 支持负载均衡（多实例随机选择）

---

## 7. 配置文件

```yaml
# application.yml
nacos:
  server: localhost:8848

services:
  user-service:
    port: 8001
  order-service:
    port: 8002
  inventory-service:
    port: 8003
```

---

## 8. Service 注解（可选）

```java
// 简单的注解，标记服务名
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {
    String value(); // 服务名
}
```

---

## 9. 优势

| 维度 | 说明 |
|------|------|
| ✅ 开发简单 | 单项目，所有代码在一起 |
| ✅ 调试方便 | 开发时启动一个进程即可 |
| ✅ 部署灵活 | 生产时可以独立部署 |
| ✅ 代码复用 | Entity/DAO 直接共享 |
| ✅ 接口变更 | 一处修改，立即生效 |
| ✅ 服务发现 | Nacos 自动注册和发现 |
| ✅ 负载均衡 | 多实例自动负载均衡 |

---

## 10. 进阶：动态服务加载

```java
// 扫描所有 @Service 注解的类
public class ServiceScanner {
    public static List<Class<?>> scan() {
        List<Class<?>> services = new ArrayList<>();
        
        // 扫描所有包下的 @Service 类
        Reflections reflections = new Reflections("");
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Service.class);
        
        services.addAll(classes);
        return services;
    }
}

// Bootstrap 改进
public class Bootstrap {
    public static void main(String[] args) {
        String serviceName = System.getProperty("service", "all");
        
        App app = LiteJava.create();
        // ... 初始化 Nacos、RPC
        
        if ("all".equals(serviceName)) {
            // 自动扫描所有服务
            List<Class<?>> services = ServiceScanner.scan();
            for (Class<?> serviceClass : services) {
                loadService(app, serviceClass);
            }
        } else {
            // 只加载指定服务
            Class<?> serviceClass = findServiceClass(serviceName);
            loadService(app, serviceClass);
        }
        
        app.run();
    }
}
```

---

## 11. 完整目录示例

```
my-app/
├── pom.xml
├── src/main/java/
│   ├── user/                        # 用户服务模块
│   │   ├── UserService.java         # @Service("user-service")
│   │   ├── UserController.java      # 路由注册
│   │   ├── UserDAO.java             # 数据访问
│   │   ├── entity/
│   │   │   └── User.java
│   │   ├── vo/
│   │   │   └── UserVO.java
│   │   └── mapper/
│   │       └── UserMapper.java
│   │
│   ├── order/                       # 订单服务模块
│   │   ├── OrderService.java        # @Service("order-service")
│   │   ├── OrderController.java
│   │   ├── OrderDAO.java
│   │   ├── entity/
│   │   │   └── Order.java
│   │   ├── vo/
│   │   │   └── OrderVO.java
│   │   └── mapper/
│   │       └── OrderMapper.java
│   │
│   ├── inventory/                   # 库存服务模块
│   │   ├── InventoryService.java    # @Service("inventory-service")
│   │   ├── InventoryController.java
│   │   ├── InventoryDAO.java
│   │   ├── entity/
│   │   │   └── Inventory.java
│   │   └── mapper/
│   │       └── InventoryMapper.java
│   │
│   ├── common/                      # 公共模块
│   │   ├── exception/
│   │   │   ├── BusinessException.java
│   │   │   └── ServiceException.java
│   │   └── util/
│   │       └── DateUtil.java
│   │
│   └── Bootstrap.java               # 启动器
│
├── src/main/resources/
│   ├── application.yml              # 配置文件
│   └── mapper/                      # MyBatis XML
│       ├── UserMapper.xml
│       ├── OrderMapper.xml
│       └── InventoryMapper.xml
│
└── README.md
```

---

## 11. 总结

**核心思想**：
1. **一个文件 = 一个服务** → 简单直观
2. **默认微服务** → 生产环境独立部署
3. **开发时单体** → 本地调试方便
4. **RPC + Nacos** → 成熟的服务发现

**适用场景**：
- ✅ 中小团队
- ✅ 快速迭代
- ✅ 需要灵活部署

**下一步**：
- 解决跨服务事务问题（Saga 模式）
- 解决并发锁问题（乐观锁 + Redis）
