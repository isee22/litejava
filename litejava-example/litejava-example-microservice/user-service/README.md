# User Service

用户服务，负责用户管理和用户信息查询。

## 功能

- 用户列表查询
- 用户详情查询
- 用户创建/更新/删除
- 用户搜索

## 技术栈

- LiteJava 框架
- H2 内存数据库 (可切换 MySQL)
- Consul (服务注册)
- HikariCP (连接池)

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /user/list | 用户列表 |
| POST | /user/detail | 用户详情 |
| POST | /user/create | 创建用户 |
| POST | /user/update | 更新用户 |
| POST | /user/delete | 删除用户 |
| GET | /health | 健康检查 |

## 数据模型

```java
public class User {
    public Long id;
    public String username;
    public String email;
    public String phone;
    public Integer status;      // 1:正常 0:禁用
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
```

## 配置

```yaml
server:
  port: 8081

service:
  name: user-service
  host: auto           # Docker 自动检测 IP

database:
  url: jdbc:h2:mem:userdb
  username: sa
  password: 

consul:
  host: consul
  port: 8500
```

## 启动

```bash
# 本地
java -jar target/user-service-1.0.0-jdk8-shaded.jar

# Docker
docker-compose up -d user-service
```

## 依赖服务

- Consul (服务注册)
- Gateway (流量入口)
