import BASE from './index'

async function request(url: string, options: RequestInit = {}) {
  const token = localStorage.getItem('token')
  const headers: Record<string, string> = { 'Content-Type': 'application/json', ...(options.headers as Record<string, string>) }
  if (token) headers['Authorization'] = 'Bearer ' + token
  const res = await fetch(BASE + url, { ...options, headers })
  if (!res.ok) {
    let msg = '请求失败'
    try {
      const errData = await res.json()
      msg = errData.error || msg
    } catch {}
    throw new Error(msg)
  }
  return res.json()
}

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
