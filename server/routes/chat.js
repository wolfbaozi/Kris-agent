import { Router } from 'express'
import { authMiddleware } from '../middleware/auth.js'
import { streamChatResponse } from '../services/chat.js'

const router = Router()

router.post('/', authMiddleware, async (req, res) => {
  const { messages, keyId, mcpIds, skillIds } = req.body
  if (!Array.isArray(messages) || messages.length === 0) {
    return res.status(400).json({ error: 'messages 不能为空' })
  }

  try {
    const found = await streamChatResponse(req.user.id, { keyId, mcpIds, skillIds, messages }, res)
    if (!found) res.status(400).json({ error: '请先配置 API Key' })
  } catch (error) {
    console.error('[chat error]', error)
    if (!res.headersSent) {
      res.status(500).json({ error: error instanceof Error ? error.message : '服务器错误' })
    }
  }
})

export default router
