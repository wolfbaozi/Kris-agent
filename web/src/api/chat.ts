import type { ChatRequestMessage } from '../types/chat'

export async function streamChat(
  messages: ChatRequestMessage[],
  onChunk: (text: string) => void,
  signal?: AbortSignal,
  keyId?: number | null,
): Promise<void> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem('token')
  if (token) headers['Authorization'] = 'Bearer ' + token

  const body: Record<string, unknown> = { messages }
  if (keyId) body.keyId = keyId

  const response = await fetch('/api/chat', {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
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
      if (!payload || payload === '[DONE]') continue
      try {
        const parsed = JSON.parse(payload)
        if (typeof parsed === 'string') onChunk(parsed)
      } catch {}
    }
  }
}
