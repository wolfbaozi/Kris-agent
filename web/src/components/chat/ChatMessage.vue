<script setup lang="ts">
import type { ChatMessage } from '../../types/chat'

defineProps<{
  message: ChatMessage
  streaming?: boolean
}>()
</script>

<template>
  <div class="message" :class="message.role">
    <div class="avatar">{{ message.role === 'user' ? '你' : 'Kris' }}</div>
    <div class="bubble">
      <p class="content">{{ message.content }}<span v-if="streaming" class="cursor">▍</span></p>
    </div>
  </div>
</template>

<style scoped>
.message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.message.assistant .avatar {
  background: #2d6a4f;
  color: #d8f3dc;
}

.message.user .avatar {
  background: #1d3557;
  color: #a8dadc;
}

.bubble {
  max-width: min(720px, 85%);
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message.assistant .bubble {
  background: #21262d;
  border: 1px solid #30363d;
}

.message.user .bubble {
  background: #1f6feb;
  color: #fff;
}

.content {
  margin: 0;
}

.cursor {
  animation: blink 1s step-end infinite;
  color: #58a6ff;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}
</style>
