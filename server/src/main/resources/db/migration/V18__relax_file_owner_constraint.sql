-- V18: Relax foreign key constraint on file_asset.owner_user_id to allow graceful deletion and system-generated files
ALTER TABLE file_asset DROP FOREIGN KEY fk_file_owner;
ALTER TABLE file_asset MODIFY COLUMN owner_user_id BIGINT NULL;
ALTER TABLE file_asset ADD CONSTRAINT fk_file_owner FOREIGN KEY (owner_user_id) REFERENCES platform_user (id) ON DELETE SET NULL;
