# Game Client - Web 客户端

Vue 3 + Vite 构建的游戏 Web 客户端。

## 技术栈

- Vue 3 (Composition API)
- Vite 5
- Pinia (状态管理)
- Vue Router 4
- Plain CSS

## 目录结构

```
game-client/
├── src/
│   ├── components/          # 通用组件
│   │   ├── Card.vue         # 扑克牌
│   │   ├── MahjongTile.vue  # 麻将牌
│   │   ├── PlayerArea.vue   # 玩家区域
│   │   └── MessageBox.vue   # 消息提示
│   ├── views/               # 页面
│   │   ├── Login.vue        # 登录页
│   │   ├── Lobby.vue        # 大厅页
│   │   ├── Room.vue         # 房间页
│   │   └── games/           # 游戏页面
│   │       ├── Doudizhu.vue
│   │       ├── Mahjong.vue
│   │       └── Gobang.vue
│   ├── stores/              # Pinia 状态
│   │   ├── user.js          # 用户状态
│   │   └── game.js          # 游戏状态
│   ├── utils/
│   │   ├── api.js           # HTTP API
│   │   ├── websocket.js     # WebSocket 管理
│   │   └── constants.js     # 协议常量
│   ├── router/
│   ├── styles/
│   ├── App.vue
│   └── main.js
├── index.html
├── vite.config.js
└── package.json
```

## 启动

```bash
cd game-client
npm install
npm run dev
```

访问 http://localhost:3000

**注意**: 开发模式下，Vite 会代理 HTTP 请求到各服务端口，无需启动 Nginx。

## 构建

```bash
npm run build
```

输出到 `dist/` 目录，可部署到 Nginx 或其他静态服务器。

## 连接流程

```
┌─────────────────────────────────────────────────────────────┐
│                      连接流程                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. HTTP 登录                                               │
│     POST /api/account/login                                 │
│     → {userId, session}                                     │
│                                                             │
│  2. HTTP 匹配/创建房间                                       │
│     POST /api/hall/match/start                              │
│     → {roomId, ip, port, token}                             │
│                                                             │
│  3. WebSocket 直连 GameServer                               │
│     ws://{ip}:{port}/game                                   │
│                                                             │
│  4. WebSocket 登录                                          │
│     {cmd: 1, data: {token: "xxx"}}                          │
│     → 进入房间，开始游戏                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘

【开发模式】
  - HTTP: Vite 代理 → AccountServer/HallServer
  - WebSocket: 直连 GameServer (HallServer 返回的地址)

【生产模式】
  - HTTP: Nginx 代理 → AccountServer/HallServer
  - WebSocket: 直连 GameServer 或 Nginx 代理
```

## API 端点

### Account Server (8101)

```javascript
// 登录
POST /login
{username, password} → {userId, session, name, coins}

// 注册
POST /register
{username, password, name} → {userId}

// 获取玩家信息
GET /player/{userId} → {userId, name, coins, gems, level}
```

### Hall Server (8201)

```javascript
// 创建房间
GET /create_private_room?userId=x&gameType=doudizhu
→ {roomId, ip, port, token}

// 加入房间
GET /enter_private_room?userId=x&roomId=123456
→ {roomId, ip, port, token}

// 开始匹配
POST /match/start
{userId, gameType, level, name} → {status, roomId?, token?}

// 取消匹配
POST /match/cancel
{userId}
```

### WebSocket 协议

```javascript
// 消息格式
{cmd: number, code?: number, data?: object}

// 命令号
Cmd = {
    LOGIN: 1,
    LOGOUT: 3,
    PING: 4,
    USER_JOIN: 500,
    USER_EXIT: 501,
    READY: 504,
    GAME_START: 510,
    GAME_OVER: 511,
    DEAL: 520,
    TURN: 522,
    // 游戏特定: 1000+
}
```

## 功能状态

- [x] 登录/注册
- [x] 游戏大厅
- [x] 匹配系统
- [x] 创建/加入房间
- [x] 斗地主游戏
- [ ] 麻将游戏
- [ ] 五子棋游戏
- [ ] 其他游戏

## 配置

`vite.config.js`:
```javascript
export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      // 开发模式：代理到各服务端口
      '/api/account': {
        target: 'http://localhost:8101',
        rewrite: path => path.replace(/^\/api\/account/, '')
      },
      '/api/hall': {
        target: 'http://localhost:8201',
        rewrite: path => path.replace(/^\/api\/hall/, '')
      }
    }
  }
})
```

**开发模式**: Vite 代理 HTTP 请求，WebSocket 直连 GameServer
**生产模式**: Nginx 统一代理（可选）
