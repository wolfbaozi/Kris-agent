<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useMcpStore } from '../stores/mcp'
import { useAuthStore } from '../stores/auth'
import { useRoleOptionStore } from '../stores/roleOption'
import { mcpApi } from '../api/index'
import { emitError } from '../api/error'

const store = useMcpStore()
const auth = useAuthStore()
const roleStore = useRoleOptionStore()

defineEmits<{ (e: 'close'): void }>()

const showForm = ref(false)
const editingId = ref<number | null>(null)
const errorMsg = ref('')
const aiLoading = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const useAiMode = ref(true)
const actionLoading = ref<Record<number, string>>({})

const isDeveloper = computed(() => auth.role === 'developer')

const form = ref({
  name: '',
  runEnv: 'all',
  command: '',
  argsText: '',
  envText: '',
})

const simpleForm = ref({
  description: '',
  role: auth.role || 'developer',
})

const isKris = auth.user?.username === 'Kris'

async function loadList() {
  await store.fetchList()
}

function openAdd() {
  editingId.value = null
  form.value = { name: '', runEnv: 'all', command: '', argsText: '', envText: '' }
  simpleForm.value = { description: '', role: auth.role || 'developer' }
  useAiMode.value = true
  showForm.value = true
  errorMsg.value = ''
}

function openEdit(mcp: any) {
  if ((mcp.isGlobal || mcp.is_global) && !isKris) return
  editingId.value = mcp.id
  form.value = {
    name: mcp.name,
    runEnv: mcp.runEnv || mcp.run_env || 'all',
    command: mcp.command || '',
    argsText: Array.isArray(mcp.args) ? JSON.stringify(mcp.args) : (typeof mcp.args === 'string' ? mcp.args : ''),
    envText: (mcp.env && typeof mcp.env === 'object' && !Array.isArray(mcp.env)) ? JSON.stringify(mcp.env) : (typeof mcp.env === 'string' ? mcp.env : ''),
  }
  useAiMode.value = false
  showForm.value = true
  errorMsg.value = ''
}

async function save() {
  errorMsg.value = ''

  if (!isDeveloper.value && !editingId.value) {
    return
  }

  const trimName = form.value.name.trim()
  const duplicate = store.list.find(
    (m) => m.name === trimName && m.id !== editingId.value
  )
  if (duplicate) {
    errorMsg.value = `MCP名称"${trimName}"已存在`
    return
  }

  let args: string[] | undefined
  let env: Record<string, string> | undefined
  try {
    if (form.value.argsText.trim()) {
      args = JSON.parse(form.value.argsText)
      if (!Array.isArray(args)) throw new Error('args 必须是数组')
    }
  } catch (e: any) {
    errorMsg.value = 'args 格式错误: ' + e.message
    return
  }
  try {
    if (form.value.envText.trim()) {
      env = JSON.parse(form.value.envText)
      if (typeof env !== 'object' || Array.isArray(env)) throw new Error('env 必须是对象')
    }
  } catch (e: any) {
    errorMsg.value = 'env 格式错误: ' + e.message
    return
  }
  if (editingId.value) {
    await store.update(editingId.value, {
      name: form.value.name,
      runEnv: form.value.runEnv,
      command: form.value.command,
      args,
      env,
    })
  } else {
    await store.create({
      name: form.value.name,
      runEnv: form.value.runEnv,
      command: form.value.command,
      args,
      env,
    })
  }
  showForm.value = false
}

async function aiCreate() {
  errorMsg.value = ''
  const desc = simpleForm.value.description.trim()
  if (!desc) {
    errorMsg.value = '请描述你想要的 MCP 功能'
    return
  }
  aiLoading.value = true
  try {
    await mcpApi.aiCreate(desc, simpleForm.value.role)
    await store.fetchList()
    showForm.value = false
  } finally {
    aiLoading.value = false
  }
}

function triggerFileUpload() {
  fileInputRef.value?.click()
}

