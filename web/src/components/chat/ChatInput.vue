<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { keysApi } from '../../api/index'

interface KeyOption {
  id: number
  provider: string
  model: string
  base_url: string
}

defineProps<{
  disabled?: boolean
  isStreaming?: boolean
}>()

const emit = defineEmits<{
  send: [content: string]
  stop: []
  selectKey: [keyId: number]
}>()

const input = ref('')
const keys = ref<KeyOption[]>([])
const textRef = ref<HTMLTextAreaElement | null>(null)

function adjustHeight() {
  const el = textRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 200) + 'px'
}

async function onInput() {
  await nextTick()
  adjustHeight()
}

function submit() {
  const text = input.value.trim()
  if (!text) return
  emit('send', text)
  input.value = ''
  nextTick(() => adjustHeight())
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    submit()
  }
}

function onSelectKey(e: Event) {
  const target = e.target as HTMLSelectElement
  const id = parseInt(target.value, 10)
  if (id) emit('selectKey', id)
}

onMounted(async () => {
  try {
    keys.value = await keysApi.list()
  } catch {}
})
</script>

<template>
  <div class="chat-input">
    <div class="input-box">
      <textarea
        ref="textRef"
        v-model="input"
        :disabled="disabled"
        placeholder="输入问题，Enter 发送，Shift+Enter 换行"
        rows="1"
        @keydown="onKeydown"
        @input="onInput"
      />
      <div class="input-toolbar">
        <select v-if="keys.length > 0" class="model-select" @change="onSelectKey">
          <option v-for="k in keys" :key="k.id" :value="k.id">
            {{ k.provider.toUpperCase() }} - {{ k.model || '默认' }}
          </option>
        </select>
        <div class="toolbar-spacer" />
        <button v-if="isStreaming" type="button" class="btn-stop" @click="$emit('stop')">
          停止生成
        </button>
        <button type="button" class="btn-send" :disabled="disabled || !input.trim()" @click="submit">
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-input {
  border-top: 1px solid #30363d;
  padding: 16px 20px;
  background: #161b22;
}

.input-box {
  border: 1px solid #30363d;
  border-radius: 12px;
  background: #0d1117;
  overflow: hidden;
}

.input-box:focus-within {
  border-color: #58a6ff;
}

textarea {
  width: 100%;
  resize: none;
  min-height: 40px;
  max-height: 200px;
  padding: 10px 14px 8px;
  border: none;
  background: transparent;
  color: #e6edf3;
  font: inherit;
  line-height: 1.5;
  box-sizing: border-box;
  outline: none;
  overflow-y: auto;
}

textarea:disabled {
  opacity: 0.6;
}

.input-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px 10px;
}

.model-select {
  padding: 3px 8px;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: #161b22;
  color: #e6edf3;
  font-size: 11px;
  cursor: pointer;
  max-width: 220px;
}

.model-select:focus {
  outline: none;
  border-color: #58a6ff;
}

.toolbar-spacer {
  flex: 1;
}

button {
  padding: 6px 14px;
  border: none;
  border-radius: 7px;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  font-size: 12px;
}

.btn-send {
  background: #238636;
}

.btn-send:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-send:not(:disabled):hover {
  background: #2ea043;
}

.btn-stop {
  background: #da3633;
}

.btn-stop:hover {
  background: #f85149;
}
</style>
