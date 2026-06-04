import { defineStore } from 'pinia'
import { ref } from 'vue'
import { md5 } from 'js-md5'
import { authApi } from '../api/index'

export interface UserInfo {
  userId: number
  username: string
  token: string
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(null)
  const isLoggedIn = ref(false)

  function loadFromStorage() {
    const token = localStorage.getItem('token')
    const username = localStorage.getItem('username')
    const userId = localStorage.getItem('userId')
    if (token && username && userId) {
      user.value = { token, username, userId: Number(userId) }
      isLoggedIn.value = true
    }
  }

  async function login(username: string, password: string) {
    const data = await authApi.login(username, md5(password))
    setSession(data)
  }

  async function register(username: string, password: string) {
    const data = await authApi.register(username, md5(password))
    setSession(data)
  }

  function setSession(data: { token: string; userId: number; username: string }) {
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('userId', String(data.userId))
    user.value = { token: data.token, username: data.username, userId: data.userId }
    isLoggedIn.value = true
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('userId')
    user.value = null
    isLoggedIn.value = false
  }

  return { user, isLoggedIn, loadFromStorage, login, register, logout }
})
