package litejava;

import litejava.exception.*;
import litejava.plugin.*;
import litejava.plugin.CachePlugin;
import litejava.util.Maps;

import java.io.*;
import java.util.*;
import java.util.function.*;

/**
 * LiteJava 应用容器 - 纯插件架构的轻量级 Web 框架核心
 * 
 * <p>App 是 LiteJava 的核心类，采用纯插件架构设计：
 * <ul>
 *   <li>所有功能通过插件提供（服务器、JSON、配置、日志等）</li>
 *   <li>零魔法 - 所见即所得，无隐藏规则</li>
 *   <li>显式优于隐式 - 所有配置通过代码完成</li>
 *   <li>性能比肩 Go 语言框架</li>
 * </ul>
 * 
 * <h2>设计理念</h2>
 * <ul>
 *   <li>简洁直接的 API</li>
 *   <li>灵活的路由（分组、参数、通配符）</li>
 *   <li>Koa-style 洋葱中间件模型</li>
 *   <li>核心模块零外部依赖</li>
 *   <li>插件自由装配，无强制依赖</li>
 * </ul>
 * 
 * <h2>快速开始</h2>
 * <pre>{@code
 * // 方式一：使用 LiteJava.create() 快速启动（推荐）
 * // 预装插件：YamlConf + Jackson + MemoryCache + HttpServer
 * App app = LiteJava.create();
 * app.get("/", ctx -> ctx.json(Map.of("msg", "Hello")));
 * app.run();
 * 
 * // 方式二：手动配置（完全控制）
 * App app = new App();
 * app.use(new YamlConfPlugin());    // 配置插件
 * app.use(new JacksonPlugin());     // JSON 插件
 * app.use(new HttpServerPlugin());  // 服务器插件
 * app.get("/", ctx -> ctx.text("Hello"));
 * app.run();
 * }</pre>
 * 
 * <h2>RESTful API 示例</h2>
 * <pre>{@code
 * App app = LiteJava.create();
 * 
 * // 标准 CRUD 路由
 * app.get("/users", ctx -> ctx.json(userService.list()));           // 列表
 * app.get("/users/:id", ctx -> ctx.json(userService.get(ctx.paramLong("id"))));  // 详情
 * app.post("/users", ctx -> ctx.json(userService.create(ctx.bindJSON())));       // 创建
 * app.put("/users/:id", ctx -> ctx.json(userService.update(ctx.paramLong("id"), ctx.bindJSON())));  // 更新
 * app.delete("/users/:id", ctx -> ctx.ok(userService.delete(ctx.paramLong("id"))));  // 删除
 * 
 * app.run();
 * }</pre>
 * 
 * <h2>路由分组</h2>
 * <pre>{@code
 * // API 版本分组
 * app.group("/api/v1", api -> {
 *     api.get("/books", bookHandler::list);      // GET /api/v1/books
 *     api.post("/books", bookHandler::create);   // POST /api/v1/books
 *     api.get("/books/:id", bookHandler::get);   // GET /api/v1/books/:id
 * });
 * 
 * // 嵌套分组
 * app.group("/admin", admin -> {
 *     admin.use(new AuthPlugin(token -> jwtPlugin.verify(token)));  // 分组级中间件
 *     admin.group("/users", users -> {
 *         users.get("/", UserController::list);
 *         users.delete("/:id", UserController::delete);
 *     });
 * });
 * }</pre>
 * 
 * <h2>路径参数</h2>
 * <pre>{@code
 * // :name 匹配单个路径段（不含 /）
 * app.get("/users/:id", ctx -> {
 *     long id = ctx.paramLong("id");  // 自动类型转换
 *     ctx.json(userService.get(id));
 * });
 * 
 * // 多个参数
 * app.get("/users/:userId/posts/:postId", ctx -> {
 *     long userId = ctx.paramLong("userId");
 *     long postId = ctx.paramLong("postId");
 *     ctx.json(postService.get(userId, postId));
 * });
 * 
 * // *name 通配符匹配剩余路径（含 /）
 * app.get("/files/*filepath", ctx -> {
 *     String path = ctx.wildcardPath;  // 例如 "images/logo.png"
 *     ctx.file(new File(uploadDir, path));
 * });
 * }</pre>
 * 
 * <h2>中间件（洋葱模型）</h2>
 * <pre>{@code
 * // 全局中间件
 * app.use(new RequestLogPlugin());  // 请求日志
 * app.use(new CorsPlugin());        // 跨域处理
 * 
 * // 自定义异常处理（内置 ExceptionPlugin 默认处理）
 * app.exception.handler = (ctx, e) -> {
 *     ctx.status(500).json(Maps.of("error", e.getMessage()));
 * };
 * 
 * // 认证中间件（推荐使用内置 AuthPlugin）
 * app.use(new AuthPlugin(token -> jwtPlugin.verify(token))
 *     .whitelist("/", "/login")
 *     .whitelistPrefix("/static"));
 * }</pre>
 * 
 * <h2>自定义扩展</h2>
 * <p>通过替换内置插件实现自定义行为：
 * <pre>{@code
 * // 自定义异常处理
 * app.exception.handler = (ctx, e) -> {
 *     ctx.status(500).json(Maps.of("error", e.getMessage()));
 * };
 * 
 * // 或继承 ExceptionPlugin
 * public class MyExceptionPlugin extends ExceptionPlugin {
 *     @Override
 *     public void handleError(Context ctx, Exception e) {
 *         log.error("Request failed: " + ctx.path, e);
 *         ctx.status(500).json(Maps.of("code", -1, "msg", e.getMessage()));
 *     }
 * }
 * app.use(new MyExceptionPlugin());
 * 
 * // 自定义请求处理流程
 * public class MyHandlerPlugin extends HandlerPlugin {
 *     @Override
 *     public void handle(Context ctx) throws Exception {
 *         long start = System.currentTimeMillis();
 *         try {
 *             super.handle(ctx);
 *         } finally {
 *             System.out.println(ctx.method + " " + ctx.path + " " + (System.currentTimeMillis() - start) + "ms");
 *         }
 *     }
 * }
 * app.use(new MyHandlerPlugin());
 * }</pre>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see Context 请求上下文
 * @see Router 路由器
 * @see Plugin 插件基类
 * @see MiddlewarePlugin 中间件插件
 */
