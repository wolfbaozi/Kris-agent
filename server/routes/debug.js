import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { streamChatResponse } from '../services/chat.js'

const router = Router()

router.post('/skill', authMiddleware, async (req, res) => {
  const { form, testMessage } = req.body || {}
  if (!form || !form.name) {
    return res.status(400).json({ error: 'Skill名称不能为空' })
  }
  if (!testMessage || !testMessage.trim()) {
    return res.status(400).json({ error: '测试消息不能为空' })
  }

  let skillId = null
  try {
    const { name, skillType, description, propertiesText, requiredText, toolCode, promptContent } = form

    if (!skillType || !['tool', 'prompt'].includes(skillType)) {
      return res.status(400).json({ error: 'skillType必须为tool或prompt' })
    }

    let toolSchema = null
    if (skillType === 'tool') {
      let properties = {}
      let required = []
      try {
        if (propertiesText && propertiesText.trim()) {
          properties = JSON.parse(propertiesText)
        }
      } catch {
        return res.status(400).json({ error: 'properties 格式错误' })
      }
      try {
        if (requiredText && requiredText.trim()) {
          required = JSON.parse(requiredText)
          if (!Array.isArray(required)) throw new Error()
        }
      } catch {
        return res.status(400).json({ error: 'required 格式错误' })
      }
      toolSchema = {
        description: description || '',
        parameters: {
          type: 'object',
          properties,
          ...(required.length > 0 ? { required } : {}),
        },
      }
    }

    const [result] = await pool.query(
      `INSERT INTO skills (user_id, name, skill_type, source_type, tool_schema, tool_code, prompt_content, file_path)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        req.user.id,
        name,
        skillType,
        'database',
        toolSchema ? JSON.stringify(toolSchema) : null,
        toolCode || null,
        promptContent || null,
        '',
      ]
    )
    skillId = result.insertId

    const messages = [
      { role: 'user', content: testMessage.trim() },
    ]

    res.setHeader('Content-Type', 'text/event-stream')
    res.setHeader('Cache-Control', 'no-cache')
    res.setHeader('Connection', 'keep-alive')
    res.flushHeaders()

    const writeSSE = (data) => {
      res.write(`data: ${JSON.stringify(data)}\n\n`)
      if (typeof res.flush === 'function') res.flush()
    }

    writeSSE({ type: 'text-delta', content: `[已创建 Skill "${name}", 开始调试...]\n\n` })

    const found = await streamChatResponse(req.user.id, { skillIds: [skillId], messages }, res, true)
    if (!found) {
      writeSSE({ type: 'error', content: '请先配置 API Key' })
      writeSSE({ type: 'done' })
      res.end()
    }
  } catch (error) {
    console.error('[debug skill error]', error)
    if (!res.headersSent) {
      return res.status(500).json({ error: error instanceof Error ? error.message : '调试失败' })
    }
    try {
      res.write(`data: ${JSON.stringify({ type: 'error', content: '调试过程出错' })}\n\n`)
      res.write(`data: ${JSON.stringify({ type: 'done' })}\n\n`)
      res.end()
    } catch {}
  }
})

router.post('/mcp', authMiddleware, async (req, res) => {
  const { form, testMessage } = req.body || {}
  if (!form || !form.name) {
    return res.status(400).json({ error: 'MCP名称不能为空' })
  }
  if (!testMessage || !testMessage.trim()) {
    return res.status(400).json({ error: '测试消息不能为空' })
  }

  let mcpId = null
  try {
    const { name, runEnv, command, argsText, envText } = form

    let args = null
    let env = null
    try {
      if (argsText && argsText.trim()) {
        args = JSON.parse(argsText)
      }
    } catch {
      return res.status(400).json({ error: 'args 格式错误' })
    }
    try {
      if (envText && envText.trim()) {
        env = JSON.parse(envText)
      }
    } catch {
      return res.status(400).json({ error: 'env 格式错误' })
    }

    const [result] = await pool.query(
      `INSERT INTO mcp_servers (user_id, name, run_env, source_type, server_type, command, args, env, file_path, config)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        req.user.id,
        name,
        runEnv || 'all',
        'database',
        '',
        command || '',
        args ? JSON.stringify(args) : null,
        env ? JSON.stringify(env) : null,
        '',
        null,
      ]
    )
    mcpId = result.insertId

    const messages = [
      { role: 'user', content: testMessage.trim() },
    ]

    res.setHeader('Content-Type', 'text/event-stream')
    res.setHeader('Cache-Control', 'no-cache')
    res.setHeader('Connection', 'keep-alive')
    res.flushHeaders()

    const writeSSE = (data) => {
      res.write(`data: ${JSON.stringify(data)}\n\n`)
      if (typeof res.flush === 'function') res.flush()
    }

    writeSSE({ type: 'text-delta', content: `[已创建 MCP "${name}", 开始调试...]\n\n` })

    const found = await streamChatResponse(req.user.id, { mcpIds: [mcpId], messages }, res, true)
    if (!found) {
      writeSSE({ type: 'error', content: '请先配置 API Key' })
      writeSSE({ type: 'done' })
      res.end()
    }
  } catch (error) {
    console.error('[debug mcp error]', error)
    if (!res.headersSent) {
      return res.status(500).json({ error: error instanceof Error ? error.message : '调试失败' })
    }
    try {
      res.write(`data: ${JSON.stringify({ type: 'error', content: '调试过程出错' })}\n\n`)
      res.write(`data: ${JSON.stringify({ type: 'done' })}\n\n`)
      res.end()
    } catch {}
  }
})

export default router
