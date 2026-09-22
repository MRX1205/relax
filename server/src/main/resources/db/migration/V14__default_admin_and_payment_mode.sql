-- V14: Default admin account setup, permissions fix, and payment mode configuration

-- 1. Ensure default administrator account (13800000000) exists
-- Default password hash corresponds to '123456': $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
INSERT INTO platform_user (id, wechat_open_id, nickname, avatar_url, phone, status, last_role, password_hash)
VALUES (
    1000,
    'mock:admin-super-001',
    '系统管理员',
    NULL,
    '13800000000',
    'ACTIVE',
    'SUPER_ADMIN',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi'
)
ON DUPLICATE KEY UPDATE
    status = 'ACTIVE',
    password_hash = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    last_role = 'SUPER_ADMIN';

-- 2. Assign all essential roles: USER (1), TECHNICIAN (2), ADMIN (3), SUPER_ADMIN (4)
INSERT IGNORE INTO iam_user_role (user_id, role_id, status)
SELECT id, 1, 'ENABLED' FROM platform_user WHERE phone = '13800000000';

INSERT IGNORE INTO iam_user_role (user_id, role_id, status)
SELECT id, 2, 'ENABLED' FROM platform_user WHERE phone = '13800000000';

INSERT IGNORE INTO iam_user_role (user_id, role_id, status)
SELECT id, 3, 'ENABLED' FROM platform_user WHERE phone = '13800000000';

INSERT IGNORE INTO iam_user_role (user_id, role_id, status)
SELECT id, 4, 'ENABLED' FROM platform_user WHERE phone = '13800000000';

-- 3. Assign all permission groups (SUPPLY, ORDERS, FINANCE, OPERATIONS) to default admin 13800000000
INSERT IGNORE INTO iam_user_permission_group (user_id, group_id, granted_by)
SELECT id, 1, id FROM platform_user WHERE phone = '13800000000';
INSERT IGNORE INTO iam_user_permission_group (user_id, group_id, granted_by)
SELECT id, 2, id FROM platform_user WHERE phone = '13800000000';
INSERT IGNORE INTO iam_user_permission_group (user_id, group_id, granted_by)
SELECT id, 3, id FROM platform_user WHERE phone = '13800000000';
INSERT IGNORE INTO iam_user_permission_group (user_id, group_id, granted_by)
SELECT id, 4, id FROM platform_user WHERE phone = '13800000000';

-- 4. Ensure a technician profile exists for 13800000000 so tech workbench is not blank
INSERT INTO technician (id, user_id, service_name, real_name, phone, intro, experience_years, status, online_status)
SELECT 
    3000, 
    id, 
    '总台特聘·综合理疗', 
    '平台总管', 
    '13800000000', 
    '平台资深特聘理疗导师，精通各类中式推拿、精油SPA与特色调理。', 
    12, 
    'ACTIVE', 
    'ONLINE'
FROM platform_user 
WHERE phone = '13800000000'
ON DUPLICATE KEY UPDATE 
    status = 'ACTIVE',
    online_status = 'ONLINE';

-- 5. Payment mode configuration in payment_config table
-- Values: 'MOCK' (模拟支付), 'WXPAY' (真实微信支付), 'OFFLINE' (现场支付/仅预约无需支付)
INSERT INTO payment_config (id, config_key, config_value, description)
VALUES (10, 'payment.mode', 'MOCK', '平台支付模式: MOCK-模拟支付, WXPAY-微信支付, OFFLINE-仅预约现场支付')
ON DUPLICATE KEY UPDATE description = '平台支付模式: MOCK-模拟支付, WXPAY-微信支付, OFFLINE-仅预约现场支付';
