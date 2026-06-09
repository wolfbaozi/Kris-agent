<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ChatWindow from '../components/chat/ChatWindow.vue'
import KeysModal from '../components/KeysModal.vue'
import McpModal from '../components/McpModal.vue'
import SkillModal from '../components/SkillModal.vue'
import SkillDebugModal from '../components/SkillDebugModal.vue'
import McpDebugModal from '../components/McpDebugModal.vue'
import { useRouter } from 'vue-router'
import { useMcpStore } from '../stores/mcp'
import { useSkillStore } from '../stores/skill'

const router = useRouter()
const showKeys = ref(false)
const showMcp = ref(false)
const showSkill = ref(false)
const showSkillDebug = ref(false)
const showMcpDebug = ref(false)
const chatWindowRef = ref<InstanceType<typeof ChatWindow> | null>(null)
const mcpStore = useMcpStore()
const skillStore = useSkillStore()

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('userId')
  router.push('/login')
}

function onKeysModalClose() {
  showKeys.value = false
  chatWindowRef.value?.refreshInputKeys?.()
}

onMounted(() => {
  mcpStore.fetchList()
  skillStore.fetchList()
})
</script>

<template>
  <div class="chat-page">
    <header class="chat-header">
      <span class="app-title">Kris Agent</span>
      <div class="header-actions">
        <button class="btn-outline" @click="showMcp = true">MCP 管理</button>
        <button class="btn-outline" @click="showSkill = true">Skill 管理</button>
        <button class="btn-outline" @click="showKeys = true">API Key 管理</button>
        <button class="btn-outline" @click="logout">退出登录</button>
      </div>
    </header>
    <main class="chat-main">
      <ChatWindow
        ref="chatWindowRef"
        @open-skill-debug="showSkillDebug = true"
        @open-mcp-debug="showMcpDebug = true"
      />
    </main>
    <KeysModal v-if="showKeys" @close="onKeysModalClose" />
    <McpModal v-if="showMcp" @close="showMcp = false" />
    <SkillModal v-if="showSkill" @close="showSkill = false" />
    <SkillDebugModal v-if="showSkillDebug" @close="showSkillDebug = false" />
    <McpDebugModal v-if="showMcpDebug" @close="showMcpDebug = false" />
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
