CREATE TABLE platform_user (
    id BIGINT NOT NULL,
    wechat_open_id VARCHAR(128) NOT NULL,
    union_id VARCHAR(128) NULL,
    nickname VARCHAR(64) NOT NULL DEFAULT '',
    avatar_url VARCHAR(500) NULL,
    phone VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_role VARCHAR(32) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (wechat_open_id),
    UNIQUE (phone)
);

CREATE TABLE auth_access_token (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    token_digest CHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (token_digest),
    INDEX idx_auth_token_user (user_id),
    CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES platform_user (id)
);

CREATE TABLE iam_role (
    id BIGINT NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    PRIMARY KEY (id),
    UNIQUE (code)
);

CREATE TABLE iam_permission (
    id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (code)
);

CREATE TABLE iam_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    granted_by BIGINT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES platform_user (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES iam_role (id)
);

CREATE TABLE iam_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES iam_role (id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES iam_permission (id)
);

CREATE TABLE iam_permission_group (
    id BIGINT NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    PRIMARY KEY (id),
    UNIQUE (code)
);

CREATE TABLE iam_group_permission (
    group_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (group_id, permission_id),
    CONSTRAINT fk_group_permission_group FOREIGN KEY (group_id) REFERENCES iam_permission_group (id),
    CONSTRAINT fk_group_permission_permission FOREIGN KEY (permission_id) REFERENCES iam_permission (id)
);

CREATE TABLE iam_user_permission_group (
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    granted_by BIGINT NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, group_id),
    CONSTRAINT fk_user_group_user FOREIGN KEY (user_id) REFERENCES platform_user (id),
    CONSTRAINT fk_user_group_group FOREIGN KEY (group_id) REFERENCES iam_permission_group (id)
);

CREATE TABLE service_region (
    id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    code VARCHAR(12) NOT NULL,
    name VARCHAR(64) NOT NULL,
    level VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    PRIMARY KEY (id),
    UNIQUE (code)
);

CREATE TABLE service_area (
    id BIGINT NOT NULL,
    region_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (region_id),
    CONSTRAINT fk_service_area_region FOREIGN KEY (region_id) REFERENCES service_region (id)
);

CREATE TABLE user_address (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    contact_name VARCHAR(40) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    region_id BIGINT NOT NULL,
    detail VARCHAR(255) NOT NULL,
    longitude DECIMAL(10, 6) NOT NULL,
    latitude DECIMAL(10, 6) NOT NULL,
    label VARCHAR(20) NULL,
    is_default TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_address_user (user_id),
    CONSTRAINT fk_user_address_user FOREIGN KEY (user_id) REFERENCES platform_user (id),
    CONSTRAINT fk_user_address_region FOREIGN KEY (region_id) REFERENCES service_region (id)
);

CREATE TABLE file_asset (
    id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    storage_provider VARCHAR(20) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(180) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    expected_size BIGINT NOT NULL,
    actual_size BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    upload_token_digest CHAR(64) NULL,
    upload_expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    UNIQUE (object_key),
    INDEX idx_file_owner (owner_user_id),
    CONSTRAINT fk_file_owner FOREIGN KEY (owner_user_id) REFERENCES platform_user (id)
);

CREATE TABLE content_agreement (
    id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    version VARCHAR(30) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    effective_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (type, version)
);

CREATE TABLE user_agreement_consent (
    user_id BIGINT NOT NULL,
    agreement_id BIGINT NOT NULL,
    context VARCHAR(40) NOT NULL,
    agreed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, agreement_id, context),
    CONSTRAINT fk_consent_user FOREIGN KEY (user_id) REFERENCES platform_user (id),
    CONSTRAINT fk_consent_agreement FOREIGN KEY (agreement_id) REFERENCES content_agreement (id)
);

CREATE TABLE audit_log (
    id BIGINT NOT NULL,
    operator_id BIGINT NULL,
    action VARCHAR(80) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(80) NULL,
    detail VARCHAR(1000) NULL,
    ip_address VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_operator (operator_id)
);

INSERT INTO iam_role (id, code, name) VALUES
    (1, 'USER', '用户'),
    (2, 'TECHNICIAN', '技师'),
    (3, 'ADMIN', '管理员'),
    (4, 'SUPER_ADMIN', '超级管理员');

INSERT INTO iam_permission (id, code, name) VALUES
    (1, 'project:read', '查看项目'),
    (2, 'project:write', '管理项目'),
    (3, 'technician:audit', '审核技师'),
    (4, 'order:read', '查看订单'),
    (5, 'order:manage', '处理订单'),
    (6, 'order:refund', '订单退款'),
    (7, 'settlement:read', '查看结算'),
    (8, 'settlement:manage', '管理结算'),
    (9, 'content:manage', '管理运营内容'),
    (10, 'region:manage', '管理服务区域'),
    (11, 'file:private:read', '读取私有文件'),
    (12, 'admin:manage', '管理管理员权限'),
    (13, 'audit:read', '查看审计日志');

