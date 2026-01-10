-- Nacos 配置中心数据库
CREATE DATABASE IF NOT EXISTS nacos DEFAULT CHARACTER SET utf8mb4;
USE nacos;

-- 配置信息表
CREATE TABLE IF NOT EXISTS config_info (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  data_id varchar(255) NOT NULL,
  group_id varchar(128) DEFAULT NULL,
  content longtext NOT NULL,
  md5 varchar(32) DEFAULT NULL,
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  src_user text,
  src_ip varchar(50) DEFAULT NULL,
  app_name varchar(128) DEFAULT NULL,
  tenant_id varchar(128) DEFAULT '',
  c_desc varchar(256) DEFAULT NULL,
  c_use varchar(64) DEFAULT NULL,
  effect varchar(64) DEFAULT NULL,
  type varchar(64) DEFAULT NULL,
  c_schema text,
  encrypted_data_key text,
  PRIMARY KEY (id),
  UNIQUE KEY uk_configinfo_datagrouptenant (data_id,group_id,tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 配置历史表
CREATE TABLE IF NOT EXISTS his_config_info (
  id bigint(20) unsigned NOT NULL,
  nid bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  data_id varchar(255) NOT NULL,
  group_id varchar(128) NOT NULL,
  app_name varchar(128) DEFAULT NULL,
  content longtext NOT NULL,
  md5 varchar(32) DEFAULT NULL,
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  src_user text,
  src_ip varchar(50) DEFAULT NULL,
  op_type char(10) DEFAULT NULL,
  tenant_id varchar(128) DEFAULT '',
  encrypted_data_key text,
  PRIMARY KEY (nid),
  KEY idx_gmt_create (gmt_create),
  KEY idx_gmt_modified (gmt_modified),
  KEY idx_did (data_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 租户信息表
CREATE TABLE IF NOT EXISTS tenant_info (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  kp varchar(128) NOT NULL,
  tenant_id varchar(128) DEFAULT '',
  tenant_name varchar(128) DEFAULT '',
  tenant_desc varchar(256) DEFAULT NULL,
  create_source varchar(32) DEFAULT NULL,
  gmt_create bigint(20) NOT NULL,
  gmt_modified bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_info_kptenantid (kp,tenant_id),
  KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
