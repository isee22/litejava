-- 支付服务表结构

USE payment_db;

-- 支付单表
CREATE TABLE IF NOT EXISTS `payment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支付ID',
    `payment_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '支付单号',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `channel` VARCHAR(20) COMMENT '支付渠道：alipay/wechat/unionpay',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待支付，1-处理中，2-成功，3-失败，4-已退款',
    `paid_at` DATETIME COMMENT '支付时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_payment_no` (`payment_no`),
    INDEX `idx_order_no` (`order_no`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单表';

-- 退款单表
CREATE TABLE IF NOT EXISTS `refund` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '退款ID',
    `refund_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '退款单号',
    `payment_no` VARCHAR(32) NOT NULL COMMENT '支付单号',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    `reason` VARCHAR(255) COMMENT '退款原因',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待处理，1-处理中，2-成功，3-失败',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_refund_no` (`refund_no`),
    INDEX `idx_payment_no` (`payment_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款单表';
