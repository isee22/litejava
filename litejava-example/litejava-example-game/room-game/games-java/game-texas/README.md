# Game Texas - 德州扑克游戏服务器

## 概述

德州扑克游戏服务器，支持2-9人，继承自 `GameServer` 基类。

## 配置

```yaml
server:
  id: texas-1
  host: localhost
  wsPort: 9400
  httpPort: 9401

game:
  type: texas
  maxPlayers: 9
```

## 游戏流程

```
1. 所有玩家准备
        │
        ▼
2. 游戏开始 (GAME_START)
   - 确定庄家位置
   - 小盲/大盲下注
        │
        ▼
3. 发底牌 (DEAL)
   - 每人2张底牌
        │
        ▼
4. 翻牌前 (Pre-Flop)
   - 从大盲下家开始行动
   - 跟注/加注/弃牌
        │
        ▼
5. 翻牌 (Flop) - COMMUNITY
   - 发3张公共牌
   - 新一轮下注
        │
        ▼
6. 转牌 (Turn) - COMMUNITY
   - 发第4张公共牌
   - 新一轮下注
        │
        ▼
7. 河牌 (River) - COMMUNITY
   - 发第5张公共牌
   - 最后一轮下注
        │
        ▼
8. 摊牌 (SHOWDOWN)
   - 比较牌型大小
   - 分配底池
        │
        ▼
9. 游戏结束 (GAME_OVER)
```

## 协议

### 命令号 (3000+)

| cmd | 名称 | 说明 |
|-----|------|------|
| 3001 | CALL | 跟注 |
| 3002 | RAISE | 加注 |
| 3003 | CHECK | 过牌 |
| 3004 | FOLD | 弃牌 |
| 3005 | ALLIN | 全下 |
| 3006 | ACTION_RESULT | 行动结果 |
| 3010 | COMMUNITY | 公共牌 |
| 3011 | SHOWDOWN | 摊牌 |

### 跟注 (cmd=3001)

```json
// 请求
{"cmd": 3001}

// 广播
{
  "cmd": 3006,
  "data": {
    "seat": 2,
    "action": "call",
    "amount": 100,
    "pot": 500,
    "nextSeat": 3
  }
}
```

### 加注 (cmd=3002)

```json
// 请求
{"cmd": 3002, "data": {"amount": 200}}

// 广播
{
  "cmd": 3006,
  "data": {
    "seat": 3,
    "action": "raise",
    "amount": 200,
    "pot": 700,
    "nextSeat": 0
  }
}
```

### 公共牌 (cmd=3010)

```json
// 翻牌
{
  "cmd": 3010,
  "data": {
    "stage": "flop",
    "cards": [0, 13, 26]
  }
}

// 转牌
{
  "cmd": 3010,
  "data": {
    "stage": "turn",
    "cards": [0, 13, 26, 39]
  }
}
```

### 摊牌 (cmd=3011)

```json
{
  "cmd": 3011,
  "data": {
    "results": [
      {
        "seat": 0,
        "cards": [1, 14],
        "bestHand": [1, 14, 0, 13, 26],
        "handType": "FLUSH",
        "win": true,
        "amount": 1000
      },
      {
        "seat": 2,
        "cards": [5, 18],
        "bestHand": [5, 18, 0, 13, 26],
        "handType": "TWO_PAIR",
        "win": false,
        "amount": 0
      }
    ]
  }
}
```

## 牌型 (从大到小)

| 牌型 | 说明 |
|-----|------|
| ROYAL_FLUSH | 皇家同花顺 (10-A同花) |
| STRAIGHT_FLUSH | 同花顺 |
| FOUR_OF_A_KIND | 四条 |
| FULL_HOUSE | 葫芦 (三条+一对) |
| FLUSH | 同花 |
| STRAIGHT | 顺子 |
| THREE_OF_A_KIND | 三条 |
| TWO_PAIR | 两对 |
| ONE_PAIR | 一对 |
| HIGH_CARD | 高牌 |

## 启动

```bash
mvn exec:java -pl game-texas -Dexec.mainClass=game.texas.TexasServer
```
