package example.controller;

import example.service.UserService;
import litejava.Context;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.Map;

/**
 * 页面控制器 - 模板渲染和重定向示例
 */
@RestController
public class PageController {
    
    @Inject
    private UserService userService;
    
    /**
     * 首页 - 渲染模板
     * GET /
     */
    @GetMapping("/")
    public void index(Context ctx) {
        ctx.render("index", Map.of(
            "title", "LiteJava Spring Boot Style",
            "users", userService.findAll()
        ));
    }
    
    /**
     * 重定向示例
     * GET /old-page -> /
     */
    @GetMapping("/old-page")
    public void redirect(Context ctx) {
        ctx.redirect("/");
    }
    
    /**
     * 设置响应头示例
     * GET /download
     */
    @GetMapping("/download")
    public void download(Context ctx) {
        ctx.header("Content-Disposition", "attachment; filename=data.json");
        ctx.header("Content-Type", "application/json");
        ctx.json(Map.of("data", userService.findAll()));
    }
    
    /**
     * 健康检查
     * GET /health
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis()
        );
    }
}