public class App {
    
    // ==================== 应用配置 ====================
    
    /** 开发模式，true 时显示详细错误信息和堆栈 */
    public boolean devMode = true;
    
    /** 运行环境：dev/test/prod */
    public String env = "dev";
    
    // ==================== 核心组件 ====================
    
    /** 路由插件，管理所有路由规则 */
    public RouterPlugin router = new RouterPlugin();
    
    /** 中间件列表，按注册顺序执行（洋葱模型） */
    public List<MiddlewarePlugin> middlewares = new ArrayList<>();
    
    /** 已注册的插件 Map，key 为插件名称（默认类名） */
    public final Map<String, Plugin> plugins = new LinkedHashMap<>();
    
    /** 内置异常处理插件（单例，始终在中间件最前面） */
    public ExceptionPlugin exception = new ExceptionPlugin();
    
    /** 内置请求处理插件 */
    public HandlerPlugin handler = new HandlerPlugin();
    
    // ==================== 内置插件引用（快捷访问） ====================
    
    /** 服务器插件（必须注册一个） */
    public ServerPlugin server;
    
    /** 配置插件，默认空配置，可替换为 YamlConfPlugin */
    public ConfPlugin conf = new ConfPlugin();
    
    /** JSON 插件，用于序列化/反序列化 */
    public JsonPlugin json;
    
    /** 日志插件 */
    public LogPlugin log = new LogPlugin();
    
    /** 视图/模板插件 */
    public ViewPlugin view;
    
    /** 文件处理插件 */
    public FilePlugin file = new FilePlugin();
    
    /** 缓存插件 */
    public CachePlugin cache;
    
    /** 响应格式插件，默认标准三件套 */
    public ResultPlugin result = new ResultPlugin();
    
    // ==================== 内部状态 ====================
    
    /** 优雅停机标志 */
    private volatile boolean stopping = false;
    
    /** 启动前回调列表 */
    private final List<Runnable> onReadyCallbacks = new ArrayList<>();
    
    /** 启动后回调列表 */
    private final List<Runnable> onStartedCallbacks = new ArrayList<>();
    
    // ==================== 构造函数 ====================
    
    public App() {
    }
    
    // ==================== 链式配置 API ====================
    
    /**
     * 设置开发模式
     * @param devMode true 显示详细错误，false 隐藏敏感信息
     * @return this
     */
    public App devMode(boolean devMode) {
        this.devMode = devMode;
        return this;
    }
    
    /**
     * 设置运行环境
     * @param env 环境名：dev/test/prod
     * @return this
     */
    public App env(String env) {
        this.env = env;
        this.devMode = "dev".equals(env);
        return this;
    }
    
