package benchmark;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Spring Boot Benchmark Server - Virtual Threads enabled
 */
@SpringBootApplication
@Controller
public class SpringBootServer {
    
    @Autowired
    private JdbcTemplate jdbc;
    
    public static void main(String[] args) {
        System.setProperty("spring.main.banner-mode", "off");
        System.setProperty("spring.threads.virtual.enabled", "true");
        System.setProperty("spring.mvc.static-path-pattern", "/static/**");
        System.setProperty("spring.web.resources.static-locations", "classpath:/static/");
        System.setProperty("spring.thymeleaf.prefix", "classpath:/templates/");
        System.setProperty("spring.thymeleaf.suffix", "");
        SpringApplication.run(SpringBootServer.class, args);
    }
    
    @GetMapping("/text")
    @ResponseBody
    public String text() { return "Hello, World!"; }
    
    @GetMapping("/json")
    @ResponseBody
    public Map<String, Object> json() {
        return Map.of("message", "Hello, World!", "framework", "Spring Boot", "timestamp", System.currentTimeMillis());
    }
    
    @GetMapping("/users")
    @ResponseBody
    public Map<String, Object> users(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int size) {
        return Map.of("data", jdbc.queryForList("SELECT * FROM users ORDER BY id LIMIT ? OFFSET ?", Math.min(size, 100), (page - 1) * size), "page", page, "size", size);
    }
    
    @GetMapping("/posts")
    @ResponseBody
    public Map<String, Object> posts(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int size) {
        return Map.of("data", jdbc.queryForList("SELECT p.*, u.name as author FROM posts p LEFT JOIN users u ON p.user_id = u.id ORDER BY p.id LIMIT ? OFFSET ?", Math.min(size, 100), (page - 1) * size), "page", page, "size", size);
    }
    
    @GetMapping("/dynamic")
    public String dynamic(Model model) {
        model.addAttribute("framework", "Spring Boot");
        model.addAttribute("users", jdbc.queryForList("SELECT * FROM users ORDER BY id LIMIT 10"));
        return "users.html";
    }
}
