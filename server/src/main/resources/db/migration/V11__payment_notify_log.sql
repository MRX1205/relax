-- 支付通知日志表
CREATE TABLE payment_notify_log (
    id BIGINT NOT NULL,
    payment_no VARCHAR(32) NOT NULL,
    transaction_id VARCHAR(64) NOT NULL,
    result VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_pnl_payment_tx ON payment_notify_log (payment_no, transaction_id);
