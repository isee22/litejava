-- 创建数据库
CREATE DATABASE IF NOT EXISTS litejava DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE litejava;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100),
    password VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 测试数据
INSERT INTO users (username, email, password) VALUES
('admin', 'admin@example.com', '123456'),
('test', 'test@example.com', '123456'),
('demo', 'demo@example.com', '123456');