INSERT INTO iam_role_permission (role_id, permission_id) VALUES
    (4, 1), (4, 2), (4, 3), (4, 4), (4, 5), (4, 6), (4, 7),
    (4, 8), (4, 9), (4, 10), (4, 11), (4, 12), (4, 13);

INSERT INTO iam_permission_group (id, code, name) VALUES
    (1, 'SUPPLY', '供给管理'),
    (2, 'ORDERS', '订单管理'),
    (3, 'FINANCE', '财务管理'),
    (4, 'OPERATIONS', '运营管理');

INSERT INTO iam_group_permission (group_id, permission_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 11),
    (2, 4), (2, 5), (2, 11),
    (3, 4), (3, 6), (3, 7), (3, 8), (3, 11),
    (4, 1), (4, 9), (4, 10), (4, 13);

INSERT INTO service_region (id, parent_id, code, name, level) VALUES
    (441900000, NULL, '441900', '东莞市', 'CITY'),
    (441900003, 441900000, '441900003', '东城街道', 'TOWN'),
    (441900004, 441900000, '441900004', '南城街道', 'TOWN'),
    (441900005, 441900000, '441900005', '万江街道', 'TOWN'),
    (441900006, 441900000, '441900006', '莞城街道', 'TOWN'),
    (441900101, 441900000, '441900101', '石碣镇', 'TOWN'),
    (441900102, 441900000, '441900102', '石龙镇', 'TOWN'),
    (441900103, 441900000, '441900103', '茶山镇', 'TOWN'),
    (441900104, 441900000, '441900104', '石排镇', 'TOWN'),
    (441900105, 441900000, '441900105', '企石镇', 'TOWN'),
    (441900106, 441900000, '441900106', '横沥镇', 'TOWN'),
    (441900107, 441900000, '441900107', '桥头镇', 'TOWN'),
    (441900108, 441900000, '441900108', '谢岗镇', 'TOWN'),
    (441900109, 441900000, '441900109', '东坑镇', 'TOWN'),
    (441900110, 441900000, '441900110', '常平镇', 'TOWN'),
    (441900111, 441900000, '441900111', '寮步镇', 'TOWN'),
    (441900112, 441900000, '441900112', '大朗镇', 'TOWN'),
    (441900113, 441900000, '441900113', '黄江镇', 'TOWN'),
    (441900114, 441900000, '441900114', '清溪镇', 'TOWN'),
    (441900115, 441900000, '441900115', '塘厦镇', 'TOWN'),
    (441900116, 441900000, '441900116', '凤岗镇', 'TOWN'),
    (441900117, 441900000, '441900117', '樟木头镇', 'TOWN'),
    (441900118, 441900000, '441900118', '大岭山镇', 'TOWN'),
    (441900119, 441900000, '441900119', '长安镇', 'TOWN'),
    (441900121, 441900000, '441900121', '虎门镇', 'TOWN'),
    (441900122, 441900000, '441900122', '厚街镇', 'TOWN'),
    (441900123, 441900000, '441900123', '沙田镇', 'TOWN'),
    (441900124, 441900000, '441900124', '道滘镇', 'TOWN'),
    (441900125, 441900000, '441900125', '洪梅镇', 'TOWN'),
    (441900126, 441900000, '441900126', '麻涌镇', 'TOWN'),
    (441900127, 441900000, '441900127', '望牛墩镇', 'TOWN'),
    (441900128, 441900000, '441900128', '中堂镇', 'TOWN'),
    (441900129, 441900000, '441900129', '高埗镇', 'TOWN');

INSERT INTO service_area (id, region_id, name)
SELECT id, id, name FROM service_region WHERE level = 'TOWN';

INSERT INTO content_agreement (id, type, version, title, content, effective_at) VALUES
    (1, 'USER_AGREEMENT', '1.0', '用户协议', '本版本为开发阶段协议占位文本，正式上线前须由项目方审核并替换。', CURRENT_TIMESTAMP),
    (2, 'PRIVACY_POLICY', '1.0', '隐私政策', '平台仅在提供账号、联系和上门服务所必需的范围内处理个人信息。正式上线前须完成隐私合规审核。', CURRENT_TIMESTAMP),
    (3, 'TRANSACTION_RULES', '1.0', '交易规则', '本版本为开发阶段交易规则占位文本，订单与支付阶段确认后更新。', CURRENT_TIMESTAMP);
