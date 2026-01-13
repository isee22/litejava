# Game Mahjong - 麻将游戏服务器

## 概述

麻将游戏服务器，支持4人麻将，继承自 `GameServer` 基类。

## 配置

```yaml
server:
  id: mahjong-1
  host: localhost
  wsPort: 9300
  httpPort: 9301

game:
  type: mahjong
  maxPlayers: 4
```

## 游戏流程

```
1. 所有玩家准备
        │
        ▼
2. 游戏开始 (GAME_START)
   - 洗牌、发牌
   - 确定庄家
        │
        ▼
3. 发牌 (DEAL)
   - 每人13张牌
   - 庄家14张
        │
        ▼
4. 游戏循环
   ┌─────────────────────────────────────┐
   │                                     │
   │  摸牌 → 出牌 → 其他玩家响应         │
   │         │                           │
   │         ▼                           │
   │  ┌─────────────┐                    │
   │  │ 可以碰/杠/胡?│                    │
   │  └──────┬──────┘                    │
   │         │                           │
   │    ┌────┴────┐                      │
   │    │         │                      │
   │    ▼         ▼                      │
   │  有操作    无操作                    │
   │  (碰/杠/胡) (下家摸牌)               │
   │                                     │
   └─────────────────────────────────────┘
        │
        ▼
5. 游戏结束 (GAME_OVER)
   - 有人胡牌
   - 或流局
```

## 协议

### 命令号 (2000+)

| cmd | 名称 | 说明 |
|-----|------|------|
| 2001 | DISCARD | 出牌 |
| 2002 | DISCARD_RESULT | 出牌结果 |
| 2003 | PENG | 碰 |
| 2004 | PENG_RESULT | 碰结果 |
| 2005 | GANG | 杠 |
| 2006 | GANG_RESULT | 杠结果 |
| 2007 | HU | 胡 |
| 2008 | HU_RESULT | 胡结果 |
| 2009 | PASS | 过 |
| 2010 | ACTION | 可执行操作提示 |

### 出牌 (cmd=2001)

```json
// 请求
{"cmd": 2001, "data": {"tile": 0}}

// 广播
{
  "cmd": 2002,
  "data": {
    "seat": 0,
    "tile": 0,
    "nextSeat": 1
  }
}
```

### 碰 (cmd=2003)

```json
// 请求
{"cmd": 2003}

// 广播
{
  "cmd": 2004,
  "data": {
    "seat": 2,
    "tiles": [0, 0, 0],
    "fromSeat": 0
  }
}
```

### 杠 (cmd=2005)

```json
// 请求 - 明杠/暗杠/补杠
{"cmd": 2005, "data": {"tile": 0, "type": "ming"}}

// 广播
{
  "cmd": 2006,
  "data": {
    "seat": 1,
    "tiles": [0, 0, 0, 0],
    "type": "ming",
    "fromSeat": 0
  }
}
```

### 胡 (cmd=2007)

```json
// 请求
{"cmd": 2007}

// 广播
{
  "cmd": 2008,
  "data": {
    "seat": 2,
    "tiles": [...],
    "fan": 3,
    "score": 24
  }
}
```

## 麻将牌编码

```
万: 0-8   (一万到九万)
条: 9-17  (一条到九条)
筒: 18-26 (一筒到九筒)
风: 27-30 (东南西北)
箭: 31-33 (中发白)
```

## 启动

```bash
mvn exec:java -pl game-mahjong -Dexec.mainClass=game.mahjong.MahjongServer
```
