-- Add content column for database-backed file storage
ALTER TABLE file_asset ADD COLUMN content BLOB NULL;

-- Add payment configuration table
CREATE TABLE payment_config (
    id BIGINT NOT NULL,
    config_key VARCHAR(64) NOT NULL,
    config_value TEXT NOT NULL,
    description VARCHAR(200) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (config_key)
);

-- Insert default payment config placeholders
INSERT INTO payment_config (id, config_key, config_value, description) VALUES
(1, 'wxpay.app-id', '', '微信小程序AppID'),
(2, 'wxpay.mch-id', '', '微信支付商户号'),
(3, 'wxpay.api-key', '', '微信支付API密钥(V3)'),
(4, 'wxpay.serial-no', '', '商户API证书序列号'),
(5, 'wxpay.private-key', '', '商户API私钥(PEM格式)'),
(6, 'wxpay.cert-path', '', '商户API证书路径'),
(7, 'wxpay.notify-url', 'https://api.example.com/api/v1/payments/wechat/notify', '支付回调地址'),
(8, 'wxpay.refund-notify-url', 'https://api.example.com/api/v1/refunds/wechat/notify', '退款回调地址'),
(9, 'wxpay.enabled', 'false', '是否启用真实微信支付');
