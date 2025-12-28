package litejava.plugins.annotation;

import litejava.Context;
import litejava.Handler;
import litejava.Plugin;
import litejava.plugin.ClassScanner;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spring MVC 注解路由插件 - 使用 Spring 原生注解，无需 Spring 容器
 * 
 * <h2>配置</h2>
 * <pre>
 * springmvc:
 *   packages: com.example.controller
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/users")
 * public class UserController {
 *     @GetMapping
 *     public List<User> list() { return userService.list(); }
 * }
 * }</pre>
 */
public class SpringMvcAnnotationPlugin extends Plugin {

    /** 路径变量提取正则 */
    private static final Pattern PATH_VAR_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    /** 要扫描的包名，多个用逗号分隔 */
    public String packages;
    
    /** 已注册的 Controller 类列表 */
    public List<Class<?>> controllers = new ArrayList<>();
    
    /** 自定义实例创建器 */
    public java.util.function.Function<Class<?>, Object> instanceProvider;
    
    /** 主类，用于自动检测扫描包 */
    public Class<?> mainClass;
    
    public SpringMvcAnnotationPlugin() {
    }
    
    public SpringMvcAnnotationPlugin(Class<?> mainClass) {
        this.mainClass = mainClass;
    }
    
    @Override
    public void config() {
        packages = app.conf.getString("springmvc", "packages", packages);
        
        // 确定扫描包
        String scanPackages = packages;
        if (scanPackages == null && mainClass != null) {
            scanPackages = mainClass.getPackage().getName();
        }
        
        if (scanPackages != null && !scanPackages.isEmpty()) {
            ClassScanner scanner = ClassScanner.getInstance();
            for (String pkg : scanPackages.split(",")) {
                String trimmed = pkg.trim();
                if (!trimmed.isEmpty()) {
                    for (Class<?> clazz : scanner.scan(trimmed)) {
                        if (clazz.isAnnotationPresent(RestController.class) || 
                            clazz.isAnnotationPresent(Controller.class)) {
                            controllers.add(clazz);
                        }
                    }
                }
            }
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
        Plugin guice = app.plugins.get("GuicePlugin");
        if (guice != null) {
            try {
                Method getMethod = guice.getClass().getMethod("get", Class.class);
                return getMethod.invoke(guice, clazz);
            } catch (Exception e) {
                // fallback
            }
        }
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
        
        // 提取路径变量名（按顺序）
        String templatePath = basePath + path;
        List<String> pathVarNames = extractPathVariables(templatePath);
        
        String fullPath = templatePath.replaceAll("\\{([^}]+)\\}", ":$1");
        if (!fullPath.startsWith("/")) fullPath = "/" + fullPath;
        
        Handler handler = ctx -> invokeMethod(ctx, instance, method, pathVarNames);
        
        switch (httpMethod) {
            case "GET": app.get(fullPath, handler); break;
            case "POST": app.post(fullPath, handler); break;
            case "PUT": app.put(fullPath, handler); break;
            case "DELETE": app.delete(fullPath, handler); break;
            case "PATCH": app.patch(fullPath, handler); break;
        }
    }
    
    /**
     * 从路径模板中提取变量名（按顺序）
     * 例如：/users/{id}/posts/{postId} -> ["id", "postId"]
     */
    private List<String> extractPathVariables(String path) {
        List<String> vars = new ArrayList<>();
        Matcher matcher = PATH_VAR_PATTERN.matcher(path);
        while (matcher.find()) {
            vars.add(matcher.group(1));
        }
        return vars;
    }
    
    private void invokeMethod(Context ctx, Object instance, Method method, List<String> pathVarNames) throws Exception {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        
        // 统计 @PathVariable 参数的索引，用于按顺序匹配
        int pathVarIndex = 0;
        
        for (int i = 0; i < params.length; i++) {
            PathVariable pv = params[i].getAnnotation(PathVariable.class);
            if (pv != null) {
                args[i] = resolvePathVariable(ctx, params[i], pv, pathVarNames, pathVarIndex++);
            } else {
                args[i] = resolveParameter(ctx, params[i]);
            }
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
    
    /**
     * 解析 @PathVariable 参数
     * 优先使用注解指定的名称，否则按顺序从路径模板中匹配
     */
    private Object resolvePathVariable(Context ctx, Parameter param, PathVariable pv, 
                                       List<String> pathVarNames, int index) {
        String name = pv.value().isEmpty() ? pv.name() : pv.value();
        
        // 如果注解没有指定名称，从路径模板中按顺序获取
        if (name.isEmpty()) {
            if (index < pathVarNames.size()) {
                name = pathVarNames.get(index);
            } else {
                name = param.getName(); // fallback
            }
        }
        
        return convertValue(ctx.pathParam(name), param.getType());
    }
    
    private Object resolveParameter(Context ctx, Parameter param) {
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
