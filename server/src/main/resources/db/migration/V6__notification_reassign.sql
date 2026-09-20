-- 通知消息
CREATE TABLE notification (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(500) NOT NULL,
    related_order_no VARCHAR(32) NULL,
    read_at TIMESTAMP NULL,
    send_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_notify_user (user_id, read_at),
    CONSTRAINT fk_notify_user FOREIGN KEY (user_id) REFERENCES platform_user (id)
);

-- 改派记录
CREATE TABLE order_reassignment (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    from_technician_id BIGINT NOT NULL,
    to_technician_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    operator_id BIGINT NOT NULL,
    user_confirmed TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_reassign_order (order_id),
    CONSTRAINT fk_reassign_order FOREIGN KEY (order_id) REFERENCES service_order (id)
);

-- 订单增加接单时间字段
ALTER TABLE service_order ADD COLUMN accepted_at TIMESTAMP NULL;
ALTER TABLE service_order ADD COLUMN service_started_at TIMESTAMP NULL;
ALTER TABLE service_order ADD COLUMN service_completed_at TIMESTAMP NULL;
