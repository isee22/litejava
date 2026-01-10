-- 通知服务表结构

USE notification_db;

-- 通知记录表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `type` VARCHAR(20) NOT NULL COMMENT '类型：sms/email/push/wechat',
    `title` VARCHAR(100) COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '内容',
    `target` VARCHAR(100) COMMENT '目标：手机号/邮箱/设备ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待发送，1-发送中，2-成功，3-失败',
    `error_msg` VARCHAR(255) COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知记录表';

-- 通知模板表
CREATE TABLE IF NOT EXISTS `template` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '模板编码',
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `type` VARCHAR(20) NOT NULL COMMENT '类型：sms/email/push/wechat',
    `title_template` VARCHAR(100) COMMENT '标题模板',
    `content_template` TEXT NOT NULL COMMENT '内容模板',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知模板表';

-- 初始模板
INSERT INTO `template` (`code`, `name`, `type`, `title_template`, `content_template`) VALUES
('ORDER_CREATED', '订单创建通知', 'sms', '订单通知', '您的订单 ${orderNo} 已创建，金额 ${amount} 元'),
('ORDER_PAID', '订单支付成功', 'sms', '支付成功', '您的订单 ${orderNo} 已支付成功'),
('ORDER_SHIPPED', '订单发货通知', 'sms', '发货通知', '您的订单 ${orderNo} 已发货，快递单号：${expressNo}'),
('VERIFY_CODE', '验证码', 'sms', '验证码', '您的验证码是 ${code}，5分钟内有效');
