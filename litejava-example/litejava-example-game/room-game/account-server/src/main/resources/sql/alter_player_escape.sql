-- 添加逃跑和信用分字段
ALTER TABLE player ADD COLUMN escapeGames INT DEFAULT 0 COMMENT '逃跑次数';
ALTER TABLE player ADD COLUMN creditScore INT DEFAULT 100 COMMENT '信用分';
