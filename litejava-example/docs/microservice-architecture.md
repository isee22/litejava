# 微服务架构 - 独立服务 + RPC 调用

## 1. 核心理念

**每个 Service 是独立的微服务，通过 RPC + 服务发现通信**

```
order-service/      ← 独立项目，独立部署
user-service/       ← 独立项目，独立部署
inventory-service/  ← 独立项目，独立部署

通过 Nacos/Consul 服务发现 + RPC 调用
```

---

## 2. 项目结构

### 2.1 多项目结构

```
my-services/
├── common/                      # 公共模块
│   ├── pom.xml
│   └── src/main/java/
│       ├── dto/                 # 数据传输对象
│       │   ├── UserDTO.java
│       │   ├── OrderDTO.java
│       │   └── InventoryDTO.java
│       ├── api/                 # 服务接口定义
│       │   ├── UserService.java
│       │   ├── OrderService.java
│       │   └── InventoryService.java
│       └── exception/           # 公共异常
│
├── order-service/               # 订单服务
│   ├── pom.xml
│   └── src/main/java/
│       ├── OrderApp.java        # 启动类
│       ├── controller/
│       │   └── OrderController.java
│       ├── service/
│       │   └── OrderServiceImpl.java
│       └── dao/
│           └── OrderDAO.java
│
├── user-service/                # 用户服务
│   ├── pom.xml
│   └── src/main/java/
│       ├── UserApp.java
│       ├── controller/
│       │   └── UserController.java
│       ├── service/
│       │   └── UserServiceImpl.java
│       └── dao/
│           └── UserDAO.java
│
└── inventory-service/           # 库存服务
    ├── pom.xml
    └── src/main/java/
        ├── InventoryApp.java
        ├── controller/
        │   └── InventoryController.java
        ├── service/
        │   └── InventoryServiceImpl.java
        └── dao/
            └── InventoryDAO.java
```

---

## 3. 服务接口定义（common 模块）

### 3.1 定义服务接口

```java
// common/src/main/java/api/UserService.java
public interface UserService {
    UserDTO getUser(Long userId);
    boolean checkBalance(Long userId, BigDecimal amount);
    void deductBalance(Long userId, BigDecimal amount);
}

// common/src/main/java/api/InventoryService.java
public interface InventoryService {
    boolean checkStock(Long skuId, int quantity);
    boolean deductStock(Long skuId, int quantity);
    void releaseStock(Long skuId, int quantity);
}
```

### 3.2 定义 DTO

```java
// common/src/main/java/dto/UserDTO.java
public class UserDTO {
    public Long id;
    public String username;
    public BigDecimal balance;
}

// common/src/main/java/dto/OrderDTO.java
public class OrderDTO {
    public Long id;
    public Long userId;
    public Long skuId;
    public int quantity;
    public BigDecimal amount;
}
```

---

## 4. 服务实现

### 4.1 用户服务实现

```java
// user-service/src/main/java/UserApp.java
public class UserApp {
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // 注册到 Nacos
        NacosPlugin nacos = new NacosPlugin("localhost:8848");
        app.use(nacos);
        nacos.register("user-service", "localhost", 8001);
        
        // 暴露服务接口
        UserServiceImpl userService = new UserServiceImpl();
        app.exposeService(UserService.class, userService);
        
        // 启动
        app.run(8001);
    }
}

// user-service/src/main/java/service/UserServiceImpl.java
public class UserServiceImpl implements UserService {
    private UserDAO userDAO;
    
    @Override
    public UserDTO getUser(Long userId) {
        User user = userDAO.findById(userId);
        return UserDTO.from(user);
    }
    
    @Override
    public boolean checkBalance(Long userId, BigDecimal amount) {
        User user = userDAO.findById(userId);
        return user.balance.compareTo(amount) >= 0;
    }
    
    @Override
    public void deductBalance(Long userId, BigDecimal amount) {
        userDAO.deductBalance(userId, amount);
    }
}
```

---

### 4.2 订单服务实现（调用其他服务）

