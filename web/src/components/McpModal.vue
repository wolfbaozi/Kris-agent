<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMcpStore } from '../stores/mcp'
import { useAuthStore } from '../stores/auth'

const store = useMcpStore()
const auth = useAuthStore()

defineEmits<{ (e: 'close'): void }>()

const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref({
  name: '',
  runEnv: 'all',
  command: '',
  argsText: '',
  envText: '',
})
const errorMsg = ref('')
const isKris = auth.user?.username === 'Kris'

async function loadList() {
  try {
    await store.fetchList()
  } catch (e: any) {
    errorMsg.value = e.message
  }
}

function openAdd() {
  editingId.value = null
  form.value = { name: '', runEnv: 'all', command: '', argsText: '', envText: '' }
  showForm.value = true
  errorMsg.value = ''
}

function openEdit(mcp: any) {
  if (mcp.is_global && !isKris) return
  editingId.value = mcp.id
  form.value = {
    name: mcp.name,
    runEnv: mcp.run_env,
    command: mcp.command || '',
    argsText: mcp.args ? JSON.stringify(mcp.args) : '',
    envText: mcp.env ? JSON.stringify(mcp.env) : '',
  }
  showForm.value = true
  errorMsg.value = ''
}

async function save() {
  errorMsg.value = ''
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
  try {
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
  } catch (e: any) {
    errorMsg.value = e.message
  }
}

async function deleteMcp(id: number) {
  if (!confirm('确定删除该 MCP 配置？')) return
  try {
    await store.remove(id)
  } catch (e: any) {
    errorMsg.value = e.message
  }
}

async function toggleMcp(id: number) {
  try {
    await store.toggle(id)
  } catch (e: any) {
    errorMsg.value = e.message
  }
}

onMounted(loadList)
</script>

<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h2>MCP 管理</h2>
        <button class="btn-close" @click="$emit('close')">✕</button>
      </div>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <div class="list">
        <div v-if="store.list.length === 0" class="empty">暂无 MCP 配置，请先添加</div>

        <div v-for="m in store.list" :key="m.id" class="card">
          <div class="card-info">
            <span class="name">{{ m.name }}</span>
            <span v-if="m.is_global" class="tag-global">全局</span>
            <span class="tag env-tag">{{ m.run_env }}</span>
            <span v-if="!m.enabled" class="tag tag-off">已禁用</span>
          </div>
          <div class="card-actions">
            <button class="btn-outline-sm" @click="toggleMcp(m.id)">
              {{ m.enabled ? '禁用' : '启用' }}
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

      <button class="btn-add" @click="openAdd">+ 添加 MCP</button>

      <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
        <div class="modal form-modal">
          <h2>{{ editingId ? '编辑' : '添加' }} MCP 配置</h2>

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

          <div class="modal-actions">
            <button class="btn-outline" @click="showForm = false">取消</button>
            <button class="btn-primary" :disabled="!form.name" @click="save">保存</button>
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
  composes: tag;
  background: #1f6feb;
  color: #fff;
}
.env-tag {
  background: #30363d;
  color: #8b949e;
}
.tag-off {
  background: #da363322;
  color: #da3633;
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
  width: 400px;
  position: relative;
}
.form-modal h2 {
  margin: 0 0 12px;
  font-size: 16px;
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
input:focus {
  outline: none;
  border-color: #58a6ff;
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
</style>
