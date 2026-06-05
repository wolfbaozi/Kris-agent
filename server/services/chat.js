import { streamText } from 'ai'
import { createOpenAI } from '@ai-sdk/openai'
import pool from '../db.js'
import { decrypt } from '../crypto.js'
import { loadMcpTools } from './mcp-loader.js'
import { loadSkills } from './skill-loader.js'

const BASE_SYSTEM_PROMPT = `你是一个 helpful 的前端学习助手，擅长 Vue3、TypeScript、AI Agent 开发。
回答要简洁、结构化，必要时给出可运行的代码示例。
如果用户问的问题超出你的知识或需要实时数据，请明确说明。`

async function getKeyRecord(userId, keyId) {
  if (keyId) {
    const [rows] = await pool.query(
      'SELECT provider, encrypted_key, model, base_url FROM api_keys WHERE id = ? AND user_id = ?',
      [keyId, userId],
    )
    return rows[0] ?? null
  }
  const [rows] = await pool.query(
    'SELECT provider, encrypted_key, model, base_url FROM api_keys WHERE user_id = ? LIMIT 1',
    [userId],
  )
  return rows[0] ?? null
}

function buildModel(keyRec) {
  const apiKey = decrypt(keyRec.encrypted_key)
  const provider = keyRec.provider
  const modelName = keyRec.model || (provider === 'deepseek' ? 'deepseek-chat' : 'gpt-4o-mini')
  const baseURL = keyRec.base_url || (provider === 'deepseek' ? 'https://api.deepseek.com' : undefined)
  const client = createOpenAI({ apiKey, ...(baseURL ? { baseURL } : {}) })
  return client(modelName)
}

function writeSSE(res, data) {
  res.write(`data: ${JSON.stringify(data)}\n\n`)
  if (typeof res.flush === 'function') res.flush()
}

export async function streamChatResponse(userId, { keyId, mcpIds, skillIds, messages }, res, passThrough = false) {
  const keyRec = await getKeyRecord(userId, keyId)
  if (!keyRec) return false

  const model = buildModel(keyRec)

  const [mcpTools, { tools: skillTools, systemPromptAppend }] = await Promise.all([
    loadMcpTools(userId, mcpIds),
    loadSkills(userId, skillIds),
  ])

  const tools = { ...mcpTools, ...skillTools }
  const fullSystemPrompt = systemPromptAppend
    ? `${BASE_SYSTEM_PROMPT}\n\n${systemPromptAppend}`
    : BASE_SYSTEM_PROMPT

  if (!passThrough) {
    res.setHeader('Content-Type', 'text/event-stream')
    res.setHeader('Cache-Control', 'no-cache')
    res.setHeader('Connection', 'keep-alive')
    res.flushHeaders()
  }

  const result = streamText({
    model,
    system: fullSystemPrompt,
    messages,
    tools,
    maxSteps: 5,
  })

  for await (const part of result.fullStream) {
    switch (part.type) {
      case 'tool-call': {
        writeSSE(res, {
          type: 'tool-call',
          toolName: part.toolName,
          args: part.args,
        })
        break
      }
      case 'tool-result': {
        writeSSE(res, {
          type: 'tool-result',
          toolName: part.toolName,
          result: part.result,
        })
        break
      }
      case 'text-delta': {
        writeSSE(res, {
          type: 'text-delta',
          content: part.textDelta,
        })
        break
      }
      case 'error': {
        writeSSE(res, {
          type: 'error',
          content: part.error instanceof Error ? part.error.message : String(part.error),
        })
        break
      }
      default:
        break
    }
  }
  writeSSE(res, { type: 'done' })
  res.end()
  return true
}
