# Game Doudizhu - 斗地主游戏服务器

## 概述

斗地主游戏服务器，继承自 `GameServer` 基类，实现斗地主特有的游戏逻辑。

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│                    DoudizhuServer                           │
│  (继承 GameServer<DoudizhuGame>)                            │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   RoomMgr   │  │ DoudizhuGame│  │  CardRule   │         │
│  │  (房间管理)  │  │  (游戏逻辑)  │  │  (牌型判断)  │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

## 核心类说明

### DoudizhuServer
游戏服务器入口，处理：
- WebSocket 连接管理
- 消息路由到具体游戏逻辑
- 游戏开始/结束的生命周期

### DoudizhuGame
纯游戏逻辑，无网络代码：
- 发牌 `deal()`
- 叫地主 `bid()`
- 出牌 `play()`
- 不出 `pass()`

### CardRule
牌型判断和比较：
- 判断牌型 `getType(cards)`
- 比较大小 `canBeat(lastCards, cards)`

## 游戏流程

```
┌──────────────────────────────────────────────────────────────────┐
│                        完整游戏流程                               │
└──────────────────────────────────────────────────────────────────┘

1. 匹配成功，玩家连接游戏服务器
   ┌────────┐         ┌────────────────┐
   │ Client │ ──────> │ DoudizhuServer │
   └────────┘  LOGIN  └────────────────┘
                            │
                            ▼
                      验证房间和座位
                            │
                            ▼
                      返回房间状态

2. 所有玩家准备
   ┌────────┐         ┌────────────────┐
   │ Client │ ──────> │ DoudizhuServer │
   └────────┘  READY  └────────────────┘
                            │
                            ▼
                      检查是否全部准备
                            │
                      ┌─────┴─────┐
                      │ 全部准备?  │
                      └─────┬─────┘
                        Yes │
                            ▼
                      调用 startGame()

3. 游戏开始 - 发牌
   ┌────────────────┐
   │  startGame()   │
   └───────┬────────┘
           │
           ▼
   ┌────────────────┐
   │ DoudizhuGame   │
   │   .deal()      │  ← 洗牌、发牌
   └───────┬────────┘
           │
           ▼
   广播 GAME_START (bidSeat=随机)
           │
           ▼
   单播 DEAL 给每个玩家 (各自的手牌)

4. 叫地主阶段 (status=0)
   ┌────────┐         ┌────────────────┐
   │ Client │ ──────> │ DoudizhuServer │
   └────────┘  BID    └────────────────┘
              {bid:true/false}
                            │
                            ▼
                      game.bid(seat, wantBid)
                            │
                      ┌─────┴─────┐
                      │ 结果判断   │
                      └─────┬─────┘
                            │
           ┌────────────────┼────────────────┐
           │                │                │
           ▼                ▼                ▼
      有人叫地主       无人叫地主        继续叫地主
      (确定地主)       (重新发牌)       (下一个人)
           │                │                │
           ▼                ▼                ▼
      广播 BID_RESULT   广播 BID_RESULT   广播 BID_RESULT
      {landlordSeat,    {redeal:true}    {nextBidSeat}
       bottomCards}

5. 出牌阶段 (status=1)
   ┌────────┐         ┌────────────────┐
   │ Client │ ──────> │ DoudizhuServer │
   └────────┘  PLAY   └────────────────┘
              {cards:[...]}
                            │
                            ▼
                      验证: 是否轮到你?
                      验证: 是否有这些牌?
                      验证: 牌型是否合法?
                      验证: 是否能压过上家?
                            │
                            ▼
                      game.play(seat, cards)
                            │
                      ┌─────┴─────┐
                      │ 牌出完了?  │
                      └─────┬─────┘
                            │
           ┌────────────────┴────────────────┐
           │ No                              │ Yes
           ▼                                 ▼
      广播 PLAY_RESULT                  广播 PLAY_RESULT
      {nextSeat, remainCards}           {gameOver:true, winner}
                                             │
                                             ▼
                                        游戏结束处理

6. 不出 (PASS)
   ┌────────┐         ┌────────────────┐
   │ Client │ ──────> │ DoudizhuServer │
   └────────┘  PASS   └────────────────┘
                            │
                            ▼
                      验证: 是否可以不出?
                      (上家出的牌不是自己的才能不出)
                            │
                            ▼
                      game.pass(seat)
                            │
                      ┌─────┴─────┐
                      │ 都不出了?  │
                      └─────┬─────┘
                            │
           ┌────────────────┴────────────────┐
           │ No                              │ Yes (passCount >= 2)
           ▼                                 ▼
      广播 PLAY_RESULT                  广播 PLAY_RESULT
      {pass:true, nextSeat}             {clearLast:true}
                                        (清空上一手，重新出牌)

7. 游戏结束
   ┌────────────────┐
   │  onGameOver()  │
   └───────┬────────┘
           │
           ├─> 计算得分
           ├─> 记录战绩 (PlayerData)
           ├─> 保存录像 (GameReplay)
           ├─> 清理快照 (RoomSnapshot)
           └─> 重置房间状态 (等待下一局)
```

