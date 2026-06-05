import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { streamChat } from '../api/chat'
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
  const controller = ref<AbortController | null>(null)
  const selectedKeyId = ref<number | null>(null)
  const selectedMcpIds = ref<number[]>([])
  const selectedSkillIds = ref<number[]>([])

  const canSend = computed(() => !isStreaming.value)

  function stopGeneration() {
    if (controller.value) {
      controller.value.abort()
      controller.value = null
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
    const ctrl = new AbortController()
    controller.value = ctrl

    try {
      await streamChat(
        payload,
        (chunk) => {
          if (!messages.value.length) return
          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg.id !== assistantId || lastMsg.role !== 'assistant') return
          if (chunk.type === 'text-delta') {
            lastMsg.content += chunk.content || ''
          } else if (chunk.type === 'tool-call') {
            lastMsg.content += `\n[调用工具: ${chunk.toolName}(${JSON.stringify(chunk.args)})]\n\n`
          } else if (chunk.type === 'tool-result') {
            lastMsg.content += `\n[工具 ${chunk.toolName} 返回: ${typeof chunk.result === 'string' ? chunk.result : JSON.stringify(chunk.result)}]\n`
          }
        },
        ctrl.signal,
        selectedKeyId.value,
        selectedMcpIds.value,
        selectedSkillIds.value,
      )
    } catch (e: any) {
      if (e.name === 'AbortError') {
        const target = messages.value.find((m) => m.id === assistantId)
        if (target && !target.content) target.content = '(已停止生成)'
      } else {
        const target = messages.value.find((m) => m.id === assistantId)
        const message = e instanceof Error ? e.message : '发送失败'
        error.value = message
        if (target && !target.content) {
          target.content = `出错了：${message}`
        }
      }
    } finally {
      isStreaming.value = false
      controller.value = null
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
    canSend,
    selectedKeyId,
    selectedMcpIds,
    selectedSkillIds,
    sendMessage,
    clearMessages,
    stopGeneration,
  }
})