    // ==================== 插件注册 ====================
    
    /**
     * 注册插件
     * 
     * <p>插件会自动识别类型并注册到对应字段：
     * <ul>
     *   <li>ConfPlugin → app.conf</li>
     *   <li>LogPlugin → app.log</li>
     *   <li>JsonPlugin → app.json</li>
     *   <li>ViewPlugin → app.view</li>
     *   <li>ServerPlugin → app.server</li>
     *   <li>MiddlewarePlugin → app.middlewares</li>
     * </ul>
     * 
     * @param plugin 插件实例
     * @return this
     */
    public App use(Plugin plugin) {
        return use(plugin.getClass().getSimpleName(), plugin);
    }
    
    /**
     * 注册命名插件（多实例场景）
     * 
     * <pre>{@code
     * // 多数据源
     * app.use("primary", new HikariPlugin("datasource.primary"));
     * app.use("secondary", new HikariPlugin("datasource.secondary"));
     * 
     * // 获取
     * HikariPlugin primary = app.getPlugin("primary", HikariPlugin.class);
     * HikariPlugin secondary = app.getPlugin("secondary", HikariPlugin.class);
     * }</pre>
     * 
     * @param name 插件名称
     * @param plugin 插件实例
     * @return this
     */
    public App use(String name, Plugin plugin) {
        plugin.app = this;
        
        // 单例插件：移除同类型（包括子类）的旧插件
        if (plugin.singleton()) {
            Class<?> pluginClass = plugin.getClass();
            // 找到最顶层的单例父类
            while (pluginClass.getSuperclass() != null && 
                   pluginClass.getSuperclass() != Plugin.class &&
                   pluginClass.getSuperclass() != MiddlewarePlugin.class) {
                try {
                    Plugin parent = (Plugin) pluginClass.getSuperclass().getDeclaredConstructor().newInstance();
                    if (parent.singleton()) {
                        pluginClass = pluginClass.getSuperclass();
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    // 无法实例化父类，停止向上查找
                    break;
                }
            }
            
            final Class<?> baseClass = pluginClass;
            Iterator<Map.Entry<String, Plugin>> it = plugins.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Plugin> entry = it.next();
                Plugin existing = entry.getValue();
                if (baseClass.isInstance(existing) && existing != plugin) {
                    log.warn("Replacing " + existing.getClass().getSimpleName() + " with " + plugin.getClass().getSimpleName());
                    try {
                        existing.uninstall();
                    } catch (Exception e) {
                        log.warn("Failed to uninstall plugin: " + existing.getClass().getSimpleName() + " - " + e.getMessage());
                    }
                    it.remove();
                    if (existing instanceof MiddlewarePlugin) {
                        middlewares.remove(existing);
                    }
                }
            }
        } else {
            // 非单例：检查是否已有同类型插件（非命名注册时警告）
            if (name.equals(plugin.getClass().getSimpleName())) {
                Plugin existing = getPlugin(plugin.getClass());
                if (existing != null && existing != plugin) {
                    log.warn(plugin.getClass().getSimpleName() + ": Multiple instances detected. " +
                            "Use app.use(name, plugin) for multi-instance scenarios.");
                }
            }
        }
        
        plugin.config();
        plugins.put(name, plugin);
        
        // 自动注册到对应字段
        // 单例插件始终替换，非单例插件只在字段为 null 时设置
        if (plugin instanceof ConfPlugin) {
            this.conf = (ConfPlugin) plugin;
        } else if (plugin instanceof LogPlugin) {
            this.log = (LogPlugin) plugin;
        } else if (plugin instanceof JsonPlugin) {
            this.json = (JsonPlugin) plugin;
        } else if (plugin instanceof ViewPlugin) {
            this.view = (ViewPlugin) plugin;
        } else if (plugin instanceof FilePlugin) {
            this.file = (FilePlugin) plugin;
        } else if (plugin instanceof CachePlugin) {
            this.cache = (CachePlugin) plugin;
        } else if (plugin instanceof ResultPlugin) {
            this.result = (ResultPlugin) plugin;
        } else if (plugin instanceof ServerPlugin) {
            this.server = (ServerPlugin) plugin;
        } else if (plugin instanceof RouterPlugin) {
            this.router = (RouterPlugin) plugin;
        } else if (plugin instanceof ExceptionPlugin) {
            // 替换内置异常处理插件
            this.exception = (ExceptionPlugin) plugin;
        } else if (plugin instanceof HandlerPlugin) {
            // 替换内置请求处理插件
            this.handler = (HandlerPlugin) plugin;
        } else if (plugin instanceof MiddlewarePlugin) {
            middlewares.add((MiddlewarePlugin) plugin);
        }
        
        return this;
    }
    
