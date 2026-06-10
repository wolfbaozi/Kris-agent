import { defineStore } from 'pinia'
import { ref } from 'vue'
import { roleOptionApi } from '../api/index'
import type { RoleOption } from '../types/role'

export const useRoleOptionStore = defineStore('roleOption', () => {
  const list = ref<RoleOption[]>([])

  async function fetchList() {
    try {
      const data = await roleOptionApi.list(true)
      list.value = data || []
    } catch {
      // silently ignore
    }
  }

  return { list, fetchList }
})
