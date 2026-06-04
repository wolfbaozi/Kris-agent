import pool from './db.js'

const sql = `
CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS api_keys (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  provider VARCHAR(20) NOT NULL,
  encrypted_key VARCHAR(512) NOT NULL,
  model VARCHAR(100) DEFAULT '',
  base_url VARCHAR(255) DEFAULT '',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mcp_servers (
  id          INT          AUTO_INCREMENT PRIMARY KEY,
  user_id     INT          NOT NULL,
  name        VARCHAR(100) NOT NULL,
  is_global   TINYINT(1)   NOT NULL DEFAULT 0,
  run_env     ENUM('local','production','all') NOT NULL DEFAULT 'all',
  source_type ENUM('database','file') NOT NULL DEFAULT 'database',
  server_type VARCHAR(50)  NOT NULL DEFAULT '',
  command     VARCHAR(255) NOT NULL DEFAULT '',
  args        JSON         DEFAULT NULL,
  env         JSON         DEFAULT NULL,
  file_path   VARCHAR(500) NOT NULL DEFAULT '',
  enabled     TINYINT(1)   NOT NULL DEFAULT 1,
  config      JSON         DEFAULT NULL,
  created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS skills (
  id             INT          AUTO_INCREMENT PRIMARY KEY,
  user_id        INT          NOT NULL,
  name           VARCHAR(100) NOT NULL,
  is_global      TINYINT(1)   NOT NULL DEFAULT 0,
  skill_type     ENUM('tool','prompt') NOT NULL DEFAULT 'tool',
  source_type    ENUM('database','file') NOT NULL DEFAULT 'database',
  tool_schema    JSON         DEFAULT NULL,
  tool_code      TEXT         DEFAULT NULL,
  prompt_content TEXT         DEFAULT NULL,
  file_path      VARCHAR(500) NOT NULL DEFAULT '',
  enabled        TINYINT(1)   NOT NULL DEFAULT 1,
  created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
`

async function init() {
  console.log('Initializing database...')
  await pool.query(sql)
  console.log('Database tables created.')
  process.exit(0)
}

init()
