-- 技师收入明细
CREATE TABLE technician_income (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    gross_amount DECIMAL(10,2) NOT NULL,
    platform_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
    payable_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (order_id),
    INDEX idx_income_tech (technician_id, status),
    CONSTRAINT fk_income_order FOREIGN KEY (order_id) REFERENCES service_order (id),
    CONSTRAINT fk_income_tech FOREIGN KEY (technician_id) REFERENCES technician (id)
);

-- 结算单
CREATE TABLE settlement_batch (
    id BIGINT NOT NULL,
    settlement_no VARCHAR(32) NOT NULL,
    technician_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    operator_id BIGINT NULL,
    paid_at TIMESTAMP NULL,
    proof_file_id BIGINT NULL,
    reference_no VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (settlement_no),
    INDEX idx_settle_tech (technician_id),
    CONSTRAINT fk_settle_tech FOREIGN KEY (technician_id) REFERENCES technician (id)
);

-- 结算明细
CREATE TABLE settlement_item (
    id BIGINT NOT NULL,
    settlement_id BIGINT NOT NULL,
    income_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (income_id),
    CONSTRAINT fk_item_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_batch (id),
    CONSTRAINT fk_item_income FOREIGN KEY (income_id) REFERENCES technician_income (id)
);

-- 轮播图
CREATE TABLE banner (
    id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    image_file_id BIGINT NULL,
    link_type VARCHAR(20) NULL,
    link_value VARCHAR(200) NULL,
    sort INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 订单完成后自动生成收入明细（通过应用层实现）
