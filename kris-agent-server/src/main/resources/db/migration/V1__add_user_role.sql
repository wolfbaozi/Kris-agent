ALTER TABLE users ADD COLUMN role ENUM('developer','product_manager') NOT NULL DEFAULT 'developer' AFTER password_hash;
