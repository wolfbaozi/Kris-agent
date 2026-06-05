import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()

router.use(authMiddleware)

router.get('/', async (req, res) => {
  const currentEnv = process.env.RUN_ENV || 'production'
  const [rows] = await pool.query(
    `SELECT id, user_id, name, is_global, run_env, source_type, server_type, command, args, env, file_path, enabled, config, created_at
     FROM mcp_servers
     WHERE enabled = 1
       AND (run_env = 'all' OR run_env = ?)
       AND (user_id = ? OR is_global = 1)
       AND (is_global = 1 OR user_id = ?)`,
    [currentEnv, req.user.id, req.user.id]
  )
  const list = rows.map((row) => {
    const item = { ...row }
    if (item.is_global === 1 || item.user_id !== req.user.id) {
      delete item.env
      delete item.args
      delete item.command
    }
    return item
  })
  res.json(list)
})

router.post('/', async (req, res) => {
  const { name, runEnv, sourceType, serverType, command, args, env, filePath, config } = req.body || {}
  if (!name) {
    return res.status(400).json({ error: 'MCP名称不能为空' })
  }
  const [result] = await pool.query(
    `INSERT INTO mcp_servers (user_id, name, run_env, source_type, server_type, command, args, env, file_path, config)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      req.user.id,
      name,
      runEnv || 'all',
      sourceType || 'database',
      serverType || '',
      command || '',
      args ? JSON.stringify(args) : null,
      env ? JSON.stringify(env) : null,
      filePath || '',
      config ? JSON.stringify(config) : null,
    ]
  )
  res.status(201).json({ id: result.insertId, name })
})

router.put('/:id', async (req, res) => {
  const { name, runEnv, sourceType, serverType, command, args, env, filePath, config, isGlobal } = req.body || {}

  const [rows] = await pool.query('SELECT * FROM mcp_servers WHERE id = ? AND user_id = ?', [req.params.id, req.user.id])
  if (rows.length === 0) {
    return res.status(404).json({ error: 'MCP配置不存在或无权限修改' })
  }

  const current = rows[0]
  const updatedName = name ?? current.name
  const updatedRunEnv = runEnv ?? current.run_env
  const updatedSourceType = sourceType ?? current.source_type
  const updatedServerType = serverType ?? current.server_type
  const updatedCommand = command ?? current.command
  const updatedArgs = args !== undefined ? JSON.stringify(args) : current.args
  const updatedEnv = env !== undefined ? JSON.stringify(env) : current.env
  const updatedFilePath = filePath ?? current.file_path
  const updatedConfig = config !== undefined ? JSON.stringify(config) : current.config
  const updatedIsGlobal = isGlobal !== undefined ? isGlobal : current.is_global

  await pool.query(
    `UPDATE mcp_servers
     SET name = ?, run_env = ?, source_type = ?, server_type = ?, command = ?, args = ?, env = ?,
         file_path = ?, config = ?, is_global = ?
     WHERE id = ?`,
    [
      updatedName, updatedRunEnv, updatedSourceType, updatedServerType,
      updatedCommand, updatedArgs, updatedEnv, updatedFilePath,
      updatedConfig, updatedIsGlobal, req.params.id,
    ]
  )
  res.json({ ok: true })
})

router.delete('/:id', async (req, res) => {
  const [rows] = await pool.query('SELECT id FROM mcp_servers WHERE id = ? AND user_id = ?', [req.params.id, req.user.id])
  if (rows.length === 0) {
    return res.status(404).json({ error: 'MCP配置不存在或无权限删除' })
  }
  await pool.query('DELETE FROM mcp_servers WHERE id = ?', [req.params.id])
  res.json({ ok: true })
})

router.patch('/:id/toggle', async (req, res) => {
  const [rows] = await pool.query('SELECT id, enabled FROM mcp_servers WHERE id = ? AND user_id = ?', [req.params.id, req.user.id])
  if (rows.length === 0) {
    return res.status(404).json({ error: 'MCP配置不存在或无权限操作' })
  }
  const newEnabled = rows[0].enabled ? 0 : 1
  await pool.query('UPDATE mcp_servers SET enabled = ? WHERE id = ?', [newEnabled, req.params.id])
  res.json({ enabled: !!newEnabled })
})

export default router
