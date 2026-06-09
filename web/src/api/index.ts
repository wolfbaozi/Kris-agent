import { request, uploadRequest } from './request'

export const authApi = {
  login: (username: string, password: string) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
  register: (username: string, password: string) =>
    request('/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) }),
  me: () => request('/auth/me'),
}

export const aiGenApi = {
  skill: (description: string) => {
    const role = localStorage.getItem('role') || 'developer'
    return request('/ai-gen/skill', { method: 'POST', body: JSON.stringify({ description, role }) })
  },
  mcp: (description: string) => {
    const role = localStorage.getItem('role') || 'developer'
    return request('/ai-gen/mcp', { method: 'POST', body: JSON.stringify({ description, role }) })
  },
}

export const fileApi = {
  upload: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return uploadRequest('/files', formData)
  },
}

export const keysApi = {
  list: () => request('/apikeys'),
  create: (data: { provider: string; apiKey: string; model?: string; baseUrl?: string }) =>
    request('/apikeys', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: { apiKey?: string; model?: string; baseUrl?: string }) =>
    request('/apikeys/' + id, { method: 'PUT', body: JSON.stringify(data) }),
  remove: (id: number) => request('/apikeys/' + id, { method: 'DELETE' }),
}

