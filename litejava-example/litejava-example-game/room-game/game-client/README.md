# 游戏客户端

Vue 3 + Vite 构建的游戏客户端，采用 BabyKylin 模式架构。

## 架构

```
src/
├── components/          # 通用组件
│   ├── Card.vue        # 扑克牌组件
│   ├── MessageBox.vue  # 消息提示
│   └── PlayerArea.vue  # 玩家区域
├── stores/             # Pinia 状态管理
│   ├── user.js         # 用户状态
│   └── game.js         # 游戏状态
├── utils/
│   ├── websocket.js    # WebSocket 管理 + HTTP API
│   └── message.js      # 消息工具
├── views/              # 页面
│   ├── Login.vue       # 登录页
│   ├── Lobby.vue       # 大厅页
│   ├── Room.vue        # 房间页
│   └── Game.vue        # 游戏页
├── router/             # 路由配置
├── styles/             # 全局样式
├── App.vue
└── main.js
```

## BabyKylin 模式连接流程

```
┌─────────┐      ┌─────────┐      ┌─────────┐      ┌─────────┐
│  客户端  │      │ Account │      │  Hall   │      │  Game   │
└────┬────┘      └────┬────┘      └────┬────┘      └────┬────┘
     │                │                │                │
     │ 1. HTTP 登录   │                │                │
     │───────────────>│                │                │
     │   {userId}     │                │                │
     │<───────────────│                │                │
     │                │                │                │
     │ 2. HTTP 创建/加入房间           │                │
     │────────────────────────────────>│                │
     │                │                │ 3. 选择服务器   │
     │                │                │ 4. 调用 GameServer
     │                │                │───────────────>│
     │   {roomid, ip, port, token}     │                │
     │<────────────────────────────────│                │
     │                │                │                │
     │ 5. WebSocket 连接 GameServer    │                │
     │─────────────────────────────────────────────────>│
     │ 6. LOGIN {token}                │                │
     │─────────────────────────────────────────────────>│
     │   验证 token，进入房间           │                │
     │<─────────────────────────────────────────────────│
```

**核心原则**：
- 进入游戏前全部用 HTTP（登录、创建房间、匹配）
- 进入游戏后用 WebSocket（实时游戏通信）

## 启动

```bash
cd game-client
npm install
npm run dev
```

访问 http://localhost:3000

## API 端点

### Account Server (8101)
- `POST /auth/login` - 登录
- `POST /auth/register` - 注册
- `GET /player/{userId}` - 获取玩家信息

### Hall Server (8201)
- `POST /create_room` - 创建房间
- `POST /enter_room` - 加入房间
- `POST /match/start` - 开始匹配
- `POST /match/cancel` - 取消匹配
- `GET /room/configs` - 获取房间配置

### GameServer (WebSocket)
连接地址: `ws://{ip}:{port}/{gameType}`

## 协议命令号

见 `src/utils/websocket.js`:
```js
Cmd = {
  // 系统 (1-99)
  LOGIN: 1,
  LOGOUT: 3,
  PING: 4,
  
  // 游戏通用 (500-599)
  USER_JOIN: 500,
  USER_EXIT: 501,
  READY: 504,
  GAME_START: 510,
  GAME_OVER: 511,
  DEAL: 520,
  TURN: 522,
  ...
}
```

## 功能

- [x] 登录/注册（HTTP）
- [x] 游戏大厅（选择游戏类型）
- [x] 匹配系统（HTTP 轮询）
- [x] 创建/加入房间
- [x] 斗地主游戏界面
- [ ] 麻将游戏界面
- [ ] 五子棋游戏界面
