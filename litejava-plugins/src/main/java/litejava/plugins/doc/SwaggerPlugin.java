package litejava.plugins.doc;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.*;
import io.swagger.v3.oas.models.responses.*;
import io.swagger.v3.oas.models.security.*;
import litejava.*;
import litejava.Route;

import java.util.*;
import java.util.regex.*;

/**
 * Swagger UI 插件 - 从路由表自动生成 OpenAPI 3.0 文档
 * 
 * <h2>配置</h2>
 * <pre>
 * # application.yml
 * swagger:
 *   title: My API
 *   version: 1.0.0
 *   path: /swagger-ui
 * </pre>
 * 
 * <h2>使用</h2>
 * <pre>{@code
 * app.use(new SwaggerPlugin());
 * 
 * // 路由会自动生成文档，也可以添加元数据
 * app.get("/users/:id", handler)
 *    .summary("获取用户")
 *    .description("根据ID获取用户详情")
 *    .tag("用户");
 * }</pre>
 * 
 * <h2>访问</h2>
 * <ul>
 * <li>Swagger UI: http://localhost:8080/swagger-ui</li>
 * <li>OpenAPI JSON: http://localhost:8080/swagger-ui/openapi.json</li>
 * </ul>
 */
public class SwaggerPlugin extends Plugin {
    
    public String title = "API Documentation";
    public String version = "1.0.0";
    public String description = "";
    public String path = "/swagger-ui";
    
    public String cachedSpec;
    
    public SwaggerPlugin() {}
    public SwaggerPlugin(String title, String version) { 
        this.title = title; 
        this.version = version; 
    }
    
    @Override
    public void config() {
        title = app.conf.getString("swagger", "title", title);
        version = app.conf.getString("swagger", "version", version);
        description = app.conf.getString("swagger", "description", description);
        path = app.conf.getString("swagger", "path", path);
        
        // 注册路由
        app.get(path + "/openapi.json", ctx -> ctx.data(getSpec().getBytes("UTF-8"), "application/json"));
        app.get(path, ctx -> ctx.html(getSwaggerUI()));
    }
    
