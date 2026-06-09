<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useSkillStore } from '../stores/skill'
import { useAuthStore } from '../stores/auth'

const store = useSkillStore()
const auth = useAuthStore()

defineEmits<{ (e: 'close'): void }>()

const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref({
  name: '',
  skillType: 'tool' as 'tool' | 'prompt',
  description: '',
  propertiesText: '',
  requiredText: '',
  toolCode: '',
  promptContent: '',
})
const errorMsg = ref('')
const isKris = auth.user?.username === 'Kris'

const skillTypeLabel = computed(() => (form.value.skillType === 'tool' ? '函数式 Tool' : 'Prompt 指令'))

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
  showForm.value = true
  errorMsg.value = ''
}

function openEdit(s: any) {
  if (s.is_global && !isKris) return
  editingId.value = s.id
  form.value = {
    name: s.name,
    skillType: s.skill_type,
    description: s.tool_schema?.description || '',
    propertiesText: s.tool_schema?.parameters
      ? JSON.stringify(s.tool_schema.parameters.properties || {})
      : '',
    requiredText: s.tool_schema?.parameters?.required
      ? JSON.stringify(s.tool_schema.parameters.required)
      : '',
    toolCode: s.tool_code || '',
    promptContent: s.prompt_content || '',
  }
  showForm.value = true
  errorMsg.value = ''
}

async function save() {
  errorMsg.value = ''

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

onMounted(loadList)
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
            <span v-if="s.is_global" class="tag-global">全局</span>
            <span class="tag type-tag">{{ s.skill_type === 'tool' ? 'Tool' : 'Prompt' }}</span>
            <span v-if="!s.enabled" class="tag tag-off">已禁用</span>
          </div>
          <div class="card-actions">
            <button class="btn-outline-sm" @click="toggleSkill(s.id)">
              {{ s.enabled ? '禁用' : '启用' }}
            </button>
            <button
              v-if="!s.is_global || isKris"
              class="btn-outline-sm"
              @click="openEdit(s)"
            >
              编辑
            </button>
            <button
              v-if="!s.is_global || isKris"
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

            <label>参数 Schema（JSON 对象，key=参数名，value={type,description}）</label>
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
  width: 440px;
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
