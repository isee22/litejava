# Games Go - Go 游戏服务器

Go 语言实现的游戏服务器模块，展示多语言兼容性。

## 模块结构

```
games-go/
├── go-game-core/        # 核心模块
│   ├── cmd.go           # 协议常量
│   ├── config.go        # 配置加载
│   ├── server.go        # GameServer 基类
│   ├── room.go          # 房间管理
│   └── session.go       # 会话管理
│
└── game-mahjong2/       # 二人麻将
    ├── main.go          # 入口
    ├── handler.go       # 消息处理
    ├── game_logic.go    # 游戏逻辑
    └── config.yml       # 配置
```

## 游戏列表

| 游戏 | 人数 | WS 端口 | HTTP 端口 |
|------|------|---------|-----------|
| game-mahjong2 | 2 | 9600 | 9601 |

## 启动

```bash
cd games-go/game-mahjong2
go run .
```

## 协议兼容

Go 游戏服务器与 Java 版本使用完全相同的协议：

- 向 HallServer 注册: `GET /register_gs`
- 心跳: `GET /heartbeat`
- 创建房间: `GET /create_room`
- 进入房间: `GET /enter_room`
- WebSocket 消息: `{cmd, code, data}`

## 开发新游戏

### 1. 创建目录

```
games-go/game-xxx/
├── go.mod
├── main.go
├── handler.go
├── game_logic.go
└── config.yml
```

### 2. 引用核心模块

```go
// go.mod
module game-xxx

require go-game-core v0.0.0

replace go-game-core => ../go-game-core
```

### 3. 实现游戏服务器

```go
package main

import (
    "go-game-core"
)

type MyGameServer struct {
    *core.GameServer
}

func (s *MyGameServer) OnGameCmd(userId int64, room *core.Room, cmd int, data map[string]interface{}) {
    switch cmd {
    case 1001:
        // 处理游戏命令
    }
}

func main() {
    server := &MyGameServer{
        GameServer: core.NewGameServer("config.yml"),
    }
    server.Start()
}
```

### 4. 配置文件

```yaml
server:
  id: xxx-1
  wsPort: 9800
  httpPort: 9801

game:
  type: xxx
  maxPlayers: 4

registry:
  url: http://localhost:8201
```
