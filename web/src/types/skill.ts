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

export interface SkillDebugForm extends SkillFormData {
  description?: string
  propertiesText?: string
  requiredText?: string
  toolCode?: string
  promptContent?: string
}
