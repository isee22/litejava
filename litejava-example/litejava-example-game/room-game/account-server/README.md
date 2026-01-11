# Account Server - 账号服务器

## 职责

账号服务器是独立的认证和用户数据服务，负责：
- 用户登录/注册/鉴权
- 游客登录
- 玩家数据管理（金币、钻石、等级等）
- 签到系统
- 商城系统
- 礼物系统
- 排行榜
- 好友系统

## 迁移自 BabyKylin

本模块从 Node.js 版本 `account_server` 迁移而来，包含以下接口：

| 原接口 | Java 接口 | 说明 |
|-------|----------|------|
| /guest | /guest | 游客登录 |
| /auth | /api/login | 账号密码登录 |
| /register | /api/register | 注册 |
| /get_version | /get_version | 获取版本 |
| /get_serverinfo | /get_serverinfo | 获取服务器信息 |
| /base_info | /base_info | 获取用户基本信息 |
| /get_user_info | /get_user_info | 获取用户详细信息 (dealer_api) |
| /add_user_gems | /add_user_gems | 添加钻石 (dealer_api) |

## 配置

`src/main/resources/application.yml`:
```yaml
server:
  id: account-1
  httpPort: 8101

app:
  version: "1.0.0"
  web: ""
  priKey: game_secret_key

hall:
  ip: localhost
  port: 7100

mybatis:
  url: jdbc:mysql://localhost:3306/game
  username: root
  password: 123456
```

## API 接口

### 游客登录
```
GET /guest?account={deviceId}
```
响应：
```json
{
  "account": "guest_xxx",
  "halladdr": "localhost:7100",
  "sign": "md5签名"
}
```

### 账号登录
```
POST /api/login
{"username": "xxx", "password": "xxx"}
```

### 注册
```
POST /api/register
{"username": "xxx", "password": "xxx"}
```

### 获取版本
```
GET /get_version
```

### 获取服务器信息
```
GET /get_serverinfo
```

### 获取用户基本信息
```
GET /base_info?userid={userId}
```

### 获取用户详细信息
```
GET /get_user_info?userid={userId}
```

### 添加钻石
```
GET /add_user_gems?userid={userId}&gems={amount}
```

## 启动

```bash
mvn exec:java -pl account-server -Dexec.mainClass=game.account.AccountServer
```
