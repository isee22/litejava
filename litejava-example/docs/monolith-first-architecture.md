# 单体优先架构 - 开发简单，部署灵活

## 1. 核心理念

**开发时是单体，部署时是微服务**

```
开发阶段：
  一个项目 → 所有模块在一起 → 本地调试方便

部署阶段：
  同一份代码 → 启动参数不同 → 部署成不同服务
```

---

## 2. 传统微服务的痛点

### 痛点 1：开发麻烦
```
order-service/     ← 项目1
user-service/      ← 项目2
inventory-service/ ← 项目3

改个接口 → 3个项目都要改 → 3个 IDE 窗口切换
```

### 痛点 2：调试麻烦
```
本地调试：
  启动 order-service   → 8001
  启动 user-service    → 8002
  启动 inventory-service → 8003
  
  内存占用 1GB+
  启动时间 30秒+
  改代码要重启 3 个服务
```

### 痛点 3：依赖地狱
```
order-service 依赖 user-service
  → user-service 挂了，order-service 也挂
  → 本地调试要启动一堆服务
```

---

## 3. 单体优先方案

### 3.1 项目结构

```
my-app/
├── pom.xml
├── src/main/java/
│   ├── App.java                    # 启动入口
│   ├── order/                      # 订单模块
│   │   ├── OrderController.java
│   │   ├── OrderService.java
│   │   └── OrderDAO.java
│   ├── user/                       # 用户模块
│   │   ├── UserController.java
│   │   ├── UserService.java
│   │   └── UserDAO.java
│   └── inventory/                  # 库存模块
│       ├── InventoryController.java
│       ├── InventoryService.java
│       └── InventoryDAO.java
└── config/
    ├── app-all.yml                 # 单体模式配置
    ├── app-order.yml               # 订单服务配置
    ├── app-user.yml                # 用户服务配置
    └── app-inventory.yml           # 库存服务配置
```

**关键点**：
- ✅ 一个项目，所有模块在一起
- ✅ 模块之间直接调用（开发阶段）
- ✅ 通过配置决定启动哪些模块（部署阶段）

---

### 3.2 模块化设计

```java
// 模块接口
public interface Module {
    String name();
    void init(App app);
    void destroy();
}

// 订单模块
public class OrderModule implements Module {
    private UserService userService;
    private InventoryService inventoryService;
    
    @Override
    public String name() {
        return "order";
    }
    
    @Override
    public void init(App app) {
        // 注册路由
        app.post("/order/create", this::createOrder);
        
        // 依赖注入（本地调用或远程调用）
        this.userService = app.getService(UserService.class);
        this.inventoryService = app.getService(InventoryService.class);
    }
    
    private void createOrder(Context ctx) {
        Long userId = ctx.bodyLong("userId");
        Long skuId = ctx.bodyLong("skuId");
        
        // 调用其他模块（透明，不关心本地还是远程）
        User user = userService.getUser(userId);
        boolean success = inventoryService.deduct(skuId, 1);
        
        if (success) {
            Order order = new Order();
            order.userId = userId;
            order.skuId = skuId;
            orderDAO.insert(order);
            ctx.ok(order);
        } else {
            ctx.fail(400, "库存不足");
        }
    }
}
```

---

### 3.3 启动模式切换

```java
public class App {
    private Map<String, Module> modules = new HashMap<>();
    private ServiceLocator serviceLocator;
    
    public static void main(String[] args) {
        App app = new App();
        
        // 读取配置：启动哪些模块
        String mode = System.getProperty("app.mode", "all");
        
        switch (mode) {
            case "all":
                // 单体模式：启动所有模块
                app.loadModule(new OrderModule());
                app.loadModule(new UserModule());
                app.loadModule(new InventoryModule());
                app.useLocalCall(); // 本地调用
                break;
                
            case "order":
                // 订单服务：只启动订单模块
                app.loadModule(new OrderModule());
                app.useRemoteCall(); // 远程调用
                break;
                
            case "user":
                app.loadModule(new UserModule());
                app.useRemoteCall();
                break;
                
            case "inventory":
                app.loadModule(new InventoryModule());
                app.useRemoteCall();
                break;
        }
        
        app.run();
    }
    
    public void loadModule(Module module) {
        modules.put(module.name(), module);
        module.init(this);
    }
    
    // 本地调用：直接方法调用
    private void useLocalCall() {
        serviceLocator = new LocalServiceLocator(modules);
    }
    
    // 远程调用：HTTP 调用
    private void useRemoteCall() {
        serviceLocator = new RemoteServiceLocator();
    }
    
    public <T> T getService(Class<T> serviceClass) {
        return serviceLocator.getService(serviceClass);
    }
}
```

