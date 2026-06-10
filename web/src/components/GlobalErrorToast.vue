<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { onError } from '../api/error'

interface Toast {
  id: number
  message: string
}

const toasts = ref<Toast[]>([])
let nextId = 0
let unsubscribe: (() => void) | undefined

onMounted(() => {
  unsubscribe = onError((message) => {
    const id = nextId++
    toasts.value.push({ id, message })
    setTimeout(() => {
      toasts.value = toasts.value.filter(t => t.id !== id)
    }, 5000)
  })
})

onUnmounted(() => {
  unsubscribe?.()
})

function close(id: number) {
  toasts.value = toasts.value.filter(t => t.id !== id)
}
</script>

<template>
  <div class="toast-container">
    <div v-for="toast in toasts" :key="toast.id" class="toast">
      <span class="toast-message">{{ toast.message }}</span>
      <button class="toast-close" @click="close(toast.id)">×</button>
    </div>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 12px;
  right: 12px;
  left: 12px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 400px;
  margin: 0 auto;
}

@media (min-width: 769px) {
  .toast-container {
    left: auto;
    margin: 0;
  }
}

.toast {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #21262d;
  border: 1px solid #f85149;
  border-radius: 6px;
  color: #e6edf3;
  font-size: 14px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.toast-message {
  flex: 1;
}

.toast-close {
  background: none;
  border: none;
  color: #8b949e;
  font-size: 20px;
  cursor: pointer;
  padding: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toast-close:hover {
  color: #e6edf3;
}
</style>
