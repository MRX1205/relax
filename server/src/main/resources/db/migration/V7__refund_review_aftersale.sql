-- 退款单
CREATE TABLE refund_order (
    id BIGINT NOT NULL,
    refund_no VARCHAR(32) NOT NULL,
    order_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    operator_id BIGINT NULL,
    wechat_refund_id VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (refund_no),
    INDEX idx_refund_order (order_id),
    CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES service_order (id)
);

-- 退款通知
CREATE TABLE refund_notify (
    id BIGINT NOT NULL,
    refund_no VARCHAR(32) NOT NULL,
    wechat_refund_id VARCHAR(64) NULL,
    process_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_refund_notify (refund_no)
);

-- 用户评价
CREATE TABLE review (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    score INT NOT NULL,
    content VARCHAR(1000) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (order_id),
    INDEX idx_review_tech (technician_id),
    CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES service_order (id),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES platform_user (id)
);

-- 售后工单
CREATE TABLE after_sale_case (
    id BIGINT NOT NULL,
    case_no VARCHAR(32) NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assignee_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (case_no),
    INDEX idx_aftersale_order (order_id),
    CONSTRAINT fk_aftersale_order FOREIGN KEY (order_id) REFERENCES service_order (id),
    CONSTRAINT fk_aftersale_user FOREIGN KEY (user_id) REFERENCES platform_user (id)
);

-- 售后处理记录
CREATE TABLE after_sale_record (
    id BIGINT NOT NULL,
    case_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_record_case (case_id),
    CONSTRAINT fk_record_case FOREIGN KEY (case_id) REFERENCES after_sale_case (id)
);

-- 订单增加退款金额跟踪
ALTER TABLE service_order ADD COLUMN refunded_amount DECIMAL(10,2) NOT NULL DEFAULT 0;
