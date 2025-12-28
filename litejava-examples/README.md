# LiteJava Examples

LiteJava 框架示例项目集合。

## 示例列表

| 项目 | 说明 | 外部依赖 |
|------|------|----------|
| `litejava-example-custom-app` | 自定义 App 配置 | 无 |
| `litejava-example-guice` | Google Guice 依赖注入 | 无 |
| `litejava-example-jdbc` | JDBC 数据库访问 | MySQL |
| `litejava-example-mybatis` | MyBatis SQL 映射 | MySQL |
| `litejava-example-jpa` | JPA ORM | MySQL |
| `litejava-example-multi-datasource` | 多数据源（主从分离） | 无 (H2) |
| `litejava-example-springmvcAnnotation` | Spring MVC 注解路由 | 无 |
| `litejava-example-jaxrsAnnotation` | JAX-RS 注解路由 | 无 |
| `litejava-example-jerseyAnnotation` | Jersey 运行时 | 无 |
| `litejava-example-springboot` | Spring Boot 集成 | 无 |
| `litejava-example-session` | Redis 分布式 Session | Redis |
| `litejava-example-client` | 前端示例 (Vue) | Node.js |

## 运行示例

```bash
# 构建
mvn package -pl litejava-examples/litejava-example-jdbc -am -DskipTests -q

# 运行
java -jar litejava-examples/litejava-example-jdbc/target/litejava-example-jdbc-1.0.0-jdk8.jar
```

## 数据库配置

需要 MySQL 的示例使用以下默认配置：

```yaml
datasource:
  url: jdbc:mysql://localhost:3306/litejava?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
  username: root
  password: 123456
```

创建数据库：
```sql
CREATE DATABASE litejava CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
