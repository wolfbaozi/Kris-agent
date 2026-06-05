import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { skillApi, type SkillConfig, type SkillFormData } from '../api/skill'

export const useSkillStore = defineStore('skill', () => {
  const list = ref<SkillConfig[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const enabledList = computed(() => list.value.filter(s => s.enabled))

  async function fetchList() {
    loading.value = true
    error.value = null
    try {
      list.value = await skillApi.list()
    } catch (e: any) {
      error.value = e.message || '加载Skill列表失败'
    } finally {
      loading.value = false
    }
  }

  async function create(data: SkillFormData) {
    await skillApi.create(data)
    await fetchList()
  }

  async function update(id: number, data: SkillFormData & { isGlobal?: number }) {
    await skillApi.update(id, data)
    await fetchList()
  }

  async function remove(id: number) {
    await skillApi.remove(id)
    await fetchList()
  }

  async function toggle(id: number) {
    await skillApi.toggle(id)
    await fetchList()
  }

  return { list, loading, error, enabledList, fetchList, create, update, remove, toggle }
})
