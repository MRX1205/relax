-- V12: 技师项目管理、技师全量资料与相册、地图常驻位置支持

-- 1. 项目表增加创建者类型与创建者ID（平台 / 技师）
ALTER TABLE service_project ADD COLUMN IF NOT EXISTS creator_type VARCHAR(20) DEFAULT 'PLATFORM' NOT NULL;
ALTER TABLE service_project ADD COLUMN IF NOT EXISTS creator_id BIGINT DEFAULT 0 NOT NULL;

-- 2. 技师表增加详细风采资料与常驻地图位置
ALTER TABLE technician ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500) NULL;
ALTER TABLE technician ADD COLUMN IF NOT EXISTS age INT NULL;
ALTER TABLE technician ADD COLUMN IF NOT EXISTS age_tag VARCHAR(32) NULL;
ALTER TABLE technician ADD COLUMN IF NOT EXISTS height INT NULL;
ALTER TABLE technician ADD COLUMN IF NOT EXISTS weight INT NULL;
ALTER TABLE technician ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 6) NULL;
ALTER TABLE technician ADD COLUMN IF NOT EXISTS longitude DECIMAL(10, 6) NULL;
ALTER TABLE technician ADD COLUMN IF NOT EXISTS base_address VARCHAR(255) NULL;
ALTER TABLE technician ADD COLUMN IF NOT EXISTS certifications_json VARCHAR(500) NULL;

-- 3. 技师照片表增加直链URL支持
ALTER TABLE technician_photo ADD COLUMN IF NOT EXISTS file_url VARCHAR(500) NULL;

-- 4. 初始化示例技师风采资料
UPDATE technician SET
    age = 28,
    age_tag = '95后',
    height = 168,
    weight = 50,
    certifications_json = '["实名认证", "健康档案", "金牌技师", "安心服务"]',
    latitude = 23.020670,
    longitude = 113.751790,
    base_address = '东莞市南城街道鸿福路200号第一国际'
WHERE id = 3001;

UPDATE technician SET
    age = 26,
    age_tag = '95后',
    height = 165,
    weight = 48,
    certifications_json = '["实名认证", "精油调配师", "健康档案", "安心服务"]',
    latitude = 23.035000,
    longitude = 113.738000,
    base_address = '东莞市莞城街道东正路20号'
WHERE id = 3002;

UPDATE technician SET
    age = 32,
    age_tag = '90后',
    height = 175,
    weight = 68,
    certifications_json = '["实名认证", "康复理疗师", "持证上岗", "安心服务"]',
    latitude = 22.980000,
    longitude = 113.880000,
    base_address = '东莞市东城街道东城西路58号'
WHERE id = 3003;
