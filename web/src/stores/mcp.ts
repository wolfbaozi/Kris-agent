import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { mcpApi, type McpConfig, type McpFormData, type McpQuota } from '../api/mcp'

export const useMcpStore = defineStore('mcp', () => {
  const list = ref<McpConfig[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const quota = ref<McpQuota | null>(null)
  const runningIds = ref<Set<number>>(new Set())

  const enabledList = computed(() => list.value.filter(m => m.enabled))

  async function fetchList() {
    loading.value = true
    error.value = null
    try {
      list.value = await mcpApi.list()
    } finally {
      loading.value = false
    }
  }

  async function fetchQuota() {
    try {
      quota.value = await mcpApi.getQuota(true)
    } catch {
      // silently ignore
    }
  }

  async function fetchRunningStatus() {
    try {
      const ids = await mcpApi.getRunningStatus(true)
      runningIds.value = new Set(ids)
    } catch {
      // silently ignore
    }
  }

  async function create(data: McpFormData) {
    await mcpApi.create(data)
    await fetchList()
    await fetchQuota()
  }

  async function update(id: number, data: McpFormData & { isGlobal?: number }) {
    await mcpApi.update(id, data)
    await fetchList()
  }

  async function remove(id: number) {
    await mcpApi.remove(id)
    await fetchList()
    await fetchQuota()
  }

  async function toggle(id: number) {
    await mcpApi.toggle(id)
    await fetchList()
    await fetchQuota()
  }

  async function start(id: number) {
    await mcpApi.start(id)
    await fetchRunningStatus()
  }

  async function stop(id: number) {
    await mcpApi.stop(id)
    await fetchRunningStatus()
  }

  async function listTools(id: number) {
    return await mcpApi.listTools(id)
  }

  async function callTool(id: number, toolName: string, args: Record<string, any>) {
    return await mcpApi.callTool(id, toolName, args)
  }

  return {
    list, loading, error, enabledList, quota, runningIds,
    fetchList, fetchQuota, fetchRunningStatus, create, update, remove, toggle,
    start, stop, listTools, callTool,
  }
})
