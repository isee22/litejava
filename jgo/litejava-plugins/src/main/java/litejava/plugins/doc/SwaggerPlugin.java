package litejava.plugins.doc;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.integration.GenericOpenApiContext;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import litejava.*;

import java.util.*;

/**
 * Swagger UI 插件 - 使用 swagger-core 生成 OpenAPI 3.0 文档
 * 
 * <h2>快速开始</h2>
 * <pre>{@code
 * // 1. 注册插件
 * app.use(new SwaggerPlugin("My API", "1.0.0").scan(AuthController.class, UserController.class));
 * 
 * // 2. 访问文档
 * // Swagger UI: http://localhost:8080/swagger-ui
 * // OpenAPI JSON: http://localhost:8080/swagger-ui/openapi.json
 * }</pre>
 * 
 * <h2>支持的 Swagger 注解</h2>
 * 
 * <h3>类级别注解</h3>
 * <pre>{@code
 * @Tag(name = "用户管理", description = "用户相关接口")
 * public class UserController { ... }
 * }</pre>
 * 
 * <h3>方法级别注解</h3>
 * <pre>{@code
 * @Operation(
 *     summary = "创建用户",           // 接口摘要（必填）
 *     description = "创建新用户账号",  // 详细描述
 *     tags = {"用户管理"},            // 标签分组
 *     deprecated = false             // 是否废弃
 * )
 * void createUser(Context ctx) { ... }
 * }</pre>
 * 
 * <h3>响应注解</h3>
 * <pre>{@code
 * @ApiResponse(responseCode = "200", description = "成功",
 *     content = @Content(schema = @Schema(implementation = User.class)))
 * @ApiResponse(responseCode = "400", description = "参数错误")
 * @ApiResponse(responseCode = "401", description = "未授权")
 * @ApiResponse(responseCode = "404", description = "用户不存在")
 * void getUser(Context ctx) { ... }
 * 
 * // 或使用 @ApiResponses 包装多个响应
 * @ApiResponses({
 *     @ApiResponse(responseCode = "200", description = "成功"),
 *     @ApiResponse(responseCode = "500", description = "服务器错误")
 * })
 * }</pre>
 * 
 * <h3>参数注解</h3>
 * <pre>{@code
 * @Parameter(name = "id", description = "用户ID", required = true, in = ParameterIn.PATH)
 * @Parameter(name = "page", description = "页码", in = ParameterIn.QUERY)
 * @Parameter(name = "Authorization", description = "认证令牌", in = ParameterIn.HEADER)
 * void getUser(Context ctx) { ... }
 * }</pre>
 * 
 * <h3>请求体注解</h3>
 * <pre>{@code
 * @RequestBody(description = "用户信息", required = true,
 *     content = @Content(schema = @Schema(implementation = CreateUserDTO.class)))
 * void createUser(Context ctx) { ... }
 * }</pre>
 * 
 * <h3>数据模型注解</h3>
 * <pre>{@code
 * @Schema(description = "用户信息")
 * public class User {
 *     @Schema(description = "用户ID", example = "1")
 *     public Long id;
 *     
 *     @Schema(description = "用户名", example = "john", required = true)
 *     public String username;
 *     
 *     @Schema(description = "邮箱", example = "john@example.com")
 *     public String email;
 *     
 *     @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
 *     public Date createdAt;
 * }
 * }</pre>
 * 
 * <h2>完整示例</h2>
 * <pre>{@code
 * import io.swagger.v3.oas.annotations.*;
 * import io.swagger.v3.oas.annotations.media.*;
 * import io.swagger.v3.oas.annotations.responses.*;
 * import io.swagger.v3.oas.annotations.tags.Tag;
 * 
 * @Tag(name = "认证", description = "用户认证相关接口")
 * public class AuthController {
 *     
 *     @Operation(summary = "用户登录", description = "使用用户名和密码登录系统")
 *     @RequestBody(description = "登录凭证", required = true,
 *         content = @Content(schema = @Schema(implementation = LoginRequest.class)))
 *     @ApiResponse(responseCode = "200", description = "登录成功",
 *         content = @Content(schema = @Schema(implementation = LoginResponse.class)))
 *     @ApiResponse(responseCode = "401", description = "用户名或密码错误")
 *     void login(Context ctx) {
 *         Map<String, Object> body = ctx.bindJSON();
 *         // ... 登录逻辑
 *     }
 *     
 *     @Operation(summary = "获取当前用户")
 *     @Parameter(name = "Authorization", description = "Bearer Token", 
 *         required = true, in = ParameterIn.HEADER)
 *     @ApiResponse(responseCode = "200", description = "成功",
 *         content = @Content(schema = @Schema(implementation = User.class)))
 *     @ApiResponse(responseCode = "401", description = "未授权")
 *     void me(Context ctx) {
 *         // ... 获取当前用户
 *     }
 * }
 * 
 * // 数据模型
 * @Schema(description = "登录请求")
 * public class LoginRequest {
 *     @Schema(description = "用户名", example = "admin", required = true)
 *     public String username;
 *     @Schema(description = "密码", example = "123456", required = true)
 *     public String password;
 * }
 * 
 * @Schema(description = "登录响应")
 * public class LoginResponse {
 *     @Schema(description = "访问令牌")
 *     public String token;
 *     @Schema(description = "过期时间(秒)", example = "3600")
 *     public int expiresIn;
 * }
 * 
 * // 注册路由和文档
 * AuthController auth = new AuthController();
 * app.post("/api/auth/login", auth::login);
 * app.get("/api/auth/me", auth::me);
 * app.use(new SwaggerPlugin("My API", "1.0.0").scan(AuthController.class));
 * }</pre>
 * 
 * <h2>常用注解速查</h2>
 * <table border="1">
 * <tr><th>注解</th><th>位置</th><th>用途</th></tr>
 * <tr><td>@Tag</td><td>类</td><td>接口分组</td></tr>
 * <tr><td>@Operation</td><td>方法</td><td>接口描述</td></tr>
 * <tr><td>@Parameter</td><td>方法</td><td>参数说明</td></tr>
 * <tr><td>@RequestBody</td><td>方法</td><td>请求体说明</td></tr>
 * <tr><td>@ApiResponse</td><td>方法</td><td>响应说明</td></tr>
 * <tr><td>@Schema</td><td>类/字段</td><td>数据模型说明</td></tr>
 * </table>
 * 
 * @see <a href="https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations">Swagger Annotations Wiki</a>
 */
