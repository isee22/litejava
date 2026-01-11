-- 创建数据库
CREATE DATABASE IF NOT EXISTS game CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE game;

-- 账号表
CREATE TABLE IF NOT EXISTS account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    password VARCHAR(64) NOT NULL,
    create_time BIGINT NOT NULL,
    last_login_time BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 玩家表
CREATE TABLE IF NOT EXISTS player (
    userId BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    avatar VARCHAR(256),
    sex TINYINT NOT NULL DEFAULT 0 COMMENT '0=未知, 1=男, 2=女',
    coins BIGINT NOT NULL DEFAULT 10000,
    diamonds BIGINT NOT NULL DEFAULT 0,
    level INT NOT NULL DEFAULT 1,
    exp BIGINT NOT NULL DEFAULT 0,
    vipLevel INT NOT NULL DEFAULT 0,
    vipExp BIGINT NOT NULL DEFAULT 0,
    totalGames INT NOT NULL DEFAULT 0,
    winGames INT NOT NULL DEFAULT 0,
    loginDays INT NOT NULL DEFAULT 1,
    createTime BIGINT NOT NULL,
    lastLoginTime BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 签到配置表
CREATE TABLE IF NOT EXISTS sign_in_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    day INT NOT NULL,
    rewardType VARCHAR(32) NOT NULL,
    rewardAmount INT NOT NULL,
    description VARCHAR(128)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 签到记录表
CREATE TABLE IF NOT EXISTS sign_in_record (
    userId BIGINT PRIMARY KEY,
    signDay INT NOT NULL DEFAULT 0,
    lastSignTime BIGINT NOT NULL DEFAULT 0,
    totalSignDays INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 物品配置表
CREATE TABLE IF NOT EXISTS item_config (
    item_id INT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    `desc` VARCHAR(256),
    price INT NOT NULL DEFAULT 0,
    coin_price INT NOT NULL DEFAULT 0,
    duration INT NOT NULL DEFAULT 0 COMMENT '有效时长(秒), 0=永久',
    effect VARCHAR(256) COMMENT '效果JSON',
    enabled TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 玩家物品表
CREATE TABLE IF NOT EXISTS player_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_id INT NOT NULL,
    count INT NOT NULL DEFAULT 1,
    expire_time BIGINT NOT NULL DEFAULT 0 COMMENT '过期时间戳, 0=永久',
    create_time BIGINT NOT NULL,
    update_time BIGINT NOT NULL,
    UNIQUE KEY uk_user_item (user_id, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 房间配置表
CREATE TABLE IF NOT EXISTS room_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_type VARCHAR(32) NOT NULL,
    room_level VARCHAR(32) NOT NULL,
    room_name VARCHAR(64) NOT NULL,
    min_players INT NOT NULL DEFAULT 2,
    max_players INT NOT NULL DEFAULT 4,
    min_coins INT NOT NULL DEFAULT 0,
    max_coins INT NOT NULL DEFAULT 0,
    base_score INT NOT NULL DEFAULT 1,
    init_chips INT NOT NULL DEFAULT 0,
    round_time INT NOT NULL DEFAULT 30,
    match_timeout INT NOT NULL DEFAULT 60,
    extra_config TEXT,
    status TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    create_time BIGINT NOT NULL,
    update_time BIGINT NOT NULL,
    UNIQUE KEY uk_game_level (game_type, room_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 好友表
CREATE TABLE IF NOT EXISTS friend (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    create_time BIGINT NOT NULL,
    UNIQUE KEY uk_user_friend (user_id, friend_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 好友请求表
CREATE TABLE IF NOT EXISTS friend_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_id BIGINT NOT NULL,
    to_id BIGINT NOT NULL,
    message VARCHAR(128),
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=待处理, 1=已接受, 2=已拒绝',
    create_time BIGINT NOT NULL,
    update_time BIGINT NOT NULL,
    INDEX idx_to_id_status (to_id, status),
    INDEX idx_from_to (from_id, to_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化签到配置
INSERT IGNORE INTO sign_in_config (day, rewardType, rewardAmount, description) VALUES
(1, 'coins', 1000, '第1天签到奖励'),
(2, 'coins', 2000, '第2天签到奖励'),
(3, 'coins', 3000, '第3天签到奖励'),
(4, 'coins', 5000, '第4天签到奖励'),
(5, 'coins', 8000, '第5天签到奖励'),
(6, 'coins', 10000, '第6天签到奖励'),
(7, 'diamonds', 10, '第7天签到奖励');

-- 初始化房间配置
INSERT IGNORE INTO room_config (game_type, room_level, room_name, min_players, max_players, min_coins, base_score, round_time, match_timeout, status, create_time, update_time) VALUES
('doudizhu', 'beginner', '初级场', 3, 3, 1000, 10, 30, 60, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('doudizhu', 'intermediate', '中级场', 3, 3, 50000, 50, 30, 60, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('gobang', 'beginner', '初级场', 2, 2, 0, 10, 60, 60, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('mahjong', 'beginner', '初级场', 4, 4, 1000, 1, 30, 60, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000);

-- 初始化物品配置
INSERT IGNORE INTO item_config (item_id, name, type, `desc`, price, coin_price, duration, effect) VALUES
(1001, '双倍金币卡(1小时)', 'coin_card', '游戏获得金币翻倍', 50, 5000, 3600, '{"multiply":2}'),
(1002, '双倍金币卡(1天)', 'coin_card', '游戏获得金币翻倍', 200, 0, 86400, '{"multiply":2}'),
(1003, '双倍经验卡(1小时)', 'exp_card', '游戏获得经验翻倍', 30, 3000, 3600, '{"multiply":2}'),
(1004, '双倍经验卡(1天)', 'exp_card', '游戏获得经验翻倍', 100, 0, 86400, '{"multiply":2}'),
(2001, '玫瑰花', 'gift', '送给好友的礼物', 10, 1000, 0, '{"charm":10}'),
(2002, '巧克力', 'gift', '送给好友的礼物', 20, 2000, 0, '{"charm":25}'),
(2003, '钻戒', 'gift', '送给好友的礼物', 100, 0, 0, '{"charm":150}'),
(2004, '跑车', 'gift', '送给好友的礼物', 500, 0, 0, '{"charm":1000}'),
(3001, '表情包-基础', 'emoji', '解锁基础表情', 50, 5000, 0, '{"pack":"basic"}'),
(3002, '表情包-搞笑', 'emoji', '解锁搞笑表情', 100, 0, 0, '{"pack":"funny"}'),
(4001, '小喇叭', 'horn', '全服广播消息', 5, 500, 0, null),
(4002, '大喇叭', 'horn_vip', '全服广播消息(带特效)', 20, 0, 0, '{"effect":"golden"}');
