import type { ChatRequestMessage } from '../types/chat'
import { streamRequest } from './streamRequest'

export interface StreamChunk {
  type: 'text-delta' | 'tool-call' | 'tool-result' | 'error' | 'done'
  content?: string
  toolName?: string
  args?: unknown
  result?: unknown
}

export async function streamChat(
  messages: ChatRequestMessage[],
  onChunk: (chunk: StreamChunk) => void,
  signal?: AbortSignal,
  keyId?: number | null,
  mcpIds?: number[],
  skillIds?: number[],
): Promise<void> {
  const body: Record<string, unknown> = { messages }
  if (keyId) body.keyId = keyId
  if (mcpIds && mcpIds.length > 0) body.mcpIds = mcpIds
  if (skillIds && skillIds.length > 0) body.skillIds = skillIds

  await streamRequest('/chat', body, onChunk, signal)
}
