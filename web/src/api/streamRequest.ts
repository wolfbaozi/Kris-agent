import type { StreamChunk } from '../types/chat'
import { emitError } from './error'

const BASE_URL = '/api'

function buildStreamHeaders(): Record<string, string> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem('token')
  if (token) headers['Authorization'] = 'Bearer ' + token
  return headers
}

async function parseStreamError(res: Response): Promise<string> {
  try {
    const data = await res.json()
    return data.error || `请求失败 (${res.status})`
  } catch {
    return `请求失败 (${res.status})`
  }
}

function handleUnauthorized() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('userId')
  localStorage.removeItem('role')
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

function parseSSELine(line: string): StreamChunk | null {
  if (!line.startsWith('data:')) return null
  const payload = line.slice(5).trim()
  if (!payload) return null
  try {
    return JSON.parse(payload) as StreamChunk
  } catch {
    return null
  }
}

async function readStream(body: ReadableStream<Uint8Array>, onChunk: (chunk: StreamChunk) => void) {
  const reader = body.getReader()
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
      const chunk = parseSSELine(line)
      if (!chunk) continue
      if (chunk.type === 'done') return
      onChunk(chunk)
    }
  }
}

export async function streamRequest(
  url: string,
  body: unknown,
  onChunk: (chunk: StreamChunk) => void,
  signal?: AbortSignal,
  silent = false,
): Promise<void> {
  const response = await fetch(BASE_URL + url, {
    method: 'POST',
    headers: buildStreamHeaders(),
    body: JSON.stringify(body),
    signal,
  })

  if (response.status === 401) {
    handleUnauthorized()
    const error = new Error('登录已过期，请重新登录')
    if (!silent) emitError(error.message)
    throw error
  }

  if (!response.ok) {
    const message = await parseStreamError(response)
    const error = new Error(message)
    if (!silent) emitError(error.message)
    throw error
  }

  if (!response.body) {
    const error = new Error('响应体为空')
    if (!silent) emitError(error.message)
    throw error
  }

  await readStream(response.body, onChunk)
}
