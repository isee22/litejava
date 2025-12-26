package com.example;

import litejava.plugins.DebugPlugin;
import litejava.plugins.LiteJava;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试 DebugPlugin（不需要数据库）
 */
public class TestApp {
    
    public static void main(String[] args) {
        litejava.App app = LiteJava.create();
        
        // 简单路由
        app.get("/", ctx -> {
            Map<String, Object> data = new HashMap<>();
            data.put("msg", "Hello LiteJava!");
            ctx.json(data);
        });
        
        app.run();
    }
}
