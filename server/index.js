import cors from 'cors'
import dotenv from 'dotenv'
import express from 'express'
import { streamText } from 'ai'
import { createOpenAI } from '@ai-sdk/openai'

dotenv.config()

const app = express()
const PORT = process.env.PORT || 3001

app.use(cors())
app.use(express.json())

function createModel() {
  const provider = process.env.AI_PROVIDER || 'deepseek'

  if (provider === 'openai') {
    const openai = createOpenAI({
      apiKey: process.env.OPENAI_API_KEY,
    })
    return openai(process.env.OPENAI_MODEL || 'gpt-4o-mini')
  }

  const deepseek = createOpenAI({
    baseURL: 'https://api.deepseek.com',
    apiKey: process.env.DEEPSEEK_API_KEY,
  })
  return deepseek(process.env.DEEPSEEK_MODEL || 'deepseek-chat')
}

const SYSTEM_PROMPT = `你是一个 helpful 的前端学习助手，擅长 Vue3、TypeScript、AI Agent 开发。
回答要简洁、结构化，必要时给出可运行的代码示例。
如果用户问的问题超出你的知识或需要实时数据，请明确说明。`

app.get('/api/health', (_req, res) => {
  const provider = process.env.AI_PROVIDER || 'deepseek'
  const hasKey =
    provider === 'openai'
      ? Boolean(process.env.OPENAI_API_KEY)
      : Boolean(process.env.DEEPSEEK_API_KEY)

  res.json({
    ok: true,
    provider,
    modelConfigured: hasKey,
  })
})

app.post('/api/chat', async (req, res) => {
  try {
    const { messages } = req.body

    if (!Array.isArray(messages) || messages.length === 0) {
      res.status(400).json({ error: 'messages 不能为空' })
      return
    }

    const provider = process.env.AI_PROVIDER || 'deepseek'
    const apiKey =
      provider === 'openai'
        ? process.env.OPENAI_API_KEY
        : process.env.DEEPSEEK_API_KEY

    if (!apiKey) {
      res.status(500).json({
        error: `未配置 API Key，请在 server/.env 中设置 ${
          provider === 'openai' ? 'OPENAI_API_KEY' : 'DEEPSEEK_API_KEY'
        }`,
      })
      return
    }

    const result = streamText({
      model: createModel(),
      system: SYSTEM_PROMPT,
      messages,
    })

    result.pipeTextStreamToResponse(res)
  } catch (error) {
    console.error('[chat error]', error)
    if (!res.headersSent) {
      res.status(500).json({
        error: error instanceof Error ? error.message : '服务器错误',
      })
    }
  }
})

app.listen(PORT, () => {
  console.log(`Agent BFF running at http://localhost:${PORT}`)
})
