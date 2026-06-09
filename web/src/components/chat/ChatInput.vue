<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { keysApi } from '../../api/index'
import { useMcpStore } from '../../stores/mcp'
import { useSkillStore } from '../../stores/skill'

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
  selectMcp: [mcpIds: number[]]
  selectSkill: [skillIds: number[]]
  openSkillDebug: []
  openMcpDebug: []
}>()

const input = ref('')
const keys = ref<KeyOption[]>([])
const textRef = ref<HTMLTextAreaElement | null>(null)
const mcpStore = useMcpStore()
const skillStore = useSkillStore()
const selectedMcpIds = ref<Set<number>>(new Set())
const selectedSkillIds = ref<Set<number>>(new Set())

watch(selectedMcpIds, (ids) => emit('selectMcp', [...ids]), { deep: true })
watch(selectedSkillIds, (ids) => emit('selectSkill', [...ids]), { deep: true })

function toggleMcp(id: number) {
  const s = new Set(selectedMcpIds.value)
  s.has(id) ? s.delete(id) : s.add(id)
  selectedMcpIds.value = s
}

function toggleSkill(id: number) {
  const s = new Set(selectedSkillIds.value)
  s.has(id) ? s.delete(id) : s.add(id)
  selectedSkillIds.value = s
}

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

const selectedKeyId = ref<number | null>(null)

async function loadKeys() {
  try {
    keys.value = await keysApi.list()
    if (keys.value.length > 0 && !selectedKeyId.value) {
      selectedKeyId.value = keys.value[0].id
      emit('selectKey', selectedKeyId.value)
    }
  } catch {}
}

function onSelectKey(e: Event) {
  const target = e.target as HTMLSelectElement
  const id = parseInt(target.value, 10)
  if (id) {
    selectedKeyId.value = id
    emit('selectKey', id)
  }
}

defineExpose({ refreshKeys: loadKeys })

onMounted(async () => {
  await loadKeys()
  mcpStore.fetchList()
  skillStore.fetchList()
})
</script>

<template>
  <div class="chat-input">
    <div class="ext-selects" v-if="mcpStore.enabledList.length > 0 || skillStore.enabledList.length > 0">
      <div v-if="mcpStore.enabledList.length > 0" class="ext-group">
        <span class="ext-label">MCP</span>
        <label
          v-for="m in mcpStore.enabledList"
          :key="m.id"
          class="ext-item"
          :class="{ active: selectedMcpIds.has(m.id) }"
        >
          <input
            type="checkbox"
            :checked="selectedMcpIds.has(m.id)"
            @change="toggleMcp(m.id)"
          />
          <span>{{ m.name }}</span>
        </label>
      </div>
      <div v-if="skillStore.enabledList.length > 0" class="ext-group">
        <span class="ext-label">Skill</span>
        <label
          v-for="s in skillStore.enabledList"
          :key="s.id"
          class="ext-item"
          :class="{ active: selectedSkillIds.has(s.id) }"
        >
          <input
            type="checkbox"
            :checked="selectedSkillIds.has(s.id)"
            @change="toggleSkill(s.id)"
          />
          <span>{{ s.name }}</span>
        </label>
      </div>
    </div>

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
        <select v-if="keys.length > 0" class="model-select" :value="selectedKeyId ?? ''" @change="onSelectKey">
          <option value="">选择模型</option>
          <option v-for="k in keys" :key="k.id" :value="k.id">
            {{ k.provider.toUpperCase() }} - {{ k.model || '默认' }}
          </option>
        </select>
        <button type="button" class="btn-debug" @click="$emit('openSkillDebug')">新 Skill 调试</button>
        <button type="button" class="btn-debug" @click="$emit('openMcpDebug')">新 MCP 调试</button>
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

.ext-selects {
  display: flex;
  gap: 16px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.ext-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.ext-label {
  font-size: 11px;
  color: #8b949e;
  font-weight: 600;
}

.ext-item {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  border: 1px solid #30363d;
  border-radius: 4px;
  font-size: 11px;
  color: #c9d1d9;
  cursor: pointer;
  user-select: none;
}

.ext-item.active {
  border-color: #58a6ff;
  background: #1f6feb22;
  color: #58a6ff;
}

.ext-item input[type="checkbox"] {
  display: none;
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

.btn-debug {
  padding: 4px 10px;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: transparent;
  color: #58a6ff;
  font-size: 11px;
  cursor: pointer;
  font-weight: 500;
}

.btn-debug:hover {
  background: #1f6feb22;
  border-color: #58a6ff;
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
