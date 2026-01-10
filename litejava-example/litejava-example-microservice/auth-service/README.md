# Auth Service

认证服务，负责用户登录、注册和 Token 管理。

## 功能

- 用户登录 (返回 JWT Token)
- 用户注册
- Token 刷新
- Token 验证

## 技术栈

- LiteJava 框架
- JWT (jjwt)
- H2 内存数据库
- BCrypt (密码加密)

## API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /auth/login | 用户登录 | 否 |
| POST | /auth/register | 用户注册 | 否 |
| POST | /auth/refresh | 刷新 Token | 是 |
| POST | /auth/logout | 退出登录 | 是 |
| GET | /health | 健康检查 | 否 |

## 登录流程

```
1. 用户提交 username + password
2. 验证用户名密码
3. 生成 JWT Token (包含 userId, username)
4. 返回 Token 给客户端
5. 客户端后续请求携带 Authorization: Bearer <token>
```

## JWT Token 结构

```json
{
  "sub": "1001",           // userId
  "username": "admin",
  "iat": 1704844800,       // 签发时间
  "exp": 1704931200        // 过期时间 (24小时)
}
```

## 配置

```yaml
server:
  port: 8085

service:
  name: auth-service
  host: auto

jwt:
  secret: "litejava-microservice-jwt-secret-key-32"
  expiration: 86400000    # 24小时 (毫秒)
```

## 启动

```bash
# 本地
java -jar target/auth-service-1.0.0-jdk8-shaded.jar

# Docker
docker-compose up -d auth-service
```

## 调用示例

```bash
# 登录
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 响应
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "admin"
  },
  "msg": "success"
}
```
