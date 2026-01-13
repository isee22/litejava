# Account Server - 账号服务

独立的用户认证和数据管理服务，与游戏逻辑完全解耦。

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│                     AccountServer (8101)                    │
├─────────────────────────────────────────────────────────────┤
│  Controller 层                                              │
│  ├── AuthController      登录/注册/游客                     │
│  ├── PlayerController    玩家数据/金币/钻石                  │
│  ├── FriendController    好友系统                           │
│  ├── RankController      排行榜                             │
│  ├── ShopController      商城/购买                          │
│  ├── SignController      签到                               │
│  └── RoomConfigController 游戏配置 (供 HallServer 调用)      │
├─────────────────────────────────────────────────────────────┤
│  Service 层                                                 │
│  ├── AuthService         认证逻辑                           │
│  ├── PlayerService       玩家业务                           │
│  └── ...                                                    │
├─────────────────────────────────────────────────────────────┤
│  数据层                                                     │
│  ├── Entity              数据库实体                         │
│  ├── Mapper              MyBatis 映射                       │
│  └── VO                  接口返回对象                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │     MySQL       │
                    └─────────────────┘
```

## 功能模块

| 模块 | 接口 | 说明 |
|------|------|------|
| 认证 | `/guest`, `/login`, `/register` | 游客/账号登录、注册 |
| 玩家 | `/player/*` | 获取/更新玩家数据 |
| 好友 | `/friend/*` | 添加/删除好友、好友列表 |
| 排行 | `/rank/*` | 金币/胜场排行榜 |
| 商城 | `/shop/*` | 商品列表、购买 |
| 签到 | `/sign/*` | 每日签到、奖励 |
| 配置 | `/room/config` | 游戏配置 (供内部调用) |

## API 接口

### 游客登录
```http
GET /guest?account={deviceId}

Response:
{
  "account": "guest_xxx",
  "halladdr": "localhost:8201",
  "sign": "md5签名"
}
```

### 账号登录
```http
POST /login
Content-Type: application/json

{"username": "test", "password": "123456"}

Response:
{
  "code": 0,
  "data": {
    "userId": 10001,
    "session": "xxx",
    "name": "玩家名",
    "coins": 10000,
    "gems": 100
  }
}
```

### 注册
```http
POST /register
Content-Type: application/json

{"username": "test", "password": "123456", "name": "昵称"}

Response:
{"code": 0, "data": {"userId": 10001}}
```

### 获取玩家信息
```http
GET /player/{userId}

Response:
{
  "code": 0,
  "data": {
    "userId": 10001,
    "name": "玩家名",
    "coins": 10000,
    "gems": 100,
    "level": 5,
    "exp": 1200
  }
}
```

### 获取游戏配置 (内部接口)
```http
GET /room/config

Response:
{
  "code": 0,
  "data": [
    {"gameType": "doudizhu", "maxPlayers": 3, "minPlayers": 3},
    {"gameType": "mahjong", "maxPlayers": 4, "minPlayers": 4},
    {"gameType": "gobang", "maxPlayers": 2, "minPlayers": 2}
  ]
}
```

## 配置文件

`src/main/resources/application.yml`:
```yaml
server:
  id: account-1
  httpPort: 8101

app:
  version: "1.0.0"
  priKey: game_secret_key    # 签名密钥

hall:
  ip: localhost
  port: 8201

# 数据库配置
datasource:
  url: jdbc:mysql://localhost:3306/room_game?useSSL=false&serverTimezone=UTC
  username: root
  password: 123456
```

## 数据库

初始化脚本: `src/main/resources/sql/init.sql`

主要表:
- `t_user` - 用户账号
- `t_player` - 玩家数据
- `t_friend` - 好友关系
- `t_game_config` - 游戏配置
- `t_shop_item` - 商城商品
- `t_sign_record` - 签到记录

## 启动

```bash
# 确保 MySQL 已启动并执行初始化脚本
mvn exec:java -pl account-server -Dexec.mainClass=game.account.AccountServer
```

## 依赖

```xml
<dependencies>
    <dependency>
        <groupId>litejava</groupId>
        <artifactId>litejava-plugins</artifactId>
    </dependency>
    <!-- 数据库 -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>
</dependencies>
```

**注意**: AccountServer 不依赖 `game-core`，与游戏逻辑完全解耦。
