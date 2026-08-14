CREATE TABLE app_setting (
    setting_key VARCHAR(100) NOT NULL,
    setting_value VARCHAR(500) NOT NULL,
    description VARCHAR(255) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key)
);

INSERT INTO app_setting (setting_key, setting_value, description)
VALUES ('schema.baseline', '1', 'Stage 1 database baseline');