async function handleFileUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  errorMsg.value = ''
  aiLoading.value = true
  try {
    await mcpApi.upload(file, simpleForm.value.role)
    await store.fetchList()
    showForm.value = false
  } finally {
    aiLoading.value = false
    input.value = ''
  }
}

async function deleteMcp(id: number) {
  if (!confirm('确定删除该 MCP 配置？')) return
  await store.remove(id)
}

async function toggleMcp(id: number) {
  await store.toggle(id)
}

async function exportMcp(id: number) {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(`/api/mcps/${id}/export`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!response.ok) throw new Error('导出失败')
    const blob = await response.blob()
    const disposition = response.headers.get('Content-Disposition')
    const filenameMatch = disposition?.match(/filename="(.+)"/)
    const filename = filenameMatch ? filenameMatch[1] : `mcp-${id}.json`
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  } catch (e: any) {
    emitError(e.message)
  }
}

async function startMcp(id: number) {
  actionLoading.value = { ...actionLoading.value, [id]: 'start' }
  try {
    await store.start(id)
  } finally {
    const newLoading = { ...actionLoading.value }
    delete newLoading[id]
    actionLoading.value = newLoading
  }
}

async function stopMcp(id: number) {
  actionLoading.value = { ...actionLoading.value, [id]: 'stop' }
  try {
    await store.stop(id)
  } finally {
    const newLoading = { ...actionLoading.value }
    delete newLoading[id]
    actionLoading.value = newLoading
  }
}

onMounted(() => {
  loadList()
  store.fetchQuota()
  store.fetchRunningStatus()
  roleStore.fetchList()
})
</script>

