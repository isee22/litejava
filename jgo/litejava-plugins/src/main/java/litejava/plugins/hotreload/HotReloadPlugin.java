package litejava.plugins.hotreload;

import litejava.Plugin;

import java.io.File;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 热重载插件（开发模式）
 * 
 * <p>监控文件/目录变化，自动触发回调。支持配置文件、模板、静态资源、源码等任意文件类型。
 * 
 * <h2>配置文件方式</h2>
 * <pre>
 * # application.yml
 * hotreload:
 *   enabled: true
 *   pollInterval: 1000
 *   dirs: src/main/resources,templates,static
 * </pre>
 * 
 * <h2>场景1：配置文件热重载</h2>
 * <pre>{@code
 * HotReloadPlugin hotReload = new HotReloadPlugin()
 *     .watch("src/main/resources", "*.yml,*.properties", path -> {
 *         System.out.println("[HotReload] Config changed: " + path);
 *         app.conf.reload();  // 重新加载配置
 *     });
 * app.use(hotReload);
 * }</pre>
 * 
 * <h2>场景2：模板文件热重载</h2>
 * <pre>{@code
 * hotReload.watch("templates", "*.html,*.ftl", path -> {
 *     System.out.println("[HotReload] Template changed: " + path);
 *     // 模板引擎通常会自动重新加载
 * });
 * }</pre>
 * 
 * <h2>场景3：静态资源热重载</h2>
 * <pre>{@code
 * hotReload.watch("static", "*.css,*.js", path -> {
 *     System.out.println("[HotReload] Static file changed: " + path);
 *     // 静态文件无需特殊处理，下次请求自动返回新内容
 * });
 * }</pre>
 * 
 * <h2>场景4：Java 源码变化检测</h2>
 * <pre>{@code
 * // 监控源码目录，变化时提示重新编译
 * hotReload.watch("src/main/java", "*.java", path -> {
 *     System.out.println("[HotReload] Source changed: " + path);
 *     System.out.println("[HotReload] Please rebuild and restart...");
 * });
 * 
 * // 或监控编译产物，变化时自动重启应用
 * hotReload.watch("target/classes", "*.class", path -> {
 *     System.out.println("[HotReload] Class changed, restarting...");
 *     System.exit(0);  // 配合外部脚本自动重启
 * });
 * }</pre>
 * 
 * <h2>场景5：监控所有文件</h2>
 * <pre>{@code
 * // 不指定 pattern，监控目录下所有文件
 * hotReload.watch("uploads", path -> {
 *     System.out.println("[HotReload] File changed: " + path);
 * });
 * }</pre>
 * 
 * <h2>Java 代码热重载说明</h2>
 * <p>标准 JVM 不支持运行时替换类定义，Java 代码热重载需要：
 * <ul>
 *   <li>IDE Debug 模式 - 支持方法体修改（推荐开发时使用）</li>
 *   <li>DCEVM - 修改版 JVM，支持类结构变更</li>
 *   <li>JRebel - 商业工具，完整热重载</li>
 *   <li>自动重启 - 检测变化后重启应用（简单有效）</li>
 * </ul>
 * 
 * <h2>注意事项</h2>
 * <ul>
 *   <li>仅用于开发环境，生产环境请设置 enabled=false</li>
 *   <li>配置文件、模板、静态资源可直接热重载</li>
 *   <li>Java 代码只能检测变化，真正热重载需要特殊 JVM 支持</li>
 * </ul>
 */
public class HotReloadPlugin extends Plugin {
    
    /** 监控目录和回调 */
    private Map<String, WatchConfig> watchers = new HashMap<>();
    
    /** 文件修改时间缓存 */
    private Map<Path, Long> lastModified = new HashMap<>();
    
    /** 轮询间隔（毫秒） */
    public long pollInterval = 1000;
    
    /** 是否启用 */
    public boolean enabled = true;
    
    /** 调度器 */
    private ScheduledExecutorService scheduler;
    
    /** WatchService */
    private WatchService watchService;
    
    /** 默认回调（当没有指定回调时使用） */
    private Consumer<Path> defaultCallback;
    
    /**
     * 监控配置
     */
    private static class WatchConfig {
        String directory;
        String pattern;  // 文件匹配模式，如 "*.yml,*.properties"
        Consumer<Path> callback;
        
        WatchConfig(String directory, String pattern, Consumer<Path> callback) {
            this.directory = directory;
            this.pattern = pattern;
            this.callback = callback;
        }
    }
    
    /**
     * 监控目录（所有文件）
     */
    public HotReloadPlugin watch(String directory, Consumer<Path> onChange) {
        return watch(directory, "*", onChange);
    }
    
    /**
     * 监控目录（指定文件类型）
     * 
     * @param directory 目录路径
     * @param pattern 文件匹配模式，如 "*.yml,*.properties" 或 "*.class"
     * @param onChange 变化回调
     */
    public HotReloadPlugin watch(String directory, String pattern, Consumer<Path> onChange) {
        watchers.put(directory, new WatchConfig(directory, pattern, onChange));
        return this;
    }
    
    /**
     * 设置默认回调（用于配置文件指定的目录）
     */
    public HotReloadPlugin onFileChange(Consumer<Path> callback) {
        this.defaultCallback = callback;
        return this;
    }
    
    /**
     * 设置轮询间隔
     */
    public HotReloadPlugin pollInterval(long millis) {
        this.pollInterval = millis;
        return this;
    }
    
    /**
     * 启用/禁用
     */
    public HotReloadPlugin enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
    
