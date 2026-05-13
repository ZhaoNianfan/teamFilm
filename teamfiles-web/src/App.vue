<template>
  <router-view v-if="authReady" />
  <div v-else class="app-loading">
    <el-icon class="loading-icon" :size="32"><Loading /></el-icon>
    <p>加载中...</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const authReady = ref(false)
const userStore = useUserStore()
const router = useRouter()

onMounted(async () => {
  const token = localStorage.getItem('token')
  if (token) {
    try {
      await userStore.fetchUserInfo()
      if (!userStore.userInfo) {
        userStore.resetState()
        router.replace('/login')
        authReady.value = true
        return
      }
    } catch {
      userStore.resetState()
      router.replace('/login')
      authReady.value = true
      return
    }
  }
  // If on a non-public page with no token, redirect to login
  if (!token && window.location.pathname !== '/login' && window.location.pathname !== '/change-password') {
    router.replace('/login')
  }
  authReady.value = true
})
</script>

<style scoped>
.app-loading { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; color: #909399; }
.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
</style>
