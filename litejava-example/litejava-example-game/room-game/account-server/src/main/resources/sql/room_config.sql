-- 房间配置表 (场次配置)
CREATE TABLE IF NOT EXISTS room_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_type VARCHAR(32) NOT NULL COMMENT '游戏类型',
    room_level VARCHAR(32) NOT NULL COMMENT '场次级别: beginner/intermediate/advanced/master',
    room_name VARCHAR(64) NOT NULL COMMENT '场次名称',
    min_players INT NOT NULL DEFAULT 2 COMMENT '最小玩家数',
    max_players INT NOT NULL DEFAULT 4 COMMENT '最大玩家数',
    min_coins INT NOT NULL DEFAULT 0 COMMENT '入场最低金币',
    max_coins INT NOT NULL DEFAULT 0 COMMENT '入场最高金币(0=不限)',
    base_score INT NOT NULL DEFAULT 1 COMMENT '底分',
    init_chips INT NOT NULL DEFAULT 0 COMMENT '初始筹码',
    round_time INT NOT NULL DEFAULT 30 COMMENT '每回合时间(秒)',
    match_timeout INT NOT NULL DEFAULT 60 COMMENT '匹配超时(秒)',
    extra_config TEXT COMMENT '扩展配置(JSON)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=关闭, 1=开启',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    create_time BIGINT NOT NULL,
    update_time BIGINT NOT NULL,
    UNIQUE KEY uk_game_level (game_type, room_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间配置表';

-- 斗地主场次
INSERT INTO room_config (game_type, room_level, room_name, min_players, max_players, min_coins, max_coins, base_score, init_chips, round_time, match_timeout, extra_config, status, sort_order, create_time, update_time) VALUES
('doudizhu', 'beginner', '初级场', 3, 3, 1000, 50000, 10, 0, 30, 60, '{}', 1, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('doudizhu', 'intermediate', '中级场', 3, 3, 50000, 200000, 50, 0, 30, 60, '{}', 1, 2, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('doudizhu', 'advanced', '高级场', 3, 3, 200000, 0, 200, 0, 30, 60, '{}', 1, 3, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000);

-- 麻将场次
INSERT INTO room_config (game_type, room_level, room_name, min_players, max_players, min_coins, max_coins, base_score, init_chips, round_time, match_timeout, extra_config, status, sort_order, create_time, update_time) VALUES
('mahjong', 'beginner', '初级场', 4, 4, 1000, 50000, 1, 0, 30, 60, '{"type":"sichuan"}', 1, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('mahjong', 'intermediate', '中级场', 4, 4, 50000, 200000, 5, 0, 30, 60, '{"type":"sichuan"}', 1, 2, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('mahjong', 'advanced', '高级场', 4, 4, 200000, 0, 20, 0, 30, 60, '{"type":"sichuan"}', 1, 3, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000);

-- 德州扑克场次
INSERT INTO room_config (game_type, room_level, room_name, min_players, max_players, min_coins, max_coins, base_score, init_chips, round_time, match_timeout, extra_config, status, sort_order, create_time, update_time) VALUES
('texas', 'beginner', '初级场', 2, 9, 1000, 50000, 5, 1000, 30, 60, '{"blinds":[5,10]}', 1, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('texas', 'intermediate', '中级场', 2, 9, 50000, 200000, 25, 5000, 30, 60, '{"blinds":[25,50]}', 1, 2, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('texas', 'advanced', '高级场', 2, 9, 200000, 0, 100, 20000, 30, 60, '{"blinds":[100,200]}', 1, 3, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000);

-- 牛牛场次
INSERT INTO room_config (game_type, room_level, room_name, min_players, max_players, min_coins, max_coins, base_score, init_chips, round_time, match_timeout, extra_config, status, sort_order, create_time, update_time) VALUES
('niuniu', 'beginner', '初级场', 2, 6, 1000, 50000, 10, 0, 15, 60, '{}', 1, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('niuniu', 'intermediate', '中级场', 2, 6, 50000, 200000, 50, 0, 15, 60, '{}', 1, 2, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('niuniu', 'advanced', '高级场', 2, 6, 200000, 0, 200, 0, 15, 60, '{}', 1, 3, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000);

-- 五子棋场次
INSERT INTO room_config (game_type, room_level, room_name, min_players, max_players, min_coins, max_coins, base_score, init_chips, round_time, match_timeout, extra_config, status, sort_order, create_time, update_time) VALUES
('gobang', 'beginner', '初级场', 2, 2, 0, 0, 10, 0, 60, 60, '{"boardSize":15}', 1, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('gobang', 'intermediate', '中级场', 2, 2, 10000, 0, 50, 0, 60, 60, '{"boardSize":15}', 1, 2, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000);

-- MOBA场次
INSERT INTO room_config (game_type, room_level, room_name, min_players, max_players, min_coins, max_coins, base_score, init_chips, round_time, match_timeout, extra_config, status, sort_order, create_time, update_time) VALUES
('moba', 'normal', '匹配模式', 2, 10, 0, 0, 0, 0, 0, 120, '{"teamSize":5}', 1, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('moba', 'ranked', '排位模式', 2, 10, 0, 0, 0, 0, 0, 120, '{"teamSize":5}', 1, 2, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000);
