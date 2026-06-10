export type ChatRole = 'user' | 'assistant' | 'system'

export interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  createdAt: number
}

export interface ChatRequestMessage {
  role: ChatRole
  content: string
}

export interface StreamChunk {
  type: 'text-delta' | 'tool-call' | 'tool-result' | 'error' | 'done'
  content?: string
  toolName?: string
  args?: unknown
  result?: unknown
}
