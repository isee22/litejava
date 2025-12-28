package example.controller;

import litejava.Context;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Spring MVC 注解测试 Controller
 * 
 * 测试各种注解场景：
 * - @PathVariable 单参数/多参数/显式指定
 * - @RequestParam 必填/可选/默认值
 * - @RequestHeader 请求头
 * - @RequestBody JSON 请求体
 * - 混合参数
 */
@RestController
@RequestMapping("/test")
public class AnnotationTestController {
    
    // ==================== @PathVariable 测试 ====================
    
    /**
     * 单个路径变量 - 自动映射（从路径模板推断）
     * GET /test/path/auto/123
     */
    @GetMapping("/path/auto/{id}")
    public Map<String, Object> pathVariableAuto(@PathVariable Long id) {
        return Map.of("type", "auto", "id", id);
    }
    
    /**
     * 单个路径变量 - 显式指定名称
     * GET /test/path/explicit/123
     */
    @GetMapping("/path/explicit/{userId}")
    public Map<String, Object> pathVariableExplicit(@PathVariable("userId") Long id) {
        return Map.of("type", "explicit", "userId", id);
    }
    
    /**
     * 多个路径变量 - 自动映射（按顺序）
     * GET /test/path/multi/123/456
     */
    @GetMapping("/path/multi/{userId}/{postId}")
    public Map<String, Object> pathVariableMultiAuto(@PathVariable Long userId, @PathVariable Long postId) {
        return Map.of("type", "multi-auto", "userId", userId, "postId", postId);
    }
    
    /**
     * 多个路径变量 - 显式指定（可以乱序）
     * GET /test/path/multi-explicit/123/456
     */
    @GetMapping("/path/multi-explicit/{userId}/{postId}")
    public Map<String, Object> pathVariableMultiExplicit(
            @PathVariable("postId") Long post, 
            @PathVariable("userId") Long user) {
        return Map.of("type", "multi-explicit", "userId", user, "postId", post);
    }
    
    /**
     * 路径变量 - 字符串类型
     * GET /test/path/string/hello
     */
    @GetMapping("/path/string/{name}")
    public Map<String, Object> pathVariableString(@PathVariable String name) {
        return Map.of("type", "string", "name", name);
    }
    
    // ==================== @RequestParam 测试 ====================
    
    /**
     * 查询参数 - 必填（显式指定名称）
     * GET /test/query/required?name=test
     */
    @GetMapping("/query/required")
    public Map<String, Object> queryParamRequired(@RequestParam("name") String name) {
        return Map.of("type", "required", "name", name);
    }
    
    /**
     * 查询参数 - 可选（required=false）
     * GET /test/query/optional?name=test 或 GET /test/query/optional
     */
    @GetMapping("/query/optional")
    public Map<String, Object> queryParamOptional(@RequestParam(value = "name", required = false) String name) {
        return Map.of("type", "optional", "name", name != null ? name : "null");
    }
    
    /**
     * 查询参数 - 默认值
     * GET /test/query/default 或 GET /test/query/default?page=2
     */
    @GetMapping("/query/default")
    public Map<String, Object> queryParamDefault(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return Map.of("type", "default", "page", page, "size", size);
    }
    
    /**
     * 查询参数 - 显式指定名称（下划线风格）
     * GET /test/query/explicit?user_name=test
     */
    @GetMapping("/query/explicit")
    public Map<String, Object> queryParamExplicit(@RequestParam("user_name") String userName) {
        return Map.of("type", "explicit", "userName", userName);
    }
    
    /**
     * 多个查询参数
     * GET /test/query/multi?name=test&age=18&active=true
     */
    @GetMapping("/query/multi")
    public Map<String, Object> queryParamMulti(
            @RequestParam("name") String name,
            @RequestParam("age") int age,
            @RequestParam("active") boolean active) {
        return Map.of("type", "multi", "name", name, "age", age, "active", active);
    }
    
    // ==================== @RequestHeader 测试 ====================
    
    /**
     * 请求头 - 自动映射
     * GET /test/header/auto (Header: X-Token: abc123)
     */
    @GetMapping("/header/auto")
    public Map<String, Object> headerAuto(@RequestHeader("X-Token") String token) {
        return Map.of("type", "header", "token", token);
    }
    
    /**
     * 请求头 - 可选
     * GET /test/header/optional
     */
    @GetMapping("/header/optional")
    public Map<String, Object> headerOptional(@RequestHeader(value = "X-Debug", required = false) String debug) {
        return Map.of("type", "header-optional", "debug", debug != null ? debug : "null");
    }
    
    // ==================== @RequestBody 测试 ====================
    
    /**
     * JSON 请求体
     * POST /test/body/json {"name":"test","age":18}
     */
    @PostMapping("/body/json")
    public Map<String, Object> bodyJson(@RequestBody Map<String, Object> body) {
        return Map.of("type", "body", "received", body);
    }
    
    // ==================== 混合参数测试 ====================
    
    /**
     * 路径变量 + 查询参数
     * GET /test/mixed/path-query/123?name=test
     */
    @GetMapping("/mixed/path-query/{id}")
    public Map<String, Object> mixedPathQuery(
            @PathVariable Long id,
            @RequestParam("name") String name) {
        return Map.of("type", "path+query", "id", id, "name", name);
    }
    
    /**
     * 路径变量 + 请求体
     * PUT /test/mixed/path-body/123 {"name":"updated"}
     */
    @PutMapping("/mixed/path-body/{id}")
    public Map<String, Object> mixedPathBody(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return Map.of("type", "path+body", "id", id, "body", body);
    }
    
    /**
     * 路径变量 + 查询参数 + Context
     * GET /test/mixed/all/123?page=2
     */
    @GetMapping("/mixed/all/{id}")
    public Map<String, Object> mixedAll(
            @PathVariable Long id,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Context ctx) {
        return Map.of(
            "type", "all",
            "id", id,
            "page", page,
            "method", ctx.method,
            "path", ctx.path
        );
    }
    
    // ==================== HTTP 方法测试 ====================
    
    /**
     * POST 方法
     */
    @PostMapping("/method/post")
    public Map<String, Object> methodPost(@RequestBody Map<String, Object> body) {
        return Map.of("method", "POST", "body", body);
    }
    
    /**
     * PUT 方法
     */
    @PutMapping("/method/put/{id}")
    public Map<String, Object> methodPut(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Map.of("method", "PUT", "id", id, "body", body);
    }
    
    /**
     * DELETE 方法
     */
    @DeleteMapping("/method/delete/{id}")
    public Map<String, Object> methodDelete(@PathVariable Long id) {
        return Map.of("method", "DELETE", "id", id);
    }
    
    /**
     * PATCH 方法
     */
    @PatchMapping("/method/patch/{id}")
    public Map<String, Object> methodPatch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Map.of("method", "PATCH", "id", id, "body", body);
    }
}