    @Override
    public void config() {
        // 从配置文件读取
        if (app.conf != null) {
            enabled = app.conf.getBool("hotreload", "enabled", enabled);
            pollInterval = app.conf.getInt("hotreload", "pollInterval", (int) pollInterval);
            
            // 读取监控目录列表
            String dirsConfig = app.conf.getString("hotreload", "dirs", "");
            if (!dirsConfig.isEmpty()) {
                for (String dir : dirsConfig.split(",")) {
                    dir = dir.trim();
                    if (!dir.isEmpty() && !watchers.containsKey(dir)) {
                        // 使用默认回调或打印日志
                        Consumer<Path> callback = defaultCallback != null ? defaultCallback : 
                            path -> System.out.println("[HotReload] Changed: " + path);
                        watchers.put(dir, new WatchConfig(dir, "*", callback));
                    }
                }
            }
        }
        
        if (!enabled || watchers.isEmpty()) {
            return;
        }
        
        boolean useWatchService = tryInitWatchService();
        
        if (useWatchService) {
            startWatchServiceMonitor();
        } else {
            startPollingMonitor();
        }
        
        System.out.println("[HotReload] Watching " + watchers.size() + " directories (poll=" + pollInterval + "ms)");
        for (String dir : watchers.keySet()) {
            System.out.println("[HotReload]   - " + dir);
        }
    }
    
    @Override
    public void uninstall() {
        enabled = false;
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
    
    private boolean tryInitWatchService() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            
            for (WatchConfig config : watchers.values()) {
                Path path = Paths.get(config.directory);
                if (Files.isDirectory(path)) {
                    registerDirectory(path);
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println("[HotReload] WatchService not available, using polling mode");
            return false;
        }
    }
    
    private void registerDirectory(Path dir) {
        try {
            dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            
            // 递归注册子目录
            File[] files = dir.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        registerDirectory(file.toPath());
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }
    
    private void startWatchServiceMonitor() {
        Thread watchThread = new Thread(() -> {
            while (enabled) {
                try {
                    WatchKey key = watchService.poll(pollInterval, TimeUnit.MILLISECONDS);
                    if (key == null) continue;
                    
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                        
                        Path changed = (Path) event.context();
                        Path dir = (Path) key.watchable();
                        Path fullPath = dir.resolve(changed);
                        
                        // 找到匹配的监控配置
                        for (WatchConfig config : watchers.values()) {
                            if (fullPath.startsWith(config.directory) && matchesPattern(fullPath, config.pattern)) {
                                System.out.println("[HotReload] " + event.kind() + ": " + fullPath);
                                try {
                                    config.callback.accept(fullPath);
                                } catch (Exception e) {
                                    System.err.println("[HotReload] Callback error: " + e.getMessage());
                                }
                                break;
                            }
                        }
                    }
                    
                    key.reset();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "HotReload-Watcher");
        watchThread.setDaemon(true);
        watchThread.start();
    }
    
    private void startPollingMonitor() {
        // 初始化文件修改时间
        for (WatchConfig config : watchers.values()) {
            scanDirectory(new File(config.directory), config.pattern);
        }
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HotReload-Poller");
            t.setDaemon(true);
            return t;
        });
        
        scheduler.scheduleWithFixedDelay(() -> {
            for (WatchConfig config : watchers.values()) {
                checkDirectory(new File(config.directory), config.pattern, config.callback);
            }
        }, pollInterval, pollInterval, TimeUnit.MILLISECONDS);
    }
    
    private void scanDirectory(File dir, String pattern) {
        if (!dir.exists() || !dir.isDirectory()) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, pattern);
            } else if (matchesPattern(file.toPath(), pattern)) {
                lastModified.put(file.toPath(), file.lastModified());
            }
        }
    }
    
    private void checkDirectory(File dir, String pattern, Consumer<Path> onChange) {
        if (!dir.exists() || !dir.isDirectory()) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                checkDirectory(file, pattern, onChange);
            } else if (matchesPattern(file.toPath(), pattern)) {
                Path path = file.toPath();
                long currentModified = file.lastModified();
                Long previousModified = lastModified.get(path);
                
                if (previousModified == null) {
                    // 新文件
                    lastModified.put(path, currentModified);
                    System.out.println("[HotReload] NEW: " + path);
                    try {
                        onChange.accept(path);
                    } catch (Exception e) {
                        System.err.println("[HotReload] Callback error: " + e.getMessage());
                    }
                } else if (currentModified > previousModified) {
                    // 文件修改
                    lastModified.put(path, currentModified);
                    System.out.println("[HotReload] MODIFIED: " + path);
                    try {
                        onChange.accept(path);
                    } catch (Exception e) {
                        System.err.println("[HotReload] Callback error: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * 检查文件是否匹配模式
     * 
     * @param path 文件路径
     * @param pattern 模式，如 "*" 或 "*.yml,*.properties"
     */
    private boolean matchesPattern(Path path, String pattern) {
        if (pattern == null || pattern.equals("*")) {
            return true;
        }
        
        String filename = path.getFileName().toString().toLowerCase();
        
        for (String p : pattern.split(",")) {
            p = p.trim().toLowerCase();
            if (p.equals("*")) {
                return true;
            }
            if (p.startsWith("*.")) {
                String ext = p.substring(1);  // ".yml"
                if (filename.endsWith(ext)) {
                    return true;
                }
            } else if (filename.equals(p) || filename.matches(p.replace("*", ".*"))) {
                return true;
            }
        }
        
        return false;
    }
}
