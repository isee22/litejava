package example.controller;

import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户 Controller - 使用 Spring MVC 注解
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private static final Map<Long, Map<String, Object>> users = new ConcurrentHashMap<>();
    private static final AtomicLong idGen = new AtomicLong(0);
    
    static {
        createUser("alice", "alice@example.com");
        createUser("bob", "bob@example.com");
    }
    
    @GetMapping
    public Map<String, Object> list() {
        return Map.of("list", new ArrayList<>(users.values()));
    }
    
    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable long id) {
        Map<String, Object> user = users.get(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }
    
    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String email = (String) body.get("email");
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        return createUser(name, email);
    }
    
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> user = users.get(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (body.containsKey("name")) user.put("name", body.get("name"));
        if (body.containsKey("email")) user.put("email", body.get("email"));
        return user;
    }
    
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable long id) {
        if (users.remove(id) == null) {
            throw new RuntimeException("User not found");
        }
        return Map.of("deleted", true);
    }
    
    private static Map<String, Object> createUser(String name, String email) {
        long id = idGen.incrementAndGet();
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", id);
        user.put("name", name);
        user.put("email", email);
        users.put(id, user);
        return user;
    }
}
