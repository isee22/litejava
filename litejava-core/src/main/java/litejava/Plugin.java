package litejava;

/**
 * 插件基类 - LiteJava 唯一的扩展机制
 * 
 * <p>LiteJava 采用纯插件架构，所有功能都通过插件提供：
 * <ul>
 *   <li>服务器插件：HttpServerPlugin, NettyServerPlugin, JettyServerPlugin</li>
 *   <li>配置插件：ConfPlugin, YamlConfPlugin</li>
 *   <li>JSON 插件：JsonPlugin, JacksonPlugin, GsonPlugin</li>
 *   <li>中间件插件：CorsPlugin, LogPlugin, RecoveryPlugin</li>
 *   <li>数据库插件：DatabasePlugin, MyBatisPlugin</li>
 *   <li>缓存插件：MemoryCachePlugin, RedisCachePlugin</li>
 *   <li>视图插件：ThymeleafPlugin, FreemarkerPlugin</li>
 * </ul>
 * 
 * <h2>插件生命周期</h2>
 * <ol>
 *   <li>创建插件实例</li>
 *   <li>调用 app.use(plugin) 注册</li>
 *   <li>框架调用 plugin.config() 初始化</li>
 *   <li>应用运行期间插件提供服务</li>
 *   <li>应用停止时调用 plugin.uninstall() 清理</li>
 * </ol>
 * 
 * <h2>自定义插件示例</h2>
 * <pre>{@code
 * public class MyPlugin extends Plugin {
 *     
 *     // 使用 public 字段（LiteJava 风格）
 *     public int timeout = 30;
 *     public String endpoint = "http://localhost";
 *     
 *     @Override
 *     public void config() {
 *         // 从配置文件读取参数
 *         timeout = app.conf.getInt("my", "timeout", timeout);
 *         endpoint = app.conf.getString("my", "endpoint", endpoint);
 *         
 *         // 初始化资源
 *         initConnection();
 *     }
 *     
 *     @Override
 *     public void uninstall() {
 *         // 清理资源
 *         closeConnection();
 *     }
 *     
 *     // 插件提供的功能
 *     public void doSomething() {
 *         // ...
 *     }
 * }
 * 
 * // 使用插件
 * MyPlugin my = new MyPlugin();
 * my.timeout = 60;  // 可选：代码配置
 * app.use(my);
 * 
 * // 在 handler 中使用
 * app.get("/test", ctx -> {
 *     MyPlugin plugin = ctx.plugin(MyPlugin.class);
 *     plugin.doSomething();
 * });
 * }</pre>
 * 
 * <h2>插件类型</h2>
 * <table>
 *   <tr><th>类型</th><th>基类</th><th>说明</th></tr>
 *   <tr><td>普通插件</td><td>Plugin</td><td>通用扩展</td></tr>
 *   <tr><td>中间件</td><td>MiddlewarePlugin</td><td>请求拦截</td></tr>
 *   <tr><td>服务器</td><td>ServerPlugin</td><td>HTTP 服务器</td></tr>
 *   <tr><td>配置</td><td>ConfPlugin</td><td>配置读取</td></tr>
 *   <tr><td>JSON</td><td>JsonPlugin</td><td>序列化</td></tr>
 *   <tr><td>视图</td><td>ViewPlugin</td><td>模板渲染</td></tr>
 * </table>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see App#use(Plugin) 注册插件
 * @see MiddlewarePlugin 中间件插件
 */
public class Plugin {
    
    /** 应用实例引用，在 config() 调用前由框架设置 */
    public App app;
    
    /**
     * 配置插件（子类重写）
     * 
     * <p>在此方法中：
     * <ul>
     *   <li>从 app.conf 读取配置</li>
     *   <li>初始化资源（连接池、客户端等）</li>
     *   <li>注册到 app 的对应字段</li>
     * </ul>
     */
    public void config() {}
    
    /**
     * 卸载插件（可选重写）
     * 
     * <p>在此方法中清理资源：
     * <ul>
     *   <li>关闭连接</li>
     *   <li>释放线程池</li>
     *   <li>清理临时文件</li>
     * </ul>
     */
    public void uninstall() {}
}
