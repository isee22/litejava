package litejava.plugin;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import litejava.Plugin;

/**
 * 日志插件 - 简单高效的日志实现（零外部依赖）
 * 
 * <p>特点：
 * <ul>
 *   <li>零外部依赖，纯 JDK 实现</li>
 *   <li>支持日志级别：DEBUG, INFO, WARN, ERROR</li>
 *   <li>支持文件输出</li>
 *   <li>自动显示调用者类名</li>
 *   <li>可配置时间格式</li>
 * </ul>
 * 
 * <h2>输出格式</h2>
 * <pre>
 * [2024-01-01 12:00:00] [INFO ] [UserController] Server started on port 8080
 * [2024-01-01 12:00:01] [ERROR] [UserService] Database connection failed
 * </pre>
 * 
 * <h2>配置项</h2>
 * <pre>
 * log.level=INFO           # 日志级别：DEBUG, INFO, WARN, ERROR
 * log.file=logs/app.log    # 日志文件路径（可选）
 * log.pattern=yyyy-MM-dd HH:mm:ss  # 时间格式
 * log.showCaller=true      # 是否显示调用者类名
 * </pre>
 * 
 * <h2>使用方式</h2>
 * <pre>{@code
 * // 在 handler 中使用
 * app.get("/users", ctx -> {
 *     app.log.info("Fetching users");
 *     ctx.json(userService.list());
 * });
 * 
 * // 记录异常
 * try {
 *     // ...
 * } catch (Exception e) {
 *     app.log.error("Operation failed", e);
 * }
 * }</pre>
 * 
 * <h2>日志级别</h2>
 * <table>
 *   <tr><th>级别</th><th>值</th><th>说明</th></tr>
 *   <tr><td>DEBUG</td><td>0</td><td>调试信息</td></tr>
 *   <tr><td>INFO</td><td>1</td><td>一般信息（默认）</td></tr>
 *   <tr><td>WARN</td><td>2</td><td>警告信息</td></tr>
 *   <tr><td>ERROR</td><td>3</td><td>错误信息</td></tr>
 * </table>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 */
public class LogPlugin extends Plugin {
    
    // ==================== 日志级别常量 ====================
    
    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;
    
    // ==================== 配置项 ====================
    
    /** 当前日志级别 */
    public int level = INFO;
    
    /** 日志文件路径 */
    public String file;
    
    /** 时间格式 */
    public String pattern = "yyyy-MM-dd HH:mm:ss";
    
    /** 是否显示调用者类名 */
    public boolean showCaller = true;
    
    // ==================== 内部状态 ====================
    
    /** 文件输出流 */
    public PrintWriter fileWriter;
    
    /** 时间格式化器 */
    public DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
    
    @Override
    public void config() {
        String levelStr = app.conf.getString("log", "level", "INFO");
        level = parseLevel(levelStr);
        file = app.conf.getString("log", "file", null);
        pattern = app.conf.getString("log", "pattern", pattern);
        showCaller = app.conf.getBool("log", "showCaller", true);
        
        formatter = DateTimeFormatter.ofPattern(pattern);
        
        if (file != null && !file.isEmpty()) {
            try {
                File logFile = new File(file);
                if (logFile.getParentFile() != null) {
                    logFile.getParentFile().mkdirs();
                }
                fileWriter = new PrintWriter(new FileWriter(logFile, true), true);
            } catch (IOException e) {
                System.err.println("Failed to open log file: " + file);
            }
        }
    }
    
    @Override
    public void uninstall() {
        if (fileWriter != null) {
            fileWriter.close();
        }
    }
    
    /**
     * 解析日志级别字符串
     */
    private int parseLevel(String levelStr) {
        switch (levelStr.toUpperCase()) {
            case "DEBUG": return DEBUG;
            case "INFO": return INFO;
            case "WARN": return WARN;
            case "ERROR": return ERROR;
            default: return INFO;
        }
    }
    
    /**
     * 获取调用者类名（简短名称）
     */
    private String getCallerClass() {
        if (!showCaller) return null;
        
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        // 跳过: getStackTrace, getCallerClass, log, debug/info/warn/error
        for (int i = 4; i < stack.length; i++) {
            String className = stack[i].getClassName();
            // 跳过 LogPlugin 自身和子类
            if (!className.contains("LogPlugin")) {
                // 返回简短类名
                int lastDot = className.lastIndexOf('.');
                return lastDot > 0 ? className.substring(lastDot + 1) : className;
            }
        }
        return "Unknown";
    }
    
    // ==================== 日志方法 ====================
    
    /**
     * 记录 DEBUG 级别日志
     */
    public void debug(String msg) {
        if (level <= DEBUG) log("DEBUG", msg, null);
    }
    
    /**
     * 记录 INFO 级别日志
     */
    public void info(String msg) {
        if (level <= INFO) log("INFO", msg, null);
    }
    
    /**
     * 记录 WARN 级别日志
     */
    public void warn(String msg) {
        if (level <= WARN) log("WARN", msg, null);
    }
    
    /**
     * 记录 ERROR 级别日志
     */
    public void error(String msg) {
        if (level <= ERROR) log("ERROR", msg, null);
    }
    
    /**
     * 记录 ERROR 级别日志（带异常）
     */
    public void error(String msg, Throwable e) {
        if (level <= ERROR) log("ERROR", msg, e);
    }
    
    /**
     * 输出日志
     */
    protected void log(String levelName, String msg, Throwable e) {
        String time = LocalDateTime.now().format(formatter);
        String caller = getCallerClass();
        
        String line;
        if (caller != null) {
            line = String.format("[%s] [%-5s] [%s] %s", time, levelName, caller, msg);
        } else {
            line = String.format("[%s] [%-5s] %s", time, levelName, msg);
        }
        
        if ("ERROR".equals(levelName)) {
            System.err.println(line);
            if (e != null) e.printStackTrace(System.err);
        } else {
            System.out.println(line);
        }
        
        if (fileWriter != null) {
            fileWriter.println(line);
            if (e != null) e.printStackTrace(fileWriter);
        }
    }
}
