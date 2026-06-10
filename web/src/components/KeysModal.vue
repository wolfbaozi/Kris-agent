<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { keysApi } from '../api/index'
import type { KeyItem } from '../types/key'

defineEmits<{ (e: 'close'): void }>()

const keys = ref<KeyItem[]>([])
const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ provider: 'deepseek', apiKey: '', model: '', baseUrl: '' })

async function loadKeys() {
  keys.value = await keysApi.list()
}

function openAdd() {
  editingId.value = null
  form.value = { provider: 'deepseek', apiKey: '', model: '', baseUrl: '' }
  showForm.value = true
}

function onProviderChange(p: string) {
  if (editingId.value) return
  if (p === 'doubao' || p === 'volcano') {
    form.value.baseUrl = 'https://ark.cn-beijing.volces.com/api/v3'
    form.value.model = ''
  } else {
    form.value.baseUrl = ''
    form.value.model = ''
  }
}

function openEdit(k: KeyItem) {
  editingId.value = k.id
  form.value = { provider: k.provider, apiKey: '', model: k.model, baseUrl: k.base_url }
  showForm.value = true
}

async function saveKey() {
  if (editingId.value) {
    const payload: any = {}
    if (form.value.apiKey) payload.apiKey = form.value.apiKey
    payload.model = form.value.model
    payload.baseUrl = form.value.baseUrl
    await keysApi.update(editingId.value, payload)
  } else {
    await keysApi.create(form.value)
  }
  showForm.value = false
  await loadKeys()
}

async function deleteKey(id: number) {
  if (!confirm('确定删除该 API Key？')) return
  await keysApi.remove(id)
  await loadKeys()
}

onMounted(loadKeys)
</script>

<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h2>API Key 管理</h2>
        <button class="btn-close" @click="$emit('close')">✕</button>
      </div>

      <div class="keys-list">
        <div v-if="keys.length === 0" class="empty">暂无 API Key，请先添加</div>

        <div v-for="k in keys" :key="k.id" class="key-card">
          <div class="key-info">
            <span class="provider-tag">{{ k.provider }}</span>
            <span class="model-name">{{ k.model || '(默认模型)' }}</span>
            <span class="key-masked">****</span>
          </div>
          <div class="key-actions">
            <button class="btn-outline-sm" @click="openEdit(k)">编辑</button>
            <button class="btn-danger-sm" @click="deleteKey(k.id)">删除</button>
          </div>
        </div>
      </div>

      <button class="btn-add" @click="openAdd">+ 添加 Key</button>

      <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
        <div class="modal form-modal">
          <h2>{{ editingId ? '编辑' : '添加' }} API Key</h2>
          <label>提供商</label>
          <select v-model="form.provider" :disabled="!!editingId" @change="onProviderChange(form.provider)">
            <option value="deepseek">DeepSeek</option>
            <option value="openai">OpenAI</option>
            <option value="doubao">豆包（字节）</option>
            <option value="volcano">火山引擎</option>
          </select>
          <label>API Key</label>
          <input v-model="form.apiKey" type="password" :placeholder="editingId ? '留空则不修改' : '请输入 API Key'" />
          <label>模型名称（可选）</label>
          <input v-model="form.model" :placeholder="['doubao','volcano'].includes(form.provider) ? '如 ep-xxxxxxxx 或 doubao-lite-32k-240828' : '如 deepseek-chat / gpt-4o-mini'" />
          <label>Base URL（可选，OpenAI可不填）</label>
          <input v-model="form.baseUrl" placeholder="自定义 baseURL" />
          <div class="modal-actions">
            <button class="btn-outline" @click="showForm = false">取消</button>
            <button class="btn-primary" :disabled="!editingId && !form.apiKey" @click="saveKey">保存</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.modal {
  width: 480px;
  max-height: 80vh;
  padding: 24px;
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.modal-header h2 {
  margin: 0;
  font-size: 18px;
  color: #e6edf3;
}

.btn-close {
  width: 28px;
  height: 28px;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: transparent;
  color: #8b949e;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close:hover {
  background: #30363d;
  color: #e6edf3;
}

.error {
  color: #f85149;
  font-size: 13px;
  margin: 0 0 12px;
}

.keys-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 12px;
  min-height: 0;
}

.empty {
  color: #8b949e;
  font-size: 14px;
  padding: 20px 0;
  text-align: center;
}

.key-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  margin-bottom: 8px;
  border: 1px solid #30363d;
  border-radius: 8px;
  background: #0d1117;
}

.key-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.provider-tag {
  display: inline-block;
  padding: 2px 6px;
  background: #1f6feb;
  color: #fff;
  border-radius: 4px;
  font-size: 11px;
  text-transform: uppercase;
  flex-shrink: 0;
}

.model-name {
  color: #c9d1d9;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.key-masked {
  color: #8b949e;
  font-size: 12px;
  font-family: monospace;
  flex-shrink: 0;
}

.key-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.btn-add {
  padding: 8px 0;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: transparent;
  color: #58a6ff;
  font-size: 14px;
  cursor: pointer;
  width: 100%;
}

.btn-add:hover {
  background: #1f6feb22;
}

.form-modal {
  width: 400px;
  position: relative;
}

.form-modal h2 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #e6edf3;
}

label {
  display: block;
  font-size: 12px;
  color: #8b949e;
  margin-bottom: 4px;
  margin-top: 10px;
}

select,
input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: #0d1117;
  color: #e6edf3;
  font-size: 13px;
  box-sizing: border-box;
}

select:focus,
input:focus {
  outline: none;
  border-color: #58a6ff;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.btn-primary {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  background: #238636;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-outline {
  padding: 8px 16px;
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

.btn-outline-sm {
  padding: 3px 10px;
  border: 1px solid #30363d;
  border-radius: 4px;
  background: transparent;
  color: #c9d1d9;
  font-size: 11px;
  cursor: pointer;
}

.btn-outline-sm:hover {
  background: #30363d;
}

.btn-danger-sm {
  padding: 3px 10px;
  border: 1px solid #f85149;
  border-radius: 4px;
  background: transparent;
  color: #f85149;
  font-size: 11px;
  cursor: pointer;
}

.btn-danger-sm:hover {
  background: #f8514922;
}
</style>
