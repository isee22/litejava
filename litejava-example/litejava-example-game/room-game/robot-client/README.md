# Robot Client - 机器人客户端

## 概述

机器人客户端，用于测试和填充游戏房间。可以模拟真实玩家进行匹配和游戏。

## 配置

```yaml
server:
  httpPort: 8900

gateway:
  httpUrl: http://localhost:7101
  wsUrl: ws://localhost:7100/ws

robot:
  count: 10  # 机器人数量
```

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│                      RobotClient                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  RobotManager                                               │
│  ├── Robot[0] ──> WsClient ──> Gateway ──> Match ──> Game  │
│  ├── Robot[1] ──> WsClient ──> Gateway ──> Match ──> Game  │
│  ├── Robot[2] ──> WsClient ──> Gateway ──> Match ──> Game  │
│  └── ...                                                    │
│                                                             │
│  每个 Robot:                                                │
│  - 自动登录                                                 │
│  - 自动匹配                                                 │
│  - 自动游戏 (使用 AI)                                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 机器人流程

```
1. 启动
   - 创建 N 个机器人
        │
        ▼
2. 登录
   - 连接 Gateway
   - 发送登录请求
   - 获取 token
        │
        ▼
3. 匹配
   - 连接 Match Server
   - 开始匹配
   - 等待匹配成功
        │
        ▼
4. 游戏
   - 连接 Game Server
   - 准备
   - 使用 AI 进行游戏
        │
        ▼
5. 游戏结束
   - 返回步骤 3 继续匹配
```

## 机器人 AI

每种游戏有对应的 AI 实现：

| 游戏 | AI 类 | 策略 |
|-----|-------|------|
| 斗地主 | DoudizhuAI | 简单出牌策略 |
| 麻将 | MahjongAI | 基础胡牌策略 |
| 五子棋 | GobangAI | 简单防守进攻 |
| 牛牛 | NiuniuAI | 自动亮牌 |
| 德州 | TexasAI | 简单下注策略 |

## HTTP 接口

| 接口 | 说明 |
|-----|------|
| GET /status | 机器人状态 |
| POST /start | 启动机器人 |
| POST /stop | 停止机器人 |

### 状态查询

```json
GET /status

{
  "total": 10,
  "online": 8,
  "matching": 3,
  "gaming": 5
}
```

## 启动

```bash
mvn exec:java -pl robot-client -Dexec.mainClass=game.robot.RobotClient
```

## 注意事项

1. 机器人 ID 从 900000 开始，避免与真实玩家冲突
2. 机器人会自动重连和重新匹配
3. 可以通过 HTTP 接口动态控制机器人数量
