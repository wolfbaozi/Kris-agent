import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { streamChat, checkHealth } from '../api/chat'
import type { ChatMessage } from '../types/chat'

let messageId = 0

function createId() {
  messageId += 1
  return `msg-${messageId}-${Date.now()}`
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([
    {
      id: createId(),
      role: 'assistant',
      content: '你好，我是 Agent 学习助手。可以问我 Vue3、TypeScript 或 AI Agent 相关问题。',
      createdAt: Date.now(),
    },
  ])
  const isStreaming = ref(false)
  const error = ref<string | null>(null)
  const provider = ref('')
  const modelConfigured = ref(false)

  const canSend = computed(() => !isStreaming.value)

  async function fetchHealth() {
    try {
      const data = await checkHealth()
      provider.value = data.provider
      modelConfigured.value = data.modelConfigured
    } catch {
      provider.value = 'unknown'
      modelConfigured.value = false
    }
  }

  async function sendMessage(content: string) {
    const text = content.trim()
    if (!text || isStreaming.value) return

    error.value = null
    messages.value.push({
      id: createId(),
      role: 'user',
      content: text,
      createdAt: Date.now(),
    })

    const payload = messages.value
      .filter((m) => m.role !== 'system' && m.content.trim())
      .map(({ role, content: body }) => ({ role, content: body }))

    const assistantId = createId()
    messages.value.push({
      id: assistantId,
      role: 'assistant',
      content: '',
      createdAt: Date.now(),
    })

    isStreaming.value = true
    const controller = new AbortController()

    try {
      await streamChat(
        payload,
        (chunk) => {
          const target = messages.value.find((m) => m.id === assistantId)
          if (target) target.content += chunk
        },
        controller.signal,
      )
    } catch (e) {
      const target = messages.value.find((m) => m.id === assistantId)
      const message = e instanceof Error ? e.message : '发送失败'
      error.value = message
      if (target && !target.content) {
        target.content = `出错了：${message}`
      }
    } finally {
      isStreaming.value = false
    }
  }

  function clearMessages() {
    if (isStreaming.value) return
    messages.value = [
      {
        id: createId(),
        role: 'assistant',
        content: '会话已清空。继续提问吧。',
        createdAt: Date.now(),
      },
    ]
    error.value = null
  }

  return {
    messages,
    isStreaming,
    error,
    provider,
    modelConfigured,
    canSend,
    fetchHealth,
    sendMessage,
    clearMessages,
  }
})
