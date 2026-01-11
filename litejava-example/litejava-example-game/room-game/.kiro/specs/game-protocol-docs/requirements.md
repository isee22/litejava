# Requirements Document

## Introduction

本项目是一个房间制游戏服务端平台，支持多种游戏类型。目前 docs/ 目录下已有斗地主、五子棋、麻将、狼人杀的完整协议文档，但德州扑克、牛牛、MOBA 三个游戏模块的协议信息仅存在于各自的 README.md 中，缺少统一格式的独立协议文档。本需求旨在补充这三个游戏的协议文档，使文档体系完整统一。

## Glossary

- **Protocol_Document**: 游戏通信协议文档，描述客户端与服务器之间的消息格式、命令号、数据结构
- **Command_Number (cmd)**: 消息命令号，用于标识消息类型
- **Card_Encoding**: 扑克牌值编码规则，将牌面映射为数字
- **Hand_Type**: 牌型，如顺子、同花等
- **Frame_Sync**: 帧同步，一种网络游戏同步机制

## Requirements

### Requirement 1

**User Story:** As a 客户端开发者, I want 完整的德州扑克协议文档, so that 我可以正确实现德州扑克游戏的客户端逻辑。

#### Acceptance Criteria

1. WHEN 创建德州扑克协议文档 THEN THE Protocol_Document SHALL 包含游戏流程说明（准备→发牌→下注轮次→摊牌→结算）
2. WHEN 创建德州扑克协议文档 THEN THE Protocol_Document SHALL 包含完整的命令号表格（cmd 3001-3011）及其方向说明
3. WHEN 创建德州扑克协议文档 THEN THE Protocol_Document SHALL 包含每个命令的请求和响应 JSON 示例
4. WHEN 创建德州扑克协议文档 THEN THE Protocol_Document SHALL 包含牌值编码规则（花色*13+点数）
5. WHEN 创建德州扑克协议文档 THEN THE Protocol_Document SHALL 包含牌型定义表（皇家同花顺到高牌）及比较规则
6. WHEN 创建德州扑克协议文档 THEN THE Protocol_Document SHALL 包含错误码定义表
7. WHEN 创建德州扑克协议文档 THEN THE Protocol_Document SHALL 包含特殊规则说明（边池、全下、盲注等）

### Requirement 2

**User Story:** As a 客户端开发者, I want 完整的牛牛协议文档, so that 我可以正确实现牛牛游戏的客户端逻辑。

#### Acceptance Criteria

1. WHEN 创建牛牛协议文档 THEN THE Protocol_Document SHALL 包含游戏流程说明（准备→下注→发牌→亮牌→结算）
2. WHEN 创建牛牛协议文档 THEN THE Protocol_Document SHALL 包含完整的命令号表格（cmd 4001-4006）及其方向说明
3. WHEN 创建牛牛协议文档 THEN THE Protocol_Document SHALL 包含每个命令的请求和响应 JSON 示例
4. WHEN 创建牛牛协议文档 THEN THE Protocol_Document SHALL 包含牌值编码规则
5. WHEN 创建牛牛协议文档 THEN THE Protocol_Document SHALL 包含牛型定义表（无牛到五小牛）及倍数规则
6. WHEN 创建牛牛协议文档 THEN THE Protocol_Document SHALL 包含错误码定义表
7. WHEN 创建牛牛协议文档 THEN THE Protocol_Document SHALL 包含特殊规则说明（庄家轮换、比牌规则等）

### Requirement 3

**User Story:** As a 客户端开发者, I want 完整的MOBA协议文档, so that 我可以正确实现MOBA游戏的客户端帧同步逻辑。

#### Acceptance Criteria

1. WHEN 创建MOBA协议文档 THEN THE Protocol_Document SHALL 包含游戏流程说明（匹配→选英雄→加载→游戏→结算）
2. WHEN 创建MOBA协议文档 THEN THE Protocol_Document SHALL 包含完整的命令号表格（cmd 6001-6010）及其方向说明
3. WHEN 创建MOBA协议文档 THEN THE Protocol_Document SHALL 包含每个命令的请求和响应 JSON 示例
4. WHEN 创建MOBA协议文档 THEN THE Protocol_Document SHALL 包含帧同步机制说明（帧率、输入收集、广播）
5. WHEN 创建MOBA协议文档 THEN THE Protocol_Document SHALL 包含错误码定义表
6. WHEN 创建MOBA协议文档 THEN THE Protocol_Document SHALL 包含断线重连机制说明

### Requirement 4

**User Story:** As a 文档维护者, I want 更新文档目录索引, so that 新增的协议文档可以被正确索引和发现。

#### Acceptance Criteria

1. WHEN 完成协议文档创建 THEN THE docs/README.md SHALL 在文档目录表格中添加德州扑克协议文档链接
2. WHEN 完成协议文档创建 THEN THE docs/README.md SHALL 在文档目录表格中添加牛牛协议文档链接
3. WHEN 完成协议文档创建 THEN THE docs/README.md SHALL 在文档目录表格中添加MOBA协议文档链接
