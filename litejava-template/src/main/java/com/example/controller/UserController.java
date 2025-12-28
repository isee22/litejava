package com.example.controller;

import com.example.model.User;
import com.example.service.UserService;
import litejava.Context;
import litejava.Routes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    public Routes routes() {
        return new Routes()
            // 页面路由
            .get("/users", this::listPage)
            .get("/users/new", this::newPage)
            .get("/users/:id", this::detailPage)
            .get("/users/:id/edit", this::editPage)
            // API 路由
            .get("/api/users", this::list)
            .get("/api/users/:id", this::get)
            .post("/api/users", this::create)
            .put("/api/users/:id", this::update)
            .delete("/api/users/:id", this::delete)
            .end();
    }
    
    // ==================== 页面 ====================
    
    void listPage(Context ctx) {
        List<User> users = userService.findAll();
        Map<String, Object> data = new HashMap<>();
        data.put("title", "用户列表");
        data.put("users", users);
        ctx.render("users/list", data);
    }
    
    void newPage(Context ctx) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "新建用户");
        ctx.render("users/form", data);
    }
    
    void detailPage(Context ctx) {
        Long id = ctx.pathParam("id", Long.class);
        User user = userService.findById(id);
        if (user == null) {
            ctx.status(404).render("error/404", Map.of("message", "用户不存在"));
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("title", user.username);
        data.put("user", user);
        ctx.render("users/detail", data);
    }
    
    void editPage(Context ctx) {
        Long id = ctx.pathParam("id", Long.class);
        User user = userService.findById(id);
        if (user == null) {
            ctx.status(404).render("error/404", Map.of("message", "用户不存在"));
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("title", "编辑用户");
        data.put("user", user);
        ctx.render("users/form", data);
    }
    
    // ==================== API ====================
    
    void list(Context ctx) {
        List<User> users = userService.findAll();
        ctx.ok(Map.of("users", users, "total", users.size()));
    }
    
    void get(Context ctx) {
        Long id = ctx.pathParam("id", Long.class);
        User user = userService.findById(id);
        if (user != null) {
            ctx.ok(user);
        } else {
            ctx.fail(404, -1, "用户不存在");
        }
    }
    
    void create(Context ctx) {
        User user = ctx.bindJSON(User.class);
        if (user.username == null || user.username.isEmpty()) {
            ctx.fail(400, -1, "用户名不能为空");
            return;
        }
        User created = userService.create(user);
        ctx.status(201).ok(created);
    }
    
    void update(Context ctx) {
        Long id = ctx.pathParam("id", Long.class);
        User user = ctx.bindJSON(User.class);
        user.id = id;
        User updated = userService.update(user);
        ctx.ok(updated);
    }
    
    void delete(Context ctx) {
        Long id = ctx.pathParam("id", Long.class);
        if (userService.delete(id)) {
            ctx.ok("删除成功");
        } else {
            ctx.fail(404, -1, "用户不存在");
        }
    }
}
