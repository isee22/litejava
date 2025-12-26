package litejava.plugins.log;

import litejava.plugin.LogPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SLF4J 日志插件 - 使用 SLF4J 输出日志
 * 
 * <h2>重要：需要手动添加依赖</h2>
 * <p>使用此插件需要在项目 pom.xml 中添加 SLF4J 实现（二选一）：
 * 
 * <h3>方案一：Logback（推荐）</h3>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.slf4j</groupId>
 *     <artifactId>slf4j-api</artifactId>
 *     <version>2.0.9</version>
 * </dependency>
 * <dependency>
 *     <groupId>ch.qos.logback</groupId>
 *     <artifactId>logback-classic</artifactId>
 *     <version>1.4.11</version>
 * </dependency>
 * }</pre>
 * 
 * <h3>方案二：SLF4J Simple（简单场景）</h3>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.slf4j</groupId>
 *     <artifactId>slf4j-api</artifactId>
 *     <version>2.0.9</version>
 * </dependency>
 * <dependency>
 *     <groupId>org.slf4j</groupId>
 *     <artifactId>slf4j-simple</artifactId>
 *     <version>2.0.9</version>
 * </dependency>
 * }</pre>
 * 
 * <p><b>注意：</b>必须显式声明 slf4j-api:2.0.9 以避免版本冲突。
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // LiteJava.create() 默认使用此插件
 * App app = LiteJava.create();
 * 
 * // 在代码中使用
 * app.log.info("Server started");
 * app.log.error("Error occurred", exception);
 * }</pre>
 * 
 * <h2>如果不想使用 SLF4J</h2>
 * <p>可以使用内置的 LogPlugin（输出到 System.out）：
 * <pre>{@code
 * App app = new App();
 * // 不调用 LiteJava.create()，手动配置插件
 * app.use(new YamlConfPlugin());
 * app.use(new JacksonPlugin());
 * // app.log 默认就是 LogPlugin，无需额外配置
 * app.use(new HttpServerPlugin());
 * app.run();
 * }</pre>
 * 
 * @see litejava.plugin.LogPlugin 内置日志插件（无外部依赖）
 */
public class Slf4jLogPlugin extends LogPlugin {
    
    private static final Logger logger = LoggerFactory.getLogger("litejava");
    
    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }
    
    @Override
    public void info(String msg) {
        logger.info(msg);
    }
    
    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }
    
    @Override
    public void error(String msg) {
        logger.error(msg);
    }
    
    @Override
    public void error(String msg, Throwable e) {
        logger.error(msg, e);
    }
}
