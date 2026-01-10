-- Seata 分布式事务数据库
CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4;
USE seata;

-- 全局事务表
CREATE TABLE IF NOT EXISTS global_table (
  xid VARCHAR(128) NOT NULL,
  transaction_id BIGINT,
  status TINYINT NOT NULL,
  application_id VARCHAR(32),
  transaction_service_group VARCHAR(32),
  transaction_name VARCHAR(128),
  timeout INT,
  begin_time BIGINT,
  application_data VARCHAR(2000),
  gmt_create DATETIME,
  gmt_modified DATETIME,
  PRIMARY KEY (xid),
  KEY idx_status_gmt_modified (status, gmt_modified),
  KEY idx_transaction_id (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分支事务表
CREATE TABLE IF NOT EXISTS branch_table (
  branch_id BIGINT NOT NULL,
  xid VARCHAR(128) NOT NULL,
  transaction_id BIGINT,
  resource_group_id VARCHAR(32),
  resource_id VARCHAR(256),
  branch_type VARCHAR(8),
  status TINYINT,
  client_id VARCHAR(64),
  application_data VARCHAR(2000),
  gmt_create DATETIME(6),
  gmt_modified DATETIME(6),
  PRIMARY KEY (branch_id),
  KEY idx_xid (xid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 锁表
CREATE TABLE IF NOT EXISTS lock_table (
  row_key VARCHAR(128) NOT NULL,
  xid VARCHAR(128),
  transaction_id BIGINT,
  branch_id BIGINT NOT NULL,
  resource_id VARCHAR(256),
  table_name VARCHAR(32),
  pk VARCHAR(36),
  status TINYINT NOT NULL DEFAULT 0,
  gmt_create DATETIME,
  gmt_modified DATETIME,
  PRIMARY KEY (row_key),
  KEY idx_branch_id (branch_id),
  KEY idx_xid (xid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS distributed_lock (
  lock_key VARCHAR(20) NOT NULL,
  lock_value VARCHAR(20) NOT NULL,
  expire BIGINT,
  PRIMARY KEY (lock_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO distributed_lock (lock_key, lock_value, expire) VALUES ('AsyncCommitting', ' ', 0);
INSERT INTO distributed_lock (lock_key, lock_value, expire) VALUES ('RetryCommitting', ' ', 0);
INSERT INTO distributed_lock (lock_key, lock_value, expire) VALUES ('RetryRollbacking', ' ', 0);
INSERT INTO distributed_lock (lock_key, lock_value, expire) VALUES ('TxTimeoutCheck', ' ', 0);

-- 业务数据库的 undo_log 表（每个业务库都需要）
USE litejava_order;
CREATE TABLE IF NOT EXISTS undo_log (
  branch_id BIGINT NOT NULL,
  xid VARCHAR(128) NOT NULL,
  context VARCHAR(128) NOT NULL,
  rollback_info LONGBLOB NOT NULL,
  log_status INT NOT NULL,
  log_created DATETIME NOT NULL,
  log_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (branch_id, xid),
  KEY idx_log_created (log_created)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE litejava_product;
CREATE TABLE IF NOT EXISTS undo_log (
  branch_id BIGINT NOT NULL,
  xid VARCHAR(128) NOT NULL,
  context VARCHAR(128) NOT NULL,
  rollback_info LONGBLOB NOT NULL,
  log_status INT NOT NULL,
  log_created DATETIME NOT NULL,
  log_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (branch_id, xid),
  KEY idx_log_created (log_created)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE litejava_payment;
CREATE TABLE IF NOT EXISTS undo_log (
  branch_id BIGINT NOT NULL,
  xid VARCHAR(128) NOT NULL,
  context VARCHAR(128) NOT NULL,
  rollback_info LONGBLOB NOT NULL,
  log_status INT NOT NULL,
  log_created DATETIME NOT NULL,
  log_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (branch_id, xid),
  KEY idx_log_created (log_created)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
