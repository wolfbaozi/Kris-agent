import type { StreamChunk } from '../types/chat'
import type { SkillDebugForm } from '../types/skill'
import type { McpDebugForm } from '../types/mcp'
import { streamRequest } from './streamRequest'

export type { SkillDebugForm } from '../types/skill'
export type { McpDebugForm } from '../types/mcp'

export function debugSkill(
  form: SkillDebugForm,
  testMessage: string,
  onChunk: (chunk: StreamChunk) => void,
  signal?: AbortSignal,
  silent = false,
): Promise<void> {
  return streamRequest('/debug/skill', { form, testMessage }, onChunk, signal, silent)
}

export function debugMcp(
  form: McpDebugForm,
  testMessage: string,
  onChunk: (chunk: StreamChunk) => void,
  signal?: AbortSignal,
  silent = false,
): Promise<void> {
  return streamRequest('/debug/mcp', { form, testMessage }, onChunk, signal, silent)
}