<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h2>MCP 管理</h2>
        <button class="btn-close" @click="$emit('close')">✕</button>
      </div>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <div v-if="store.quota" class="quota-bar">
        <span>配额：{{ store.quota.enabledMcp }}/{{ store.quota.maxConcurrentMcp }} 运行中</span>
        <span class="quota-sep">|</span>
        <span>{{ store.quota.totalMcp }}/{{ store.quota.maxMcpCount }} 总配置</span>
      </div>

      <div class="list">
        <div v-if="store.list.length === 0" class="empty">暂无 MCP 配置，请先添加</div>

        <div v-for="m in store.list" :key="m.id" class="card">
          <div class="card-info">
            <span class="name">{{ m.name }}</span>
            <span v-if="m.is_global" class="tag-global">全局</span>
            <span class="tag env-tag">{{ m.run_env }}</span>
            <span v-if="m.source_type === 'ai_gen'" class="tag ai-tag">AI</span>
            <span v-if="store.runningIds.has(m.id)" class="tag running-tag">运行中</span>
            <span v-if="!m.enabled" class="tag tag-off">已禁用</span>
          </div>
          <div class="card-actions">
            <button class="btn-outline-sm" @click="toggleMcp(m.id)">
              {{ m.enabled ? '禁用' : '启用' }}
            </button>
            <button
              v-if="m.enabled && !store.runningIds.has(m.id)"
              class="btn-outline-sm btn-running"
              :disabled="!!actionLoading[m.id]"
              @click="startMcp(m.id)"
            >
              {{ actionLoading[m.id] === 'start' ? '启动中...' : '启动' }}
            </button>
            <button
              v-if="m.enabled && store.runningIds.has(m.id)"
              class="btn-danger-sm"
              :disabled="!!actionLoading[m.id]"
              @click="stopMcp(m.id)"
            >
              {{ actionLoading[m.id] === 'stop' ? '停止中...' : '停止' }}
            </button>
            <button
              v-if="!m.is_global || isKris"
              class="btn-outline-sm"
              @click="exportMcp(m.id)"
            >
              导出
            </button>
            <button
              v-if="!m.is_global || isKris"
              class="btn-outline-sm"
              @click="openEdit(m)"
            >
              编辑
            </button>
            <button
              v-if="!m.is_global || isKris"
              class="btn-danger-sm"
              @click="deleteMcp(m.id)"
            >
              删除
            </button>
          </div>
        </div>
      </div>

      <button class="btn-add" @click="openAdd" :disabled="!!(store.quota && !store.quota.canCreate)">
        {{ store.quota && !store.quota.canCreate ? '已达配额上限' : '+ 添加 MCP' }}
      </button>

      <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
        <div class="modal form-modal">
          <h2>{{ editingId ? '编辑' : '添加' }} MCP 配置</h2>

          <template v-if="!editingId && isDeveloper">
            <div class="mode-switch">
              <button
                class="mode-btn"
                :class="{ active: useAiMode }"
                @click="useAiMode = true"
              >
                AI 生成
              </button>
              <button
                class="mode-btn"
                :class="{ active: !useAiMode }"
                @click="useAiMode = false"
              >
                手动填写
              </button>
            </div>
          </template>

          <template v-if="!editingId && useAiMode">
            <div class="mode-hint">
              用文字描述你想要的 MCP 服务器功能，AI 会自动生成配置（如文件系统、数据库等）
            </div>

            <label>角色身份</label>
            <select v-model="simpleForm.role">
              <option v-for="opt in roleStore.list" :key="opt.roleKey" :value="opt.roleKey">
                {{ opt.roleLabel }}
              </option>
            </select>

            <label>功能描述</label>
            <textarea
              v-model="simpleForm.description"
              placeholder="例如：我需要一个能访问本地文件系统的 MCP 服务器，可以读写指定目录下的文件..."
              rows="5"
              class="code-input"
            />

            <div class="ai-actions">
              <button
                class="btn-ai"
                :disabled="aiLoading || !simpleForm.description.trim()"
                @click="aiCreate"
              >
                {{ aiLoading ? 'AI 生成中...' : 'AI 智能生成' }}
              </button>
              <span class="or-text">或</span>
              <button class="btn-upload" @click="triggerFileUpload" :disabled="aiLoading">
                上传配置文件
              </button>
              <input
                ref="fileInputRef"
                type="file"
                accept=".json,.txt,.md"
                style="display: none"
                @change="handleFileUpload"
              />
            </div>
          </template>

          <template v-if="editingId || (!editingId && !useAiMode && isDeveloper)">
            <label>MCP 名称</label>
            <input v-model="form.name" placeholder="如：文件系统 MCP" />

            <label>运行环境</label>
            <select v-model="form.runEnv">
              <option value="all">所有环境</option>
              <option value="local">仅本地开发</option>
              <option value="production">仅线上生产</option>
            </select>

            <label>启动命令</label>
            <input v-model="form.command" placeholder="如: node 或 npx" />

            <label>命令参数（JSON 数组）</label>
            <input v-model="form.argsText" placeholder='如: ["./server.js", "--port=8080"]' />

            <label>环境变量（JSON 对象）</label>
            <input v-model="form.envText" placeholder='如: {"API_KEY": "xxx"}' />
          </template>

          <div class="modal-actions">
            <button class="btn-outline" @click="showForm = false">取消</button>
            <button
              v-if="editingId || (!useAiMode && isDeveloper)"
              class="btn-primary"
              :disabled="!form.name"
              @click="save"
            >
              保存
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}
.modal {
  width: 520px;
  max-height: 80vh;
  padding: 24px;
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.modal-header h2 {
  margin: 0;
  font-size: 18px;
  color: #e6edf3;
}
.btn-close {
  width: 28px;
  height: 28px;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: transparent;
  color: #8b949e;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.btn-close:hover {
  background: #30363d;
  color: #e6edf3;
}
.error {
  color: #f85149;
  font-size: 13px;
  margin: 0 0 12px;
}
.list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 12px;
  min-height: 0;
}
.empty {
  color: #8b949e;
  font-size: 14px;
  padding: 20px 0;
  text-align: center;
}
.card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  margin-bottom: 8px;
  border: 1px solid #30363d;
  border-radius: 8px;
  background: #0d1117;
}
.card-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.name {
  color: #e6edf3;
  font-size: 14px;
  font-weight: 500;
}
.tag {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  flex-shrink: 0;
}
.tag-global {
  background: #1f6feb;
  color: #fff;
}
.env-tag {
  background: #30363d;
  color: #8b949e;
}
.ai-tag {
  background: #238636;
  color: #fff;
}
.tag-off {
  background: #da363322;
  color: #da3633;
}
.running-tag {
  background: #23863622;
  color: #3fb950;
}
.btn-running {
  border-color: #3fb950;
  color: #3fb950;
}
.btn-running:hover:not(:disabled) {
  background: #23863622;
}
.card-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
.btn-add {
  padding: 8px 0;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: transparent;
  color: #58a6ff;
  font-size: 14px;
  cursor: pointer;
  width: 100%;
}
.btn-add:hover {
  background: #1f6feb22;
}
.form-modal {
  width: 440px;
  position: relative;
  max-height: 85vh;
  overflow-y: auto;
}
.form-modal h2 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #e6edf3;
}
.mode-switch {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  border: 1px solid #30363d;
  border-radius: 8px;
  padding: 4px;
  background: #0d1117;
}
.mode-btn {
  flex: 1;
  padding: 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #8b949e;
  font-size: 13px;
  cursor: pointer;
  font-weight: 500;
}
.mode-btn.active {
  background: #238636;
  color: #fff;
}
.mode-btn:hover:not(.active) {
  background: #30363d;
  color: #e6edf3;
}
label {
  display: block;
  font-size: 12px;
  color: #8b949e;
  margin-bottom: 4px;
  margin-top: 10px;
}
select,
input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: #0d1117;
  color: #e6edf3;
  font-size: 13px;
  box-sizing: border-box;
}
select:focus,
input:focus,
textarea:focus {
  outline: none;
  border-color: #58a6ff;
}
.code-input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: #0d1117;
  color: #e6edf3;
  font-size: 13px;
  font-family: monospace;
  resize: vertical;
  box-sizing: border-box;
  min-height: 80px;
  max-height: 200px;
  overflow-y: auto;
}
.mode-hint {
  font-size: 13px;
  color: #8b949e;
  margin-bottom: 12px;
  padding: 10px;
  background: #0d1117;
  border-radius: 6px;
  border: 1px solid #30363d;
}
.ai-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
}
.btn-ai {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 6px;
  background: linear-gradient(135deg, #238636, #1f6feb);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
.btn-ai:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-upload {
  padding: 8px 14px;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: transparent;
  color: #c9d1d9;
  font-size: 13px;
  cursor: pointer;
}
.btn-upload:hover {
  background: #30363d;
}
.btn-upload:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.or-text {
  color: #8b949e;
  font-size: 13px;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
.btn-primary {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  background: #238636;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-outline {
  padding: 8px 16px;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: transparent;
  color: #c9d1d9;
  font-size: 13px;
  cursor: pointer;
}
.btn-outline:hover {
  background: #30363d;
}
.btn-outline-sm {
  padding: 3px 10px;
  border: 1px solid #30363d;
  border-radius: 4px;
  background: transparent;
  color: #c9d1d9;
  font-size: 11px;
  cursor: pointer;
}
.btn-outline-sm:hover {
  background: #30363d;
}
.btn-danger-sm {
  padding: 3px 10px;
  border: 1px solid #f85149;
  border-radius: 4px;
  background: transparent;
  color: #f85149;
  font-size: 11px;
  cursor: pointer;
}
.btn-danger-sm:hover {
  background: #f8514922;
}
.quota-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 12px;
  background: #0d1117;
  border: 1px solid #30363d;
  border-radius: 6px;
  font-size: 12px;
  color: #8b949e;
}
.quota-sep {
  color: #30363d;
}
</style>
