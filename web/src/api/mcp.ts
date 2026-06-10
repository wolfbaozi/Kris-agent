import { request } from './request'
import type { McpConfig, McpFormData, McpQuota, McpTool } from '../types/mcp'

export type { McpConfig, McpFormData, McpQuota, McpTool }

export const mcpApi = {
  list: (): Promise<McpConfig[]> => request('/mcps'),

  getQuota: (silent = false): Promise<McpQuota> => request('/mcps/quota', { silent }),

  getRunningStatus: (silent = false): Promise<number[]> => request('/mcps/running-status', { silent }),

  create: (data: McpFormData) =>
    request('/mcps', { method: 'POST', body: JSON.stringify(data) }),

  update: (id: number, data: McpFormData & { isGlobal?: number }) =>
    request('/mcps/' + id, { method: 'PUT', body: JSON.stringify(data) }),

  remove: (id: number) =>
    request('/mcps/' + id, { method: 'DELETE' }),

  toggle: (id: number) =>
    request('/mcps/' + id + '/toggle', { method: 'PATCH' }),

  start: (id: number) =>
    request('/mcps/' + id + '/start', { method: 'POST' }),

  stop: (id: number) =>
    request('/mcps/' + id + '/stop', { method: 'POST' }),

  listTools: (id: number): Promise<McpTool[]> =>
    request('/mcps/' + id + '/tools'),

  callTool: (id: number, toolName: string, args: Record<string, any>) =>
    request('/mcps/' + id + '/tools/' + toolName + '/call', {
      method: 'POST',
      body: JSON.stringify(args),
    }),
}
