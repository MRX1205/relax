-- 排班时段锁
CREATE TABLE schedule_lock (
    id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    lock_date DATE NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    order_no VARCHAR(32) NULL,
    expire_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_lock_tech_date (technician_id, lock_date, start_time, end_time),
    CONSTRAINT fk_lock_technician FOREIGN KEY (technician_id) REFERENCES technician (id)
);

-- 订单主表
CREATE TABLE service_order (
    id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
    service_date DATE NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    note VARCHAR(500) NULL,
    cancel_reason VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (order_no),
    INDEX idx_order_user (user_id),
    INDEX idx_order_tech (technician_id),
    INDEX idx_order_status (status),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES platform_user (id),
    CONSTRAINT fk_order_technician FOREIGN KEY (technician_id) REFERENCES technician (id),
    CONSTRAINT fk_order_project FOREIGN KEY (project_id) REFERENCES service_project (id)
);

-- 订单项目快照
CREATE TABLE order_project_snapshot (
    order_id BIGINT NOT NULL,
    project_name VARCHAR(100) NOT NULL,
    duration_minutes INT NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    override_price DECIMAL(10,2) NULL,
    actual_price DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (order_id),
    CONSTRAINT fk_snap_order FOREIGN KEY (order_id) REFERENCES service_order (id)
);

-- 订单地址快照
CREATE TABLE order_address_snapshot (
    order_id BIGINT NOT NULL,
    contact_name VARCHAR(40) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    region_name VARCHAR(64) NOT NULL,
    detail VARCHAR(255) NOT NULL,
    longitude DECIMAL(10,6) NULL,
    latitude DECIMAL(10,6) NULL,
    PRIMARY KEY (order_id),
    CONSTRAINT fk_addr_order FOREIGN KEY (order_id) REFERENCES service_order (id)
);

-- 订单金额
CREATE TABLE order_amount (
    order_id BIGINT NOT NULL,
    project_amount DECIMAL(10,2) NOT NULL,
    travel_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    payable_amount DECIMAL(10,2) NOT NULL,
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    refunded_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (order_id),
    CONSTRAINT fk_amt_order FOREIGN KEY (order_id) REFERENCES service_order (id)
);

-- 订单状态日志
CREATE TABLE order_status_log (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(20) NULL,
    to_status VARCHAR(20) NOT NULL,
    operator_type VARCHAR(20) NOT NULL,
    operator_id BIGINT NULL,
    reason VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_log_order (order_id),
    CONSTRAINT fk_log_order FOREIGN KEY (order_id) REFERENCES service_order (id)
);

-- 支付单
CREATE TABLE payment_order (
    id BIGINT NOT NULL,
    payment_no VARCHAR(32) NOT NULL,
    order_id BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'WECHAT',
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    prepay_id VARCHAR(128) NULL,
    transaction_id VARCHAR(64) NULL,
    expire_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (payment_no),
    INDEX idx_payment_order (order_id),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES service_order (id)
);

-- 支付通知
CREATE TABLE payment_notify (
    id BIGINT NOT NULL,
    payment_no VARCHAR(32) NOT NULL,
    raw_digest VARCHAR(128) NULL,
    transaction_id VARCHAR(64) NULL,
    process_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_notify_payment (payment_no)
);

-- 优惠券模板
CREATE TABLE coupon_template (
    id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    min_spend DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_count INT NOT NULL DEFAULT 0,
    issued_count INT NOT NULL DEFAULT 0,
    start_at TIMESTAMP NULL,
    end_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 用户优惠券
CREATE TABLE user_coupon (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    locked_order_id BIGINT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_coupon_user (user_id, status),
    CONSTRAINT fk_coupon_user FOREIGN KEY (user_id) REFERENCES platform_user (id),
    CONSTRAINT fk_coupon_template FOREIGN KEY (template_id) REFERENCES coupon_template (id)
);
