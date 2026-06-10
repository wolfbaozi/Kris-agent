export interface AuthResponse {
  token: string
  userId: number
  username: string
  role?: string
}

export interface MeResponse {
  role?: string
}

export interface UserInfo {
  userId: number
  username: string
  token: string
  role: string
}
