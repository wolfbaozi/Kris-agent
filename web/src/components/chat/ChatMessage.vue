<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import type { ChatMessage } from '../../types/chat'

const props = defineProps<{
  message: ChatMessage
  streaming?: boolean
}>()

const contentRef = ref<HTMLElement | null>(null)

function escapeHtml(str: string): string {
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

const md: MarkdownIt = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    const code = lang && hljs.getLanguage(lang)
      ? hljs.highlight(str, { language: lang, ignoreIllegals: true }).value
      : escapeHtml(str)
    const langLabel = lang ? `<span class="code-lang">${lang}</span>` : ''
    const copyBtn = '<button class="copy-btn">复制</button>'
    return `<pre class="hljs"><div class="code-header">${langLabel}${copyBtn}</div><code>${code}</code></pre>`
  }
})

const renderedContent = computed(() => {
  const content = props.message.content
  const rendered = md.render(content || ' ')
  if (props.streaming) {
    return rendered.replace(/<\/p>\s*$/, '<span class="cursor">▍</span></p>')
      || rendered + '<span class="cursor">▍</span>'
  }
  return rendered
})

function handleCopy(e: MouseEvent) {
  const btn = (e.target as HTMLElement).closest('.copy-btn') as HTMLButtonElement | null
  if (!btn) return
  const pre = btn.closest('pre')
  const code = pre?.querySelector('code')?.innerText ?? ''
  navigator.clipboard.writeText(code).then(() => {
    btn.textContent = '已复制'
    setTimeout(() => { btn.textContent = '复制' }, 2000)
  })
}

onMounted(() => {
  contentRef.value?.addEventListener('click', handleCopy)
})

onUnmounted(() => {
  contentRef.value?.removeEventListener('click', handleCopy)
})

watch(renderedContent, () => {
  contentRef.value?.removeEventListener('click', handleCopy)
  contentRef.value?.addEventListener('click', handleCopy)
})
</script>

<template>
  <div class="message" :class="message.role">
    <div class="avatar">{{ message.role === 'user' ? '你' : 'Kris' }}</div>
    <div class="bubble">
      <div ref="contentRef" class="content markdown-body" v-html="renderedContent"></div>
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
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
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
  max-width: min(720px, 88%);
  padding: 0 12px;
  border-radius: 12px;
  line-height: 1.6;
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

.content :deep(pre) {
  background: #0d1117;
  padding: 0;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}

.content :deep(pre code) {
  display: block;
  padding: 16px;
}

.content :deep(.code-header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #161b22;
  border-bottom: 1px solid #30363d;
  border-radius: 8px 8px 0 0;
}

.content :deep(.code-lang) {
  font-size: 0.75em;
  color: #8b949e;
  font-family: 'Fira Code', 'Consolas', monospace;
}

.content :deep(.copy-btn) {
  font-size: 0.75em;
  color: #8b949e;
  background: transparent;
  border: 1px solid #30363d;
  border-radius: 4px;
  padding: 2px 8px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.content :deep(.copy-btn:hover) {
  color: #e6edf3;
  border-color: #8b949e;
}

.content :deep(code) {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.9em;
}

.content :deep(p code) {
  background: rgba(110, 118, 129, 0.4);
  padding: 2px 6px;
  border-radius: 4px;
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

@media (max-width: 768px) {
  .message {
    gap: 8px;
    margin-bottom: 12px;
  }

  .avatar {
    width: 28px;
    height: 28px;
    font-size: 10px;
  }

  .bubble {
    max-width: 90%;
    padding: 0 10px;
  }

  .content :deep(pre code) {
    padding: 12px;
    font-size: 12px;
  }
}
</style>
