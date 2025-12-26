package litejava.plugins.route;

import litejava.Context;
import litejava.Handler;
import litejava.Plugin;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * JAX-RS 兼容插件 - 使用标准 JAX-RS 注解定义路由
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>javax.ws.rs</groupId>
 *     <artifactId>javax.ws.rs-api</artifactId>
 *     <version>2.1.1</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>配置 (可选)</h2>
 * <pre>{@code
 * jaxrs.packages=com.example.resource,com.example.api
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 自动扫描主类所在包
 * app.use(new JaxRsPlugin(Main.class));
 * 
 * // Resource 定义 (标准 JAX-RS 注解)
 * @Path("/users")
 * public class UserResource {
 *     
 *     @GET
 *     @Produces(MediaType.APPLICATION_JSON)
 *     public List<User> list() {
 *         return userService.list();
 *     }
 *     
 *     @GET
 *     @Path("/{id}")
 *     public User get(@PathParam("id") Long id) {
 *         return userService.get(id);
 *     }
 *     
 *     @POST
 *     @Consumes(MediaType.APPLICATION_JSON)
 *     public User create(User user) {
 *         return userService.create(user);
 *     }
 *     
 *     @PUT
 *     @Path("/{id}")
 *     public User update(@PathParam("id") Long id, User user) {
 *         return userService.update(id, user);
 *     }
 *     
 *     @DELETE
 *     @Path("/{id}")
 *     public void delete(@PathParam("id") Long id) {
 *         userService.delete(id);
 *     }
 * }
 * 
 * // 配合 DI 使用
 * GuicePlugin di = new GuicePlugin();
 * JaxRsPlugin jaxrs = new JaxRsPlugin(Main.class);
 * jaxrs.instanceProvider = di::get;
 * 
 * app.use(di);
 * app.use(jaxrs);
 * }</pre>
 * 
 * <h2>支持的注解</h2>
 * <ul>
 *   <li>{@code @Path} - 路径 (类和方法级别)</li>
 *   <li>{@code @GET}, {@code @POST}, {@code @PUT}, {@code @DELETE}, {@code @PATCH} - HTTP 方法</li>
 *   <li>{@code @PathParam} - 路径参数</li>
 *   <li>{@code @QueryParam} - 查询参数</li>
 *   <li>{@code @HeaderParam} - 请求头参数</li>
 *   <li>{@code @Consumes}, {@code @Produces} - Content-Type</li>
 * </ul>
 * 
 * <h2>返回值处理</h2>
 * <ul>
 *   <li>返回对象 → 自动 JSON 序列化</li>
 *   <li>返回 String → 文本响应</li>
 *   <li>返回 void → 空响应</li>
 * </ul>
 */
public class JaxRsPlugin extends Plugin {
    
    public List<Class<?>> resources = new ArrayList<>();
    public java.util.function.Function<Class<?>, Object> instanceProvider;
    public Class<?> mainClass;
    
    public JaxRsPlugin() {}
    
    public JaxRsPlugin(Class<?> mainClass) {
        this.mainClass = mainClass;
    }
    
    @Override
    public void config() {
        // 从配置读取要扫描的包
        String packages = app.conf.getString("jaxrs", "packages", null);
        if (packages != null) {
            for (String pkg : packages.split(",")) {
                scanPackage(pkg.trim());
            }
        }
        
        // 如果没有配置包，尝试扫描主类所在包
        if (resources.isEmpty() && packages == null) {
            String basePackage = detectBasePackage();
            if (basePackage != null) {
                app.log.info("JaxRsPlugin: Auto-scanning package '" + basePackage + "'");
                scanPackage(basePackage);
            }
        }
        
        if (resources.isEmpty()) {
            app.log.warn("JaxRsPlugin: No @Path resources found. " +
                "Configure 'jaxrs.packages' or use JaxRsPlugin(MainClass.class)");
        }
        
        for (Class<?> clazz : resources) {
            registerResource(clazz);
        }
        
        app.log.info("JaxRsPlugin registered " + resources.size() + " resource(s)");
    }

