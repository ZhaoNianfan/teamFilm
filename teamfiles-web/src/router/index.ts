import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginPage.vue'),
    meta: { public: true },
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('@/views/login/ChangePasswordPage.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardPage.vue'),
        meta: { title: '首页概览' },
      },
      {
        path: 'files',
        name: 'Files',
        component: () => import('@/views/files/FileListPage.vue'),
        meta: { title: '我的文件' },
      },
      {
        path: 'files/team',
        name: 'TeamFiles',
        component: () => import('@/views/files/FileListPage.vue'),
        meta: { title: '团队文件', role: 'FORMAL' },
      },
      {
        path: 'files/:id/preview',
        name: 'FilePreview',
        component: () => import('@/views/preview/FilePreviewPage.vue'),
        meta: { title: '文件预览' },
      },
      {
        path: 'tags',
        name: 'Tags',
        component: () => import('@/views/tags/TagManagePage.vue'),
        meta: { title: '标签管理' },
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/search/SearchResultPage.vue'),
        meta: { title: '搜索' },
      },
      {
        path: 'recycle',
        name: 'Recycle',
        component: () => import('@/views/recycle/RecycleBinPage.vue'),
        meta: { title: '回收站' },
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUsersPage.vue'),
        meta: { title: '用户管理', role: 'ADMIN' },
      },
      {
        path: 'admin/system',
        name: 'SystemConfig',
        component: () => import('@/views/system/SystemConfigPage.vue'),
        meta: { title: '系统设置', role: 'ADMIN' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/ProfilePage.vue'),
        meta: { title: '个人信息' },
      },
      {
        path: 'ai/chat',
        name: 'AiChat',
        component: () => import('@/views/ai/AiChatPage.vue'),
        meta: { title: 'AI 对话' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.public) {
    next()
    return
  }

  const savedToken = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!userStore.token && !savedToken) {
    next('/login')
    return
  }

  if (to.meta.role) {
    const required = to.meta.role as string
    // ADMIN has access to everything
    if (!userStore.hasRole(required) && !userStore.hasRole('ADMIN')) {
      next('/dashboard')
      return
    }
  }

  next()
})

export default router
