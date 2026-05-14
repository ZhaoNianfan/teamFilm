<template>
  <el-container class="app-layout">
    <el-header class="app-header">
      <div class="header-left">
        <span class="app-title">TeamFiles</span>
      </div>
      <div class="header-right">
        <el-dropdown trigger="click">
          <span class="user-info">
            <el-avatar :size="32" :src="userStore.userInfo?.avatar" :style="{backgroundColor: avatarColor}">{{ avatarIcon }}</el-avatar>
            <span class="username">{{ userStore.userInfo?.nickname }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/profile')">个人信息</el-dropdown-item>
              <el-dropdown-item @click="$router.push('/ai/chat')">AI 对话</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-container>
      <el-aside class="app-sidebar" :width="sidebarWidth">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapsed"
          router
          @select="handleMenuSelect"
        >
          <el-menu-item index="/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <span>首页概览</span>
          </el-menu-item>
          <el-sub-menu index="/files-group">
            <template #title>
              <el-icon><Folder /></el-icon>
              <span>文件管理</span>
            </template>
            <el-menu-item index="/files">我的文件</el-menu-item>
            <el-menu-item v-if="userStore.hasRole('ADMIN') || userStore.hasRole('FORMAL')" index="/files/team">
              团队文件
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/tags">
            <el-icon><CollectionTag /></el-icon>
            <span>标签管理</span>
          </el-menu-item>
          <el-menu-item index="/search">
            <el-icon><Search /></el-icon>
            <span>搜索</span>
          </el-menu-item>
          <el-menu-item index="/recycle">
            <el-icon><Delete /></el-icon>
            <span>回收站</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.hasRole('ADMIN')" index="/admin/users">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.hasRole('ADMIN')" index="/admin/system">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapsed = ref(false)
const sidebarWidth = computed(() => (isCollapsed.value ? '64px' : '220px'))
const activeMenu = computed(() => route.path)
const avatarColor = computed(() => {
  const role = userStore.userInfo?.role
  if (role === 'ADMIN') return '#f56c6c'
  if (role === 'FORMAL') return '#409eff'
  return '#909399'
})
const avatarIcon = computed(() => {
  if (userStore.userInfo?.avatar) return ''
  const role = userStore.userInfo?.role
  if (role === 'ADMIN') return '👑'
  if (role === 'FORMAL') return '👤'
  return '👥'
})
onMounted(async () => {
  if (userStore.token) {
    await userStore.fetchUserInfo()
  }
})

function handleMenuSelect() {}

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-layout { height: 100vh; }
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #409eff;
  color: #fff;
  padding: 0 20px;
}
.app-title { font-size: 20px; font-weight: bold; }
.search-input { width: 300px; }
.header-right { display: flex; align-items: center; cursor: pointer; }
.user-info { display: flex; align-items: center; gap: 8px; }
.username { color: #fff; }
.app-sidebar { border-right: 1px solid #e6e6e6; overflow-y: auto; }
.app-main { background: #f5f7fa; padding: 20px; }
</style>
