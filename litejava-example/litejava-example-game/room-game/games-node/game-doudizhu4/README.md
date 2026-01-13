# 四人斗地主 (Node.js)

Node.js 实现的四人斗地主游戏服务器。

## 游戏规则

- 4 人对战 (1 地主 vs 3 农民)
- 两副牌 (108 张，去掉大小王)
- 地主先出，顺时针轮流
- 地主出完牌获胜，或农民任一人出完牌农民获胜

## 端口

| 类型 | 端口 |
|------|------|
| WebSocket | 9700 |
| HTTP | 9701 |

## 启动

```bash
cd games-node/game-doudizhu4
npm install
npm start
```

## 配置

`config.yml`:
```yaml
server:
  id: doudizhu4-1
  wsPort: 9700
  httpPort: 9701

game:
  type: doudizhu4
  maxPlayers: 4

registry:
  url: http://localhost:8201
```

## 文件结构

```
game-doudizhu4/
├── server.js            # 入口，继承 GameServer
├── src/
│   ├── doudizhu4-cmd.js # 游戏命令号
│   └── game-logic.js    # 游戏逻辑 (发牌、出牌、牌型判断)
├── config.yml
└── package.json
```

## 协议

### 游戏命令 (1000+)

| cmd | 方向 | 说明 |
|-----|------|------|
| 1001 | C→S | 叫地主 `{bid: true/false}` |
| 1002 | S→C | 叫地主结果 `{seatIndex, bid, landlordSeat?, bottomCards?}` |
| 1003 | C→S | 出牌 `{cards: [0, 13, 26]}` |
| 1004 | S→C | 出牌结果 `{seatIndex, cards, pass, nextSeat, gameOver?}` |
| 1005 | C→S | 不出 |

### 牌编码

```
牌值 = 花色 * 13 + 点数

花色: 0=方块, 1=梅花, 2=红桃, 3=黑桃
点数: 0=3, 1=4, ..., 10=K, 11=A, 12=2

两副牌: 0-51 (第一副), 52-103 (第二副)
共 104 张 (无大小王)
```

### 牌型

| 牌型 | 说明 |
|------|------|
| SINGLE | 单张 |
| PAIR | 对子 |
| THREE | 三张 |
| THREE_ONE | 三带一 |
| THREE_TWO | 三带二 |
| STRAIGHT | 顺子 (5+) |
| DOUBLE_STRAIGHT | 连对 (3+对) |
| PLANE | 飞机 |
| BOMB | 炸弹 (4张) |

## 游戏流程

```
1. 匹配成功，4人进入房间
2. 全部准备
3. 发牌 (每人 25 张，底牌 8 张)
4. 叫地主 (轮流叫，有人叫则确定)
5. 地主获得底牌
6. 出牌阶段 (地主先出)
7. 游戏结束 (地主或农民出完牌)
```
