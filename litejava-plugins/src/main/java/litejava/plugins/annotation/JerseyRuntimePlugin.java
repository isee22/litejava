package litejava.plugins.annotation;

import litejava.Context;
import litejava.Plugin;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ApplicationHandler;

import javax.ws.rs.core.SecurityContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.security.Principal;

/**
 * Jersey 插件 - 完整 JAX-RS 运行时集成
 * 
 * <p>集成完整的 Jersey JAX-RS 运行时，支持所有 JAX-RS 特性。
 * 如只需简单的注解路由，推荐使用更轻量的 {@link JaxRsAnnotationPlugin}。
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.glassfish.jersey.core</groupId>
 *     <artifactId>jersey-server</artifactId>
 *     <version>2.39.1</version>
 * </dependency>
 * <dependency>
 *     <groupId>org.glassfish.jersey.inject</groupId>
 *     <artifactId>jersey-hk2</artifactId>
 *     <version>2.39.1</version>
 * </dependency>
 * <dependency>
 *     <groupId>org.glassfish.jersey.media</groupId>
 *     <artifactId>jersey-media-json-jackson</artifactId>
 *     <version>2.39.1</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>配置</h2>
 * <pre>
 * # application.yml
 * jersey:
 *   path: /api
 *   packages: com.example.resource
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 方式一：自动扫描包
 * JerseyPlugin jersey = new JerseyPlugin();
 * jersey.packages = "com.example.resource";
 * app.use(jersey);
 * 
 * // 方式二：手动注册 Resource
 * JerseyPlugin jersey = new JerseyPlugin();
 * jersey.register(UserResource.class);
 * app.use(jersey);
 * 
 * // 方式三：使用 ResourceConfig
 * ResourceConfig config = new ResourceConfig();
 * config.packages("com.example.resource");
 * config.register(JacksonFeature.class);
 * app.use(new JerseyPlugin(config));
 * }</pre>
 * 
 * <h2>支持的 JAX-RS 特性</h2>
 * <ul>
 *   <li>完整的 @Path, @GET, @POST 等注解</li>
 *   <li>@Context 注入（UriInfo, HttpHeaders, SecurityContext）</li>
 *   <li>ContainerRequestFilter / ContainerResponseFilter</li>
 *   <li>ExceptionMapper</li>
 *   <li>MessageBodyReader / MessageBodyWriter</li>
 *   <li>异步处理（@Suspended AsyncResponse）</li>
 * </ul>
 * 
 * <h2>vs JaxRsAnnotationPlugin</h2>
 * <ul>
 *   <li>{@link JaxRsAnnotationPlugin} - 轻量，只解析注解，LiteJava 处理路由</li>
 *   <li>JerseyPlugin - 完整 JAX-RS 运行时，支持所有 JAX-RS 特性</li>
 * </ul>
 * 
 * @see JaxRsAnnotationPlugin 轻量级 JAX-RS 注解支持
 */
public class JerseyRuntimePlugin extends Plugin {
    
    /** Jersey 资源配置 */
    public ResourceConfig resourceConfig;
    
    /** Jersey 应用处理器 */
    public ApplicationHandler handler;
    
    /** 基础路径，默认 /api */
    public String basePath = "/api";
    
    /** 要扫描的包名，多个用逗号分隔 */
    public String packages;
    
    public JerseyRuntimePlugin() {
        this.resourceConfig = new ResourceConfig();
    }
    
    public JerseyRuntimePlugin(ResourceConfig config) {
        this.resourceConfig = config;
    }
    
    /**
     * 设置基础路径
     */
    public JerseyRuntimePlugin path(String path) {
        this.basePath = path.startsWith("/") ? path : "/" + path;
        return this;
    }
    
    /**
     * 扫描包
     */
    public JerseyRuntimePlugin packages(String... pkgs) {
        resourceConfig.packages(pkgs);
        return this;
    }
    
    /**
     * 注册 Resource 类
     */
    public JerseyRuntimePlugin register(Class<?> clazz) {
        resourceConfig.register(clazz);
        return this;
    }
    
    /**
     * 注册组件实例
     */
    public JerseyRuntimePlugin register(Object component) {
        resourceConfig.register(component);
        return this;
    }
    
    @Override
    public void config() {
        // 集中加载配置
        basePath = app.conf.getString("jersey", "path", basePath);
        packages = app.conf.getString("jersey", "packages", packages);
        
        // 使用 Jersey 原生的包扫描
        if (packages != null && !packages.isEmpty()) {
            resourceConfig.packages(packages.split(","));
        }
        
        // 初始化 Jersey
        handler = new ApplicationHandler(resourceConfig);
        
        // 注册通配符路由，将请求转发给 Jersey
        String routePath = basePath.endsWith("/") ? basePath + "*path" : basePath + "/*path";
        app.route("ANY", routePath, this::handleRequest);
        app.route("ANY", basePath, this::handleRequest);
        
        app.log.info("JerseyRuntimePlugin configured at " + basePath);
    }

    private void handleRequest(Context ctx) {
        try {
            String requestUri = ctx.path;
            if (ctx.query != null && !ctx.query.isEmpty()) {
                requestUri += "?" + ctx.query;
            }
            
            // Jersey 的 baseUri 是应用根路径，requestUri 是完整请求路径
            // Jersey 会用 requestUri - baseUri 来匹配 @Path
            URI baseUri = URI.create("http://localhost" + basePath + "/");
            URI fullUri = URI.create("http://localhost" + requestUri);
            
            ContainerRequest request = new ContainerRequest(
                baseUri,
                fullUri,
                ctx.method,
                getSecurityContext(ctx),
                new org.glassfish.jersey.internal.PropertiesDelegate() {
                    private final java.util.Map<String, Object> props = new java.util.HashMap<>();
                    @Override public Object getProperty(String name) { return props.get(name); }
                    @Override public java.util.Collection<String> getPropertyNames() { return props.keySet(); }
                    @Override public void setProperty(String name, Object value) { props.put(name, value); }
                    @Override public void removeProperty(String name) { props.remove(name); }
                },
                null
            );
            
            for (java.util.Map.Entry<String, String> entry : ctx.headers.entrySet()) {
                request.header(entry.getKey(), entry.getValue());
            }
            
            byte[] body = ctx.getRawData();
            if (body != null && body.length > 0) {
                request.setEntityStream(new ByteArrayInputStream(body));
            }
            
            ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
            java.util.concurrent.Future<ContainerResponse> future = handler.apply(request, responseBody);
            ContainerResponse response = future.get();
            
            ctx.status(response.getStatus());
            response.getHeaders().forEach((name, values) -> {
                if (values != null && !values.isEmpty()) {
                    ctx.header(name, values.get(0).toString());
                }
            });
            
            byte[] respBody = responseBody.toByteArray();
            if (respBody.length > 0) {
                ctx.data(respBody);
            }
            
        } catch (Exception e) {
            app.log.error("Jersey error: " + e.getMessage());
            ctx.status(500);
            ctx.json(litejava.util.Maps.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
        }
    }
    
    private SecurityContext getSecurityContext(Context ctx) {
        return new SecurityContext() {
            @Override public Principal getUserPrincipal() { return null; }
            @Override public boolean isUserInRole(String role) { return false; }
            @Override public boolean isSecure() { return false; }
            @Override public String getAuthenticationScheme() { return null; }
        };
    }
}