---

### 3.4 服务定位器（关键）

```java
// 服务定位器接口
public interface ServiceLocator {
    <T> T getService(Class<T> serviceClass);
}

// 本地调用（单体模式）
public class LocalServiceLocator implements ServiceLocator {
    private Map<String, Module> modules;
    
    @Override
    public <T> T getService(Class<T> serviceClass) {
        // 直接返回本地实例
        String moduleName = getModuleName(serviceClass);
        Module module = modules.get(moduleName);
        return (T) module.getServiceInstance(serviceClass);
    }
}

// 远程调用（微服务模式）
public class RemoteServiceLocator implements ServiceLocator {
    private RpcClient rpcClient;
    private Map<Class<?>, Object> proxyCache = new ConcurrentHashMap<>();
    
    @Override
    public <T> T getService(Class<T> serviceClass) {
        // 返回动态代理，方法调用转换为 HTTP 请求
        return (T) proxyCache.computeIfAbsent(serviceClass, clazz -> {
            return Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[]{clazz},
                (proxy, method, args) -> {
                    String serviceName = getServiceName(clazz);
                    String path = "/" + serviceName + "/" + method.getName();
                    return rpcClient.call(serviceName, path, args);
                }
            );
        });
    }
}
```

**使用示例**：
```java
// 业务代码不变，自动适配本地/远程调用
public class OrderService {
    private UserService userService;
    
    public void createOrder(Long userId) {
        // 单体模式：直接方法调用
        // 微服务模式：HTTP 调用 user-service
        User user = userService.getUser(userId);
        
        // 业务逻辑...
    }
}
```

---

## 4. 部署方式

### 4.1 开发环境（单体模式）

```bash
# 启动所有模块
java -Dapp.mode=all -jar my-app.jar

# 一个进程，所有功能都有
# 内存占用：100MB
# 启动时间：2秒
```

### 4.2 生产环境（微服务模式）

```bash
# 订单服务
java -Dapp.mode=order -Dserver.port=8001 -jar my-app.jar

# 用户服务
java -Dapp.mode=user -Dserver.port=8002 -jar my-app.jar

# 库存服务
java -Dapp.mode=inventory -Dserver.port=8003 -jar my-app.jar
```

**配置文件**：
```yaml
# app-order.yml
app:
  mode: order
  
services:
  user:
    url: http://user-service:8002
  inventory:
    url: http://inventory-service:8003
```

---

## 5. 进阶：动态模块加载

### 5.1 按需启动模块

```yaml
# 灵活组合模块
app:
  modules:
    - order
    - user
    # 不启动 inventory，远程调用
```

```java
public class App {
    public static void main(String[] args) {
        App app = new App();
        
        // 读取配置
        List<String> enabledModules = config.getList("app.modules");
        
        // 动态加载模块
        for (String moduleName : enabledModules) {
            Module module = ModuleFactory.create(moduleName);
            app.loadModule(module);
        }
        
        app.run();
    }
}
```

**好处**：
- 可以把高频模块部署在一起（减少网络调用）
- 可以把低频模块单独部署（节省资源）

---

### 5.2 热插拔模块

