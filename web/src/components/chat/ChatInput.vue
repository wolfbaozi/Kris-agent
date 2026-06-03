<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  disabled?: boolean
}>()

const emit = defineEmits<{
  send: [content: string]
}>()

const input = ref('')

function submit() {
  const text = input.value.trim()
  if (!text) return
  emit('send', text)
  input.value = ''
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    submit()
  }
}
</script>

<template>
  <div class="chat-input">
    <textarea
      v-model="input"
      :disabled="disabled"
      placeholder="输入问题，Enter 发送，Shift+Enter 换行"
      rows="3"
      @keydown="onKeydown"
    />
    <div class="actions">
      <span class="hint">Agent 学习 Week 1 · 流式对话</span>
      <button type="button" :disabled="disabled || !input.trim()" @click="submit">
        发送
      </button>
    </div>
  </div>
</template>

<style scoped>
.chat-input {
  border-top: 1px solid #30363d;
  padding: 16px 20px;
  background: #161b22;
}

textarea {
  width: 100%;
  resize: vertical;
  min-height: 72px;
  max-height: 200px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #30363d;
  background: #0d1117;
  color: #e6edf3;
  font: inherit;
  line-height: 1.5;
  box-sizing: border-box;
}

textarea:focus {
  outline: none;
  border-color: #58a6ff;
}

textarea:disabled {
  opacity: 0.6;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.hint {
  font-size: 12px;
  color: #8b949e;
}

button {
  padding: 8px 18px;
  border: none;
  border-radius: 8px;
  background: #238636;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

button:not(:disabled):hover {
  background: #2ea043;
}
</style>
