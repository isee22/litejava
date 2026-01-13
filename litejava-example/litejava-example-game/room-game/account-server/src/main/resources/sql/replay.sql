-- 游戏录像表
CREATE TABLE IF NOT EXISTS game_replay (
    replay_id VARCHAR(32) PRIMARY KEY,
    game_type VARCHAR(32) NOT NULL,
    room_id VARCHAR(32) NOT NULL,
    players VARCHAR(256) NOT NULL COMMENT '玩家ID列表,逗号分隔',
    player_names VARCHAR(512) NOT NULL COMMENT '玩家名字列表,逗号分隔',
    winner INT NOT NULL DEFAULT -1,
    init_state TEXT COMMENT '初始状态JSON',
    actions TEXT COMMENT '动作列表JSON',
    action_count INT NOT NULL DEFAULT 0,
    start_time BIGINT NOT NULL,
    end_time BIGINT NOT NULL DEFAULT 0,
    INDEX idx_players (players(64)),
    INDEX idx_game_type (game_type),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户录像索引表 (加速查询用户的录像)
CREATE TABLE IF NOT EXISTS user_replay (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    replay_id VARCHAR(32) NOT NULL,
    create_time BIGINT NOT NULL,
    INDEX idx_user_time (user_id, create_time DESC),
    UNIQUE KEY uk_user_replay (user_id, replay_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