## 协议详解

### 登录 (cmd=1)
```json
// 请求
{"cmd": 1, "data": {"userId": 10001, "roomId": "123456"}}

// 响应
{
  "cmd": 2, "code": 0,
  "data": {
    "roomId": "123456",
    "seats": [
      {"seatIndex": 0, "userId": 10001, "name": "玩家1", "ready": false},
      {"seatIndex": 1, "userId": 10002, "name": "玩家2", "ready": false},
      {"seatIndex": 2, "userId": 10003, "name": "玩家3", "ready": false}
    ],
    "game": null  // 游戏未开始
  }
}
```

### 准备 (cmd=413)
```json
// 请求
{"cmd": 413}

// 广播给所有人
{"cmd": 114, "code": 0, "data": {"userId": 10001, "seatIndex": 0}}
```

### 游戏开始 (cmd=400)
```json
// 广播
{"cmd": 400, "code": 0, "data": {"numOfGames": 1, "bidSeat": 0}}
```

### 发牌 (cmd=410)
```json
// 单播给每个玩家（只能看到自己的牌）
{
  "cmd": 410, "code": 0,
  "data": {
    "cards": [0, 1, 5, 13, 26, 39, 52, ...]  // 17张牌
  }
}
```

### 叫地主 (cmd=1001)
```json
// 请求
{"cmd": 1001, "data": {"bid": true}}

// 广播 - 继续叫地主
{"cmd": 1002, "code": 0, "data": {"seatIndex": 0, "bid": true, "nextSeat": 1}}

// 广播 - 确定地主
{
  "cmd": 1002, "code": 0,
  "data": {
    "seatIndex": 0, "bid": true,
    "landlordSeat": 0,
    "bottomCards": [51, 52, 53]  // 底牌
  }
}
```

### 出牌 (cmd=1003)
```json
// 请求
{"cmd": 1003, "data": {"cards": [0, 13, 26]}}  // 三张3

// 广播
{
  "cmd": 1004, "code": 0,
  "data": {
    "seatIndex": 0,
    "cards": [0, 13, 26],
    "pass": false,
    "remainCards": 14,
    "nextSeat": 1
  }
}
```

### 不出 (cmd=1005)
```json
// 请求
{"cmd": 1005}

// 广播
{"cmd": 1004, "code": 0, "data": {"seatIndex": 1, "pass": true, "nextSeat": 2}}
```

### 游戏结束 (cmd=401)
```json
{
  "cmd": 1004, "code": 0,
  "data": {
    "seatIndex": 0,
    "cards": [52, 53],  // 王炸
    "gameOver": true,
    "winner": 0
  }
}
```

## 牌的编码

```
牌值 = 花色 * 13 + 点数

花色: 0=方块, 1=梅花, 2=红桃, 3=黑桃
点数: 0=3, 1=4, ..., 10=K, 11=A, 12=2

特殊:
52 = 小王
53 = 大王

示例:
0  = 方块3
13 = 梅花3
26 = 红桃3
39 = 黑桃3
12 = 方块2
52 = 小王
53 = 大王
```

## 牌型

| 牌型 | 说明 | 示例 |
|-----|------|------|
| SINGLE | 单张 | [0] |
| PAIR | 对子 | [0, 13] |
| THREE | 三张 | [0, 13, 26] |
| THREE_ONE | 三带一 | [0, 13, 26, 1] |
| THREE_TWO | 三带二 | [0, 13, 26, 1, 14] |
| STRAIGHT | 顺子(5+) | [0, 1, 2, 3, 4] |
| DOUBLE_STRAIGHT | 连对(3+) | [0, 13, 1, 14, 2, 15] |
| PLANE | 飞机 | [0, 13, 26, 1, 14, 27] |
| BOMB | 炸弹 | [0, 13, 26, 39] |
| ROCKET | 王炸 | [52, 53] |

## 配置

`src/main/resources/application.yml`:
```yaml
server:
  id: doudizhu-1
  host: localhost
  wsPort: 9100
  httpPort: 9101

game:
  type: doudizhu
  maxPlayers: 3

registry:
  url: http://localhost:8000
```

## 启动

```bash
mvn exec:java -pl game-doudizhu -Dexec.mainClass=game.doudizhu.DoudizhuServer
```

## 扩展实例

```bash
# 实例1
mvn exec:java -pl game-doudizhu \
  -Dserver.id=ddz-1 -Dserver.wsPort=9100 -Dserver.httpPort=9101

# 实例2
mvn exec:java -pl game-doudizhu \
  -Dserver.id=ddz-2 -Dserver.wsPort=9102 -Dserver.httpPort=9103
```
