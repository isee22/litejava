package litejava.plugins;

import litejava.App;
import litejava.plugin.HttpServerPlugin;
import litejava.plugins.cache.MemoryCachePlugin;
import litejava.plugins.config.YamlConfPlugin;
import litejava.plugins.json.JacksonPlugin;
import litejava.plugins.log.Slf4jLogPlugin;

/**
 * LiteJava 快速启动工厂 - 预装常用插件，开箱即用
 * 
 * <h2>Quick Start</h2>
 * 
 * <pre>{@code
 * App app = LiteJava.create();
 * app.get("/", ctx -> ctx.json(Map.of("msg", "Hello")));
 * app.run();
 * }</pre>
 * 
 * <h2>预装插件</h2>
 * <ul>
 *   <li>Slf4jLogPlugin - SLF4J 日志</li>
 *   <li>YamlConfPlugin - YAML 配置 (application.yml)</li>
 *   <li>JacksonPlugin - JSON 序列化</li>
 *   <li>MemoryCachePlugin - 内存缓存</li>
 *   <li>AutoConfigPlugin - 自动配置（根据配置文件启用插件）</li>
 *   <li>HttpServerPlugin - HTTP 服务器</li>
 * </ul>
 * 
 * <h2>通过配置文件启用插件</h2>
 * <pre>{@code
 * # application.yml
 * plugins:
 *   health: true      # 启用健康检查 /health
 *   cors: true        # 启用跨域支持
 *   recovery: true    # 启用异常恢复
 *   static: true      # 启用静态文件
 *   staticPath: /static
 *   staticDir: static
 * }</pre>
 * 
 * <h2>添加更多插件</h2>
 * <pre>{@code
 * App app = LiteJava.create();
 * app.use(new MyBatisPlugin());     // 数据库
 * app.use(new RedisCachePlugin());  // Redis
 * app.use(new ThymeleafPlugin());   // 模板引擎
 * app.run();
 * }</pre>
 */
public class LiteJava {

	/**
	 * 创建预装常用插件的 App
	 */
	public static App create() {
		App app = new App();
		app.use(new Slf4jLogPlugin());
		app.use(new YamlConfPlugin());
		app.use(new JacksonPlugin());
		app.use(new MemoryCachePlugin());
		app.use(new AutoConfigPlugin());
		app.use(new HttpServerPlugin());
		return app;
	}

	/**
	 * 创建预装常用插件的 App (指定配置文件)
	 */
	public static App create(String configFile) {
		App app = new App();
		app.use(new Slf4jLogPlugin());
		app.use(new YamlConfPlugin(configFile));
		app.use(new JacksonPlugin());
		app.use(new MemoryCachePlugin());
		app.use(new AutoConfigPlugin());
		app.use(new HttpServerPlugin());
		return app;
	}

	/**
	 * 创建预装常用插件的 App (多环境配置)
	 * 
	 * @param configFile 配置文件
	 * @param env        环境名 (dev/test/prod)
	 */
	public static App create(String configFile, String env) {
		App app = new App();
		app.use(new Slf4jLogPlugin());
		app.use(new YamlConfPlugin(configFile, env));
		app.use(new JacksonPlugin());
		app.use(new MemoryCachePlugin());
		app.use(new AutoConfigPlugin());
		app.use(new HttpServerPlugin());
		return app;
	}
}
