package litejava.plugins;

import litejava.MiddlewarePlugin;
import litejava.Plugin;

/**
 * 调试插件 - 启动时打印应用结构
 * 
 * <h2>功能</h2>
 * <ul>
 *   <li>打印所有已注册的插件</li>
 *   <li>按执行顺序打印中间件链</li>
 * </ul>
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * # application.yml
 * app:
 *   name: My Application    # 应用名称
 * 
 * plugins:
 *   debug: true             # 启用调试信息
 * }</pre>
 * 
 * <h2>输出示例</h2>
 * <pre>
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                      My Application                          ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║ Plugins (6):                                                 ║
 * ║   1. Slf4jLogPlugin                                          ║
 * ║   2. YamlConfPlugin                                          ║
 * ║   3. JacksonPlugin                                           ║
 * ║   4. MemoryCachePlugin                                       ║
 * ║   5. HttpServerPlugin                                        ║
 * ║   6. MyBatisPlugin                                           ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║ Middleware Chain (2):                                        ║
 * ║   Request → [RecoveryPlugin] → [CorsPlugin] → Handler        ║
 * ╚══════════════════════════════════════════════════════════════╝
 * </pre>
 */
public class DebugPlugin extends Plugin {
    
    @Override
    public void config() {
        // 延迟到 run() 时打印，确保所有插件都已注册
        app.onReady(this::printStructure);
    }
    
    private void printStructure() {
        // 从配置读取应用名称
        String appName = app.conf.getString("app", "name", "LiteJava App Structure");
        
        StringBuilder sb = new StringBuilder();
        String line = "══════════════════════════════════════════════════════════════";
        
        sb.append("\n╔").append(line).append("╗\n");
        sb.append("║").append(center(appName, 62)).append("║\n");
        sb.append("╠").append(line).append("╣\n");
        
        // 打印插件列表
        sb.append("║ Plugins (").append(app.plugins.size()).append("):").append(pad(50 - String.valueOf(app.plugins.size()).length())).append("║\n");
        int i = 1;
        for (String name : app.plugins.keySet()) {
            String entry = "  " + i + ". " + name;
            sb.append("║").append(padRight(entry, 62)).append("║\n");
            i++;
        }
        
        sb.append("╠").append(line).append("╣\n");
        
        // 打印中间件链
        sb.append("║ Middleware Chain (").append(app.middlewares.size()).append("):").append(pad(44 - String.valueOf(app.middlewares.size()).length())).append("║\n");
        
        if (app.middlewares.isEmpty()) {
            sb.append("║").append(padRight("  (none)", 62)).append("║\n");
        } else {
            StringBuilder chain = new StringBuilder("  Request");
            for (MiddlewarePlugin mw : app.middlewares) {
                chain.append(" → [").append(mw.getClass().getSimpleName()).append("]");
            }
            chain.append(" → Handler");
            
            // 分行显示长链
            String chainStr = chain.toString();
            while (chainStr.length() > 60) {
                int breakPoint = chainStr.lastIndexOf(" → ", 60);
                if (breakPoint <= 0) breakPoint = 60;
                sb.append("║").append(padRight(chainStr.substring(0, breakPoint), 62)).append("║\n");
                chainStr = "         " + chainStr.substring(breakPoint);
            }
            sb.append("║").append(padRight(chainStr, 62)).append("║\n");
        }
        
        sb.append("╚").append(line).append("╝");
        
        // 直接输出到控制台，不依赖日志框架
        System.out.println(sb.toString());
    }
    
    private String center(String s, int width) {
        if (s.length() >= width) return s;
        int leftPad = (width - s.length()) / 2;
        return pad(leftPad) + s + pad(width - s.length() - leftPad);
    }
    
    private String padRight(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return s + pad(width - s.length());
    }
    
    private String pad(int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(' ');
        return sb.toString();
    }
}