```java
// 运行时加载模块
app.post("/admin/module/load", ctx -> {
    String moduleName = ctx.bodyString("module");
    Module module = ModuleFactory.create(moduleName);
    app.loadModule(module);
    ctx.ok("模块加载成功");
});

// 运行时卸载模块
app.post("/admin/module/unload", ctx -> {
    String moduleName = ctx.bodyString("module");
    Module module = modules.get(moduleName);
    module.destroy();
    modules.remove(moduleName);
    ctx.ok("模块卸载成功");
});
```

---

## 6. 对比分析

| 维度 | 传统微服务 | 单体优先 |
|------|-----------|----------|
| 开发体验 | ❌ 多项目切换 | ✅ 单项目开发 |
| 本地调试 | ❌ 启动多个服务 | ✅ 启动一个进程 |
| 代码复用 | ❌ 复制粘贴 | ✅ 直接引用 |
| 接口变更 | ❌ 多处修改 | ✅ 一处修改 |
| 部署灵活性 | ✅ 独立部署 | ✅ 独立部署 |
| 扩展性 | ✅ 独立扩展 | ✅ 独立扩展 |
| 故障隔离 | ✅ 服务隔离 | ✅ 服务隔离 |

---

## 7. 实施步骤

### Step 1: 模块化改造（1周）
```java
// 1. 定义模块接口
public interface Module {
    String name();
    void init(App app);
}

// 2. 改造现有代码
public class OrderModule implements Module {
    @Override
    public void init(App app) {
        app.post("/order/create", this::createOrder);
    }
}
```

### Step 2: 服务定位器（1周）
```java
// 1. 实现本地调用
LocalServiceLocator locator = new LocalServiceLocator();

// 2. 实现远程调用
RemoteServiceLocator locator = new RemoteServiceLocator();

// 3. 业务代码透明切换
userService = app.getService(UserService.class);
```

### Step 3: 配置化启动（3天）
```yaml
# 配置文件控制启动模式
app:
  mode: all  # all | order | user | inventory
```

### Step 4: 生产部署（1天）
```bash
# 同一份 JAR，不同启动参数
java -Dapp.mode=order -jar app.jar
java -Dapp.mode=user -jar app.jar
```

---

## 8. 真实案例

### Shopify 的做法
- 开发：单体 Rails 应用
- 部署：按模块拆分成多个服务
- 好处：开发效率高，部署灵活

### 我们的实践
```
开发阶段：
  启动一个进程 → 所有功能都能调试
  改代码 → 热重载 → 立即生效

测试阶段：
  启动单体 → 集成测试方便

生产阶段：
  订单服务 × 10 实例
  用户服务 × 5 实例
  库存服务 × 3 实例
```

---

## 9. 总结

**核心思想**：
1. **开发时单体** → 提高开发效率
2. **部署时微服务** → 保持灵活性
3. **透明切换** → 业务代码不变

**关键技术**：
- 模块化设计
- 服务定位器（本地/远程透明切换）
- 配置化启动

**适用场景**：
- ✅ 中小团队（< 50人）
- ✅ 业务快速迭代
- ✅ 需要灵活部署

**不适用场景**：
- ❌ 超大团队（> 100人，代码冲突多）
- ❌ 技术栈异构（Java + Go + Python）

---

## 10. 下一步

解决了开发问题，现在可以回到**无锁并发**问题：

**方案简化**：
- 不需要复杂的 Actor 模型
- 用 **数据库行锁 + 乐观锁** 就够了
- 真正的热点场景用 **Redis 原子操作**

```java
// 简单有效的方案
public void deductBalance(Long userId, BigDecimal amount) {
    // 乐观锁
    int rows = userDAO.updateWithVersion(
        "UPDATE user SET balance = balance - ?, version = version + 1 " +
        "WHERE id = ? AND balance >= ? AND version = ?",
        amount, userId, amount, currentVersion
    );
    
    if (rows == 0) {
        throw new ConcurrentUpdateException("余额不足或版本冲突");
    }
}
```

要不要我写一个**极简无锁方案**？不搞复杂的 Actor，就用数据库特性 + Redis。
