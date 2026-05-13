<template>
  <div class="profile-page">
    <h2>个人信息</h2>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>基本信息</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ userInfo?.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ userInfo?.nickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ roleLabel(userInfo?.role) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="userInfo?.status === 1 ? 'success' : 'danger'" size="small">
                {{ userInfo?.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="存储配额">{{ formatSize(userInfo?.storageQuota) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>修改密码</template>
          <el-form :model="pwdForm" label-width="100px">
            <el-form-item label="旧密码">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changePassword" :loading="pwdLoading">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changeFirstPassword } from '@/api/auth'
import request from '@/api/request'

const userStore = useUserStore()
const userInfo = ref(userStore.userInfo)

const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdLoading = ref(false)

async function changePassword() {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    ElMessage.warning('请填写密码')
    return
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  pwdLoading.value = true
  try {
    await request.put('/auth/password', {
      username: userInfo.value?.username,
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.resetState()
    window.location.href = '/login'
  } catch {
    ElMessage.error('密码修改失败')
  } finally {
    pwdLoading.value = false
  }
}

function roleLabel(role?: string): string {
  if (role === 'ADMIN') return '管理员'
  if (role === 'FORMAL') return '正式用户'
  return '访客'
}

function formatSize(bytes?: number): string {
  if (!bytes) return '0 B'
  return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

onMounted(async () => {
  if (!userInfo.value) {
    await userStore.fetchUserInfo()
    userInfo.value = userStore.userInfo
  }
})
</script>
