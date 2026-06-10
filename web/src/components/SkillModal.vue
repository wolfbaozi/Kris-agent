<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useSkillStore } from '../stores/skill'
import { useAuthStore } from '../stores/auth'
import { useRoleOptionStore } from '../stores/roleOption'
import { skillApi } from '../api/index'

const store = useSkillStore()
const auth = useAuthStore()
const roleStore = useRoleOptionStore()

defineEmits<{ (e: 'close'): void }>()

const showForm = ref(false)
const editingId = ref<number | null>(null)
const errorMsg = ref('')
const aiLoading = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const useAiMode = ref(true)

const isDeveloper = computed(() => auth.role === 'developer')

const form = ref({
  name: '',
  skillType: 'tool' as 'tool' | 'prompt',
  description: '',
  propertiesText: '',
  requiredText: '',
  toolCode: '',
  promptContent: '',
})

const simpleForm = ref({
  description: '',
  role: auth.role || 'developer',
})

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
  form.value = {
    name: '', skillType: 'tool', description: '', propertiesText: '',
    requiredText: '', toolCode: '', promptContent: '',
  }
  simpleForm.value = { description: '', role: auth.role || 'developer' }
  useAiMode.value = true
  showForm.value = true
  errorMsg.value = ''
}

function openEdit(s: any) {
  if ((s.isGlobal || s.is_global) && !isKris) return
  editingId.value = s.id
  form.value = {
    name: s.name,
    skillType: s.skillType || s.skill_type,
    description: s.toolSchema?.description || s.tool_schema?.description || '',
    propertiesText: s.toolSchema?.parameters?.properties
      ? JSON.stringify(s.toolSchema.parameters.properties)
      : (s.tool_schema?.parameters?.properties ? JSON.stringify(s.tool_schema.parameters.properties) : ''),
    requiredText: s.toolSchema?.parameters?.required
      ? JSON.stringify(s.toolSchema.parameters.required)
      : (s.tool_schema?.parameters?.required ? JSON.stringify(s.tool_schema.parameters.required) : ''),
    toolCode: s.toolCode || s.tool_code || '',
    promptContent: s.promptContent || s.prompt_content || '',
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
    (s) => s.name === trimName && s.id !== editingId.value
  )
  if (duplicate) {
    errorMsg.value = `Skill名称"${trimName}"已存在`
    return
  }

  if (form.value.skillType === 'tool') {
    let properties = {}
    let required: string[] = []
    try {
      if (form.value.propertiesText.trim()) {
        properties = JSON.parse(form.value.propertiesText)
      }
    } catch (e: any) {
      errorMsg.value = 'properties 格式错误: ' + e.message
      return
    }
    try {
      if (form.value.requiredText.trim()) {
        required = JSON.parse(form.value.requiredText)
        if (!Array.isArray(required)) throw new Error('required 必须是数组')
      }
    } catch (e: any) {
      errorMsg.value = 'required 格式错误: ' + e.message
      return
    }

    const toolSchema = {
      description: form.value.description,
      parameters: {
        type: 'object',
        properties,
        ...(required.length > 0 ? { required } : {}),
      },
    }

    try {
      if (editingId.value) {
        await store.update(editingId.value, {
          name: form.value.name,
          skillType: 'tool',
          toolSchema,
          toolCode: form.value.toolCode,
        })
      } else {
        await store.create({
          name: form.value.name,
          skillType: 'tool',
          toolSchema,
          toolCode: form.value.toolCode,
        })
      }
      showForm.value = false
    } catch (e: any) {
      errorMsg.value = e.message
    }
  } else {
    try {
      if (editingId.value) {
        await store.update(editingId.value, {
          name: form.value.name,
          skillType: 'prompt',
          promptContent: form.value.promptContent,
        })
      } else {
        await store.create({
          name: form.value.name,
          skillType: 'prompt',
          promptContent: form.value.promptContent,
        })
      }
      showForm.value = false
    } catch (e: any) {
      errorMsg.value = e.message
    }
  }
}

async function aiCreate() {
  errorMsg.value = ''
  const desc = simpleForm.value.description.trim()
  if (!desc) {
    errorMsg.value = '请描述你想要的 Skill 功能'
    return
  }
  aiLoading.value = true
  try {
    await skillApi.aiCreate(desc, simpleForm.value.role)
    await store.fetchList()
    showForm.value = false
  } catch (e: any) {
    errorMsg.value = e.message
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
    await skillApi.upload(file, simpleForm.value.role)
    await store.fetchList()
    showForm.value = false
  } catch (e: any) {
    errorMsg.value = e.message
  } finally {
    aiLoading.value = false
    input.value = ''
  }
}