public class SwaggerPlugin extends Plugin {
    
    public String title = "API Documentation";
    public String version = "1.0.0";
    public String path = "/swagger-ui";
    
    private final Set<String> classes = new HashSet<>();
    private String cachedSpec;
    
    public SwaggerPlugin() {}
    public SwaggerPlugin(String title, String version) { this.title = title; this.version = version; }
    
    /** 扫描控制器类的 Swagger 注解 */
    public SwaggerPlugin scan(Class<?>... classes) {
        for (Class<?> c : classes) this.classes.add(c.getName());
        cachedSpec = null;
        return this;
    }
    
    @Override
    public void config() {
        app.get(path + "/openapi.json", ctx -> ctx.data(getSpec().getBytes(), "application/json"));
        app.get(path, ctx -> ctx.html(
            "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + title + "</title>" +
            "<link rel=\"stylesheet\" href=\"https://unpkg.com/swagger-ui-dist@5/swagger-ui.css\">" +
            "</head><body><div id=\"swagger-ui\"></div>" +
            "<script src=\"https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js\"></script>" +
            "<script>SwaggerUIBundle({url:'" + path + "/openapi.json',dom_id:'#swagger-ui'});</script></body></html>"));
    }
    
    private String getSpec() throws Exception {
        if (cachedSpec != null) return cachedSpec;
        
        OpenAPI api = new OpenAPI().info(new Info().title(title).version(version));
        
        if (!classes.isEmpty()) {
            SwaggerConfiguration config = new SwaggerConfiguration().openAPI(api).resourceClasses(classes);
            api = new GenericOpenApiContext<>().openApiConfiguration(config).init().read();
        }
        
        cachedSpec = Json.pretty(api);
        return cachedSpec;
    }
}
