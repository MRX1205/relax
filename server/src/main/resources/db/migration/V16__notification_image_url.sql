-- Add image_url to notification table for broadcast and rich announcements
ALTER TABLE notification ADD COLUMN image_url VARCHAR(500) NULL AFTER related_order_no;
