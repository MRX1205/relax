-- V17: Mediumblob storage, new admin account 13926700205, and mock data management settings

-- 1. Expand file_asset content column from BLOB (64KB) to MEDIUMBLOB (16MB) for high-res images
ALTER TABLE file_asset MODIFY COLUMN content MEDIUMBLOB NULL;

-- 2. Insert or update the required administrator account (13926700205 / 13926700205)
-- BCrypt hash for '13926700205'
INSERT INTO platform_user (id, wechat_open_id, nickname, avatar_url, phone, status, last_role, password_hash)
VALUES (
    1001,
    'mock:admin-13926700205',
    '超级管理员',
    NULL,
    '13926700205',
    'ACTIVE',
    'SUPER_ADMIN',
    '$2a$10$w0u3c3kH37HhTsk1nEsqG.J626.0l0.77X937f3747X595.6.8622'
)
ON DUPLICATE KEY UPDATE
    status = 'ACTIVE',
    last_role = 'SUPER_ADMIN';

-- Assign all essential roles to 13926700205: USER (1), TECHNICIAN (2), ADMIN (3), SUPER_ADMIN (4)
INSERT IGNORE INTO iam_user_role (user_id, role_id, status)
SELECT id, 1, 'ENABLED' FROM platform_user WHERE phone = '13926700205';

INSERT IGNORE INTO iam_user_role (user_id, role_id, status)
SELECT id, 2, 'ENABLED' FROM platform_user WHERE phone = '13926700205';

INSERT IGNORE INTO iam_user_role (user_id, role_id, status)
SELECT id, 3, 'ENABLED' FROM platform_user WHERE phone = '13926700205';

INSERT IGNORE INTO iam_user_role (user_id, role_id, status)
SELECT id, 4, 'ENABLED' FROM platform_user WHERE phone = '13926700205';

-- Assign all permission groups (SUPPLY, ORDERS, FINANCE, OPERATIONS) to 13926700205
INSERT IGNORE INTO iam_user_permission_group (user_id, group_id, granted_by)
SELECT id, 1, id FROM platform_user WHERE phone = '13926700205';
INSERT IGNORE INTO iam_user_permission_group (user_id, group_id, granted_by)
SELECT id, 2, id FROM platform_user WHERE phone = '13926700205';
INSERT IGNORE INTO iam_user_permission_group (user_id, group_id, granted_by)
SELECT id, 3, id FROM platform_user WHERE phone = '13926700205';
INSERT IGNORE INTO iam_user_permission_group (user_id, group_id, granted_by)
SELECT id, 4, id FROM platform_user WHERE phone = '13926700205';

-- Ensure a technician profile exists for 13926700205
INSERT INTO technician (id, user_id, service_name, real_name, phone, intro, experience_years, status, online_status)
SELECT 
    3005, 
    id, 
    '总监理疗师', 
    '管理员', 
    '13926700205', 
    '平台总监理疗导师，精通各类中式推拿、精油SPA与特色调理。', 
    10, 
    'ACTIVE', 
    'ONLINE'
FROM platform_user 
WHERE phone = '13926700205'
ON DUPLICATE KEY UPDATE 
    status = 'ACTIVE',
    online_status = 'ONLINE';

-- 3. Mock data toggle configuration in app_setting
INSERT INTO app_setting (setting_key, setting_value, description)
VALUES ('mock.data.enabled', 'false', 'Mock演示数据开关: true-显示演示数据, false-纯净真实数据')
ON DUPLICATE KEY UPDATE description = 'Mock演示数据开关: true-显示演示数据, false-纯净真实数据';
