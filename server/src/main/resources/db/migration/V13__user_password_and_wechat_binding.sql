-- V13: Support password authentication and WeChat openid linking
ALTER TABLE platform_user ADD COLUMN password_hash VARCHAR(255) NULL;
ALTER TABLE platform_user MODIFY COLUMN wechat_open_id VARCHAR(128) NULL;

-- Initial default password for demo accounts: 123456
-- BCrypt hash for '123456'
UPDATE platform_user 
SET password_hash = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi'
WHERE phone IN ('13800000000', '13800003333', '13800004444', '13800005555');
