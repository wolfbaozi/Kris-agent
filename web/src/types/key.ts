export interface KeyItem {
  id: number
  provider: string
  model: string
  base_url: string
  created_at: string
}

export interface KeyCreateData {
  provider: string
  apiKey: string
  model?: string
  baseUrl?: string
}

export interface KeyUpdateData {
  apiKey?: string
  model?: string
  baseUrl?: string
}
