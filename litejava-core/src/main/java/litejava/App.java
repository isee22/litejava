package litejava;

import litejava.exception.*;
import litejava.plugin.*;

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
 * </ul>
 * 
 * <h2>设计理念</h2>
 * <p>借鉴 Go/Gin 的设计哲学：
 * <ul>
 *   <li>简洁直接的 API</li>
 *   <li>Gin-style 路由（分组、参数、通配符）</li>
 *   <li>Koa-style 洋葱中间件模型</li>
 *   <li>核心模块零外部依赖</li>
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
 * <h2>路由分组（Gin-style）</h2>
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
 *     admin.use(new AuthMiddleware());  // 分组级中间件
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
 * <h2>中间件（Koa-style 洋葱模型）</h2>
 * <pre>{@code
 * // 全局中间件
 * app.use(new LogMiddleware());     // 请求日志
 * app.use(new CorsMiddleware());    // 跨域处理
 * app.use(new RecoveryMiddleware()); // 异常恢复
 * 
 * // 自定义中间件
 * public class AuthMiddleware extends MiddlewarePlugin {
 *     @Override
 *     public void handle(Context ctx, Next next) throws Exception {
 *         String token = ctx.header("Authorization");
 *         if (token == null) {
 *             ctx.abortWithJson(401, Map.of("error", "Unauthorized"));
 *             return;
 *         }
 *         ctx.state.put("user", validateToken(token));
 *         next.run();  // 继续执行后续中间件和 handler
 *     }
 * }
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
    
    /** 服务器端口，默认 8080，可通过配置文件 server.port 覆盖 */
    public int port = 8080;
    
    /** 开发模式，true 时显示详细错误信息和堆栈 */
    public boolean devMode = true;
    
    /** 运行环境：dev/test/prod */
    public String env = "dev";
    
    // ==================== 核心组件 ====================
    
    /** 路由插件，管理所有路由规则 */
    public RouterPlugin router = new RouterPlugin();
    
    /** 中间件列表，按注册顺序执行（洋葱模型） */
    public List<MiddlewarePlugin> middlewares = new ArrayList<>();
    
    /** 已注册的插件 Map，key 为插件类名 */
    public final Map<String, Object> plugins = new LinkedHashMap<>();
    
    /** 自定义错误处理器 */
    public BiConsumer<Context, Throwable> errorHandler;
    
    // ==================== 内置插件引用 ====================
    
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
    
    // ==================== 内部状态 ====================
    
    /** 优雅停机标志 */
    private volatile boolean stopping = false;
    
    /** 插件列表（保持注册顺序） */
    private final List<Plugin> pluginList = new ArrayList<>();
    
    /** 启动前回调列表 */
    private final List<Runnable> onReadyCallbacks = new ArrayList<>();
    
    // ==================== 链式配置 API ====================
    
    /**
     * 设置服务器端口
     * @param port 端口号
     * @return this（支持链式调用）
     */
    public App port(int port) {
        this.port = port;
        return this;
    }
    
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
        plugin.app = this;
        plugin.config();
        plugins.put(plugin.getClass().getSimpleName(), plugin);
        
        // 自动注册到对应字段
        if (plugin instanceof ConfPlugin) {
            this.conf = (ConfPlugin) plugin;
        } else if (plugin instanceof LogPlugin) {
            this.log = (LogPlugin) plugin;
        } else if (plugin instanceof JsonPlugin) {
            this.json = (JsonPlugin) plugin;
        } else if (plugin instanceof ViewPlugin) {
            this.view = (ViewPlugin) plugin;
        } else if (plugin instanceof ServerPlugin) {
            this.server = (ServerPlugin) plugin;
        } else if (plugin instanceof RouterPlugin) {
            this.router = (RouterPlugin) plugin;
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
                // ignore
            }
            pluginList.remove(plugin);
            
            // 从中间件列表移除
            if (plugin instanceof MiddlewarePlugin) {
                middlewares.remove(plugin);
            }
        }
        return this;
    }
    
    // ==================== 路由注册（Gin-style）====================
    
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
     * 设置 404 处理器（Gin-style NoRoute）
     */
    public App noRoute(Handler handler) {
        router.noRoute(handler);
        return this;
    }
    
    /**
     * 设置 405 处理器（Gin-style NoMethod）
     */
    public App noMethod(Handler handler) {
        router.noMethod(handler);
        return this;
    }
    
    /**
     * 注册异步 GET 路由（返回 CompletableFuture）
     */
    public Route getAsync(String path, AsyncHandler handler) {
        return get(path, ctx -> handler.handle(ctx).join());
    }
    
    /**
     * 注册异步 POST 路由
     */
    public Route postAsync(String path, AsyncHandler handler) {
        return post(path, ctx -> handler.handle(ctx).join());
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
     * 设置全局错误处理器
     * 
     * <pre>{@code
     * app.onError((ctx, e) -> {
     *     log.error("Request failed", e);
     *     ctx.status(500).json(Map.of("error", e.getMessage()));
     * });
     * }</pre>
     */
    public App onError(BiConsumer<Context, Throwable> handler) {
        this.errorHandler = handler;
        return this;
    }
    
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
    
    // ==================== 生命周期 ====================
    
    /**
     * 启动服务器（使用配置文件中的端口）
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
        run(conf.getInt("server", "port", port));
    }
    
    /**
     * 启动服务器（指定端口）
     * @param port 服务器端口
     */
    public void run(int port) {
        // 从配置加载 devMode 和 charset
        devMode = conf.getBool("server", "devMode", devMode);
        String charset = conf.getString("server", "charset", null);
        if (charset != null) {
            Context.setCharset(charset);
        }
        
        this.port = port;
        
        for (Plugin plugin : pluginList) {
            try {
                plugin.app = this;
                plugin.config();
                plugins.put(plugin.getClass().getSimpleName(), plugin);
            } catch (Exception e) {
                throw new LiteJavaException("Failed to install plugin: " + plugin.getClass().getSimpleName(), e);
            }
        }
        
        if (devMode) {
            router.printRoutes(s -> log.info(s));
        }
        
        if (server == null) {
            throw new LiteJavaException("No server plugin registered. Use app.use(serverPlugin) first.");
        }
        
        // 冻结中间件列表，避免运行时修改
        middlewares = Collections.unmodifiableList(new ArrayList<>(middlewares));
        
        // 执行 onReady 回调（在服务器启动前）
        for (Runnable callback : onReadyCallbacks) {
            callback.run();
        }
        
        server.start();
        
        String host = conf.getString("server", "host", "0.0.0.0");
        String displayHost = "0.0.0.0".equals(host) ? "localhost" : host;
        log.info("Server started on port " + port + " [" + env + " mode]");
        log.info("  -> http://" + displayHost + ":" + port + "/");
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
        for (int i = pluginList.size() - 1; i >= 0; i--) {
            try {
                pluginList.get(i).uninstall();
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
    
    // ==================== 请求处理 ====================
    
    // 预创建的 404/405 handler，避免每次请求创建 lambda
    private final Handler default404Handler = c -> c.status(404).json(Map.of("error", "Not Found", "path", c.path));
    private final Handler default405Handler = c -> {
        c.status(405);
        c.header("Allow", String.join(", ", router.getAllowedMethods(c.path)));
        c.json(Map.of("error", "Method Not Allowed", "method", c.method, "path", c.path));
    };
    
    /**
     * 处理 HTTP 请求（由服务器插件调用）
     * 
     * <p>处理流程：
     * <ol>
     *   <li>路由匹配</li>
     *   <li>提取路径参数</li>
     *   <li>执行中间件链（洋葱模型）</li>
     *   <li>执行 handler</li>
     * </ol>
     * 
     * @param ctx 请求上下文
     * @throws Exception 处理异常
     */
    public void handle(Context ctx) throws Exception {
        RouterPlugin.RouteMatch match = router.match(ctx.method, ctx.path);
        
        Handler finalHandler;
        if (match != null) {
            // 直接赋值，避免 putAll
            if (match.params != null && !match.params.isEmpty()) {
                ctx.params = match.params;
            }
            if (match.wildcardName != null) {
                ctx.wildcardPath = match.wildcardValue;
                ctx.params.put(match.wildcardName, match.wildcardValue);
            }
            finalHandler = match.handler;
        } else if (router.hasPath(ctx.path)) {
            finalHandler = router.noMethodHandler != null ? router.noMethodHandler : default405Handler;
        } else {
            finalHandler = router.noRouteHandler != null ? router.noRouteHandler : default404Handler;
        }
        
        // 无中间件时直接执行 handler，避免创建 MiddlewareChain
        if (middlewares.isEmpty()) {
            finalHandler.handle(ctx);
        } else {
            new MiddlewareChain(middlewares, finalHandler).execute(ctx);
        }
    }
    
    /**
     * 处理请求异常
     * 
     * <p>开发模式下返回详细错误信息和堆栈，生产模式只返回通用错误。
     * 
     * @param ctx 请求上下文
     * @param e 异常
     */
    public void handleError(Context ctx, Exception e) {
        if (errorHandler != null) {
            try {
                errorHandler.accept(ctx, e);
                return;
            } catch (Exception handlerError) {
                e = handlerError;
            }
        }
        
        int status = 500;
        if (e instanceof LiteJavaException) {
            status = ((LiteJavaException) e).statusCode;
        }
        
        ctx.status(status);
        
        if (devMode) {
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("type", e.getClass().getName());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            errorResponse.put("stack", sw.toString());
            ctx.json(errorResponse);
        } else {
            ctx.json(Map.of("error", "Internal Server Error"));
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T plugin(Class<T> clazz) {
        return (T) plugins.get(clazz.getSimpleName());
    }
}
