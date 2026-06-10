<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const isRegister = ref(false)
const username = ref('')
const password = ref('')
const role = ref('developer')
const errorMsg = ref('')
const loading = ref(false)

const roleOptions = [
  { value: 'developer', label: '开发者', desc: '可以编写代码和技术配置' },
  { value: 'product_manager', label: '产品经理', desc: '用文字描述需求，AI 自动生成配置' },
  { value: 'designer', label: '设计师', desc: '用文字描述需求，AI 自动生成配置' },
]

async function submit() {
  errorMsg.value = ''
  loading.value = true
  try {
    if (isRegister.value) {
      await auth.register(username.value, password.value, role.value)
    } else {
      await auth.login(username.value, password.value)
    }
    router.push('/chat')
  } catch (e: any) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}

function toggleMode() {
  isRegister.value = !isRegister.value
  errorMsg.value = ''
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1>{{ isRegister ? '注册' : '登录' }}</h1>
      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
      <input v-model="username" placeholder="用户名" />
      <input v-model="password" type="password" placeholder="密码（至少6位）" @keyup.enter="submit" />
      <div v-if="isRegister" class="role-section">
        <label class="role-label">选择你的角色</label>
        <div class="role-options">
          <div
            v-for="opt in roleOptions"
            :key="opt.value"
            class="role-option"
            :class="{ active: role === opt.value }"
            @click="role = opt.value"
          >
            <div class="role-name">{{ opt.label }}</div>
            <div class="role-desc">{{ opt.desc }}</div>
          </div>
        </div>
      </div>
      <button :disabled="loading || !username || !password" @click="submit">
        {{ loading ? '请稍候...' : (isRegister ? '注册' : '登录') }}
      </button>
      <p class="toggle">
        {{ isRegister ? '已有账号？' : '没有账号？' }}
        <a href="#" @click.prevent="toggleMode">{{ isRegister ? '去登录' : '去注册' }}</a>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-color, #0d1117);
}

.auth-card {
  width: 380px;
  padding: 32px;
  border: 1px solid #30363d;
  border-radius: 12px;
  background: #161b22;
}

.auth-card h1 {
  margin: 0 0 16px;
  font-size: 24px;
  text-align: center;
  color: #e6edf3;
}

.error {
  color: #f85149;
  font-size: 13px;
  margin-bottom: 12px;
  text-align: center;
}

input {
  width: 100%;
  padding: 10px 12px;
  margin-bottom: 12px;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: #0d1117;
  color: #e6edf3;
  font-size: 14px;
  box-sizing: border-box;
}

input:focus {
  outline: none;
  border-color: #58a6ff;
}

.role-section {
  margin-bottom: 12px;
}

.role-label {
  display: block;
  font-size: 13px;
  color: #8b949e;
  margin-bottom: 8px;
}

.role-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.role-option {
  padding: 10px 12px;
  border: 1px solid #30363d;
  border-radius: 6px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.role-option:hover {
  border-color: #58a6ff;
}

.role-option.active {
  border-color: #58a6ff;
  background: #1f6feb22;
}

.role-name {
  font-size: 14px;
  color: #e6edf3;
  font-weight: 500;
}

.role-desc {
  font-size: 12px;
  color: #8b949e;
  margin-top: 2px;
}

button {
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 6px;
  background: #238636;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  margin-top: 4px;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toggle {
  margin: 16px 0 0;
  text-align: center;
  font-size: 13px;
  color: #8b949e;
}

.toggle a {
  color: #58a6ff;
  text-decoration: none;
}
</style>
