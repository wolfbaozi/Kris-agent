import { Router } from 'express'
import pool from '../db.js'
import { encrypt, decrypt } from '../crypto.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()

router.use(authMiddleware)

router.get('/', async (req, res) => {
  const [rows] = await pool.query('SELECT id, provider, model, base_url, created_at FROM api_keys WHERE user_id = ?', [req.user.id])
  res.json(rows)
})

router.post('/', async (req, res) => {
  const { provider, apiKey, model, baseUrl } = req.body || {}
  if (!provider || !apiKey) {
    return res.status(400).json({ error: 'provider和apiKey不能为空' })
  }
  const encrypted = encrypt(apiKey)
  const [result] = await pool.query(
    'INSERT INTO api_keys (user_id, provider, encrypted_key, model, base_url) VALUES (?, ?, ?, ?, ?)',
    [req.user.id, provider, encrypted, model || '', baseUrl || '']
  )
  res.status(201).json({ id: result.insertId, provider, model, baseUrl })
})

router.put('/:id', async (req, res) => {
  const { apiKey, model, baseUrl } = req.body || {}
  const [rows] = await pool.query('SELECT * FROM api_keys WHERE id = ? AND user_id = ?', [req.params.id, req.user.id])
  if (rows.length === 0) {
    return res.status(404).json({ error: 'API Key不存在' })
  }
  const encrypted = apiKey ? encrypt(apiKey) : rows[0].encrypted_key
  await pool.query(
    'UPDATE api_keys SET encrypted_key = ?, model = ?, base_url = ? WHERE id = ?',
    [encrypted, model ?? rows[0].model, baseUrl ?? rows[0].base_url, req.params.id]
  )
  res.json({ ok: true })
})

router.delete('/:id', async (req, res) => {
  const [rows] = await pool.query('SELECT id FROM api_keys WHERE id = ? AND user_id = ?', [req.params.id, req.user.id])
  if (rows.length === 0) {
    return res.status(404).json({ error: 'API Key不存在' })
  }
  await pool.query('DELETE FROM api_keys WHERE id = ?', [req.params.id])
  res.json({ ok: true })
})

export default router
