import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()

router.use(authMiddleware)

router.get('/', async (req, res) => {
  const [rows] = await pool.query(
    `SELECT id, user_id, name, is_global, skill_type, source_type, tool_schema, tool_code, prompt_content, file_path, enabled, created_at
     FROM skills
     WHERE enabled = 1
       AND (user_id = ? OR is_global = 1)
       AND (is_global = 1 OR user_id = ?)`,
    [req.user.id, req.user.id]
  )
  const list = rows.map((row) => {
    const item = { ...row }
    if (item.is_global === 1 || item.user_id !== req.user.id) {
      delete item.tool_code
      delete item.tool_schema
    }
    return item
  })
  res.json(list)
})

router.post('/', async (req, res) => {
  const { name, skillType, sourceType, toolSchema, toolCode, promptContent, filePath } = req.body || {}
  if (!name) {
    return res.status(400).json({ error: 'Skill名称不能为空' })
  }
  if (!skillType || !['tool', 'prompt'].includes(skillType)) {
    return res.status(400).json({ error: 'skillType必须为tool或prompt' })
  }
  const [result] = await pool.query(
    `INSERT INTO skills (user_id, name, skill_type, source_type, tool_schema, tool_code, prompt_content, file_path)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      req.user.id,
      name,
      skillType,
      sourceType || 'database',
      toolSchema ? JSON.stringify(toolSchema) : null,
      toolCode || null,
      promptContent || null,
      filePath || '',
    ]
  )
  res.status(201).json({ id: result.insertId, name })
})

router.put('/:id', async (req, res) => {
  const { name, skillType, sourceType, toolSchema, toolCode, promptContent, filePath, isGlobal } = req.body || {}

  const [rows] = await pool.query('SELECT * FROM skills WHERE id = ? AND user_id = ?', [req.params.id, req.user.id])
  if (rows.length === 0) {
    return res.status(404).json({ error: 'Skill配置不存在或无权限修改' })
  }

  const current = rows[0]
  const updatedName = name ?? current.name
  const updatedSkillType = skillType ?? current.skill_type
  const updatedSourceType = sourceType ?? current.source_type
  const updatedToolSchema = toolSchema !== undefined ? JSON.stringify(toolSchema) : current.tool_schema
  const updatedToolCode = toolCode !== undefined ? toolCode : current.tool_code
  const updatedPromptContent = promptContent !== undefined ? promptContent : current.prompt_content
  const updatedFilePath = filePath ?? current.file_path
  const updatedIsGlobal = isGlobal !== undefined ? isGlobal : current.is_global

  await pool.query(
    `UPDATE skills
     SET name = ?, skill_type = ?, source_type = ?, tool_schema = ?, tool_code = ?,
         prompt_content = ?, file_path = ?, is_global = ?
     WHERE id = ?`,
    [
      updatedName, updatedSkillType, updatedSourceType, updatedToolSchema,
      updatedToolCode, updatedPromptContent, updatedFilePath, updatedIsGlobal,
      req.params.id,
    ]
  )
  res.json({ ok: true })
})

router.delete('/:id', async (req, res) => {
  const [rows] = await pool.query('SELECT id FROM skills WHERE id = ? AND user_id = ?', [req.params.id, req.user.id])
  if (rows.length === 0) {
    return res.status(404).json({ error: 'Skill配置不存在或无权限删除' })
  }
  await pool.query('DELETE FROM skills WHERE id = ?', [req.params.id])
  res.json({ ok: true })
})

router.patch('/:id/toggle', async (req, res) => {
  const [rows] = await pool.query('SELECT id, enabled FROM skills WHERE id = ? AND user_id = ?', [req.params.id, req.user.id])
  if (rows.length === 0) {
    return res.status(404).json({ error: 'Skill配置不存在或无权限操作' })
  }
  const newEnabled = rows[0].enabled ? 0 : 1
  await pool.query('UPDATE skills SET enabled = ? WHERE id = ?', [newEnabled, req.params.id])
  res.json({ enabled: !!newEnabled })
})

export default router
