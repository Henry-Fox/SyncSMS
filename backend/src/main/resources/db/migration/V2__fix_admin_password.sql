-- Flyway migration V2: 修正初始管理员密码为 admin123
-- 说明：V1 中的 BCrypt 示例 hash 实际并非 admin123，这里统一修正

UPDATE sys_user
SET password = '$2a$10$tigmIpSInIOzIk/.wqXfLuhJIfJV9gC8Wm9wEVP5IQlCJnTpas0Tq'
WHERE username = 'admin';

