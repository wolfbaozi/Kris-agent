ALTER TABLE skills MODIFY COLUMN source_type ENUM('database','file','ai_gen') NOT NULL DEFAULT 'database';
ALTER TABLE mcp_servers MODIFY COLUMN source_type ENUM('database','file','ai_gen') NOT NULL DEFAULT 'database';
