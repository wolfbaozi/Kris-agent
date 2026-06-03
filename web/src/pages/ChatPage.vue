<script setup lang="ts">
import { ref } from 'vue'
import ChatWindow from '../components/chat/ChatWindow.vue'
import KeysModal from '../components/KeysModal.vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const showKeys = ref(false)

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('userId')
  router.push('/login')
}
</script>

<template>
  <div class="chat-page">
    <header class="chat-header">
      <span class="app-title">Kris Agent</span>
      <div class="header-actions">
        <button class="btn-outline" @click="showKeys = true">API Key 管理</button>
        <button class="btn-outline" @click="logout">退出登录</button>
      </div>
    </header>
    <main class="chat-main">
      <ChatWindow />
    </main>
    <KeysModal v-if="showKeys" @close="showKeys = false" />
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--bg-color, #0d1117);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  border-bottom: 1px solid #30363d;
  background: #161b22;
}

.app-title {
  font-size: 16px;
  font-weight: 600;
  color: #e6edf3;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.btn-outline {
  padding: 6px 14px;
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
