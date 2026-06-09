import { request } from './request'

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

export const mcpApi = {
  list: (): Promise<McpConfig[]> => request('/mcps'),

  create: (data: McpFormData) =>
    request('/mcps', { method: 'POST', body: JSON.stringify(data) }),

  update: (id: number, data: McpFormData & { isGlobal?: number }) =>
    request('/mcps/' + id, { method: 'PUT', body: JSON.stringify(data) }),

  remove: (id: number) =>
    request('/mcps/' + id, { method: 'DELETE' }),

  toggle: (id: number) =>
    request('/mcps/' + id + '/toggle', { method: 'PATCH' }),
}
