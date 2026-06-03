import cors from 'cors'
import { config } from 'dotenv'
import { fileURLToPath } from 'url'
import { resolve, dirname } from 'path'
import express from 'express'
import { streamText } from 'ai'
import { createOpenAI } from '@ai-sdk/openai'
import pool from './db.js'
import { decrypt } from './crypto.js'
import { authMiddleware } from './middleware/auth.js'
import authRoutes from './routes/auth.js'
import apikeyRoutes from './routes/apikeys.js'

const __dirname = dirname(fileURLToPath(import.meta.url))
config({ path: resolve(__dirname, '.env') })

const app = express()
const PORT = process.env.PORT || 3001

app.use(cors())
app.use(express.json())

const SYSTEM_PROMPT = `你是一个 helpful 的前端学习助手，擅长 Vue3、TypeScript、AI Agent 开发。
回答要简洁、结构化，必要时给出可运行的代码示例。
如果用户问的问题超出你的知识或需要实时数据，请明确说明。`

app.use('/api/auth', authRoutes)
app.use('/api/apikeys', apikeyRoutes)

app.get('/api/health', (_req, res) => {
  res.json({ ok: true })
})

app.post('/api/chat', authMiddleware, async (req, res) => {
  try {
    const { messages, keyId } = req.body
    if (!Array.isArray(messages) || messages.length === 0) {
      return res.status(400).json({ error: 'messages 不能为空' })
    }

    let keyRows
    if (keyId) {
      ;[keyRows] = await pool.query(
        'SELECT provider, encrypted_key, model, base_url FROM api_keys WHERE id = ? AND user_id = ?',
        [keyId, req.user.id]
      )
    } else {
      ;[keyRows] = await pool.query(
        'SELECT provider, encrypted_key, model, base_url FROM api_keys WHERE user_id = ? LIMIT 1',
        [req.user.id]
      )
    }

    if (keyRows.length === 0) {
      return res.status(400).json({ error: '请先配置 API Key' })
    }

    const keyRec = keyRows[0]
    const apiKey = decrypt(keyRec.encrypted_key)
    const provider = keyRec.provider
    const modelName = keyRec.model || (provider === 'deepseek' ? 'deepseek-chat' : 'gpt-4o-mini')
    const baseUrl = keyRec.base_url || (provider === 'deepseek' ? 'https://api.deepseek.com' : undefined)

    const openai = createOpenAI({ apiKey, ...(baseUrl ? { baseURL: baseUrl } : {}) })
    const model = openai(modelName)

    res.setHeader('Content-Type', 'text/event-stream')
    res.setHeader('Cache-Control', 'no-cache')
    res.setHeader('Connection', 'keep-alive')
    res.flushHeaders()

    const result = streamText({ model, system: SYSTEM_PROMPT, messages })
    for await (const chunk of result.textStream) {
      res.write(`data: ${JSON.stringify(chunk)}\n\n`)
      if (typeof res.flush === 'function') res.flush()
    }
    res.write('data: [DONE]\n\n')
    res.end()
  } catch (error) {
    console.error('[chat error]', error)
    if (!res.headersSent) {
      res.status(500).json({ error: error instanceof Error ? error.message : '服务器错误' })
    }
  }
})

app.listen(PORT, () => {
  console.log(`Agent BFF running at http://localhost:${PORT}`)
})