```java
// order-service/src/main/java/OrderApp.java
public class OrderApp {
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // 注册到 Nacos
        NacosPlugin nacos = new NacosPlugin("localhost:8848");
        app.use(nacos);
        nacos.register("order-service", "localhost", 8002);
        
        // RPC 客户端
        RpcClient rpc = new RpcClient(nacos);
        app.use(rpc);
        
        // 初始化 Controller
        OrderController controller = new OrderController(rpc);
        controller.init(app);
        
        app.run(8002);
    }
}

// order-service/src/main/java/controller/OrderController.java
public class OrderController {
    private RpcClient rpc;
    private UserService userService;
    private InventoryService inventoryService;
    
    public OrderController(RpcClient rpc) {
        this.rpc = rpc;
        // 获取远程服务代理
        this.userService = rpc.getService(UserService.class);
        this.inventoryService = rpc.getService(InventoryService.class);
    }
    
    public void init(App app) {
        app.post("/order/create", this::createOrder);
    }
    
    private void createOrder(Context ctx) {
        Long userId = ctx.bodyLong("userId");
        Long skuId = ctx.bodyLong("skuId");
        int quantity = ctx.bodyInt("quantity");
        BigDecimal amount = ctx.bodyDecimal("amount");
        
        // RPC 调用用户服务
        if (!userService.checkBalance(userId, amount)) {
            return ctx.fail(400, "余额不足");
        }
        
        // RPC 调用库存服务
        if (!inventoryService.checkStock(skuId, quantity)) {
            return ctx.fail(400, "库存不足");
        }
        
        // 扣余额
        userService.deductBalance(userId, amount);
        
        // 扣库存
        inventoryService.deductStock(skuId, quantity);
        
        // 创建订单
        Order order = new Order();
        order.userId = userId;
        order.skuId = skuId;
        order.quantity = quantity;
        order.amount = amount;
        orderDAO.insert(order);
        
        ctx.ok(OrderDTO.from(order));
    }
}
```

---

## 5. RPC 客户端实现

### 5.1 服务代理

```java
public class RpcClient extends Plugin {
    private NacosPlugin nacos;
    private Map<Class<?>, Object> proxyCache = new ConcurrentHashMap<>();
    private OkHttpClient httpClient;
    
    public RpcClient(NacosPlugin nacos) {
        this.nacos = nacos;
        this.httpClient = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
            .build();
    }
    
    public <T> T getService(Class<T> serviceClass) {
        return (T) proxyCache.computeIfAbsent(serviceClass, clazz -> {
            return Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[]{clazz},
                new RpcInvocationHandler(clazz)
            );
        });
    }
    
    private class RpcInvocationHandler implements InvocationHandler {
        private Class<?> serviceClass;
        
        public RpcInvocationHandler(Class<?> serviceClass) {
            this.serviceClass = serviceClass;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 1. 从 Nacos 获取服务实例
            String serviceName = getServiceName(serviceClass);
            ServiceInstance instance = nacos.getInstance(serviceName);
            
            // 2. 构造 HTTP 请求
            String url = "http://" + instance.ip + ":" + instance.port + "/rpc/" + method.getName();
            
            // 3. 发送请求
            Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(
                    MediaType.parse("application/json"),
                    JSON.toJSONString(args)
                ))
                .build();
            
            Response response = httpClient.newCall(request).execute();
            String body = response.body().string();
            
            // 4. 解析响应
            return JSON.parseObject(body, method.getReturnType());
        }
        
        private String getServiceName(Class<?> clazz) {
            // UserService → user-service
            String name = clazz.getSimpleName().replace("Service", "");
            return name.toLowerCase() + "-service";
        }
    }
}
```

---

### 5.2 服务暴露

```java
public class App {
    private Map<Class<?>, Object> services = new HashMap<>();
    
    // 暴露服务接口
    public <T> void exposeService(Class<T> serviceClass, T impl) {
        services.put(serviceClass, impl);
        
        // 自动注册 RPC 端点
        for (Method method : serviceClass.getMethods()) {
            String path = "/rpc/" + method.getName();
            post(path, ctx -> {
                // 解析参数
                Object[] args = JSON.parseArray(ctx.body(), method.getParameterTypes());
                
                // 调用本地方法
                Object result = method.invoke(impl, args);
                
                // 返回结果
                ctx.json(result);
            });
        }
    }
}
```

---

## 6. 服务发现（Nacos）

