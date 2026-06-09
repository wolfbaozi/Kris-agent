const BASE_URL = '/api'

export interface RequestOptions extends RequestInit {
  skipAuth?: boolean
}

function buildHeaders(options: RequestOptions): Record<string, string> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  }
  if (!options.skipAuth) {
    const token = localStorage.getItem('token')
    if (token) headers['Authorization'] = 'Bearer ' + token
  }
  return headers
}

async function parseError(res: Response): Promise<string> {
  try {
    const data = await res.json()
    return data.error || `请求失败 (${res.status})`
  } catch {
    return `请求失败 (${res.status})`
  }
}

export async function request<T = unknown>(url: string, options: RequestOptions = {}): Promise<T> {
  const { skipAuth: _skip, ...fetchOptions } = options
  const headers = buildHeaders(options)
  const res = await fetch(BASE_URL + url, { ...fetchOptions, headers })
  if (!res.ok) {
    const msg = await parseError(res)
    throw new Error(msg)
  }
  return res.json() as Promise<T>
}

export async function uploadRequest<T = unknown>(url: string, formData: FormData): Promise<T> {
  const headers: Record<string, string> = {}
  const token = localStorage.getItem('token')
  if (token) headers['Authorization'] = 'Bearer ' + token
  const res = await fetch(BASE_URL + url, { method: 'POST', headers, body: formData })
  if (!res.ok) {
    const msg = await parseError(res)
    throw new Error(msg)
  }
  return res.json() as Promise<T>
}