    /**
     * 移除插件
     * 
     * <p>根据插件类移除已注册的插件，会调用插件的 uninstall() 方法。
     * 
     * @param pluginClass 插件类
     * @return this
     */
    public App unuse(Class<? extends Plugin> pluginClass) {
        String name = pluginClass.getSimpleName();
        Object removed = plugins.remove(name);
        if (removed instanceof Plugin) {
            Plugin plugin = (Plugin) removed;
            try {
                plugin.uninstall();
            } catch (Exception e) {
                log.warn("Failed to uninstall plugin: " + plugin.getClass().getSimpleName() + " - " + e.getMessage());
            }
            
            // 从中间件列表移除
            if (plugin instanceof MiddlewarePlugin) {
                middlewares.remove(plugin);
            }
        }
        return this;
    }
    
    // ==================== 路由注册 ====================
    
    /**
     * 注册 GET 路由
     * @param path 路径，支持 :param 和 *wildcard
     * @param handler 处理器
     * @return Route（支持链式添加文档元数据）
     */
    public Route get(String path, Handler handler) {
        return router.get(path, handler);
    }
    
    /**
     * 注册 POST 路由
     */
    public Route post(String path, Handler handler) {
        return router.post(path, handler);
    }
    
    /**
     * 注册 PUT 路由
     */
    public Route put(String path, Handler handler) {
        return router.put(path, handler);
    }
    
    /**
     * 注册 DELETE 路由
     */
    public Route delete(String path, Handler handler) {
        return router.delete(path, handler);
    }
    
    /**
     * 注册 PATCH 路由
     */
    public Route patch(String path, Handler handler) {
        return router.patch(path, handler);
    }
    
    /**
     * 注册 ANY 路由（匹配所有 HTTP 方法）
     */
    public Route any(String path, Handler handler) {
        return router.any(path, handler);
    }
    
    /**
     * 注册指定 HTTP 方法的路由
     */
    public Route route(String method, String path, Handler handler) {
        return router.route(method, path, handler);
    }
    
    /**
     * 创建路由分组（返回子路由器）
     * @param prefix 分组前缀
     * @return 子路由器
     */
    public RouterPlugin group(String prefix) {
        return router.group(prefix);
    }
    
    /**
     * 创建路由分组（Lambda 风格）
     * 
     * <pre>{@code
     * app.group("/api", api -> {
     *     api.get("/users", UserController::list);
     * });
     * }</pre>
     */
    public App group(String prefix, Consumer<RouterPlugin> configure) {
        router.group(prefix, configure);
        return this;
    }
    
    /**
     * 批量注册路由
     */
    public App register(Routes routes) {
        router.register(routes);
        return this;
    }
    
    // ==================== 错误处理 ====================
    
    /**
     * 注册启动前回调（在服务器启动前执行）
     * 
     * <pre>{@code
     * app.onReady(() -> {
     *     System.out.println("All plugins loaded!");
     * });
     * }</pre>
     */
    public App onReady(Runnable callback) {
        this.onReadyCallbacks.add(callback);
        return this;
    }
    
    /**
     * 注册启动后回调（在服务器启动后执行）
     * 
     * <pre>{@code
     * app.onStarted(() -> {
     *     System.out.println("Server is ready to accept requests!");
     *     healthCheck.verify();
     * });
     * }</pre>
     */
    public App onStarted(Runnable callback) {
        this.onStartedCallbacks.add(callback);
        return this;
    }
    
    // ==================== 生命周期 ====================
    
