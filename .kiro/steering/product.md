# LiteJava Framework

**Java 版的 Gin** - 轻量级 Web 框架，追求简洁、单一、高效。

## 目标

取代 Spring Boot 风格，提供 Go/Gin 风格的 Java Web 开发体验。

## Core Philosophy

- **Minimal Annotations** - 路由/中间件/配置用代码，DI/ORM/验证可用标准注解 (JSR-330, JPA, Bean Validation)
- **No Getter/Setter** - 使用 public 字段或 Map
- **No Magic** - 所见即所得，无隐藏规则
- **Explicit over Implicit** - 显式优于隐式
- **Composition over Inheritance** - 组合优于继承

## Annotation Policy

框架对注解的态度：**控制边界，而非完全禁止**

| 层级 | 注解策略 | 说明 |
|------|----------|------|
| 路由/中间件 | ❌ 不用 | 代码即配置，`app.get("/users", handler)` |
| 配置 | ❌ 不用 | 配置文件 + 代码读取 |
| DI | ✅ 可选 | `@Inject`, `@Singleton`, `@Named` (JSR-330) |
| ORM | ✅ 可选 | `@Entity`, `@Table`, `@Column` (JPA) |
| 验证 | ✅ 可选 | `@NotNull`, `@Size` (Bean Validation) |

**反对的是**：Spring 式注解泛滥，一个类堆十几个注解，隐藏规则太多
**接受的是**：数据层/基础设施层的标准注解，简单明确

## Key Features

- Koa-style 洋葱中间件模型
- Gin-style 路由 (分组、通配符、参数)
- 内置 JSON 处理 (零依赖)
- 插件系统 (数据库、缓存、模板等)
- 配置系统 (.properties / YAML)
- 核心模块零外部依赖

## vs Spring Boot

| 对比项 | Spring Boot | LiteJava |
|--------|-------------|----------|
| 启动时间 | 3-10秒 | <500ms |
| 内存占用 | 200-500MB | 30-80MB |
| JAR 大小 | 30-100MB | <1MB |
| 配置方式 | 注解+YAML | 代码 |
| 依赖数量 | 100+ | 0 (core) |

## Quick Start

```java
App app = LiteJava.create();
app.get("/", ctx -> ctx.json(Map.of("msg", "Hello")));
app.run();
```
