import type { ChatRequestMessage } from '../types/chat'

export async function streamChat(
  messages: ChatRequestMessage[],
  onChunk: (text: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  const response = await fetch('/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ messages }),
    signal,
  })

  if (!response.ok) {
    let message = `请求失败 (${response.status})`
    try {
      const data = await response.json()
      if (data.error) message = data.error
    } catch {
      // ignore json parse error
    }
    throw new Error(message)
  }

  if (!response.body) {
    throw new Error('响应体为空')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    onChunk(decoder.decode(value, { stream: true }))
  }
}

export async function checkHealth(): Promise<{
  ok: boolean
  provider: string
  modelConfigured: boolean
}> {
  const response = await fetch('/api/health')
  return response.json()
}
