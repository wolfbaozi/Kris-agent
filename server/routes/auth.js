import { Router } from 'express'
import bcrypt from 'bcryptjs'
import pool from '../db.js'
import { signToken } from '../middleware/auth.js'

const router = Router()

router.post('/register', async (req, res) => {
  const { username, password } = req.body || {}
  if (!username || !password) {
    return res.status(400).json({ error: '用户名和密码不能为空' })
  }
  if (password.length < 6) {
    return res.status(400).json({ error: '密码长度至少6位' })
  }
  const [rows] = await pool.query('SELECT id FROM users WHERE username = ?', [username])
  if (rows.length > 0) {
    return res.status(409).json({ error: '用户名已存在' })
  }
  const hash = await bcrypt.hash(password, 10)
  const [result] = await pool.query('INSERT INTO users (username, password_hash) VALUES (?, ?)', [username, hash])
  const token = signToken(result.insertId, username)
  res.json({ token, userId: result.insertId, username })
})

router.post('/login', async (req, res) => {
  const { username, password } = req.body || {}
  if (!username || !password) {
    return res.status(400).json({ error: '用户名和密码不能为空' })
  }
  const [rows] = await pool.query('SELECT id, password_hash FROM users WHERE username = ?', [username])
  if (rows.length === 0) {
    return res.status(401).json({ error: '用户名或密码错误' })
  }
  const user = rows[0]
  const valid = await bcrypt.compare(password, user.password_hash)
  if (!valid) {
    return res.status(401).json({ error: '用户名或密码错误' })
  }
  const token = signToken(user.id, username)
  res.json({ token, userId: user.id, username })
})

export default router
