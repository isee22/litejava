-- 认证服务表结构

USE auth_db;

-- 账号表
CREATE TABLE IF NOT EXISTS `account` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '账号ID',
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '关联用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
    `last_login_at` DATETIME COMMENT '最后登录时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号表';

-- 初始数据（密码：123456）
INSERT INTO `account` (`user_id`, `username`, `password_hash`) VALUES
(1, 'zhangsan', '123456'),
(2, 'lisi', '123456'),
(3, 'wangwu', '123456');
