<template>
  <div class="admin-users">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新建用户
      </el-button>
    </div>

    <el-table :data="users" v-loading="loading" style="width:100%; margin-top:16px">
      <el-table-column label="ID" width="60" prop="id" />
      <el-table-column label="用户名" prop="username" />
      <el-table-column label="昵称" prop="nickname" />
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'FORMAL' ? 'primary' : 'info'" size="small">
            {{ roleLabel(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="存储配额" width="120">
        <template #default="{ row }">{{ formatSize(row.storageQuota) }}</template>
      </el-table-column>
      <el-table-column label="最后登录" width="160" prop="lastLoginTime" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="editUser(row)">编辑</el-button>
          <el-button size="small" text type="warning" @click="resetPwd(row)">重置密码</el-button>
          <el-button size="small" text type="danger" @click="deleteUser(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="editing ? '编辑用户' : '新建用户'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名" v-if="!editing">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" v-if="!editing">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width:100%">
            <el-option label="管理员 ADMIN" value="ADMIN" />
            <el-option label="正式用户 FORMAL" value="FORMAL" />
            <el-option label="访客 GUEST" value="GUEST" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="存储配额(MB)">
          <el-input-number v-model="form.storageQuotaMB" :min="1" :step="100" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveUser">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showResetPwd" title="重置密码" width="360px">
      <el-input v-model="resetPwdForm" placeholder="请输入新密码" type="password" show-password />
      <template #footer>
        <el-button @click="showResetPwd = false">取消</el-button>
        <el-button type="primary" @click="doResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

interface UserItem {
  id: number
  username: string
  nickname: string
  role: string
  status: number
  storageQuota: number
  lastLoginTime: string
}

const users = ref<UserItem[]>([])
const loading = ref(false)
const showDialog = ref(false)
const editing = ref(false)
const editId = ref<number | null>(null)
const showResetPwd = ref(false)
const resetPwdId = ref<number | null>(null)
const resetPwdForm = ref('')

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  role: 'FORMAL',
  status: 1,
  storageQuotaMB: 5120,
})

onMounted(() => loadUsers())

async function loadUsers() {
  loading.value = true
  try {
    const res = await request.get('/admin/users')
    users.value = (res.data as any).records || res.data || []
  } catch { users.value = [] }
  finally { loading.value = false }
}

function openCreate() {
  editing.value = false
  editId.value = null
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.role = 'FORMAL'
  form.status = 1
  form.storageQuotaMB = 5120
  showDialog.value = true
}

function editUser(user: UserItem) {
  editing.value = true
  editId.value = user.id
  form.username = user.username
  form.nickname = user.nickname || ''
  form.role = user.role
  form.status = user.status
  form.storageQuotaMB = Math.round(user.storageQuota / (1024 * 1024))
  showDialog.value = true
}

async function saveUser() {
  try {
    if (editing.value && editId.value) {
      await request.put(`/admin/users/${editId.value}`, {
        nickname: form.nickname,
        role: form.role,
        status: form.status,
        storageQuota: form.storageQuotaMB * 1024 * 1024,
      })
      ElMessage.success('已更新')
    } else {
      if (!form.username || !form.password) { ElMessage.warning('用户名和密码不能为空'); return }
      await request.post('/admin/users', {
        username: form.username,
        password: form.password,
        nickname: form.nickname,
        role: form.role,
        status: form.status,
        storageQuota: form.storageQuotaMB * 1024 * 1024,
      })
      ElMessage.success('已创建')
    }
    showDialog.value = false
    loadUsers()
  } catch { ElMessage.error('操作失败') }
}

function resetPwd(user: UserItem) {
  resetPwdId.value = user.id
  resetPwdForm.value = ''
  showResetPwd.value = true
}

async function doResetPwd() {
  if (!resetPwdForm.value) return
  try {
    await request.put(`/admin/users/${resetPwdId.value}/reset-password`, {
      password: resetPwdForm.value,
    })
    ElMessage.success('密码已重置')
    showResetPwd.value = false
  } catch { ElMessage.error('重置失败') }
}

async function deleteUser(user: UserItem) {
  try {
    await ElMessageBox.confirm(`确定删除用户 "${user.username}"？`, '确认删除', { type: 'warning' })
    await request.delete(`/admin/users/${user.id}`)
    ElMessage.success('已删除')
    loadUsers()
  } catch { /* cancelled */ }
}

function roleLabel(role: string): string {
  if (role === 'ADMIN') return '管理员'
  if (role === 'FORMAL') return '正式用户'
  return '访客'
}

function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
  return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-header h2 { margin: 0; }
</style>