    /**
     * 启动服务器
     * 
     * <p>端口由 ServerPlugin 管理，可通过以下方式设置：
     * <ul>
     *   <li>配置文件 server.port</li>
     *   <li>构造函数 new HttpServerPlugin(9000)</li>
     *   <li>字段设置 serverPlugin.port = 9000</li>
     * </ul>
     * 
     * <p>启动流程：
     * <ol>
     *   <li>从配置加载 devMode 和 charset</li>
     *   <li>初始化所有插件</li>
     *   <li>打印路由表（开发模式）</li>
     *   <li>启动 HTTP 服务器</li>
     *   <li>注册 shutdown hook</li>
     * </ol>
     */
    public void run() {
        // 从配置加载
        devMode = conf.getBool("server", "devMode", devMode);
        String charset = conf.getString("server", "charset", null);
        if (charset != null) {
            Context.setCharset(charset);
        }
        
        if (devMode) {
            router.printRoutes(s -> log.info(s));
        }
        
        if (server == null) {
            throw new LiteJavaException("No server plugin registered. Use app.use(serverPlugin) first.");
        }
        
        // 确保内置 ExceptionPlugin 在中间件最前面
        exception.app = this;
        exception.config();
        List<MiddlewarePlugin> finalMiddlewares = new ArrayList<>();
        finalMiddlewares.add(exception);
        finalMiddlewares.addAll(middlewares);
        
        // 冻结中间件列表，避免运行时修改
        middlewares = Collections.unmodifiableList(finalMiddlewares);
        
        // 初始化 HandlerPlugin
        handler.app = this;
        handler.config();
        
        // 执行 onReady 回调（在服务器启动前）
        for (Runnable callback : onReadyCallbacks) {
            callback.run();
        }
        
        server.start();
        
        // 调用所有插件的 onStart()
        for (Plugin plugin : plugins.values()) {
            try {
                plugin.onStart();
            } catch (Exception e) {
                log.error("Plugin onStart failed: " + plugin.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
        
        // 执行 onStarted 回调（在服务器启动后）
        for (Runnable callback : onStartedCallbacks) {
            callback.run();
        }
        
        log.info("Server started on port " + server.port + " [" + env + " mode]");
        log.info("  -> http://" + ("0.0.0.0".equals(server.host) ? "localhost" : server.host) + ":" + server.port + "/");
        Runtime.getRuntime().addShutdownHook(new Thread(this::gracefulStop));
    }
    
    /**
     * 立即停止服务器
     */
    public void stop() {
        stopping = true;
        if (server != null) {
            server.stop();
        }
        // 逆序卸载插件
        List<Plugin> pluginValues = new ArrayList<>(plugins.values());
        for (int i = pluginValues.size() - 1; i >= 0; i--) {
            try {
                pluginValues.get(i).uninstall();
            } catch (Exception e) {
                System.err.println("Error uninstalling plugin: " + e.getMessage());
            }
        }
    }
    
    /**
     * 优雅停机（等待请求处理完成）
     */
    public void gracefulStop() {
        log.info("Shutting down gracefully...");
        stopping = true;
        try {
            Thread.sleep(500);  // 等待进行中的请求完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        stop();
        log.info("Server stopped");
    }
    
    /**
     * 检查是否正在停机
     */
    public boolean isStopping() {
        return stopping;
    }
    
    /**
     * 获取已注册的插件
     * 
     * <p>支持按具体类或父类/接口查找：
     * <pre>{@code
     * // 按具体类查找
     * JdbcPlugin jdbc = app.getPlugin(JdbcPlugin.class);
     * 
     * // 按父类查找（返回第一个匹配的子类实例）
     * DataSourcePlugin ds = app.getPlugin(DataSourcePlugin.class);  // 可能返回 HikariPlugin 或 DruidPlugin
     * CachePlugin cache = app.getPlugin(CachePlugin.class);         // 可能返回 MemoryCachePlugin 或 RedisCachePlugin
     * }</pre>
     * 
     * @param clazz 插件类（可以是具体类或父类/接口）
     * @return 插件实例，未注册返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getPlugin(Class<T> clazz) {
        // 先按类名精确查找
        Plugin plugin = plugins.get(clazz.getSimpleName());
        if (plugin != null && clazz.isInstance(plugin)) {
            return (T) plugin;
        }
        
        // 按父类/接口查找（返回第一个匹配的）
        for (Plugin p : plugins.values()) {
            if (clazz.isInstance(p)) {
                return (T) p;
            }
        }
        return null;
    }
    
    /**
     * 按名称获取插件（多实例场景）
     * 
     * <pre>{@code
     * // 多数据源
     * app.use("primary", new HikariPlugin("datasource.primary"));
     * app.use("secondary", new HikariPlugin("datasource.secondary"));
     * 
     * HikariPlugin primary = app.getPlugin("primary", HikariPlugin.class);
     * HikariPlugin secondary = app.getPlugin("secondary", HikariPlugin.class);
     * }</pre>
     * 
     * @param name 插件名称
     * @param clazz 插件类
     * @return 插件实例，未注册返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getPlugin(String name, Class<T> clazz) {
        Plugin plugin = plugins.get(name);
        if (plugin != null && clazz.isInstance(plugin)) {
            return (T) plugin;
        }
        return null;
    }
}
