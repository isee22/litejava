# Game Niuniu - 牛牛游戏服务器

## 概述

牛牛（斗牛）游戏服务器，支持2-6人，继承自 `GameServer` 基类。

## 配置

```yaml
server:
  id: niuniu-1
  host: localhost
  wsPort: 9500
  httpPort: 9501

game:
  type: niuniu
  maxPlayers: 6
```

## 游戏流程

```
1. 所有玩家准备
        │
        ▼
2. 游戏开始 (GAME_START)
   - 确定庄家
        │
        ▼
3. 下注阶段 (BET)
   - 闲家下注
   - 庄家不下注
        │
        ▼
4. 发牌 (DEAL)
   - 每人发4张牌
        │
        ▼
5. 发第5张牌 (FIFTH_CARD)
   - 逐个发第5张
        │
        ▼
6. 亮牌阶段 (SHOW)
   - 玩家选择牛型
   - 或系统自动计算
        │
        ▼
7. 结算 (SETTLE)
   - 比较牛型大小
   - 计算输赢
        │
        ▼
8. 游戏结束 (GAME_OVER)
```

## 协议

### 命令号 (4000+)

| cmd | 名称 | 说明 |
|-----|------|------|
| 4001 | BET | 下注 |
| 4002 | BET_RESULT | 下注结果 |
| 4003 | SHOW | 亮牌 |
| 4004 | SHOW_RESULT | 亮牌结果 |
| 4005 | FIFTH_CARD | 第5张牌 |
| 4006 | SETTLE | 结算 |

### 下注 (cmd=4001)

```json
// 请求
{"cmd": 4001, "data": {"amount": 100}}

// 广播
{
  "cmd": 4002,
  "data": {
    "seat": 1,
    "amount": 100
  }
}
```

### 亮牌 (cmd=4003)

```json
// 请求 - 选择组成牛的3张牌
{"cmd": 4003, "data": {"cards": [0, 1, 2]}}

// 广播
{
  "cmd": 4004,
  "data": {
    "seat": 1,
    "cards": [0, 1, 2, 3, 4],
    "niu": 9,           // 牛几 (0=无牛, 10=牛牛)
    "niuCards": [0, 1, 2]
  }
}
```

### 结算 (cmd=4006)

```json
{
  "cmd": 4006,
  "data": {
    "banker": 0,
    "results": [
      {"seat": 0, "niu": 10, "score": 300},
      {"seat": 1, "niu": 7, "score": -100},
      {"seat": 2, "niu": 0, "score": -200}
    ]
  }
}
```

## 牛型

| 牛型 | 说明 | 倍数 |
|-----|------|------|
| 无牛 | 无法组成10的倍数 | 1 |
| 牛一~牛六 | 剩余2张点数和为1-6 | 1 |
| 牛七 | 剩余2张点数和为7 | 2 |
| 牛八 | 剩余2张点数和为8 | 2 |
| 牛九 | 剩余2张点数和为9 | 3 |
| 牛牛 | 剩余2张点数和为10 | 3 |
| 四花牛 | 4张花牌+1张10 | 4 |
| 五花牛 | 5张全是花牌 | 5 |
| 炸弹牛 | 4张相同 | 5 |
| 五小牛 | 5张牌点数和≤10 | 5 |

## 启动

```bash
mvn exec:java -pl game-niuniu -Dexec.mainClass=game.niuniu.NiuniuServer
```
