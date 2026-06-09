import type { StreamChunk } from './chat'
import type { SkillFormData } from './skill'
import type { McpFormData } from './mcp'
import { streamRequest } from './streamRequest'

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

export function debugSkill(
  form: SkillDebugForm,
  testMessage: string,
  onChunk: (chunk: StreamChunk) => void,
  signal?: AbortSignal,
): Promise<void> {
  return streamRequest('/debug/skill', { form, testMessage }, onChunk, signal)
}

export function debugMcp(
  form: McpDebugForm,
  testMessage: string,
  onChunk: (chunk: StreamChunk) => void,
  signal?: AbortSignal,
): Promise<void> {
  return streamRequest('/debug/mcp', { form, testMessage }, onChunk, signal)
}