    public JaxRsPlugin register(Class<?> resourceClass) {
        resources.add(resourceClass);
        return this;
    }
    
    public JaxRsPlugin scanPackage(String packageName) {
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
            app.log.warn("Failed to scan package: " + packageName + " - " + e.getMessage());
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
                if (clazz.isAnnotationPresent(Path.class)) {
                    resources.add(clazz);
                }
            }
        }
    }
    
    private String detectBasePackage() {
        if (mainClass != null) {
            return mainClass.getPackage().getName();
        }
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                if (element.getMethodName().equals("main")) {
                    return Class.forName(element.getClassName()).getPackage().getName();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
    
    private void registerResource(Class<?> clazz) {
        Path classPath = clazz.getAnnotation(Path.class);
        if (classPath == null) return;
        
        String basePath = normalizePath(classPath.value());
        Object instance = createInstance(clazz);
        
        for (Method method : clazz.getDeclaredMethods()) {
            registerMethod(basePath, instance, method);
        }
    }
    
    private Object createInstance(Class<?> clazz) {
        if (instanceProvider != null) {
            return instanceProvider.apply(clazz);
        }
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create resource: " + clazz.getName(), e);
        }
    }
    
    private void registerMethod(String basePath, Object instance, Method method) {
        String httpMethod = getHttpMethod(method);
        if (httpMethod == null) return;
        
        Path methodPath = method.getAnnotation(Path.class);
        String fullPath = basePath + (methodPath != null ? normalizePath(methodPath.value()) : "");
        
        // 转换 JAX-RS 路径参数格式 {id} -> :id
        fullPath = fullPath.replaceAll("\\{([^}]+)\\}", ":$1");
        
        Handler handler = ctx -> invokeMethod(ctx, instance, method);
        
        switch (httpMethod) {
            case "GET": app.get(fullPath, handler); break;
            case "POST": app.post(fullPath, handler); break;
            case "PUT": app.put(fullPath, handler); break;
            case "DELETE": app.delete(fullPath, handler); break;
            case "PATCH": app.patch(fullPath, handler); break;
        }
    }
    
    private String getHttpMethod(Method method) {
        if (method.isAnnotationPresent(GET.class)) return "GET";
        if (method.isAnnotationPresent(POST.class)) return "POST";
        if (method.isAnnotationPresent(PUT.class)) return "PUT";
        if (method.isAnnotationPresent(DELETE.class)) return "DELETE";
        if (method.isAnnotationPresent(PATCH.class)) return "PATCH";
        return null;
    }
    
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "";
        if (!path.startsWith("/")) path = "/" + path;
        if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
        return path;
    }

    private void invokeMethod(Context ctx, Object instance, Method method) throws Exception {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        
        for (int i = 0; i < params.length; i++) {
            args[i] = resolveParameter(ctx, params[i], method);
        }
        
        Object result;
        try {
            result = method.invoke(instance, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new RuntimeException(cause);
        }
        
        // 处理返回值
        if (result == null) {
            if (method.getReturnType() == void.class) {
                ctx.status(204);
            }
            return;
        }
        
        if (result instanceof String) {
            ctx.text((String) result);
        } else {
            ctx.json(result);
        }
    }
    
    private Object resolveParameter(Context ctx, Parameter param, Method method) {
        // @PathParam
        PathParam pathParam = param.getAnnotation(PathParam.class);
        if (pathParam != null) {
            String value = ctx.pathParam(pathParam.value());
            return convertValue(value, param.getType());
        }
        
        // @QueryParam
        QueryParam queryParam = param.getAnnotation(QueryParam.class);
        if (queryParam != null) {
            String value = ctx.queryParam(queryParam.value());
            DefaultValue defaultValue = param.getAnnotation(DefaultValue.class);
            if (value == null && defaultValue != null) {
                value = defaultValue.value();
            }
            return convertValue(value, param.getType());
        }
        
        // @HeaderParam
        HeaderParam headerParam = param.getAnnotation(HeaderParam.class);
        if (headerParam != null) {
            String value = ctx.header(headerParam.value());
            return convertValue(value, param.getType());
        }
        
        // 无注解参数 - 作为请求体
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
