-- Zipkin 链路追踪数据库
CREATE DATABASE IF NOT EXISTS zipkin DEFAULT CHARACTER SET utf8mb4;
USE zipkin;

CREATE TABLE IF NOT EXISTS zipkin_spans (
  trace_id_high BIGINT NOT NULL DEFAULT 0,
  trace_id BIGINT NOT NULL,
  id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  remote_service_name VARCHAR(255),
  parent_id BIGINT,
  debug BIT(1),
  start_ts BIGINT,
  duration BIGINT,
  PRIMARY KEY (trace_id_high, trace_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPRESSED;

CREATE TABLE IF NOT EXISTS zipkin_annotations (
  trace_id_high BIGINT NOT NULL DEFAULT 0,
  trace_id BIGINT NOT NULL,
  span_id BIGINT NOT NULL,
  a_key VARCHAR(255) NOT NULL,
  a_value BLOB,
  a_type INT NOT NULL,
  a_timestamp BIGINT,
  endpoint_ipv4 INT,
  endpoint_ipv6 BINARY(16),
  endpoint_port SMALLINT,
  endpoint_service_name VARCHAR(255),
  PRIMARY KEY (trace_id_high, trace_id, span_id, a_key, a_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPRESSED;

CREATE INDEX idx_spans_trace_id ON zipkin_spans(trace_id_high, trace_id);
CREATE INDEX idx_spans_start_ts ON zipkin_spans(start_ts);
CREATE INDEX idx_annotations_trace_id ON zipkin_annotations(trace_id_high, trace_id);
CREATE INDEX idx_annotations_ts ON zipkin_annotations(a_timestamp);
