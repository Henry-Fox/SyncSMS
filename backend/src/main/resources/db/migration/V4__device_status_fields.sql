-- Flyway migration V4: 设备在线/电量状态字段

ALTER TABLE device
    ADD COLUMN battery_percent TINYINT DEFAULT NULL COMMENT '电量百分比 0-100' AFTER last_sync_at,
    ADD COLUMN is_charging TINYINT DEFAULT NULL COMMENT '是否充电 0-否 1-是' AFTER battery_percent,
    ADD COLUMN last_seen_at DATETIME DEFAULT NULL COMMENT '最后在线时间（心跳）' AFTER is_charging;

CREATE INDEX idx_device_last_seen_at ON device (last_seen_at);

