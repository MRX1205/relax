-- 项目分类
CREATE TABLE service_category (
    id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 项目
CREATE TABLE service_project (
    id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    duration_minutes INT NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    description VARCHAR(1000) NOT NULL DEFAULT '',
    notice VARCHAR(500) NOT NULL DEFAULT '',
    cover_file_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_project_category FOREIGN KEY (category_id) REFERENCES service_category (id)
);

-- 技师项目定价（独立价格覆盖）
CREATE TABLE technician_project (
    id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    override_price DECIMAL(10,2) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (technician_id, project_id),
    CONSTRAINT fk_tech_proj_technician FOREIGN KEY (technician_id) REFERENCES technician (id),
    CONSTRAINT fk_tech_proj_project FOREIGN KEY (project_id) REFERENCES service_project (id)
);

-- 技师排班
CREATE TABLE technician_schedule (
    id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    schedule_date DATE NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_schedule_tech_date (technician_id, schedule_date),
    CONSTRAINT fk_schedule_technician FOREIGN KEY (technician_id) REFERENCES technician (id)
);

-- 技师证书
CREATE TABLE technician_certificate (
    id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    cert_type VARCHAR(40) NOT NULL,
    file_id BIGINT NOT NULL,
    certificate_no_masked VARCHAR(50) NULL,
    valid_from DATE NULL,
    valid_to DATE NULL,
    audit_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_cert_technician FOREIGN KEY (technician_id) REFERENCES technician (id)
);

-- 技师照片
CREATE TABLE technician_photo (
    id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    photo_type VARCHAR(20) NOT NULL DEFAULT 'WORK',
    sort INT NOT NULL DEFAULT 0,
    audit_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_photo_technician FOREIGN KEY (technician_id) REFERENCES technician (id)
);

-- 技师服务区域
CREATE TABLE technician_service_area (
    technician_id BIGINT NOT NULL,
    service_area_id BIGINT NOT NULL,
    PRIMARY KEY (technician_id, service_area_id),
    CONSTRAINT fk_tsa_technician FOREIGN KEY (technician_id) REFERENCES technician (id),
    CONSTRAINT fk_tsa_area FOREIGN KEY (service_area_id) REFERENCES service_area (id)
);
