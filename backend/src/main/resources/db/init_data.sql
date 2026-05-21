-- ==========================================
-- 在线教育直播平台全新安装初始化数据脚本
-- 仅包含用于首次登录的默认管理员账号
-- 默认管理员用户名: admin，密码: 123456
-- ==========================================

USE edu_live;

-- 插入默认管理员账号（如果不存在）
INSERT INTO user (id, username, password, email, role, status, avatar) 
VALUES (1, 'admin', '$2a$10$8m7thlvzi/y/DsNPR2o9Ee6OpJJ3nvRz4KiJHbozsyrOIsS89kSm6', 'admin@edu-live.com', 'ADMIN', 1, '/uploads/avatar/default_admin.jpg')
ON DUPLICATE KEY UPDATE username = VALUES(username);
