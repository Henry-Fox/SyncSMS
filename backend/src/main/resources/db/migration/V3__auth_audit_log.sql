-- Flyway migration V3: 登录/注销审计日志

CREATE TABLE IF NOT EXISTS auth_audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       DEFAULT NULL COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    action      VARCHAR(20)  NOT NULL COMMENT '动作: login_success/login_fail/logout',
    ip          VARCHAR(64)  DEFAULT NULL COMMENT 'IP',
    user_agent  VARCHAR(255) DEFAULT NULL COMMENT 'User-Agent',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_username_created_at (username, created_at),
    KEY idx_action_created_at (action, created_at)
) ENGINE=InnoDB COMMENT='登录/注销审计日志';

