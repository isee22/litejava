-- 迁移数据库列名从下划线命名到驼峰命名

-- account 表
ALTER TABLE account CHANGE create_time createTime BIGINT NOT NULL;
ALTER TABLE account CHANGE last_login_time lastLoginTime BIGINT NOT NULL;

-- friend 表
ALTER TABLE friend CHANGE user_id userId BIGINT NOT NULL;
ALTER TABLE friend CHANGE friend_id friendId BIGINT NOT NULL;
ALTER TABLE friend CHANGE create_time createTime BIGINT NOT NULL;

-- friend_request 表
ALTER TABLE friend_request CHANGE from_id fromId BIGINT NOT NULL;
ALTER TABLE friend_request CHANGE to_id toId BIGINT NOT NULL;
ALTER TABLE friend_request CHANGE create_time createTime BIGINT NOT NULL;
ALTER TABLE friend_request CHANGE update_time updateTime BIGINT NOT NULL;

-- item_config 表
ALTER TABLE item_config CHANGE item_id itemId INT NOT NULL;
ALTER TABLE item_config CHANGE coin_price coinPrice INT NOT NULL DEFAULT 0;
ALTER TABLE item_config CHANGE sort_order sortOrder INT NOT NULL DEFAULT 0;

-- player_item 表
ALTER TABLE player_item CHANGE user_id userId BIGINT NOT NULL;
ALTER TABLE player_item CHANGE item_id itemId INT NOT NULL;
ALTER TABLE player_item CHANGE expire_time expireTime BIGINT NOT NULL DEFAULT 0;
ALTER TABLE player_item CHANGE create_time createTime BIGINT NOT NULL;
ALTER TABLE player_item CHANGE update_time updateTime BIGINT NOT NULL;

-- room_config 表
ALTER TABLE room_config CHANGE game_type gameType VARCHAR(32) NOT NULL;
ALTER TABLE room_config CHANGE room_level roomLevel INT NOT NULL;
ALTER TABLE room_config CHANGE room_name roomName VARCHAR(64) NOT NULL;
ALTER TABLE room_config CHANGE min_players minPlayers INT NOT NULL DEFAULT 2;
ALTER TABLE room_config CHANGE max_players maxPlayers INT NOT NULL DEFAULT 4;
ALTER TABLE room_config CHANGE min_coins minCoins INT NOT NULL DEFAULT 0;
ALTER TABLE room_config CHANGE max_coins maxCoins INT NOT NULL DEFAULT 0;
ALTER TABLE room_config CHANGE base_score baseScore INT NOT NULL DEFAULT 1;
ALTER TABLE room_config CHANGE init_chips initChips INT NOT NULL DEFAULT 0;
ALTER TABLE room_config CHANGE round_time roundTime INT NOT NULL DEFAULT 30;
ALTER TABLE room_config CHANGE match_timeout matchTimeout INT NOT NULL DEFAULT 60;
ALTER TABLE room_config CHANGE extra_config extraConfig TEXT;
ALTER TABLE room_config CHANGE sort_order sortOrder INT NOT NULL DEFAULT 0;
ALTER TABLE room_config CHANGE create_time createTime BIGINT NOT NULL;
ALTER TABLE room_config CHANGE update_time updateTime BIGINT NOT NULL;

-- sign_in_config 表 (需要重建，结构变化较大)
DROP TABLE IF EXISTS sign_in_config;
CREATE TABLE sign_in_config (
    day INT NOT NULL PRIMARY KEY,
    coins INT NOT NULL DEFAULT 0,
    diamonds INT NOT NULL DEFAULT 0,
    itemIds VARCHAR(256),
    `desc` VARCHAR(256)
);

-- game_replay 表
ALTER TABLE game_replay CHANGE replay_id replayId VARCHAR(32) NOT NULL;
ALTER TABLE game_replay CHANGE game_type gameType VARCHAR(32) NOT NULL;
ALTER TABLE game_replay CHANGE room_id roomId VARCHAR(32) NOT NULL;
ALTER TABLE game_replay CHANGE player_names playerNames VARCHAR(512) NOT NULL;
ALTER TABLE game_replay CHANGE init_state initState TEXT;
ALTER TABLE game_replay CHANGE action_count actionCount INT NOT NULL DEFAULT 0;
ALTER TABLE game_replay CHANGE start_time startTime BIGINT NOT NULL;
ALTER TABLE game_replay CHANGE end_time endTime BIGINT NOT NULL DEFAULT 0;

-- user_replay 表
ALTER TABLE user_replay CHANGE user_id userId BIGINT NOT NULL;
ALTER TABLE user_replay CHANGE replay_id replayId VARCHAR(32) NOT NULL;
ALTER TABLE user_replay CHANGE create_time createTime BIGINT NOT NULL;
