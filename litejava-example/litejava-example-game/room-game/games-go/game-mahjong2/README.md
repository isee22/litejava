# 二人麻将 (Go)

Go 语言实现的二人麻将游戏服务器。

## 游戏规则

- 2 人对战
- 简化规则：无吃碰杠，只能自摸胡
- 使用 108 张牌 (去掉字牌)

## 端口

| 类型 | 端口 |
|------|------|
| WebSocket | 9600 |
| HTTP | 9601 |

## 启动

```bash
cd games-go/game-mahjong2
go run .
```

## 配置

`config.yml`:
```yaml
server:
  id: mahjong2-1
  wsPort: 9600
  httpPort: 9601

game:
  type: mahjong2
  maxPlayers: 2

registry:
  url: http://localhost:8201
```

## 协议

### 游戏命令 (1000+)

| cmd | 方向 | 说明 |
|-----|------|------|
| 1001 | C→S | 出牌 `{tile: 5}` |
| 1002 | S→C | 出牌结果 `{userId, tile, nextSeat}` |
| 1003 | C→S | 胡牌 |
| 1004 | S→C | 游戏结束 `{winner, score}` |

### 牌编码

```
0-8:   万 (1-9万)
9-17:  条 (1-9条)
18-26: 筒 (1-9筒)

每种牌 4 张，共 108 张
```

## 游戏流程

```
1. 匹配成功，2人进入房间
2. 双方准备
3. 发牌 (每人 13 张)
4. 庄家先摸牌
5. 循环: 摸牌 → 出牌
6. 自摸胡牌 → 游戏结束
```
