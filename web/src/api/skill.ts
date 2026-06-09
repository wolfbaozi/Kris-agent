import { request } from './request'

export interface SkillConfig {
  id: number
  user_id: number
  name: string
  is_global: number
  skill_type: 'tool' | 'prompt'
  source_type: string
  tool_schema?: any
  tool_code?: string
  prompt_content?: string
  file_path: string
  enabled: number
  created_at: string
}

export interface SkillFormData {
  name: string
  skillType: 'tool' | 'prompt'
  sourceType?: string
  toolSchema?: any
  toolCode?: string
  promptContent?: string
  filePath?: string
}

export const skillApi = {
  list: (): Promise<SkillConfig[]> => request('/skills'),

  create: (data: SkillFormData) =>
    request('/skills', { method: 'POST', body: JSON.stringify(data) }),

  update: (id: number, data: SkillFormData & { isGlobal?: number }) =>
    request('/skills/' + id, { method: 'PUT', body: JSON.stringify(data) }),

  remove: (id: number) =>
    request('/skills/' + id, { method: 'DELETE' }),

  toggle: (id: number) =>
    request('/skills/' + id + '/toggle', { method: 'PATCH' }),
}