```java
public class NacosPlugin extends Plugin {
    private String serverAddr;
    private NamingService naming;
    
    public NacosPlugin(String serverAddr) {
        this.serverAddr = serverAddr;
    }
    
    @Override
    public void config(App app) {
        try {
            Properties props = new Properties();
            props.put("serverAddr", serverAddr);
            this.naming = NamingFactory.createNamingService(props);
        } catch (Exception e) {
            throw new LiteJavaException("Nacos 初始化失败", e);
        }
    }
    
    // 注册服务
    public void register(String serviceName, String ip, int port) {
        try {
            naming.registerInstance(serviceName, ip, port);
            app.log.info("服务注册成功: {} -> {}:{}", serviceName, ip, port);
        } catch (Exception e) {
            throw new LiteJavaException("服务注册失败", e);
        }
    }
    
    // 获取服务实例（负载均衡）
    public ServiceInstance getInstance(String serviceName) {
        try {
            List<Instance> instances = naming.selectInstances(serviceName, true);
            if (instances.isEmpty()) {
                throw new LiteJavaException("服务不可用: " + serviceName);
            }
            // 随机负载均衡
            Instance instance = instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
            return new ServiceInstance(instance.getIp(), instance.getPort());
        } catch (Exception e) {
            throw new LiteJavaException("获取服务实例失败", e);
        }
    }
}

public class ServiceInstance {
    public String ip;
    public int port;
    
    public ServiceInstance(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }
}
```

---

## 7. 开发调试

### 7.1 本地启动多个服务

```bash
# Terminal 1: 启动 Nacos
docker run -d -p 8848:8848 nacos/nacos-server

# Terminal 2: 启动用户服务
cd user-service
mvn clean package
java -jar target/user-service.jar

# Terminal 3: 启动库存服务
cd inventory-service
mvn clean package
java -jar target/inventory-service.jar

# Terminal 4: 启动订单服务
cd order-service
mvn clean package
java -jar target/order-service.jar
```

### 7.2 IDEA 多服务启动配置

```xml
<!-- .idea/runConfigurations/All_Services.xml -->
<component name="ProjectRunConfigurationManager">
  <configuration name="All Services" type="CompoundRunConfigurationType">
    <toRun name="UserService" type="Application" />
    <toRun name="InventoryService" type="Application" />
    <toRun name="OrderService" type="Application" />
  </configuration>
</component>
```

**一键启动所有服务**：点击 "All Services" 配置

---

## 8. 生产部署

### 8.1 Docker Compose

```yaml
version: '3'
services:
  nacos:
    image: nacos/nacos-server
    ports:
      - "8848:8848"
  
  user-service:
    image: user-service:latest
    environment:
      - NACOS_ADDR=nacos:8848
    depends_on:
      - nacos
    deploy:
      replicas: 3
  
  inventory-service:
    image: inventory-service:latest
    environment:
      - NACOS_ADDR=nacos:8848
    depends_on:
      - nacos
    deploy:
      replicas: 2
  
  order-service:
    image: order-service:latest
    environment:
      - NACOS_ADDR=nacos:8848
    depends_on:
      - nacos
    deploy:
      replicas: 5
```

### 8.2 Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 5
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
      - name: order-service
        image: order-service:latest
        env:
        - name: NACOS_ADDR
          value: "nacos:8848"
```

---

## 9. 优势

| 维度 | 说明 |
|------|------|
| ✅ 独立部署 | 每个服务独立项目，独立部署 |
| ✅ 技术栈自由 | 可以用不同语言实现不同服务 |
| ✅ 团队协作 | 不同团队负责不同服务 |
| ✅ 故障隔离 | 单个服务挂了不影响其他服务 |
| ✅ 独立扩展 | 热点服务可以多部署实例 |
| ✅ 成熟方案 | Nacos/Consul 久经考验 |

---

## 10. 总结

**架构特点**：
1. **每个 Service 是独立项目** → 默认微服务
2. **通过 RPC + 服务发现通信** → 成熟方案
3. **接口定义在 common 模块** → 统一契约
4. **动态代理实现 RPC 调用** → 业务代码简洁

**适用场景**：
- ✅ 中大型团队
- ✅ 需要独立部署和扩展
- ✅ 技术栈可能异构

这就是标准的微服务架构，简单、成熟、可靠。