async function deleteSkill(id: number) {
  if (!confirm('确定删除该 Skill？')) return
  try {
    await store.remove(id)
  } catch (e: any) {
    errorMsg.value = e.message
  }
}

async function toggleSkill(id: number) {
  try {
    await store.toggle(id)
  } catch (e: any) {
    errorMsg.value = e.message
  }
}

async function exportSkill(id: number) {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(`/api/skills/${id}/export`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!response.ok) throw new Error('导出失败')
    const blob = await response.blob()
    const disposition = response.headers.get('Content-Disposition')
    const filenameMatch = disposition?.match(/filename="(.+)"/)
    const filename = filenameMatch ? filenameMatch[1] : `skill-${id}.json`
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  } catch (e: any) {
    errorMsg.value = e.message
  }
}

onMounted(() => {
  loadList()
  roleStore.fetchList()
})
</script>

<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h2>Skill 管理</h2>
        <button class="btn-close" @click="$emit('close')">✕</button>
      </div>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <div class="list">
        <div v-if="store.list.length === 0" class="empty">暂无 Skill 配置，请先添加</div>

        <div v-for="s in store.list" :key="s.id" class="card">
          <div class="card-info">
            <span class="name">{{ s.name }}</span>
            <span v-if="s.isGlobal || s.is_global" class="tag-global">全局</span>
            <span class="tag type-tag">{{ (s.skillType || s.skill_type) === 'tool' ? 'Tool' : 'Prompt' }}</span>
            <span v-if="(s.sourceType || s.source_type) === 'ai_gen'" class="tag ai-tag">AI</span>
            <span v-if="!s.enabled" class="tag tag-off">已禁用</span>
          </div>
          <div class="card-actions">
            <button class="btn-outline-sm" @click="toggleSkill(s.id)">
              {{ s.enabled ? '禁用' : '启用' }}
            </button>
            <button
              v-if="!(s.isGlobal || s.is_global) || isKris"
              class="btn-outline-sm"
              @click="exportSkill(s.id)"
            >
              导出
            </button>
            <button
              v-if="!(s.isGlobal || s.is_global) || isKris"
              class="btn-outline-sm"
              @click="openEdit(s)"
            >
              编辑
            </button>
            <button
              v-if="!(s.isGlobal || s.is_global) || isKris"
              class="btn-danger-sm"
              @click="deleteSkill(s.id)"
            >
              删除
            </button>
          </div>
        </div>
      </div>

      <button class="btn-add" @click="openAdd">+ 添加 Skill</button>

      <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
        <div class="modal form-modal">
          <h2>{{ editingId ? '编辑' : '添加' }} Skill</h2>

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
              用文字描述你想要的功能，AI 会自动生成 Skill 配置
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
              placeholder="例如：帮我写一个能够自动总结会议内容的工具，输入会议文字记录，输出结构化的会议纪要..."
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
            <label>Skill 名称</label>
            <input v-model="form.name" placeholder="如：代码审查" />

            <label>类型</label>
            <select v-model="form.skillType" :disabled="!!editingId">
              <option value="tool">函数式 Tool</option>
              <option value="prompt">Prompt 指令</option>
            </select>

            <template v-if="form.skillType === 'tool'">
              <label>功能描述</label>
              <input v-model="form.description" placeholder="描述此 Tool 的功能" />

              <label>参数 Schema（JSON 对象）</label>
              <input
                v-model="form.propertiesText"
                placeholder='{"expression": {"type": "string", "description": "数学表达式"}}'
              />

              <label>必填参数（JSON 数组，可选）</label>
              <input
                v-model="form.requiredText"
                placeholder='["expression"]'
              />

              <label>执行代码（函数体，可用 params 访问参数）</label>
              <textarea
                v-model="form.toolCode"
                placeholder="return { result: eval(params.expression) };"
                rows="4"
                class="code-input"
              />
            </template>

            <template v-else>
              <label>Prompt 指令内容</label>
              <textarea
                v-model="form.promptContent"
                placeholder="你是一名专业的代码审查员..."
                rows="6"
                class="code-input"
              />
            </template>
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
  width: 560px;
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
.type-tag {
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
  width: 480px;
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
</style>
