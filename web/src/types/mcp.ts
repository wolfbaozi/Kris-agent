export interface McpConfig {
  id: number
  user_id: number
  name: string
  is_global: number
  run_env: string
  source_type: string
  server_type: string
  command: string
  args?: string[]
  env?: Record<string, string>
  file_path: string
  enabled: number
  config?: any
  created_at: string
}

export interface McpFormData {
  name: string
  runEnv?: string
  sourceType?: string
  serverType?: string
  command?: string
  args?: string[]
  env?: Record<string, string>
  filePath?: string
  config?: any
}

export interface McpQuota {
  maxMcpCount: number
  maxConcurrentMcp: number
  totalMcp: number
  enabledMcp: number
  canCreate: boolean
  canEnable: boolean
}

export interface McpTool {
  mcpId: number
  name: string
  description: string
  inputSchema: any
}

export interface McpDebugForm extends McpFormData {
  argsText?: string
  envText?: string
}
