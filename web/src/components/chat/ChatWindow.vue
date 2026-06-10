<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useChatStore } from '../../stores/chat'
import ChatMessage from './ChatMessage.vue'
import ChatInput from './ChatInput.vue'

const chatStore = useChatStore()
const { messages, isStreaming, error, canSend } =
  storeToRefs(chatStore)

const listRef = ref<HTMLElement | null>(null)
const chatInputRef = ref<InstanceType<typeof ChatInput> | null>(null)

function refreshInputKeys() {
  chatInputRef.value?.refreshKeys?.()
}

defineExpose({ refreshInputKeys })

async function scrollToBottom() {
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

watch(
  [() => messages.value.length, () => messages.value[messages.value.length - 1]?.content],
  scrollToBottom,
)

</script>

<template>
  <div class="chat-layout">
    <header class="header">
      <div>
        <h1>AI Agent Demo</h1>
        <p>Vue3 + TypeScript + Node BFF · 流式对话</p>
      </div>
      <div class="header-actions">
        <button type="button" class="ghost" :disabled="isStreaming" @click="chatStore.clearMessages()">
          清空会话
        </button>
      </div>
    </header>

    <main ref="listRef" class="messages">
      <ChatMessage
        v-for="(message, index) in messages"
        :key="message.id"
        :message="message"
        :streaming="isStreaming && index === messages.length - 1 && message.role === 'assistant'"
      />
      <p v-if="error" class="error">{{ error }}</p>
    </main>

    <ChatInput
      ref="chatInputRef"
      :disabled="!canSend"
      :is-streaming="isStreaming"
      @send="chatStore.sendMessage"
      @stop="chatStore.stopGeneration"
      @select-key="chatStore.selectedKeyId = $event"
      @select-mcp="chatStore.selectedMcpIds = $event"
      @select-skill="chatStore.selectedSkillIds = $event"
    />
  </div>
</template>

<style scoped>
.chat-layout {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 54px);
  background: #0d1117;
  color: #e6edf3;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #30363d;
  background: #161b22;
}

.header h1 {
  margin: 0;
  font-size: 18px;
}

.header p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #8b949e;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status {
  font-size: 12px;
  color: #3fb950;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(63, 185, 80, 0.12);
}

.status.warn {
  color: #f85149;
  background: rgba(248, 81, 73, 0.12);
}

.ghost {
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid #30363d;
  background: transparent;
  color: #c9d1d9;
  cursor: pointer;
}

.ghost:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.error {
  margin: 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(248, 81, 73, 0.12);
  color: #ff7b72;
  font-size: 13px;
}
</style>
