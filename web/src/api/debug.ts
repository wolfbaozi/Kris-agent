import type { StreamChunk } from './chat'
import type { SkillFormData } from './skill'
import type { McpFormData } from './mcp'

export interface SkillDebugForm extends SkillFormData {
  description?: string
  propertiesText?: string
  requiredText?: string
  toolCode?: string
  promptContent?: string
}

export interface McpDebugForm extends McpFormData {
  argsText?: string
  envText?: string
}

export async function debugSkill(
  form: SkillDebugForm,
  testMessage: string,
  onChunk: (chunk: StreamChunk) => void,
  signal?: AbortSignal,
): Promise<void> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem('token')
  if (token) headers['Authorization'] = 'Bearer ' + token

  const response = await fetch('/api/debug/skill', {
    method: 'POST',
    headers,
    body: JSON.stringify({ form, testMessage }),
    signal,
  })

  if (!response.ok) {
    let message = `请求失败 (${response.status})`
    try {
      const data = await response.json()
      if (data.error) message = data.error
    } catch {}
    throw new Error(message)
  }

  if (!response.body) {
    throw new Error('响应体为空')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    while (buffer.includes('\n')) {
      const idx = buffer.indexOf('\n')
      const line = buffer.slice(0, idx).trim()
      buffer = buffer.slice(idx + 1)
      if (!line.startsWith('data:')) continue
      const payload = line.slice(5).trim()
      if (!payload) continue
      try {
        const parsed = JSON.parse(payload)
        if (parsed.type === 'done') return
        onChunk(parsed as StreamChunk)
      } catch {}
    }
  }
}

export async function debugMcp(
  form: McpDebugForm,
  testMessage: string,
  onChunk: (chunk: StreamChunk) => void,
  signal?: AbortSignal,
): Promise<void> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem('token')
  if (token) headers['Authorization'] = 'Bearer ' + token

  const response = await fetch('/api/debug/mcp', {
    method: 'POST',
    headers,
    body: JSON.stringify({ form, testMessage }),
    signal,
  })

  if (!response.ok) {
    let message = `请求失败 (${response.status})`
    try {
      const data = await response.json()
      if (data.error) message = data.error
    } catch {}
    throw new Error(message)
  }

  if (!response.body) {
    throw new Error('响应体为空')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    while (buffer.includes('\n')) {
      const idx = buffer.indexOf('\n')
      const line = buffer.slice(0, idx).trim()
      buffer = buffer.slice(idx + 1)
      if (!line.startsWith('data:')) continue
      const payload = line.slice(5).trim()
      if (!payload) continue
      try {
        const parsed = JSON.parse(payload)
        if (parsed.type === 'done') return
        onChunk(parsed as StreamChunk)
      } catch {}
    }
  }
}
