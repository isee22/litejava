package litejava.plugins.route;

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
 * Spring MVC 原生注解插件 - 使用 Spring 原生注解，无需 Spring 容器
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
 * <pre>{@code
 * springmvc.packages=com.example.controller
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 自动扫描
 * app.use(new SpringMvcPlugin(Main.class));
 * 
 * // Controller 定义 (Spring 原生注解)
 * @RestController
 * @RequestMapping("/users")
 * public class UserController {
 *     
 *     @GetMapping
 *     public List<User> list() {
 *         return userService.list();
 *     }
 *     
 *     @GetMapping("/{id}")
 *     public User get(@PathVariable Long id) {
 *         return userService.get(id);
 *     }
 *     
 *     @PostMapping
 *     public User create(@RequestBody User user) {
 *         return userService.create(user);
 *     }
 *     
 *     @PutMapping("/{id}")
 *     public User update(@PathVariable Long id, @RequestBody User user) {
 *         return userService.update(id, user);
 *     }
 *     
 *     @DeleteMapping("/{id}")
 *     public void delete(@PathVariable Long id) {
 *         userService.delete(id);
 *     }
 * }
 * 
 * // 配合 Guice DI
 * GuicePlugin di = new GuicePlugin();
 * SpringMvcPlugin mvc = new SpringMvcPlugin(Main.class);
 * mvc.instanceProvider = di::get;
 * 
 * app.use(di);
 * app.use(mvc);
 * }</pre>
 * 
 * <h2>支持的注解 (Spring 原生)</h2>
 * <ul>
 *   <li>{@code @RestController}, {@code @Controller}</li>
 *   <li>{@code @RequestMapping}</li>
 *   <li>{@code @GetMapping}, {@code @PostMapping}, {@code @PutMapping}, {@code @DeleteMapping}, {@code @PatchMapping}</li>
 *   <li>{@code @PathVariable}, {@code @RequestParam}, {@code @RequestBody}, {@code @RequestHeader}</li>
 * </ul>
 * 
 * <h2>vs SpringStylePlugin</h2>
 * <ul>
 *   <li>SpringStylePlugin - 自定义注解，零依赖</li>
 *   <li>SpringMvcPlugin - Spring 原生注解，需要 spring-web 依赖</li>
 * </ul>
 */
public class SpringMvcPlugin extends Plugin {
    
    /** 默认实例（单例访问） */
    public static SpringMvcPlugin instance;
    
    /** 已注册的 Controller 类列表 */
    public List<Class<?>> controllers = new ArrayList<>();
    
    /** 自定义实例创建器，可配合 DI 使用，如 di::get */
    public java.util.function.Function<Class<?>, Object> instanceProvider;
    
    /** 主类，用于自动检测扫描包 */
    public Class<?> mainClass;
    
    public SpringMvcPlugin() {}
    
    public SpringMvcPlugin(Class<?> mainClass) {
        this.mainClass = mainClass;
    }
    
    @Override
    public void config() {
        String packages = app.conf.getString("springmvc", "packages", null);
        if (packages != null) {
            for (String pkg : packages.split(",")) {
                scanPackage(pkg.trim());
            }
        }
        
        if (controllers.isEmpty() && packages == null) {
            String basePackage = detectBasePackage();
            if (basePackage != null) {
                app.log.info("SpringMvcPlugin: Auto-scanning package '" + basePackage + "'");
                scanPackage(basePackage);
            }
        }
        
        if (controllers.isEmpty()) {
            app.log.warn("SpringMvcPlugin: No @RestController/@Controller found.");
        }
        
        for (Class<?> clazz : controllers) {
            registerController(clazz);
        }
        
        if (instance == null) instance = this;
        app.log.info("SpringMvcPlugin registered " + controllers.size() + " controller(s)");
    }
    
    public SpringMvcPlugin register(Class<?> controllerClass) {
        controllers.add(controllerClass);
        return this;
    }
    
    public SpringMvcPlugin scanPackage(String packageName) {
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
    
    private void scanDirectory(java.io.File dir, String packageName) throws Exception {
        java.io.File[] files = dir.listFiles();
        if (files == null) return;
        
        for (java.io.File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(RestController.class) || 
                    clazz.isAnnotationPresent(Controller.class)) {
                    controllers.add(clazz);
                }
            }
        }
    }
    
    private String detectBasePackage() {
        if (mainClass != null) return mainClass.getPackage().getName();
        try {
            for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
                if (e.getMethodName().equals("main")) {
                    return Class.forName(e.getClassName()).getPackage().getName();
                }
            }
        } catch (Exception ignored) {}
        return null;
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
        // @PathVariable
        PathVariable pv = param.getAnnotation(PathVariable.class);
        if (pv != null) {
            String name = pv.value().isEmpty() ? pv.name() : pv.value();
            if (name.isEmpty()) name = param.getName();
            return convertValue(ctx.pathParam(name), param.getType());
        }
        
        // @RequestParam
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
        
        // @RequestHeader
        RequestHeader rh = param.getAnnotation(RequestHeader.class);
        if (rh != null) {
            String name = rh.value().isEmpty() ? rh.name() : rh.value();
            if (name.isEmpty()) name = param.getName();
            return convertValue(ctx.header(name), param.getType());
        }
        
        // @RequestBody
        if (param.isAnnotationPresent(RequestBody.class)) {
            return ctx.bind(param.getType());
        }
        
        // Context 参数
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
