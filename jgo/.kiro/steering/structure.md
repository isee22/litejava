# Project Structure

```
litejava/
├── pom.xml                      # Parent POM
├── litejava-core/               # Core module (zero dependencies)
│   ├── pom.xml
│   └── src/main/java/litejava/
│       ├── App.java             # 应用容器（纯插件管理）
│       ├── Context.java         # 请求上下文
│       ├── Handler.java         # 处理器接口
│       ├── Plugin.java          # 插件基类
│       ├── MiddlewarePlugin.java # 中间件插件
│       ├── exception/           # 框架异常
│       └── plugin/              # 内置插件
│           ├── RouterPlugin.java    # 路由插件（可继承扩展）
│           ├── RadixTree.java       # Radix Tree 路由数据结构
│           ├── Route.java           # 路由定义
│           ├── Routes.java          # 路由收集器
│           ├── ConfPlugin.java      # 配置 (.properties)
│           ├── LogPlugin.java       # 日志
│           ├── ServerPlugin.java    # 服务器基类
│           ├── StaticFilePlugin.java # 静态文件
│           ├── JsonPlugin.java      # JSON 基类
│           └── ViewPlugin.java      # 视图基类
│
└── litejava-plugins/            # 可选插件 (有外部依赖)
    ├── pom.xml
    └── src/main/java/litejava/plugins/
        ├── config/              # YamlConfPlugin
        ├── database/            # DatabasePlugin, MyBatisPlugin
        ├── cache/               # RedisCachePlugin, MemcacheCachePlugin
        ├── view/                # ThymeleafPlugin, FreemarkerPlugin
        ├── http/                # CorsPlugin, RecoveryPlugin
        ├── health/              # HealthPlugin
        └── server/              # NettyServerPlugin, UndertowServerPlugin
```

## Core Concepts

- `LiteJava.create()` - 创建应用 (预装 YamlConf + Jackson + MemoryCache + HttpServer)
- `new App()` - 创建裸 App，手动配置所有插件
- `app.run()` - 启动服务器
- `Plugin` - 插件基类: `config()` / `uninstall()`
- `MiddlewarePlugin` - 中间件插件 (Koa-style onion model)

## Package Convention

使用 `litejava` 扁平包名：
- 更短的 import：`import litejava.App;`
- 符合框架"简洁直接"的设计理念
