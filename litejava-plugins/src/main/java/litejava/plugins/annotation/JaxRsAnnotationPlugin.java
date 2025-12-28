package litejava.plugins.annotation;

import litejava.Context;
import litejava.Handler;
import litejava.Plugin;

import javax.ws.rs.*;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * JAX-RS 注解路由插件 - 轻量级，只解析注解并注册到 LiteJava 路由
 *
 * <p>支持自动扫描 classpath，无需配置即可使用。
 * 如需过滤器、拦截器、异步等特性，请使用 {@link JerseyRuntimePlugin}。
 *
 * <h2>使用方式</h2>
 * <pre>{@code
 * // 方式一：零配置，自动扫描整个 classpath
 * app.use(new JaxRsAnnotationPlugin());
 *
 * // 方式二：指定主类，扫描其所在包
 * app.use(new JaxRsAnnotationPlugin(Main.class));
 *
 * // 方式三：配置文件指定包
 * // jaxrs.packages=com.example.resource
 * app.use(new JaxRsAnnotationPlugin());
 * }</pre>
 *
 * <h2>支持的注解</h2>
 * <ul>
 *   <li>{@code @Path}, {@code @GET}, {@code @POST}, {@code @PUT}, {@code @DELETE}, {@code @PATCH}</li>
 *   <li>{@code @PathParam}, {@code @QueryParam}, {@code @HeaderParam}, {@code @DefaultValue}</li>
 * </ul>
 *
 * @see JerseyRuntimePlugin 完整 JAX-RS 运行时
 * @see SpringMvcAnnotationPlugin Spring MVC 注解支持
 */
public class JaxRsAnnotationPlugin extends Plugin {

    /** 要扫描的包名，多个用逗号分隔 */
    public String packages;

    /** 是否自动扫描 classpath（当未指定包时） */
    public boolean autoScan = true;

    /** 已注册的资源类 */
    public List<Class<?>> resources = new ArrayList<>();

    /** 实例提供者（用于 DI 集成） */
    public java.util.function.Function<Class<?>, Object> instanceProvider;

    /** 主类（用于自动检测包名） */
    public Class<?> mainClass;

    public JaxRsAnnotationPlugin() {}

    public JaxRsAnnotationPlugin(Class<?> mainClass) {
        this.mainClass = mainClass;
    }

    @Override
    public void config() {
        // 集中加载配置
        packages = app.conf.getString("jaxrs", "packages", packages);
        autoScan = app.conf.getBool("jaxrs", "autoScan", autoScan);

        // 1. 扫描配置的包
        if (packages != null && !packages.isEmpty()) {
            for (String pkg : packages.split(",")) {
                scanPackage(pkg.trim());
            }
        }

        // 2. 尝试扫描主类所在包
        if (resources.isEmpty() && mainClass != null) {
            String basePackage = mainClass.getPackage().getName();
            app.log.info("JaxRsAnnotationPlugin: Scanning package '" + basePackage + "'");
            scanPackage(basePackage);
        }

        // 3. 自动扫描整个 classpath
        if (resources.isEmpty() && autoScan) {
            app.log.info("JaxRsAnnotationPlugin: Auto-scanning classpath for @Path classes...");
            scanClasspath();
        }

        // 注册所有资源
        for (Class<?> clazz : resources) {
            registerResource(clazz);
        }

        app.log.info("JaxRsAnnotationPlugin: Registered " + resources.size() + " resource(s)");
    }

    public JaxRsAnnotationPlugin register(Class<?> resourceClass) {
        resources.add(resourceClass);
        return this;
    }

    public JaxRsAnnotationPlugin scanPackage(String packageName) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> urls = cl.getResources(path);

            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    scanDirectory(new File(url.toURI()), packageName);
                } else if ("jar".equals(protocol)) {
                    scanJar(url, packageName);
                }
            }
        } catch (Exception e) {
            app.log.warn("Failed to scan package: " + packageName + " - " + e.getMessage());
        }
        return this;
    }

    private void scanClasspath() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> urls = cl.getResources("");

            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    scanDirectory(new File(url.toURI()), "");
                }
            }

            // 扫描 JAR 文件
            String classpath = System.getProperty("java.class.path");
            if (classpath != null) {
                for (String path : classpath.split(File.pathSeparator)) {
                    if (path.endsWith(".jar")) {
                        scanJarFile(new File(path));
                    }
                }
            }
        } catch (Exception e) {
            app.log.warn("Failed to scan classpath: " + e.getMessage());
        }
    }

    private void scanDirectory(File dir, String packageName) {
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
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

    private void scanJar(URL url, String packageName) {
        try {
            String jarPath = url.getPath();
            if (jarPath.contains("!")) {
                jarPath = jarPath.substring(5, jarPath.indexOf("!"));
            }
            scanJarFile(new File(jarPath), packageName);
        } catch (Exception e) {
            // ignore
        }
    }

    private void scanJarFile(File jarFile) {
        scanJarFile(jarFile, null);
    }

    private void scanJarFile(File jarFile, String packageFilter) {
        if (!jarFile.exists()) return;

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            String pathFilter = packageFilter != null ? packageFilter.replace('.', '/') : null;

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!name.endsWith(".class") || name.contains("$")) continue;
                if (pathFilter != null && !name.startsWith(pathFilter)) continue;
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
            if (clazz.isAnnotationPresent(Path.class)) {
                resources.add(clazz);
            }
        } catch (Throwable e) {
            // ignore classes that can't be loaded
        }
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
        PathParam pathParam = param.getAnnotation(PathParam.class);
        if (pathParam != null) {
            return convertValue(ctx.pathParam(pathParam.value()), param.getType());
        }

        QueryParam queryParam = param.getAnnotation(QueryParam.class);
        if (queryParam != null) {
            String value = ctx.queryParam(queryParam.value());
            DefaultValue defaultValue = param.getAnnotation(DefaultValue.class);
            if (value == null && defaultValue != null) value = defaultValue.value();
            return convertValue(value, param.getType());
        }

        HeaderParam headerParam = param.getAnnotation(HeaderParam.class);
        if (headerParam != null) {
            return convertValue(ctx.header(headerParam.value()), param.getType());
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
