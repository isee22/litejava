-- 创建数据库
CREATE DATABASE IF NOT EXISTS litejava DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE litejava;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100),
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 会话表
CREATE TABLE IF NOT EXISTS sessions (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 图书表
CREATE TABLE IF NOT EXISTS books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100),
    isbn VARCHAR(20),
    description TEXT,
    cover_image VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 初始数据
INSERT INTO users (username, email, password) VALUES 
('admin', 'admin@example.com', 'admin123');

INSERT INTO books (title, author, isbn, description) VALUES 
('Java 编程思想', 'Bruce Eckel', '978-7-111-21382-6', 'Java 经典入门书籍'),
('深入理解 JVM', '周志明', '978-7-111-42190-0', 'JVM 原理深度解析'),
('Effective Java', 'Joshua Bloch', '978-0-13-468599-1', 'Java 最佳实践指南');
