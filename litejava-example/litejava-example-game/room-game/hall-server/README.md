# Hall Server - 大厅服务

房间调度和匹配服务，负责 GameServer 负载均衡和房间路由。

## 部署模式

### 单机模式 (默认)
- 使用内存缓存
- 无需 Redis
- 适合开发和小规模部署

### 集群模式
- 使用 Redis 共享状态
- 支持多实例水平扩展
- 配置 `redis.host` 启用

```yaml
# application.yml
redis:
  host: localhost
  port: 6379
```

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│                      HallServer (8201)                      │
├─────────────────────────────────────────────────────────────┤
│  对外接口 (客户端)                                           │
│  ├── /create_private_room   创建私人房间                    │
│  ├── /enter_private_room    加入私人房间                    │
│  ├── /match/start           开始匹配                        │
│  ├── /match/cancel          取消匹配                        │
│  └── /get_user_room         获取用户房间状态                 │
├─────────────────────────────────────────────────────────────┤
│  内部接口 (GameServer)                                       │
│  ├── /register_gs           GameServer 注册                 │
│  ├── /heartbeat             心跳上报负载                     │
│  └── /room_destroyed        房间销毁通知                     │
├─────────────────────────────────────────────────────────────┤
│  数据存储 (CachePlugin)                                      │
│  ├── 单机: MemoryCachePlugin                                │
│  └── 集群: RedisCachePlugin                                 │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────┐
│  Redis (集群)   │ ◄── 游戏配置由 AccountServer 写入
│  或 内存 (单机) │
└─────────────────┘
```

## 集群数据流

```
AccountServer                    Redis                      HallServer
     │                            │                            │
     │ 启动时同步配置              │                            │
     │ ─────────────────────────► │                            │
     │  hall:config:doudizhu:normal = 3                        │
     │  hall:config:mahjong:normal = 4                         │
     │                            │                            │
     │                            │ ◄───────────────────────── │
     │                            │   读取配置                  │
     │                            │                            │
     │                            │ ◄───────────────────────── │
     │                            │   存储匹配队列/房间状态     │
```

## 核心功能

### 1. GameServer 注册与负载均衡

```
GameServer 启动
      │
      ▼
GET /register_gs?clientip=x&clientport=9100&httpPort=9101&gameType=doudizhu
      │
      ▼
HallServer 记录服务器信息
      │
      ▼
每 5 秒心跳: GET /heartbeat?id=xxx&load=65
      │
      ▼
创建房间时选择 load 最低的服务器
```

负载计算: `load = rooms * 10 + players`

### 2. 6位数字房间号

- 范围: 100000 - 999999
- 便于玩家口头分享
- 自动检测冲突，循环使用

### 3. 匹配系统

```
玩家 A ──► /match/start ──► 加入队列 "doudizhu:normal"
玩家 B ──► /match/start ──► 加入队列 "doudizhu:normal"
玩家 C ──► /match/start ──► 加入队列 "doudizhu:normal"
                                    │
                                    ▼ 人数足够 (3人)
                              创建房间，返回 token
```

队列配置从 AccountServer `/room/config` 动态获取。

## API 接口

### 创建私人房间
```http
GET /create_private_room?userId=10001&gameType=doudizhu&conf={}

Response:
{
  "code": 0,
  "data": {
    "roomid": "123456",
    "ip": "192.168.1.100",
    "port": 9100,
    "token": "xxx",
    "time": 1234567890,
    "sign": "md5签名"
  }
}
```

### 加入私人房间
```http
GET /enter_private_room?userId=10002&roomId=123456&name=玩家2

Response: (同上)
```

### 开始匹配
```http
POST /match/start
Content-Type: application/json

{
  "userId": 10001,
  "gameType": "doudizhu",
  "level": "normal",
  "name": "玩家名"
}

Response (匹配成功):
{
  "code": 0,
  "data": {
    "status": "matched",
    "roomid": "234567",
    "ip": "192.168.1.100",
    "port": 9100,
    "token": "xxx"
  }
}

Response (等待中):
{
  "code": 0,
  "data": {"status": "matching"}
}
```

### 取消匹配
```http
POST /match/cancel
Content-Type: application/json

{"userId": 10001}

Response:
{"code": 0}
```

### 管理接口
```http
GET /admin/servers        # 查看所有 GameServer 状态
GET /admin/match_queues   # 查看匹配队列状态
```

## 配置文件

`src/main/resources/application.yml`:
```yaml
server:
  id: hall-1
  httpPort: 8201
  priKey: ROOM_PRI_KEY_2024   # 签名密钥

account:
  url: http://localhost:8101  # AccountServer 地址

# 可选 Redis (集群部署时使用)
# redis:
#   host: localhost
#   port: 6379
```

## 启动

```bash
mvn exec:java -pl hall-server -Dexec.mainClass=game.hall.HallServer
```

## 依赖

```xml
<dependencies>
    <dependency>
        <groupId>litejava</groupId>
        <artifactId>litejava-plugins</artifactId>
    </dependency>
</dependencies>
```

**注意**: HallServer 不依赖 `game-core`，与游戏逻辑完全解耦。
