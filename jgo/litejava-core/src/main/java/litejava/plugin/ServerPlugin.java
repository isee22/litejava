package litejava.plugin;

import litejava.Plugin;

/**
 * 服务器插件基类 - 所有 HTTP 服务器插件的父类
 * 
 * <p>提供通用的服务器配置和生命周期管理，子类需要实现：
 * <ul>
 *   <li>{@link #start()} - 启动服务器</li>
 *   <li>{@link #stop()} - 停止服务器</li>
 * </ul>
 * 
 * <h2>通用配置项</h2>
 * <pre>
 * # application.properties 或 application.yml
 * server.port=8080              # 服务器端口
 * server.host=0.0.0.0           # 绑定地址（0.0.0.0 表示所有网卡）
 * server.threads.min=10         # 最小线程数
 * server.threads.max=200        # 最大线程数
 * server.threads.idle=60        # 空闲线程超时（秒）
 * server.backlog=1024           # TCP 连接队列大小
 * server.maxRequestSize=10485760  # 最大请求体大小（字节，默认 10MB）
 * </pre>
 * 
 * <h2>内置服务器实现</h2>
 * <table>
 *   <tr><th>插件</th><th>依赖</th><th>特点</th></tr>
 *   <tr><td>HttpServerPlugin</td><td>无</td><td>JDK 内置，零依赖</td></tr>
 *   <tr><td>JdkHttpServerVTPlugin</td><td>JDK 21+</td><td>虚拟线程，高并发</td></tr>
 *   <tr><td>NettyServerPlugin</td><td>Netty</td><td>异步非阻塞，高性能</td></tr>
 *   <tr><td>JettyServerPlugin</td><td>Jetty</td><td>成熟稳定，功能丰富</td></tr>
 *   <tr><td>UndertowServerPlugin</td><td>Undertow</td><td>轻量高性能</td></tr>
 * </table>
 * 
 * <h2>自定义服务器插件</h2>
 * <pre>{@code
 * public class MyServerPlugin extends ServerPlugin {
 *     private MyServer server;
 *     
 *     @Override
 *     public void start() {
 *         server = new MyServer(host, app.port);
 *         server.setHandler(exchange -> {
 *             Context ctx = new Context();
 *             ctx.app = app;
 *             // 解析请求...
 *             app.handle(ctx);
 *             // 发送响应...
 *         });
 *         server.start();
 *     }
 *     
 *     @Override
 *     public void stop() {
 *         if (server != null) {
 *             server.stop();
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see HttpServerPlugin JDK 内置服务器
 */
public class ServerPlugin extends Plugin {
    
    // ==================== 通用服务器配置 ====================
    
    /** 绑定地址，默认 0.0.0.0（所有网卡） */
    public String host = "0.0.0.0";
    
    /** 最小线程数 */
    public int minThreads = 10;
    
    /** 最大线程数 */
    public int maxThreads = 200;
    
    /** 空闲线程超时（秒） */
    public int idleTimeout = 60;
    
    /** TCP 连接队列大小 */
    public int backlog = 1024;
    
    /** 最大请求体大小（字节），默认 10MB */
    public int maxRequestSize = 10 * 1024 * 1024;
    
    @Override
    public void config() {
        host = app.conf.getString("server", "host", host);
        minThreads = app.conf.getInt("server.threads", "min", minThreads);
        maxThreads = app.conf.getInt("server.threads", "max", maxThreads);
        idleTimeout = app.conf.getInt("server.threads", "idle", idleTimeout);
        backlog = app.conf.getInt("server", "backlog", backlog);
        maxRequestSize = app.conf.getInt("server", "maxRequestSize", maxRequestSize);
    }
    
    /**
     * 启动服务器（子类实现）
     */
    public void start() {}
    
    /**
     * 停止服务器（子类实现）
     */
    public void stop() {}
    
    @Override
    public void uninstall() {
        stop();
    }
}
