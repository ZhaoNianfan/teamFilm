<template>
  <div class="recycle-page">
    <div class="page-header">
      <h2>回收站</h2>
      <div class="header-actions">
        <el-radio-group v-model="spaceFilter" size="small" @change="loadData">
          <el-radio-button value="PERSONAL">我的删除</el-radio-button>
          <el-radio-button value="TEAM">团队删除</el-radio-button>
        </el-radio-group>
        <el-button type="danger" plain @click="handleClear" :disabled="items.length === 0">
          <el-icon><Delete /></el-icon>
          清空
        </el-button>
      </div>
    </div>

    <el-divider />

    <!-- Batch actions -->
    <div class="toolbar" v-if="selectedIds.length > 0">
      <span class="selected-info">已选 {{ selectedIds.length }} 项</span>
      <el-button size="small" type="danger" @click="handleBatchDelete">永久删除</el-button>
      <el-button size="small" @click="handleBatchRestore">恢复</el-button>
    </div>

    <el-table
      :data="items"
      v-loading="loading"
      style="width: 100%"
      @selection-change="onSelectChange"
    >
      <el-table-column type="selection" width="40" />
      <el-table-column label="名称" min-width="200">
        <template #default="{ row }">
          <div class="file-name">
            <el-icon :color="row.originalType === 'FOLDER' ? '#e6a23c' : '#409eff'">
              <Folder v-if="row.originalType === 'FOLDER'" />
              <Document v-else />
            </el-icon>
            <span>{{ row.fileName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="80" prop="originalType" />
      <el-table-column label="大小" width="100" prop="displaySize" />
      <el-table-column label="空间" width="100">
        <template #default="{ row }">
          <el-tag :type="row.storageSpace === 'TEAM' ? 'warning' : ''" size="small">
            {{ row.storageSpace === 'PERSONAL' ? '个人' : '团队' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="删除者" width="120" prop="deletedByName" />
      <el-table-column label="删除时间" width="170" prop="deletedAt" />
      <el-table-column label="剩余" width="100">
        <template #default="{ row }">
          <span :class="{ expiring: row.remainingDays <= 3 }">
            {{ row.remainingDays }}天
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="handleRestore(row)">恢复</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && items.length === 0" description="回收站为空" />

    <div class="pagination-wrapper" v-if="total > 20">
      <el-pagination
        :total="total"
        :page-size="20"
        layout="total, prev, pager, next"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { RecycleItem } from '@/api/recycle'
import {
  getRecycleList,
  restoreRecycleItem,
  deleteRecycleItem,
  batchDeleteRecycle,
  clearRecycle,
} from '@/api/recycle'

const route = useRoute()

const items = ref<RecycleItem[]>([])
const total = ref(0)
const loading = ref(false)
const selectedIds = ref<number[]>([])
const spaceFilter = ref('PERSONAL')
const currentPage = ref(1)

onMounted(() => {
  if (route.path.includes('team')) spaceFilter.value = 'TEAM'
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getRecycleList({
      storageSpace: spaceFilter.value,
      page: currentPage.value,
      size: 20,
    })
    items.value = res.data.records
    total.value = res.data.total
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

function onSelectChange(rows: RecycleItem[]) {
  selectedIds.value = rows.map((r) => r.id)
}

function onPageChange(page: number) {
  currentPage.value = page
  loadData()
}

async function handleRestore(row: RecycleItem) {
  try {
    await restoreRecycleItem(row.id)
    ElMessage.success('已恢复')
    loadData()
  } catch {
    ElMessage.error('恢复失败')
  }
}

async function handleDelete(row: RecycleItem) {
  try {
    await ElMessageBox.confirm(
      `永久删除 "${row.fileName}"？此操作不可撤销。`,
      '确认永久删除',
      { type: 'error', confirmButtonText: '永久删除' }
    )
    await deleteRecycleItem(row.id)
    ElMessage.success('已永久删除')
    loadData()
  } catch { /* cancelled */ }
}

async function handleBatchRestore() {
  for (const id of selectedIds.value) {
    await restoreRecycleItem(id)
  }
  ElMessage.success('批量恢复成功')
  selectedIds.value = []
  loadData()
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定永久删除 ${selectedIds.value.length} 项？`,
      '确认批量删除',
      { type: 'error' }
    )
    await batchDeleteRecycle(selectedIds.value)
    ElMessage.success('已永久删除')
    selectedIds.value = []
    loadData()
  } catch { /* cancelled */ }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm(
      `确定清空${spaceFilter.value === 'PERSONAL' ? '个人' : '团队'}回收站？此操作不可撤销。`,
      '确认清空',
      { type: 'error', confirmButtonText: '全部清空' }
    )
    await clearRecycle(spaceFilter.value)
    ElMessage.success('回收站已清空')
    loadData()
  } catch { /* cancelled */ }
}
</script>

<style scoped>
.recycle-page { height: 100%; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-header h2 { margin: 0; }
.header-actions { display: flex; gap: 12px; align-items: center; }
.toolbar {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 16px; background: #fef0f0; border-radius: 6px; margin-bottom: 8px;
}
.selected-info { color: #f56c6c; font-size: 13px; }
.file-name { display: flex; align-items: center; gap: 8px; }
.expiring { color: #f56c6c; font-weight: bold; }
.pagination-wrapper { padding-top: 16px; display: flex; justify-content: center; }
</style>
