import { defineStore } from 'pinia'
import { ref } from 'vue'
import { md5 } from 'js-md5'
import { authApi, aiGenApi, fileApi } from '../api/index'

export interface UserInfo {
  userId: number
  username: string
  token: string
  role: string
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(null)
  const isLoggedIn = ref(false)
  const role = ref<string>('developer')

  function loadFromStorage() {
    const token = localStorage.getItem('token')
    const username = localStorage.getItem('username')
    const userId = localStorage.getItem('userId')
    role.value = localStorage.getItem('role') || 'developer'
    if (token && username && userId) {
      user.value = { token, username, userId: Number(userId), role: role.value }
      isLoggedIn.value = true
    }
  }

  async function fetchMe() {
    try {
      const data = await authApi.me()
      role.value = data.role || 'developer'
      localStorage.setItem('role', role.value)
      if (user.value) {
        user.value.role = role.value
      }
    } catch {}
  }

  async function login(username: string, password: string) {
    const data = await authApi.login(username, md5(password))
    setSession(data)
    await fetchMe()
  }

  async function register(username: string, password: string) {
    const data = await authApi.register(username, md5(password))
    setSession(data)
    await fetchMe()
  }

  function setSession(data: { token: string; userId: number; username: string; role?: string }) {
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('userId', String(data.userId))
    const userRole = data.role || 'developer'
    localStorage.setItem('role', userRole)
    role.value = userRole
    user.value = { token: data.token, username: data.username, userId: data.userId, role: userRole }
    isLoggedIn.value = true
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('userId')
    localStorage.removeItem('role')
    user.value = null
    isLoggedIn.value = false
    role.value = 'developer'
  }

  return { user, isLoggedIn, role, loadFromStorage, fetchMe, login, register, logout }
})
