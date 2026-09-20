CREATE TABLE technician (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    service_name VARCHAR(64) NOT NULL,
    real_name VARCHAR(40) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    intro VARCHAR(500) NOT NULL DEFAULT '',
    experience_years INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    online_status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (user_id),
    CONSTRAINT fk_technician_user FOREIGN KEY (user_id) REFERENCES platform_user (id)
);

CREATE TABLE technician_application (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    service_name VARCHAR(64) NOT NULL,
    real_name VARCHAR(40) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    intro VARCHAR(500) NOT NULL DEFAULT '',
    experience_years INT NOT NULL DEFAULT 0,
    service_area_codes TEXT NOT NULL,
    photo_file_id BIGINT NULL,
    certificate_file_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500) NULL,
    reviewer_id BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_tech_app_user (user_id),
    CONSTRAINT fk_tech_app_user FOREIGN KEY (user_id) REFERENCES platform_user (id)
);
