const BASE = '/api'

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

export const authApi = {
  login: (username: string, password: string) => request('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
  register: (username: string, password: string) => request('/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) }),
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
    const token = localStorage.getItem('token')
    const headers: Record<string, string> = {}
    if (token) headers['Authorization'] = 'Bearer ' + token
    return fetch('/api/files', { method: 'POST', headers, body: formData }).then((res) => {
      if (!res.ok) throw new Error('上传失败')
      return res.json()
    })
  },
}

export const keysApi = {
  list: () => request('/apikeys'),
  create: (data: { provider: string; apiKey: string; model?: string; baseUrl?: string }) => request('/apikeys', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: { apiKey?: string; model?: string; baseUrl?: string }) => request('/apikeys/' + id, { method: 'PUT', body: JSON.stringify(data) }),
  remove: (id: number) => request('/apikeys/' + id, { method: 'DELETE' }),
}

export default BASE
