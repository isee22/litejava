package com.example.controller;

import litejava.Context;
import litejava.Routes;

import java.util.HashMap;
import java.util.Map;

/**
 * 首页控制器
 */
public class HomeController {
    
    public Routes routes() {
        return new Routes()
            .get("/", this::index)
            .get("/about", this::about)
            .end();
    }
    
    /**
     * 首页
     */
    void index(Context ctx) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "LiteJava Template");
        data.put("message", "Welcome to LiteJava!");
        ctx.render("index", data);
    }
    
    /**
     * 关于页面
     */
    void about(Context ctx) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "About");
        ctx.render("about", data);
    }
}
