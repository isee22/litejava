package litejava.plugins.annotation;

import litejava.Context;
import litejava.Handler;
import litejava.Plugin;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring MVC 注解路由插件 - 使用 Spring 原生注解，无需 Spring 容器
 * 
 * <p>轻量级实现，只解析 Spring MVC 注解并注册到 LiteJava 路由，
 * 不依赖 Spring 容器和 DispatcherServlet。
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.springframework</groupId>
 *     <artifactId>spring-web</artifactId>
 *     <version>5.3.31</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>配置</h2>
 * <pre>
 * # application.yml
 * springmvc:
 *   packages: com.example.controller
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 方式一：自动扫描主类所在包
 * app.use(new SpringMvcPlugin(Main.class));
 * 
 * // 方式二：指定扫描包
 * SpringMvcPlugin plugin = new SpringMvcPlugin();
 * plugin.packages = "com.example.controller";
 * app.use(plugin);
 * 
 * // Controller 定义（Spring 原生注解）
 * @RestController
 * @RequestMapping("/users")
 * public class UserController {
 *     @GetMapping
 *     public List<User> list() { return userService.list(); }
 *     
 *     @GetMapping("/{id}")
 *     public User get(@PathVariable Long id) { return userService.get(id); }
 *     
 *     @PostMapping
 *     public User create(@RequestBody User user) { return userService.create(user); }
 * }
 * }</pre>
 * 
 * <h2>支持的注解</h2>
 * <ul>
 *   <li>{@code @RestController}, {@code @Controller} - 控制器标记</li>
 *   <li>{@code @RequestMapping} - 路径映射（类和方法级别）</li>
 *   <li>{@code @GetMapping}, {@code @PostMapping}, {@code @PutMapping}, {@code @DeleteMapping}, {@code @PatchMapping}</li>
 *   <li>{@code @PathVariable} - 路径参数</li>
 *   <li>{@code @RequestParam} - 查询参数</li>
 *   <li>{@code @RequestBody} - 请求体</li>
 *   <li>{@code @RequestHeader} - 请求头</li>
 * </ul>
 * 
 * @see JaxRsAnnotationPlugin JAX-RS 注解支持
 * @see JerseyRuntimePlugin 完整 JAX-RS 运行时
 */
public class SpringMvcAnnotationPlugin extends Plugin {

    /** 是否启用自动扫描 classpath，默认 true */
    public boolean autoScan = true;
    
    /** 默认实例（单例访问） */
    public static SpringMvcAnnotationPlugin instance;
    
    /** 要扫描的包名，多个用逗号分隔 */
    public String packages;
    
    /** 已注册的 Controller 类列表 */
    public List<Class<?>> controllers = new ArrayList<>();
    
    /** 自定义实例创建器，可配合 DI 使用 */
    public java.util.function.Function<Class<?>, Object> instanceProvider;
    
    /** 主类，用于自动检测扫描包 */
    public Class<?> mainClass;
    
    public SpringMvcAnnotationPlugin() {
        instance = this;
    }
    
    public SpringMvcAnnotationPlugin(Class<?> mainClass) {
        instance = this;
        this.mainClass = mainClass;
    }
    
    @Override
    public void config() {
        // 集中加载配置
        packages = app.conf.getString("springmvc", "packages", packages);
        autoScan = app.conf.getBool("springmvc", "autoScan", autoScan);
        
        // 1. 扫描配置的包
        if (packages != null && !packages.isEmpty()) {
            for (String pkg : packages.split(",")) {
                scanPackage(pkg.trim());
            }
        }
        
        // 2. 尝试扫描主类所在包
        if (controllers.isEmpty() && mainClass != null) {
            String basePackage = mainClass.getPackage().getName();
            app.log.info("SpringMvcAnnotationPlugin: Scanning package '" + basePackage + "'");
            scanPackage(basePackage);
        }
        
        // 3. 自动扫描整个 classpath
        if (controllers.isEmpty() && autoScan) {
            app.log.info("SpringMvcAnnotationPlugin: Auto-scanning classpath for @RestController/@Controller...");
            scanClasspath();
        }
        
        for (Class<?> clazz : controllers) {
            registerController(clazz);
        }
        
        app.log.info("SpringMvcAnnotationPlugin: Registered " + controllers.size() + " controller(s)");
    }
    
