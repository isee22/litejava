-- 商品服务表结构

USE product_db;

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 商品表
CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `category_id` BIGINT COMMENT '分类ID',
    `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
    `description` TEXT COMMENT '描述',
    `image_url` VARCHAR(255) COMMENT '图片URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-下架，1-上架',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 初始数据
INSERT INTO `category` (`name`, `parent_id`, `sort_order`) VALUES
('手机数码', 0, 1),
('电脑办公', 0, 2),
('家用电器', 0, 3);

INSERT INTO `product` (`name`, `category_id`, `price`, `stock`, `description`) VALUES
('iPhone 15', 1, 6999.00, 100, 'Apple iPhone 15 128GB'),
('iPhone 15 Pro', 1, 8999.00, 50, 'Apple iPhone 15 Pro 256GB'),
('MacBook Pro 14', 2, 14999.00, 30, 'Apple MacBook Pro 14寸 M3芯片'),
('AirPods Pro', 1, 1899.00, 200, 'Apple AirPods Pro 第二代'),
('iPad Air', 1, 4799.00, 80, 'Apple iPad Air 10.9寸');
