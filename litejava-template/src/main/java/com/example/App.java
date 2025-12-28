package com.example;

import com.example.controller.HomeController;
import com.example.controller.UserController;
import com.example.service.UserService;
import litejava.plugins.LiteJava;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.dataSource.HikariPlugin;
import litejava.plugins.view.ThymeleafPlugin;

/**
 * LiteJava 应用入口
 * 
 * 通过 application.yml 配置启用的插件:
 * - health: 健康检查 /health
 * - cors: 跨域支持
 * - recovery: 异常恢复
 * - static: 静态文件 /static/*
 */
public class App {
    
    public static void main(String[] args) {
        // 创建应用 (自动加载 AutoConfigPlugin)
        litejava.App app = LiteJava.create();
        
        // 数据库 (MyBatis)
        HikariPlugin hikari = new HikariPlugin();
        app.use(hikari);
        MyBatisPlugin mybatis = new MyBatisPlugin(hikari);
        app.use(mybatis);
        
        // 模板引擎 (Thymeleaf)
        app.use(new ThymeleafPlugin());
        
        // 服务
        UserService userService = new UserService(mybatis);
        
        // 路由
        app.register(new HomeController().routes());
        app.register(new UserController(userService).routes());
        
        // 启动
        app.run();
    }
}
