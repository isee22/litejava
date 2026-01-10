CREATE DATABASE IF NOT EXISTS notification_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE notification_db;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'email/sms/push',
    title VARCHAR(200),
    content TEXT NOT NULL,
    target VARCHAR(200) NOT NULL COMMENT '邮箱/手机号/设备ID',
    status TINYINT DEFAULT 0 COMMENT '0:待发送 1:发送中 2:成功 3:失败',
    error_msg VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
);

CREATE TABLE IF NOT EXISTS templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    title_template VARCHAR(200),
    content_template TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO templates (code, name, type, title_template, content_template) VALUES 
('ORDER_CREATED', '订单创建通知', 'email', '您的订单已创建', '尊敬的用户，您的订单 ${orderNo} 已创建成功，金额 ${amount} 元。'),
('ORDER_PAID', '订单支付通知', 'email', '订单支付成功', '尊敬的用户，您的订单 ${orderNo} 已支付成功。'),
('ORDER_SHIPPED', '订单发货通知', 'sms', NULL, '您的订单 ${orderNo} 已发货，快递单号 ${trackingNo}。');
