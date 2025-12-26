package litejava.plugins.route;

import litejava.Context;
import litejava.Plugin;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ApplicationHandler;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.security.Principal;

/**
 * Jersey 插件 - 完整 JAX-RS 运行时集成
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
 * <pre>{@code
 * jersey.path=/api
 * jersey.packages=com.example.resource
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 方式一：自动扫描包
 * JerseyPlugin jersey = new JerseyPlugin();
 * jersey.packages("com.example.resource");
 * app.use(jersey);
 * 
 * // 方式二：手动注册 Resource
 * JerseyPlugin jersey = new JerseyPlugin();
 * jersey.register(UserResource.class);
 * jersey.register(OrderResource.class);
 * app.use(jersey);
 * 
 * // 方式三：使用 ResourceConfig
 * ResourceConfig config = new ResourceConfig();
 * config.packages("com.example.resource");
 * config.register(JacksonFeature.class);
 * config.register(MyFilter.class);
 * 
 * JerseyPlugin jersey = new JerseyPlugin(config);
 * app.use(jersey);
 * 
 * // Resource 定义 (标准 JAX-RS)
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
 *     public Response get(@PathParam("id") Long id) {
 *         User user = userService.get(id);
 *         if (user == null) {
 *             return Response.status(404).build();
 *         }
 *         return Response.ok(user).build();
 *     }
 *     
 *     @POST
 *     @Consumes(MediaType.APPLICATION_JSON)
 *     public Response create(User user) {
 *         userService.create(user);
 *         return Response.status(201).entity(user).build();
 *     }
 * }
 * }</pre>
 * 
 * <h2>vs JaxRsPlugin</h2>
 * <ul>
 *   <li>JaxRsPlugin - 轻量，只用注解，LiteJava 处理路由</li>
 *   <li>JerseyPlugin - 完整 JAX-RS 运行时，支持过滤器、拦截器、异步等</li>
 * </ul>
 * 
 * <h2>支持的 JAX-RS 特性</h2>
 * <ul>
 *   <li>完整的 @Path, @GET, @POST 等注解</li>
 *   <li>@Context 注入 (UriInfo, HttpHeaders, SecurityContext)</li>
 *   <li>ContainerRequestFilter / ContainerResponseFilter</li>
 *   <li>ExceptionMapper</li>
 *   <li>MessageBodyReader / MessageBodyWriter</li>
 *   <li>异步处理 (@Suspended AsyncResponse)</li>
 * </ul>
 */
public class JerseyPlugin extends Plugin {
    
    public ResourceConfig resourceConfig;
    public ApplicationHandler handler;
    public String basePath = "/api";
    
    public JerseyPlugin() {
        this.resourceConfig = new ResourceConfig();
    }
    
    public JerseyPlugin(ResourceConfig config) {
        this.resourceConfig = config;
    }
    
    /**
     * 设置基础路径
     */
    public JerseyPlugin path(String path) {
        this.basePath = path.startsWith("/") ? path : "/" + path;
        return this;
    }
    
    /**
     * 扫描包
     */
    public JerseyPlugin packages(String... packages) {
        resourceConfig.packages(packages);
        return this;
    }
    
    /**
     * 注册 Resource 类
     */
    public JerseyPlugin register(Class<?> clazz) {
        resourceConfig.register(clazz);
        return this;
    }
    
    /**
     * 注册组件实例
     */
    public JerseyPlugin register(Object component) {
        resourceConfig.register(component);
        return this;
    }
    
    @Override
    public void config() {
        // 从配置读取
        basePath = app.conf.getString("jersey", "path", basePath);
        String packages = app.conf.getString("jersey", "packages", null);
        if (packages != null) {
            resourceConfig.packages(packages.split(","));
        }
        
        // 初始化 Jersey
        handler = new ApplicationHandler(resourceConfig);
        
        // 注册通配符路由，将请求转发给 Jersey
        String routePath = basePath.endsWith("/") ? basePath + "*path" : basePath + "/*path";
        app.route("*", routePath, this::handleRequest);
        
        // 也处理基础路径本身
        app.route("*", basePath, this::handleRequest);
        
        app.log.info("JerseyPlugin configured at " + basePath);
    }

    private void handleRequest(Context ctx) {
        try {
            // 构建 Jersey 请求
            String requestUri = ctx.path;
            if (ctx.query != null && !ctx.query.isEmpty()) {
                requestUri += "?" + ctx.query;
            }
            
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
            
            // 复制请求头
            for (java.util.Map.Entry<String, String> entry : ctx.headers.entrySet()) {
                request.header(entry.getKey(), entry.getValue());
            }
            
            // 设置请求体
            byte[] body = ctx.getRawData();
            if (body != null && body.length > 0) {
                request.setEntityStream(new ByteArrayInputStream(body));
            }
            
            // 处理请求
            ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
            
            java.util.concurrent.Future<ContainerResponse> future = handler.apply(request, responseBody);
            ContainerResponse response = future.get();
            
            // 复制响应
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
            ctx.status(500);
            ctx.json(java.util.Map.of("error", e.getMessage()));
        }
    }
    
    private SecurityContext getSecurityContext(Context ctx) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return null;
            }
            
            @Override
            public boolean isUserInRole(String role) {
                return false;
            }
            
            @Override
            public boolean isSecure() {
                return false;
            }
            
            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        };
    }
}
