<script setup lang="ts">
import { ref } from 'vue'
import { useChatStore } from '../stores/chat'
import type { SkillDebugForm } from '../api/debug'

defineEmits<{ (e: 'close'): void }>()

const chatStore = useChatStore()
const errorMsg = ref('')

const form = ref({
  name: '',
  skillType: 'tool' as 'tool' | 'prompt',
  description: '',
  propertiesText: '',
  requiredText: '',
  toolCode: '',
  promptContent: '',
  testMessage: '',
})

function buildFormData(): SkillDebugForm {
  return {
    name: form.value.name,
    skillType: form.value.skillType,
    description: form.value.description,
    propertiesText: form.value.propertiesText,
    requiredText: form.value.requiredText,
    toolCode: form.value.toolCode,
    promptContent: form.value.promptContent,
  }
}

async function submit() {
  errorMsg.value = ''

  if (!form.value.name.trim()) {
    errorMsg.value = '请输入 Skill 名称'
    return
  }
  if (!form.value.testMessage.trim()) {
    errorMsg.value = '请输入测试消息'
    return
  }

  if (form.value.skillType === 'tool') {
    if (form.value.propertiesText.trim()) {
      try { JSON.parse(form.value.propertiesText) } catch {
        errorMsg.value = 'properties 格式错误'
        return
      }
    }
    if (form.value.requiredText.trim()) {
      try {
        const arr = JSON.parse(form.value.requiredText)
        if (!Array.isArray(arr)) throw new Error()
      } catch {
        errorMsg.value = 'required 格式错误'
        return
      }
    }
  }

  try {
    await chatStore.debugSkillCreate(buildFormData(), form.value.testMessage.trim())
  } catch (e: any) {
    errorMsg.value = e.message || '调试失败'
  }
}

const canSubmit = !chatStore.isStreaming
</script>

<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h2>创建 Skill 并调试</h2>
        <button class="btn-close" @click="$emit('close')">x</button>
      </div>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <div class="form-scroll">
        <label>Skill 名称</label>
        <input v-model="form.name" placeholder="如：代码审查" />

        <label>类型</label>
        <select v-model="form.skillType">
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
          <input v-model="form.requiredText" placeholder='["expression"]' />

          <label>执行代码（函数体，可用 params 访问参数）</label>
          <textarea
            v-model="form.toolCode"
            placeholder="return { result: eval(params.expression) };"
            rows="3"
            class="code-input"
          />
        </template>

        <template v-else>
          <label>Prompt 指令内容</label>
          <textarea
            v-model="form.promptContent"
            placeholder="你是一名专业的代码审查员..."
            rows="4"
            class="code-input"
          />
        </template>

        <label class="test-label">测试消息（用于调试该 Skill）</label>
        <textarea
          v-model="form.testMessage"
          placeholder="输入一个测试问题，如：帮我计算 3+5*2"
          rows="2"
          class="code-input"
        />
      </div>

      <div class="modal-actions">
        <button class="btn-outline" @click="$emit('close')">取消</button>
        <button
          class="btn-primary"
          :disabled="!form.name || !form.testMessage || !canSubmit"
          @click="submit"
        >
          {{ canSubmit ? '创建并调试' : '调试中...' }}
        </button>
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
  z-index: 300;
}
.modal {
  width: 500px;
  max-height: 85vh;
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
.form-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
label {
  display: block;
  font-size: 12px;
  color: #8b949e;
  margin-bottom: 4px;
  margin-top: 10px;
}
.test-label {
  color: #58a6ff;
  font-weight: 600;
  margin-top: 16px;
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
</style>
