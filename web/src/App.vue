<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from './stores/auth'
import GlobalErrorToast from './components/GlobalErrorToast.vue'

const auth = useAuthStore()

onMounted(() => {
  auth.loadFromStorage()
})
</script>

<template>
  <router-view />
  <GlobalErrorToast />
</template>
