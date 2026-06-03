import mysql2 from 'mysql2/promise'
import { config } from 'dotenv'
import { fileURLToPath } from 'url'
import { resolve, dirname } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
config({ path: resolve(__dirname, '.env') })

async function setup() {
  const connOpts = {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '3306', 10),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
  }

  const conn = await mysql2.createConnection(connOpts)
  await conn.query(`CREATE DATABASE IF NOT EXISTS \`${process.env.DB_NAME || 'kris_agent'}\` DEFAULT CHARACTER SET utf8mb4`)
  console.log('Database created.')
  await conn.end()

  const pool = mysql2.createPool({ ...connOpts, database: process.env.DB_NAME || 'kris_agent', waitForConnections: true, connectionLimit: 10 })

  await pool.query(`
    CREATE TABLE IF NOT EXISTS users (
      id INT AUTO_INCREMENT PRIMARY KEY,
      username VARCHAR(50) NOT NULL UNIQUE,
      password_hash VARCHAR(255) NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `)
  console.log('Table users created.')

  await pool.query(`
    CREATE TABLE IF NOT EXISTS api_keys (
      id INT AUTO_INCREMENT PRIMARY KEY,
      user_id INT NOT NULL,
      provider VARCHAR(20) NOT NULL,
      encrypted_key VARCHAR(512) NOT NULL,
      model VARCHAR(100) DEFAULT '',
      base_url VARCHAR(255) DEFAULT '',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `)
  console.log('Table api_keys created.')

  await pool.end()
  console.log('Setup complete.')
}

setup().catch((err) => {
  console.error('Setup failed:', err.message)
  process.exit(1)
})
