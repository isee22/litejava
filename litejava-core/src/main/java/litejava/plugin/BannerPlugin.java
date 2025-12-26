package litejava.plugin;

import litejava.Plugin;

/**
 * Banner 插件 - 启动时显示 Logo
 * 
 * <pre>{@code
 * app.use(new BannerPlugin());
 * }</pre>
 */
public class BannerPlugin extends Plugin {
    
    public static final String DEFAULT_BANNER = 
        "\n" +
        "   __    _ __          __                   \n" +
        "  / /   (_) /____     / /___ __   ______ _  \n" +
        " / /   / / __/ _ \\   / / __ `/ | / / __ `/  \n" +
        "/ /___/ / /_/  __/  / / /_/ /| |/ / /_/ /   \n" +
        "/_____/_/\\__/\\___/_/ /\\__,_/ |___/\\__,_/    \n" +
        "               /___/                        \n";
    
    public String banner = DEFAULT_BANNER;
    public String version = "1.0.0";
    public boolean showVersion = true;
    
    public BannerPlugin() {}
    
    public BannerPlugin(String banner) {
        this.banner = banner;
    }
    
    public BannerPlugin(String banner, String version) {
        this.banner = banner;
        this.version = version;
    }
    
    @Override
    public void config() {
        // 从配置读取
        String customBanner = app.conf.getString("app", "banner", null);
        if (customBanner != null && !customBanner.isEmpty()) {
            this.banner = customBanner;
        }
        
        String ver = app.conf.getString("app", "version", null);
        if (ver != null) {
            this.version = ver;
        }
        
        // 立即打印 banner
        printBanner();
    }
    
    public void printBanner() {
        System.out.println(banner);
        if (showVersion) {
            System.out.println("                         v" + version);
        }
        System.out.println();
    }
}
