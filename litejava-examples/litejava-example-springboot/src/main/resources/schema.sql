-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    createdat TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 测试数据
INSERT INTO users (name, email) VALUES ('Alice', 'alice@test.com');
INSERT INTO users (name, email) VALUES ('Bob', 'bob@test.com');
