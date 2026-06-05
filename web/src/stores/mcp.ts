import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { mcpApi, type McpConfig, type McpFormData } from '../api/mcp'

export const useMcpStore = defineStore('mcp', () => {
  const list = ref<McpConfig[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const enabledList = computed(() => list.value.filter(m => m.enabled))

  async function fetchList() {
    loading.value = true
    error.value = null
    try {
      list.value = await mcpApi.list()
    } catch (e: any) {
      error.value = e.message || '加载MCP列表失败'
    } finally {
      loading.value = false
    }
  }

  async function create(data: McpFormData) {
    await mcpApi.create(data)
    await fetchList()
  }

  async function update(id: number, data: McpFormData & { isGlobal?: number }) {
    await mcpApi.update(id, data)
    await fetchList()
  }

  async function remove(id: number) {
    await mcpApi.remove(id)
    await fetchList()
  }

  async function toggle(id: number) {
    await mcpApi.toggle(id)
    await fetchList()
  }

  return { list, loading, error, enabledList, fetchList, create, update, remove, toggle }
})
