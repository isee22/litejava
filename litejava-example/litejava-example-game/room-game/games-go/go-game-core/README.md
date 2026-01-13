# Go Game Core - Go 游戏核心模块

Go 语言游戏服务器的核心框架，与 Java `game-core` 功能对应。

## 文件结构

```
go-game-core/
├── cmd.go           # 协议命令号和错误码
├── config.go        # 配置文件加载
├── server.go        # GameServer 基类
├── room.go          # 房间和座位管理
├── session.go       # WebSocket 会话管理
├── go.mod
└── go.sum
```

## 核心结构

### 命令号 (cmd.go)

```go
const (
    // 系统
    CmdLogin       = 1
    CmdLoginResult = 2
    CmdLogout      = 3
    CmdPing        = 4
    CmdPong        = 5
    
    // 通用游戏
    CmdUserJoin    = 500
    CmdUserExit    = 501
    CmdReady       = 504
    CmdGameStart   = 510
    CmdGameOver    = 511
    CmdDeal        = 520
    CmdTurn        = 522
)

const (
    ErrOK           = 0
    ErrUnknown      = 1
    ErrNotLogin     = 2
    ErrNotInRoom    = 3
    ErrRoomNotFound = 20
    ErrRoomFull     = 21
    ErrNotYourTurn  = 40
)
```

### 房间结构 (room.go)

```go
type Room struct {
    RoomId     string
    Seats      []*Seat
    Game       interface{}
    Status     int  // 0=等待, 1=游戏中
    CreateTime int64
}

type Seat struct {
    SeatIndex int
    UserId    int64
    Name      string
    Ready     bool
    Online    bool
}

type RoomManager struct {
    rooms    map[string]*Room
    userRoom map[int64]string
}

func (rm *RoomManager) CreateRoom(roomId string, maxPlayers int) *Room
func (rm *RoomManager) GetRoom(roomId string) *Room
func (rm *RoomManager) JoinRoom(roomId string, userId int64, name string) (*Seat, error)
func (rm *RoomManager) ExitRoom(userId int64)
```

### GameServer (server.go)

```go
type GameServer struct {
    config      *Config
    roomMgr     *RoomManager
    sessionMgr  *SessionManager
    
    // 需要实现的回调
    OnStartGame func(room *Room)
    OnGameCmd   func(userId int64, room *Room, cmd int, data map[string]interface{})
}

func NewGameServer(configPath string) *GameServer
func (s *GameServer) Start()
func (s *GameServer) Send(userId int64, cmd int, data interface{})
func (s *GameServer) Broadcast(room *Room, cmd int, data interface{})
```

## 使用示例

```go
package main

import (
    core "go-game-core"
)

func main() {
    server := core.NewGameServer("config.yml")
    
    server.OnStartGame = func(room *core.Room) {
        // 初始化游戏
        game := NewMahjongGame()
        game.Deal()
        room.Game = game
        
        server.Broadcast(room, core.CmdGameStart, nil)
        
        for _, seat := range room.Seats {
            server.Send(seat.UserId, core.CmdDeal, map[string]interface{}{
                "tiles": game.GetTiles(seat.SeatIndex),
            })
        }
    }
    
    server.OnGameCmd = func(userId int64, room *core.Room, cmd int, data map[string]interface{}) {
        game := room.Game.(*MahjongGame)
        
        switch cmd {
        case 1001: // 出牌
            tile := int(data["tile"].(float64))
            game.Discard(tile)
            server.Broadcast(room, 1002, map[string]interface{}{
                "userId": userId,
                "tile":   tile,
            })
        }
    }
    
    server.Start()
}
```

## 与 Java game-core 对应

| Go | Java |
|----|------|
| `cmd.go` | `Cmd.java` + `ErrCode.java` |
| `server.go` | `GameServer.java` |
| `room.go` | `RoomMgr.java` + `Room` + `Seat` |
| `session.go` | (内置于 LiteJava) |
| `config.go` | (内置于 LiteJava) |
