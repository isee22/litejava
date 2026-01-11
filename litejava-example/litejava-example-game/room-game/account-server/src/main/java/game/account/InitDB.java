package game.account;

import java.sql.*;

public class InitDB {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306?useSSL=false&serverTimezone=UTC&allowMultiQueries=true";
        String user = "root";
        String password = "123456";
        
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            // 创建数据库
            stmt.execute("CREATE DATABASE IF NOT EXISTS game CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.execute("USE game");
            System.out.println("数据库 game 创建成功");
            
            // 删除旧表重建
            stmt.execute("DROP TABLE IF EXISTS player");
            
            // 玩家表 (完整字段)
            stmt.execute("CREATE TABLE IF NOT EXISTS player (" +
                "userId BIGINT PRIMARY KEY," +
                "name VARCHAR(64) NOT NULL," +
                "avatar VARCHAR(256)," +
                "coins BIGINT NOT NULL DEFAULT 10000," +
                "diamonds BIGINT NOT NULL DEFAULT 0," +
                "level INT NOT NULL DEFAULT 1," +
                "exp BIGINT NOT NULL DEFAULT 0," +
                "vipLevel INT NOT NULL DEFAULT 0," +
                "vipExp INT NOT NULL DEFAULT 0," +
                "totalGames INT NOT NULL DEFAULT 0," +
                "winGames INT NOT NULL DEFAULT 0," +
                "loginDays INT NOT NULL DEFAULT 1," +
                "createTime BIGINT NOT NULL," +
                "lastLoginTime BIGINT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            System.out.println("表 player 创建成功");
            
            // 签到配置表
            stmt.execute("CREATE TABLE IF NOT EXISTS sign_in_config (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "day INT NOT NULL," +
                "rewardType VARCHAR(32) NOT NULL," +
                "rewardAmount INT NOT NULL," +
                "description VARCHAR(128)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            System.out.println("表 sign_in_config 创建成功");
            
            // 签到记录表
            stmt.execute("CREATE TABLE IF NOT EXISTS sign_in_record (" +
                "userId BIGINT PRIMARY KEY," +
                "signDay INT NOT NULL DEFAULT 0," +
                "lastSignTime BIGINT NOT NULL DEFAULT 0," +
                "totalSignDays INT NOT NULL DEFAULT 0" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            System.out.println("表 sign_in_record 创建成功");
            
            // 物品配置表
            stmt.execute("CREATE TABLE IF NOT EXISTS item_config (" +
                "item_id INT PRIMARY KEY," +
                "name VARCHAR(64) NOT NULL," +
                "type VARCHAR(32) NOT NULL," +
                "`desc` VARCHAR(256)," +
                "price INT NOT NULL DEFAULT 0," +
                "coin_price INT NOT NULL DEFAULT 0," +
                "icon VARCHAR(256)," +
                "status TINYINT NOT NULL DEFAULT 1" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            System.out.println("表 item_config 创建成功");
            
            // 玩家物品表
            stmt.execute("CREATE TABLE IF NOT EXISTS player_item (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "user_id BIGINT NOT NULL," +
                "item_id INT NOT NULL," +
                "count INT NOT NULL DEFAULT 1," +
                "create_time BIGINT NOT NULL," +
                "update_time BIGINT NOT NULL," +
                "UNIQUE KEY uk_user_item (user_id, item_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            System.out.println("表 player_item 创建成功");
            
            // 房间配置表
            stmt.execute("CREATE TABLE IF NOT EXISTS room_config (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "game_type VARCHAR(32) NOT NULL," +
                "room_level VARCHAR(32) NOT NULL," +
                "room_name VARCHAR(64) NOT NULL," +
                "min_players INT NOT NULL DEFAULT 2," +
                "max_players INT NOT NULL DEFAULT 4," +
                "min_coins INT NOT NULL DEFAULT 0," +
                "max_coins INT NOT NULL DEFAULT 0," +
                "base_score INT NOT NULL DEFAULT 1," +
                "init_chips INT NOT NULL DEFAULT 0," +
                "round_time INT NOT NULL DEFAULT 30," +
                "match_timeout INT NOT NULL DEFAULT 60," +
                "extra_config TEXT," +
                "status TINYINT NOT NULL DEFAULT 1," +
                "sort_order INT NOT NULL DEFAULT 0," +
                "create_time BIGINT NOT NULL," +
                "update_time BIGINT NOT NULL," +
                "UNIQUE KEY uk_game_level (game_type, room_level)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            System.out.println("表 room_config 创建成功");
            
            // 初始化数据
            long now = System.currentTimeMillis();
            stmt.execute("INSERT IGNORE INTO room_config (game_type, room_level, room_name, min_players, max_players, min_coins, base_score, round_time, match_timeout, status, create_time, update_time) VALUES " +
                "('doudizhu', 'beginner', '初级场', 3, 3, 1000, 10, 30, 60, 1, " + now + ", " + now + ")," +
                "('gobang', 'beginner', '初级场', 2, 2, 0, 10, 60, 60, 1, " + now + ", " + now + ")," +
                "('mahjong', 'beginner', '初级场', 4, 4, 1000, 1, 30, 60, 1, " + now + ", " + now + ")");
            System.out.println("初始化数据完成");
            
            System.out.println("\n=== 数据库初始化完成 ===");
        }
    }
}
