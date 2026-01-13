# API 速查表

## HTTP API

### AccountServer (8101)

| 接口 | 方法 | 参数 | 响应 | 说明 |
|------|------|------|------|------|
| `/auth/login` | POST | `{username, password}` | `{userId, name, coins}` | 登录 |
| `/auth/register` | POST | `{username, password, name}` | `{userId}` | 注册 |
| `/player/{userId}` | GET | - | `{userId, name, coins, level}` | 玩家信息 |
| `/reconnect_info` | GET | `userId` | `{hasRoom, serverId, roomId, gameType}` | 断线重连信息 |

### HallServer (8201)

| 接口 | 方法 | 参数 | 响应 | 说明 |
|------|------|------|------|------|
| `/quick_start` | GET | `userId, gameType, level, name` | `{roomId, ip, port, token}` | 快速开始（推荐） |
| `/match/start` | POST | `{userId, gameType, level, name}` | `{status, roomId?, ip?, port?, token?}` | 开始匹配 |
| `/match/cancel` | POST | `{userId}` | `{}` | 取消匹配 |
| `/create_private_room` | GET | `userId, gameType, conf` | `{roomId, ip, port, token}` | 创建私人房间 |
| `/enter_private_room` | GET | `userId, roomid, name` | `{roomId, ip, port, token}` | 加入私人房间 |

### GameServer (9101+)

| 接口 | 方法 | 参数 | 响应 | 说明 |
|------|------|------|------|------|
| `/status` | GET | - | `{serverId, rooms, players}` | 服务器状态 |
| `/room/{roomId}` | GET | - | `{roomId, ownerId, playerCount, seats}` | 房间信息 |
| `/health` | GET | - | `{status, serverId}` | 健康检查 |

## WebSocket 命令

### 系统命令 (1-99)

| cmd | 名称 | 方向 | 请求 | 响应 | 说明 |
|-----|------|------|------|------|------|
| 1 | LOGIN | C→S | `{token}` | `{roomId, seatIndex, seats, game?}` | 登录房间 |
| 3 | LOGOUT | C→S | `{}` | `{}` | 登出 |
| 4 | PING | C↔S | `{}` | `{}` | 心跳 |

### 房间命令 (100-499)

| cmd | 名称 | 方向 | 请求 | 响应 | 说明 |
|-----|------|------|------|------|------|
| 104 | ROOM_EXIT | C→S | `{}` | `{}` | 退出房间 |
| 300 | CHAT_SEND | C→S | `{content}` | - | 发送聊天 |
| 301 | CHAT_MSG | S→C | - | `{userId, name, content}` | 聊天消息 |

### 游戏通用命令 (500-999)

| cmd | 名称 | 方向 | 数据 | 说明 |
|-----|------|------|------|------|
| 500 | USER_JOIN | S→C | `{userId, seatIndex, name}` | 有人加入 |
| 501 | USER_EXIT | S→C | `{userId}` | 有人退出 |
| 502 | USER_STATE | S→C | `{userId, online, trusteeship}` | 状态变化 |
| 503 | USER_READY | S→C | `{userId, seatIndex, ready}` | 准备状态 |
| 504 | READY | C↔S | `{}` / `{userId, seatIndex, ready}` | 准备 |
| 505 | CANCEL_READY | C→S | `{}` | 取消准备 |
| 510 | GAME_START | S→C | `{numOfGames, ...}` | 游戏开始 |
| 511 | GAME_OVER | S→C | `{winner, scores, settlements}` | 游戏结束 |
| 512 | GAME_STATE | S→C | `{...}` | 游戏状态（断线重连） |
| 520 | DEAL | S→C | `{cards}` | 发牌 |
| 521 | DRAW | S→C | `{card}` | 摸牌 |
| 522 | TURN | S→C | `{seatIndex}` | 轮到谁 |

### 斗地主命令 (1000-1099)

| cmd | 名称 | 方向 | 请求 | 响应 | 说明 |
|-----|------|------|------|------|------|
| 1001 | BID | C→S | `{bid: boolean}` | - | 叫地主 |
| 1002 | BID_RESULT | S→C | - | `{seatIndex, bid, nextSeat, landlordSeat?, bottomCards?}` | 叫地主结果 |
| 1003 | PLAY | C→S | `{cards: []}` | - | 出牌 |
| 1004 | PLAY_RESULT | S→C | - | `{seatIndex, cards, pass, nextSeat, remainCards, gameOver, winner}` | 出牌结果 |
| 1005 | PASS | C→S | `{}` | - | 不出 |

### 五子棋命令 (1100-1199)

| cmd | 名称 | 方向 | 请求 | 响应 | 说明 |
|-----|------|------|------|------|------|
| 1101 | PLACE | C→S | `{x, y}` | - | 落子 |
| 1102 | PLACE_RESULT | S→C | - | `{seatIndex, x, y, gameOver, winner}` | 落子结果 |

### 麻将命令 (1200-1299)