    public SpringMvcAnnotationPlugin register(Class<?> controllerClass) {
        controllers.add(controllerClass);
        return this;
    }
    
    public SpringMvcAnnotationPlugin scanPackage(String packageName) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            java.util.Enumeration<java.net.URL> urls = cl.getResources(path);
            
            while (urls.hasMoreElements()) {
                java.net.URL url = urls.nextElement();
                java.io.File dir = new java.io.File(url.toURI());
                if (dir.exists()) {
                    scanDirectory(dir, packageName);
                }
            }
        } catch (Exception e) {
            app.log.warn("Failed to scan package: " + packageName);
        }
        return this;
    }
    
    private void scanDirectory(java.io.File dir, String packageName) {
        java.io.File[] files = dir.listFiles();
        if (files == null) return;
        
        for (java.io.File file : files) {
            String name = file.getName();
            if (file.isDirectory()) {
                String subPackage = packageName.isEmpty() ? name : packageName + "." + name;
                scanDirectory(file, subPackage);
            } else if (name.endsWith(".class") && !name.contains("$")) {
                String className = packageName.isEmpty() ? 
                    name.replace(".class", "") : 
                    packageName + "." + name.replace(".class", "");
                tryRegisterClass(className);
            }
        }
    }
    
    private void scanClasspath() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            java.util.Enumeration<java.net.URL> urls = cl.getResources("");

            while (urls.hasMoreElements()) {
                java.net.URL url = urls.nextElement();
                if ("file".equals(url.getProtocol())) {
                    scanDirectory(new java.io.File(url.toURI()), "");
                }
            }

            // 扫描 JAR 文件
            String classpath = System.getProperty("java.class.path");
            if (classpath != null) {
                for (String path : classpath.split(java.io.File.pathSeparator)) {
                    if (path.endsWith(".jar")) {
                        scanJarFile(new java.io.File(path));
                    }
                }
            }
        } catch (Exception e) {
            app.log.warn("Failed to scan classpath: " + e.getMessage());
        }
    }
    
    private void scanJarFile(java.io.File jarFile) {
        if (!jarFile.exists()) return;

        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!name.endsWith(".class") || name.contains("$")) continue;
                // 跳过框架和常见库
                if (name.startsWith("java/") || name.startsWith("javax/") || 
                    name.startsWith("sun/") || name.startsWith("com/sun/") ||
                    name.startsWith("org/springframework/") || name.startsWith("org/apache/") ||
                    name.startsWith("com/fasterxml/") || name.startsWith("io/netty/") ||
                    name.startsWith("litejava/")) continue;

                String className = name.replace('/', '.').replace(".class", "");
                tryRegisterClass(className);
            }
        } catch (Exception e) {
            // ignore invalid jars
        }
    }
    
    private void tryRegisterClass(String className) {
        try {
            Class<?> clazz = Class.forName(className, false, 
                Thread.currentThread().getContextClassLoader());
            if (clazz.isAnnotationPresent(RestController.class) || 
                clazz.isAnnotationPresent(Controller.class)) {
                controllers.add(clazz);
            }
        } catch (Throwable e) {
            // ignore classes that can't be loaded
        }
    }

    private void registerController(Class<?> clazz) {
        String basePath = "";
        RequestMapping classMapping = clazz.getAnnotation(RequestMapping.class);
        if (classMapping != null && classMapping.value().length > 0) {
            basePath = classMapping.value()[0];
        }
        
        Object instance = createInstance(clazz);
        
        for (Method method : clazz.getDeclaredMethods()) {
            registerMethod(basePath, instance, method);
        }
    }
    
    private Object createInstance(Class<?> clazz) {
        if (instanceProvider != null) return instanceProvider.apply(clazz);
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create controller: " + clazz.getName(), e);
        }
    }
    
    private void registerMethod(String basePath, Object instance, Method method) {
        String httpMethod = null;
        String path = "";
        
        if (method.isAnnotationPresent(GetMapping.class)) {
            httpMethod = "GET";
            String[] values = method.getAnnotation(GetMapping.class).value();
            if (values.length > 0) path = values[0];
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            httpMethod = "POST";
            String[] values = method.getAnnotation(PostMapping.class).value();
            if (values.length > 0) path = values[0];
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            httpMethod = "PUT";
            String[] values = method.getAnnotation(PutMapping.class).value();
            if (values.length > 0) path = values[0];
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            httpMethod = "DELETE";
            String[] values = method.getAnnotation(DeleteMapping.class).value();
            if (values.length > 0) path = values[0];
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            httpMethod = "PATCH";
            String[] values = method.getAnnotation(PatchMapping.class).value();
            if (values.length > 0) path = values[0];
        } else if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping rm = method.getAnnotation(RequestMapping.class);
            RequestMethod[] methods = rm.method();
            httpMethod = methods.length > 0 ? methods[0].name() : "GET";
            if (rm.value().length > 0) path = rm.value()[0];
        }
        
        if (httpMethod == null) return;
        
        // 转换 Spring 路径格式 {id} -> :id
        String fullPath = (basePath + path).replaceAll("\\{([^}]+)\\}", ":$1");
        if (!fullPath.startsWith("/")) fullPath = "/" + fullPath;
        
        Handler handler = ctx -> invokeMethod(ctx, instance, method);
        
        switch (httpMethod) {
            case "GET": app.get(fullPath, handler); break;
            case "POST": app.post(fullPath, handler); break;
            case "PUT": app.put(fullPath, handler); break;
            case "DELETE": app.delete(fullPath, handler); break;
            case "PATCH": app.patch(fullPath, handler); break;
        }
    }
    
    private void invokeMethod(Context ctx, Object instance, Method method) throws Exception {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        
        for (int i = 0; i < params.length; i++) {
            args[i] = resolveParameter(ctx, params[i]);
        }
        
        Object result;
        try {
            result = method.invoke(instance, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new RuntimeException(cause);
        }
        
        if (result == null) {
            if (method.getReturnType() == void.class) ctx.status(204);
            return;
        }
        
        if (result instanceof String) {
            ctx.text((String) result);
        } else {
            ctx.json(result);
        }
    }
    
    private Object resolveParameter(Context ctx, Parameter param) {
        PathVariable pv = param.getAnnotation(PathVariable.class);
        if (pv != null) {
            String name = pv.value().isEmpty() ? pv.name() : pv.value();
            if (name.isEmpty()) name = param.getName();
            return convertValue(ctx.pathParam(name), param.getType());
        }
        
        RequestParam rp = param.getAnnotation(RequestParam.class);
        if (rp != null) {
            String name = rp.value().isEmpty() ? rp.name() : rp.value();
            if (name.isEmpty()) name = param.getName();
            String value = ctx.queryParam(name);
            if (value == null && !rp.defaultValue().equals("\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n")) {
                value = rp.defaultValue();
            }
            return convertValue(value, param.getType());
        }
        
        RequestHeader rh = param.getAnnotation(RequestHeader.class);
        if (rh != null) {
            String name = rh.value().isEmpty() ? rh.name() : rh.value();
            if (name.isEmpty()) name = param.getName();
            return convertValue(ctx.header(name), param.getType());
        }
        
        if (param.isAnnotationPresent(RequestBody.class)) {
            return ctx.bind(param.getType());
        }
        
        if (param.getType() == Context.class) {
            return ctx;
        }
        
        // 无注解的复杂类型作为 RequestBody
        if (!param.getType().isPrimitive() && param.getType() != String.class) {
            return ctx.bind(param.getType());
        }
        
        return null;
    }
    
    private Object convertValue(String value, Class<?> type) {
        if (value == null) return null;
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        return value;
    }
}