    @Override
    public void onStart() {
        int port = app.conf.getInt("server", "port", 8080);
        app.log.info("Swagger UI: http://localhost:" + port + path);
    }

    
    public String getSpec() throws Exception {
        if (cachedSpec != null) return cachedSpec;
        
        OpenAPI api = new OpenAPI();
        api.info(new Info().title(title).version(version).description(description));
        
        // 添加 Bearer Token 认证
        api.components(new Components().addSecuritySchemes("bearerAuth",
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")));
        
        // 从路由表生成 paths
        Paths paths = new Paths();
        Map<String, Set<String>> tagMap = new LinkedHashMap<>();
        
        for (Route route : app.router.getAllRoutes()) {
            // 跳过 swagger 自身的路由
            if (route.path.startsWith(path)) continue;
            
            String openApiPath = convertPath(route.path);
            PathItem pathItem = paths.get(openApiPath);
            if (pathItem == null) {
                pathItem = new PathItem();
                paths.addPathItem(openApiPath, pathItem);
            }
            
            Operation op = createOperation(route);
            
            // 收集 tags
            if (route.tags != null) {
                for (String tag : route.tags) {
                    tagMap.computeIfAbsent(tag, k -> new LinkedHashSet<>());
                }
            }
            
            // 设置到对应 HTTP 方法
            switch (route.method) {
                case "GET": pathItem.get(op); break;
                case "POST": pathItem.post(op); break;
                case "PUT": pathItem.put(op); break;
                case "DELETE": pathItem.delete(op); break;
                case "PATCH": pathItem.patch(op); break;
                case "HEAD": pathItem.head(op); break;
                case "OPTIONS": pathItem.options(op); break;
                case "ANY":
                    pathItem.get(op);
                    pathItem.post(cloneOperation(op));
                    pathItem.put(cloneOperation(op));
                    pathItem.delete(cloneOperation(op));
                    break;
            }
        }
        
        api.paths(paths);
        
        // 添加 tags
        if (!tagMap.isEmpty()) {
            List<io.swagger.v3.oas.models.tags.Tag> tags = new ArrayList<>();
            for (String name : tagMap.keySet()) {
                tags.add(new io.swagger.v3.oas.models.tags.Tag().name(name));
            }
            api.tags(tags);
        }
        
        cachedSpec = Json.pretty(api);
        return cachedSpec;
    }
    
    Operation createOperation(Route route) {
        Operation op = new Operation();
        
        // 尝试从注解读取
        java.lang.reflect.Method method = findMethod(route);
        io.swagger.v3.oas.annotations.Operation opAnnotation = null;
        io.swagger.v3.oas.annotations.tags.Tag classTag = null;
        io.swagger.v3.oas.annotations.Parameter[] paramAnnotations = null;
        
        if (method != null) {
            opAnnotation = method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class);
            // 获取方法上的 @Parameter 注解
            paramAnnotations = method.getAnnotationsByType(io.swagger.v3.oas.annotations.Parameter.class);
        }
        if (route.controllerClass != null) {
            classTag = route.controllerClass.getAnnotation(io.swagger.v3.oas.annotations.tags.Tag.class);
        }
        
        // 基本信息（优先注解，其次 Route 字段，最后自动生成）
        String summary = null;
        String desc = null;
        
        if (opAnnotation != null) {
            if (!opAnnotation.summary().isEmpty()) summary = opAnnotation.summary();
            if (!opAnnotation.description().isEmpty()) desc = opAnnotation.description();
        }
        if (summary == null && route.summary != null) summary = route.summary;
        if (desc == null && route.description != null) desc = route.description;
        if (summary == null) summary = generateSummary(route);
        
        op.summary(summary);
        if (desc != null) op.description(desc);
        
        // Tags（优先注解，其次 Route 字段，最后自动生成）
        List<String> tags = new ArrayList<>();
        if (opAnnotation != null && opAnnotation.tags().length > 0) {
            for (String t : opAnnotation.tags()) {
                if (!t.isEmpty()) tags.add(t);
            }
        }
        if (tags.isEmpty() && classTag != null && !classTag.name().isEmpty()) {
            tags.add(classTag.name());
        }
        if (tags.isEmpty() && route.tags != null && route.tags.length > 0) {
            tags.addAll(Arrays.asList(route.tags));
        }
        if (tags.isEmpty()) {
            String tag = guessTag(route.path);
            if (tag != null) tags.add(tag);
        }
        if (!tags.isEmpty()) op.tags(tags);
        
        // 构建参数名到注解的映射
        Map<String, io.swagger.v3.oas.annotations.Parameter> paramMap = new HashMap<>();
        if (paramAnnotations != null) {
            for (io.swagger.v3.oas.annotations.Parameter pa : paramAnnotations) {
                if (!pa.name().isEmpty()) {
                    paramMap.put(pa.name(), pa);
                }
            }
        }
        
        // 路径参数
        List<String> pathParams = extractPathParams(route.path);
        for (String param : pathParams) {
            Parameter p = new PathParameter()
                .name(param)
                .required(true)
                .schema(new StringSchema());
            
            // 从注解读取描述
            io.swagger.v3.oas.annotations.Parameter pa = paramMap.get(param);
            if (pa != null && !pa.description().isEmpty()) {
                p.description(pa.description());
            }
            op.addParametersItem(p);
        }
        
        // 从注解添加 Query 参数
        if (paramAnnotations != null) {
            for (io.swagger.v3.oas.annotations.Parameter pa : paramAnnotations) {
                if (pa.in() == io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY) {
                    Parameter p = new QueryParameter()
                        .name(pa.name())
                        .required(pa.required())
                        .schema(new StringSchema());
                    if (!pa.description().isEmpty()) {
                        p.description(pa.description());
                    }
                    op.addParametersItem(p);
                }
            }
        }
        
        // Request Body（POST/PUT/PATCH）
        if ("POST".equals(route.method) || "PUT".equals(route.method) || "PATCH".equals(route.method)) {
            RequestBody body = new RequestBody();
            MediaType mediaType = new MediaType();
            
            // 尝试从 @Operation 的 requestBody 获取 schema
            ObjectSchema schema = new ObjectSchema();
            if (opAnnotation != null && opAnnotation.requestBody() != null) {
                io.swagger.v3.oas.annotations.parameters.RequestBody rb = opAnnotation.requestBody();
                if (!rb.description().isEmpty()) {
                    body.description(rb.description());
                }
                body.required(rb.required());
            }
            
            mediaType.schema(schema);
            body.content(new Content().addMediaType("application/json", mediaType));
            op.requestBody(body);
        }
        
        // Responses - 从 @ApiResponses 注解读取
        ApiResponses responses = new ApiResponses();
        io.swagger.v3.oas.annotations.responses.ApiResponses apiResponsesAnnotation = null;
        if (method != null) {
            apiResponsesAnnotation = method.getAnnotation(io.swagger.v3.oas.annotations.responses.ApiResponses.class);
        }
        
        if (apiResponsesAnnotation != null && apiResponsesAnnotation.value().length > 0) {
            for (io.swagger.v3.oas.annotations.responses.ApiResponse ar : apiResponsesAnnotation.value()) {
                ApiResponse response = new ApiResponse().description(ar.description());
                
                // 处理 content
                if (ar.content().length > 0) {
                    Content content = new Content();
                    for (io.swagger.v3.oas.annotations.media.Content c : ar.content()) {
                        MediaType mediaType = new MediaType();
                        // 处理 examples
                        if (c.examples().length > 0) {
                            Map<String, io.swagger.v3.oas.models.examples.Example> examples = new LinkedHashMap<>();
                            int idx = 0;
                            for (io.swagger.v3.oas.annotations.media.ExampleObject ex : c.examples()) {
                                io.swagger.v3.oas.models.examples.Example example = new io.swagger.v3.oas.models.examples.Example();
                                if (!ex.value().isEmpty()) {
                                    example.setValue(ex.value());
                                }
                                if (!ex.name().isEmpty()) {
                                    examples.put(ex.name(), example);
                                } else {
                                    examples.put("example" + (idx++), example);
                                }
                            }
                            mediaType.setExamples(examples);
                        }
                        String mt = c.mediaType().isEmpty() ? "application/json" : c.mediaType();
                        content.addMediaType(mt, mediaType);
                    }
                    response.content(content);
                }
                
                responses.addApiResponse(ar.responseCode(), response);
            }
        } else {
            responses.addApiResponse("200", new ApiResponse().description("成功"));
        }
        op.responses(responses);
        
        // 需要认证的路由
        if (needsAuth(route.path)) {
            op.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
        }
        
        return op;
    }
    
    /** 根据 Route 信息查找对应的 Method */
    java.lang.reflect.Method findMethod(Route route) {
        if (route.controllerClass == null || route.methodName == null) return null;
        
        try {
            for (java.lang.reflect.Method m : route.controllerClass.getDeclaredMethods()) {
                if (m.getName().equals(route.methodName)) {
                    return m;
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return null;
    }

    
    Operation cloneOperation(Operation op) {
        Operation clone = new Operation();
        clone.summary(op.getSummary());
        clone.description(op.getDescription());
        clone.tags(op.getTags());
        clone.parameters(op.getParameters());
        clone.requestBody(op.getRequestBody());
        clone.responses(op.getResponses());
        clone.security(op.getSecurity());
        return clone;
    }
    
    /** 转换路径格式：:id -> {id} */
    String convertPath(String path) {
        return path.replaceAll(":([a-zA-Z][a-zA-Z0-9]*)", "{$1}")
                   .replaceAll("\\*([a-zA-Z][a-zA-Z0-9]*)", "{$1}");
    }
    
    /** 提取路径参数 */
    List<String> extractPathParams(String path) {
        List<String> params = new ArrayList<>();
        Matcher m = Pattern.compile(":([a-zA-Z][a-zA-Z0-9]*)").matcher(path);
        while (m.find()) params.add(m.group(1));
        m = Pattern.compile("\\*([a-zA-Z][a-zA-Z0-9]*)").matcher(path);
        while (m.find()) params.add(m.group(1));
        return params;
    }
    
    /** 从路径猜测 tag */
    String guessTag(String path) {
        String[] parts = path.split("/");
        for (String part : parts) {
            if (!part.isEmpty() && !part.startsWith(":") && !part.startsWith("*")) {
                if ("api".equals(part) || "v1".equals(part) || "v2".equals(part)) continue;
                return capitalize(part);
            }
        }
        return null;
    }
    
    /** 生成默认 summary */
    String generateSummary(Route route) {
        String path = route.path;
        String method = route.method;
        
        // 提取资源名
        String[] parts = path.split("/");
        String resource = null;
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (!part.isEmpty() && !part.startsWith(":") && !part.startsWith("*")) {
                resource = part;
                break;
            }
        }
        if (resource == null) resource = "资源";
        
        // 根据方法和路径生成描述
        boolean hasId = path.contains(":id") || path.contains(":") || path.contains("*");
        switch (method) {
            case "GET": return hasId ? "获取" + resource + "详情" : "获取" + resource + "列表";
            case "POST": return "创建" + resource;
            case "PUT": return "更新" + resource;
            case "DELETE": return "删除" + resource;
            case "PATCH": return "部分更新" + resource;
            default: return method + " " + path;
        }
    }
    
    /** 判断是否需要认证 */
    boolean needsAuth(String path) {
        // 公开接口
        if (path.equals("/") || path.equals("/health")) return false;
        if (path.startsWith("/api/auth")) return false;
        if (path.startsWith("/uploads")) return false;
        if (path.startsWith("/static")) return false;
        if (path.startsWith(this.path)) return false;
        
        // GET 请求的列表/详情通常公开
        // 但这里保守处理，默认需要认证
        return true;
    }
    
    String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    
    String getSwaggerUI() {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + title + "</title>" +
            "<link rel=\"stylesheet\" href=\"https://unpkg.com/swagger-ui-dist@5/swagger-ui.css\">" +
            "</head><body><div id=\"swagger-ui\"></div>" +
            "<script src=\"https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js\"></script>" +
            "<script>SwaggerUIBundle({url:'" + path + "/openapi.json',dom_id:'#swagger-ui'," +
            "deepLinking:true,presets:[SwaggerUIBundle.presets.apis]});</script></body></html>";
    }
}
