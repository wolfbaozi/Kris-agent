import { request, uploadRequest } from './request'
import type { AuthResponse, MeResponse } from '../types/auth'
import type { RoleOption } from '../types/role'
import type { KeyItem, KeyCreateData, KeyUpdateData } from '../types/key'

interface OptimizeResponse {
  text: string
}

export const authApi = {
  login: (username: string, password: string) =>
    request<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
  register: (username: string, password: string, role?: string) =>
    request<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify({ username, password, role }) }),
  me: (silent = false) => request<MeResponse>('/auth/me', { silent }),
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

export const skillApi = {
  aiCreate: (description: string, role?: string) => {
    const selectedRole = role || localStorage.getItem('role') || 'developer'
    return request('/skills/ai-create', { method: 'POST', body: JSON.stringify({ description, role: selectedRole }) })
  },
  upload: (file: File, role?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    const selectedRole = role || localStorage.getItem('role') || 'developer'
    formData.append('role', selectedRole)
    return uploadRequest('/skills/upload', formData)
  },
}

export const mcpApi = {
  aiCreate: (description: string, role?: string) => {
    const selectedRole = role || localStorage.getItem('role') || 'developer'
    return request('/mcps/ai-create', { method: 'POST', body: JSON.stringify({ description, role: selectedRole }) })
  },
  upload: (file: File, role?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    const selectedRole = role || localStorage.getItem('role') || 'developer'
    formData.append('role', selectedRole)
    return uploadRequest('/mcps/upload', formData)
  },
}

export const fileApi = {
  upload: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return uploadRequest('/files', formData)
  },
}

export const roleOptionApi = {
  list: (silent = false) => request<RoleOption[]>('/role-options', { silent }),
}

export const aiOptimizeApi = {
  optimize: (text: string, silent = false) =>
    request<OptimizeResponse>('/ai-gen/optimize', { method: 'POST', body: JSON.stringify({ text }), silent }),
}

export const keysApi = {
  list: (silent = false) => request<KeyItem[]>('/apikeys', { silent }),
  create: (data: KeyCreateData) =>
    request('/apikeys', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: KeyUpdateData) =>
    request('/apikeys/' + id, { method: 'PUT', body: JSON.stringify(data) }),
  remove: (id: number) => request('/apikeys/' + id, { method: 'DELETE' }),
}
