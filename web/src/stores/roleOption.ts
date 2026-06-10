import { defineStore } from 'pinia'
import { ref } from 'vue'
import { roleOptionApi } from '../api/index'

export interface RoleOption {
  roleKey: string
  roleLabel: string
  roleDesc: string
}

export const useRoleOptionStore = defineStore('roleOption', () => {
  const list = ref<RoleOption[]>([])

  async function fetchList() {
    try {
      const data = await roleOptionApi.list()
      list.value = data || []
    } catch {}
  }

  return { list, fetchList }
})
