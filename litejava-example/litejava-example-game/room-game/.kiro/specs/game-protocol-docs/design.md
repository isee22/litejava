# Design Document

## Overview

本设计文档描述如何为游戏平台补充三个缺失的协议文档：德州扑克、牛牛、MOBA。文档将遵循现有协议文档的统一格式（参考狼人杀协议.md），确保文档体系的一致性和完整性。

## Architecture

文档结构采用统一的层次化组织：

```
docs/
├── README.md                 # 文档索引（需更新）
├── 客户端对接文档.md
├── 游戏服务器协议.md
├── 斗地主协议.md
├── 五子棋协议.md
├── 麻将协议.md
├── 狼人杀协议.md
├── 德州扑克协议.md          # 新增
├── 牛牛协议.md              # 新增
├── MOBA协议.md              # 新增
├── 部署指南-本地.md
└── 部署指南-Docker.md
```

## Components and Interfaces

### 协议文档标准结构

每个协议文档应包含以下章节：

```markdown
# {游戏名}协议文档

## 概述
- 游戏简介
- 连接信息（端口、路径）

## 游戏流程
- 流程图
- 阶段说明

## 命令号定义
- 命令号表格（cmd、名称、方向、说明）

## 协议详情
- 每个命令的请求/响应JSON示例

## 牌值编码 / 数据编码
- 编码规则说明

## 牌型 / 游戏规则
- 规则定义表

## 错误码
- 错误码表格

## 特殊规则
- 游戏特有规则说明

## 断线重连
- 重连机制和状态恢复

## 客户端实现要点
- 实现建议
```

## Data Models

### 德州扑克数据模型

```
牌值编码: 花色 * 13 + 点数
- 花色: 0=方块, 1=梅花, 2=红桃, 3=黑桃
- 点数: 0=2, 1=3, ..., 12=A

命令号范围: 3001-3011
- 3001: CALL (跟注)
- 3002: RAISE (加注)
- 3003: CHECK (过牌)
- 3004: FOLD (弃牌)
- 3005: ALLIN (全下)
- 3006: ACTION_RESULT (行动结果)
- 3010: COMMUNITY (公共牌)
- 3011: SHOWDOWN (摊牌)

游戏阶段: PRE_FLOP → FLOP → TURN → RIVER → SHOWDOWN
```

### 牛牛数据模型

```
牌值编码: 花色 * 13 + 点数
- 花色: 0=方块, 1=梅花, 2=红桃, 3=黑桃
- 点数: 0=A, 1=2, ..., 12=K

命令号范围: 4001-4006
- 4001: BET (下注)
- 4002: BET_RESULT (下注结果)
- 4003: SHOW (亮牌)
- 4004: SHOW_RESULT (亮牌结果)
- 4005: FIFTH_CARD (第5张牌)
- 4006: SETTLE (结算)

牛型: 无牛 → 牛一~牛六 → 牛七 → 牛八 → 牛九 → 牛牛 → 特殊牛型
```

### MOBA数据模型

```
命令号范围: 6001-6010
- 6001: MOVE (移动)
- 6002: ATTACK (攻击)
- 6003: SKILL (技能)
- 6004: ATTACK_BASE (攻击基地)
- 6010: FRAME (帧数据)

帧同步: 服务器收集输入 → 打包帧数据 → 广播给所有客户端
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

由于本需求主要是文档编写任务，大部分验收标准需要人工审核文档内容的完整性和正确性。以下是可以通过自动化检查验证的属性：

Property 1: 文档文件存在性
*For any* 新增的协议文档（德州扑克、牛牛、MOBA），对应的 .md 文件应存在于 docs/ 目录下
**Validates: Requirements 1.1, 2.1, 3.1**

Property 2: 文档索引完整性
*For any* 新增的协议文档，docs/README.md 的文档目录表格中应包含该文档的链接
**Validates: Requirements 4.1, 4.2, 4.3**

## Error Handling

文档编写过程中可能遇到的问题：

1. **信息不完整**: 从 README.md 提取的协议信息可能不完整，需要参考源代码补充
2. **格式不一致**: 需要严格遵循现有文档格式，保持一致性
3. **命令号冲突**: 需要确认命令号范围不与其他游戏冲突

## Testing Strategy

### 文档验证方法

由于这是文档编写任务，测试策略主要是人工审核：

1. **结构检查**: 验证文档包含所有必需章节
2. **内容检查**: 验证命令号、JSON示例、编码规则的正确性
3. **链接检查**: 验证 README.md 中的链接可正常访问
4. **一致性检查**: 验证与现有文档格式一致

### 验收清单

- [ ] 德州扑克协议.md 包含完整的游戏流程图
- [ ] 德州扑克协议.md 包含所有命令号(3001-3011)的详细说明
- [ ] 德州扑克协议.md 包含牌型比较规则
- [ ] 牛牛协议.md 包含完整的游戏流程图
- [ ] 牛牛协议.md 包含所有命令号(4001-4006)的详细说明
- [ ] 牛牛协议.md 包含牛型倍数规则
- [ ] MOBA协议.md 包含帧同步机制说明
- [ ] MOBA协议.md 包含所有命令号(6001-6010)的详细说明
- [ ] docs/README.md 已更新文档索引