| cmd | 名称 | 方向 | 请求 | 响应 | 说明 |
|-----|------|------|------|------|------|
| 1201 | DISCARD | C→S | `{tile}` | - | 打牌 |
| 1202 | DISCARD_RESULT | S→C | - | `{seatIndex, tile}` | 打牌结果 |
| 1203 | PENG | C→S | `{}` | - | 碰 |
| 1204 | GANG | C→S | `{type}` | - | 杠 |
| 1205 | HU | C→S | `{}` | - | 胡 |

## 错误码

| code | 名称 | 说明 | 客户端处理 |
|------|------|------|------------|
| 0 | SUCCESS | 成功 | - |
| 1 | ROOM_FULL | 房间已满 | 提示用户，返回大厅 |
| 2 | ROOM_NOT_FOUND | 房间不存在 | 提示用户，返回大厅 |
| 3 | NOT_LOGIN | 未登录 | 重新登录 |
| 4 | NOT_IN_ROOM | 不在房间中 | 返回大厅 |
| 5 | INVALID_TOKEN | Token 无效 | 重新获取 token |
| 6 | NOT_YOUR_TURN | 不是你的回合 | 等待 |
| 7 | INVALID_ACTION | 无效操作 | 提示用户 |
| 8 | GAME_NOT_STARTED | 游戏未开始 | 等待 |
| 9 | NO_SERVER | 没有可用服务器 | 稍后重试 |
| 10 | INVALID_CARDS | 无效的牌 | 提示用户 |
| 11 | CANNOT_BEAT | 打不过 | 提示用户 |
| 12 | CANNOT_PASS | 不能不出 | 提示用户 |
| 13 | NOT_OWNER | 不是房主 | 提示用户 |
| 14 | NOT_ENOUGH_PLAYERS | 人数不足 | 等待其他玩家 |

## 游戏类型

| gameType | 名称 | 人数 | 端口 |
|----------|------|------|------|
| doudizhu | 斗地主 | 3 | 9100 |
| gobang | 五子棋 | 2 | 9200 |
| mahjong | 麻将 | 4 | 9300 |
| texas | 德州扑克 | 2-9 | 9400 |
| niuniu | 牛牛 | 2-6 | 9500 |
| mahjong2 | 二人麻将 (Go) | 2 | 9600 |
| doudizhu4 | 四人斗地主 (Node.js) | 4 | 9700 |

## 快速参考

### 完整流程（快速开始）

```javascript
// 1. 登录
POST /auth/login {username, password}
→ {userId, name, coins}

// 2. 快速开始
GET /quick_start?userId=xxx&gameType=doudizhu&level=1&name=xxx
→ {roomId, ip, port, token}

// 3. WebSocket 连接
ws://ip:port/game

// 4. 登录房间
→ {cmd: 1, data: {token}}
← {cmd: 1, code: 0, data: {roomId, seatIndex, seats}}

// 5. 准备
→ {cmd: 504, data: {}}
← {cmd: 504, code: 0}

// 6. 等待游戏开始
← {cmd: 510, code: 0, data: {...}}

// 7. 游戏操作
→ {cmd: 1003, data: {cards: [3,4,5]}}
← {cmd: 1004, code: 0, data: {...}}

// 8. 游戏结束
← {cmd: 511, code: 0, data: {winner, scores, settlements}}
```

### 断线重连流程

```javascript
// 1. 检查断线信息
GET /reconnect_info?userId=xxx
→ {hasRoom: true, serverId, roomId, gameType, ip, port}

// 2. 重新获取 token
GET /enter_private_room?userId=xxx&roomid=xxx&name=xxx
→ {roomId, ip, port, token}

// 3. WebSocket 连接
ws://ip:port/game

// 4. 登录房间（自动恢复游戏状态）
→ {cmd: 1, data: {token}}
← {cmd: 1, code: 0, data: {roomId, seatIndex, seats, game: {...}}}
```

### 心跳机制

```javascript
// 客户端每 30 秒发送
→ {cmd: 4}
← {cmd: 4, code: 0}

// 超时规则
// - 60 秒未收到心跳 → 服务端断开连接
// - 5 秒断线 → 自动托管
```

## 配置参考

### 客户端配置

```javascript
const config = {
  accountServer: 'http://localhost:8101',
  hallServer: 'http://localhost:8201',
  heartbeatInterval: 30000,  // 30 秒
  reconnectDelay: 3000,      // 3 秒
  reconnectMaxTries: 3,      // 最多重连 3 次
  matchTimeout: 30000,       // 匹配超时 30 秒
  matchPollInterval: 500     // 匹配轮询间隔 500ms
}
```

### 服务端配置

```yaml
# GameServer config.yml
game:
  type: doudizhu
  maxPlayers: 3
  trusteeshipTimeout: 5000  # 5 秒后托管

server:
  wsPort: 9100
  httpPort: 9101
  advertiseHost: ${ADVERTISE_HOST:127.0.0.1}
  advertiseWsPort: ${ADVERTISE_WS_PORT:9100}
  advertiseHttpPort: ${ADVERTISE_HTTP_PORT:9101}

hall:
  url: http://localhost:8201

account:
  url: http://localhost:8101
```
