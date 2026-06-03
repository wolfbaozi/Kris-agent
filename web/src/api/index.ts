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
}

export const keysApi = {
  list: () => request('/apikeys'),
  create: (data: { provider: string; apiKey: string; model?: string; baseUrl?: string }) => request('/apikeys', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: { apiKey?: string; model?: string; baseUrl?: string }) => request('/apikeys/' + id, { method: 'PUT', body: JSON.stringify(data) }),
  remove: (id: number) => request('/apikeys/' + id, { method: 'DELETE' }),
}

export default BASE
