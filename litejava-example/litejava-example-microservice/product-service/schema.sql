CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE product_db;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    category_id BIGINT,
    price DECIMAL(10, 2) NOT NULL,
    stock INT DEFAULT 0,
    description TEXT,
    image_url VARCHAR(500),
    status TINYINT DEFAULT 1 COMMENT '1:上架 0:下架',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

INSERT INTO categories (name, parent_id, sort_order) VALUES 
('电子产品', 0, 1),
('手机', 1, 1),
('电脑', 1, 2),
('配件', 1, 3);

INSERT INTO products (name, category_id, price, stock, description, status) VALUES 
('iPhone 15 Pro', 2, 8999.00, 100, 'Apple iPhone 15 Pro 256GB', 1),
('MacBook Pro 14', 3, 14999.00, 50, 'Apple MacBook Pro 14英寸 M3芯片', 1),
('AirPods Pro 2', 4, 1899.00, 200, 'Apple AirPods Pro 第二代', 1),
('iPad Air', 1, 4799.00, 80, 'Apple iPad Air 10.9英寸', 1);
