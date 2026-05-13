<template>
  <div class="system-config">
    <h2>系统设置</h2>

    <el-tabs v-model="activeTab">
      <!-- Backup -->
      <el-tab-pane label="数据备份" name="backup">
        <div class="section-header">
          <span>备份列表</span>
          <el-button type="primary" @click="doCreateBackup" :loading="backupLoading">
            <el-icon><Plus /></el-icon> 创建备份
          </el-button>
        </div>
        <el-table :data="backups" style="width:100%; margin-top:12px">
          <el-table-column label="文件名" prop="name" />
          <el-table-column label="大小" width="120">
            <template #default="{ row }">{{ formatSize(row.size) }}</template>
          </el-table-column>
          <el-table-column label="时间" width="200" prop="time" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="doRestore(row.name)">恢复</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!backupLoading && backups.length === 0" description="暂无备份" />
      </el-tab-pane>

      <!-- Operation Logs -->
      <el-tab-pane label="操作日志" name="logs">
        <div class="section-header">
          <span>操作记录</span>
          <el-button type="danger" plain @click="doCleanLogs">清理90天前日志</el-button>
        </div>
        <el-table :data="logs" v-loading="logLoading" style="width:100%; margin-top:12px" max-height="500">
          <el-table-column label="用户" width="100" prop="username" />
          <el-table-column label="模块" width="80" prop="module" />
          <el-table-column label="操作" prop="operation" min-width="150" />
          <el-table-column label="IP" width="120" prop="ip" />
          <el-table-column label="耗时" width="80">
            <template #default="{ row }">{{ row.executionTime }}ms</template>
          </el-table-column>
          <el-table-column label="结果" width="60">
            <template #default="{ row }">
              <el-tag :type="row.result === 1 ? 'success' : 'danger'" size="small">
                {{ row.result === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="170" prop="createdAt" />
        </el-table>
      </el-tab-pane>

      <!-- AI Config -->
      <el-tab-pane label="全局 AI 配置" name="ai">
        <div class="section-header">
          <span>AI 配置列表</span>
          <el-button type="primary" @click="openAiDialog()">
            <el-icon><Plus /></el-icon> 添加配置
          </el-button>
        </div>
        <el-table :data="aiConfigs" style="width:100%; margin-top:12px">
          <el-table-column label="类型" width="80" prop="apiType" />
          <el-table-column label="模型" prop="modelName" />
          <el-table-column label="Base URL" prop="apiBaseUrl" min-width="200" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.tested === 1 ? 'success' : 'warning'" size="small">{{ row.tested === 1 ? '已测试' : '未测试' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" text @click="testAiConnection(row.id)">测试</el-button>
              <el-button size="small" text @click="openAiDialog(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="deleteAiConfig(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-dialog v-model="showAiDialog" :title="editingAi ? '编辑 AI 配置' : '添加 AI 配置'" width="460px">
          <el-form label-width="90px">
            <el-form-item label="API 类型">
              <el-select v-model="aiForm.apiType" style="width:100%">
                <el-option label="OpenAI" value="OPENAI" />
                <el-option label="通义千问" value="QWEN" />
                <el-option label="智谱GLM" value="GLM" />
                <el-option label="Claude" value="CLAUDE" />
                <el-option label="本地/自定义" value="LOCAL" />
              </el-select>
            </el-form-item>
            <el-form-item label="API Key">
              <el-input v-model="aiForm.apiKey" type="password" show-password placeholder="请输入 API Key" />
            </el-form-item>
            <el-form-item label="Base URL">
              <el-input v-model="aiForm.apiBaseUrl" placeholder="可选，留空使用默认地址" />
            </el-form-item>
            <el-form-item label="模型名称">
              <el-input v-model="aiForm.modelName" placeholder="如 gpt-3.5-turbo" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showAiDialog = false">取消</el-button>
            <el-button type="primary" @click="saveAiConfig">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBackupList, createBackup, restoreBackup, getOperationLogs, cleanLogs } from '@/api/statistics'
import type { AiConfig } from '@/api/ai'
import { getAiConfigs, createSystemAiConfig, updateAiConfig, deleteAiConfig, testAiConnection as testAi } from '@/api/ai'

const activeTab = ref('backup')
const backups = ref<any[]>([])
const backupLoading = ref(false)
const logs = ref<any[]>([])
const logLoading = ref(false)

onMounted(async () => {
  await loadBackups()
})

async function loadBackups() {
  backupLoading.value = true
  try {
    const res = await getBackupList()
    backups.value = res.data || []
  } catch { backups.value = [] }
  finally { backupLoading.value = false }
}

async function doCreateBackup() {
  backupLoading.value = true
  try {
    await createBackup()
    ElMessage.success('备份已创建')
    loadBackups()
  } catch { ElMessage.error('备份失败') }
}

async function doRestore(name: string) {
  try {
    await ElMessageBox.confirm(`确定从 "${name}" 恢复？`, '确认恢复', { type: 'warning' })
    await restoreBackup(name)
    ElMessage.success('恢复已启动')
  } catch { /* cancelled */ }
}

async function loadLogs() {
  logLoading.value = true
  try {
    const res = await getOperationLogs(1, 50)
    logs.value = (res.data as any).records || []
  } catch { logs.value = [] }
  finally { logLoading.value = false }
}

async function doCleanLogs() {
  try {
    await cleanLogs(90)
    ElMessage.success('已清理')
    loadLogs()
  } catch { ElMessage.error('清理失败') }
}

function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

// AI config
const aiConfigs = ref<AiConfig[]>([])
const showAiDialog = ref(false)
const editingAi = ref<AiConfig | null>(null)
const aiForm = reactive({ apiType: 'OPENAI', apiKey: '', apiBaseUrl: '', modelName: 'gpt-3.5-turbo' })

async function loadAiConfigs() {
  try { const res = await getAiConfigs(); aiConfigs.value = res.data || [] } catch { /* */ }
}

function openAiDialog(config?: AiConfig) {
  if (config) {
    editingAi.value = config
    aiForm.apiType = config.apiType
    aiForm.apiKey = ''
    aiForm.apiBaseUrl = config.apiBaseUrl || ''
    aiForm.modelName = config.modelName || ''
  } else {
    editingAi.value = null
    aiForm.apiType = 'OPENAI'; aiForm.apiKey = ''; aiForm.apiBaseUrl = ''; aiForm.modelName = 'gpt-3.5-turbo'
  }
  showAiDialog.value = true
}

async function saveAiConfig() {
  try {
    if (editingAi.value) {
      await updateAiConfig(editingAi.value.id, {
        apiType: aiForm.apiType,
        ...(aiForm.apiKey ? { apiKey: aiForm.apiKey } : {}),
        apiBaseUrl: aiForm.apiBaseUrl,
        modelName: aiForm.modelName,
      })
      ElMessage.success('已更新')
    } else {
      await createSystemAiConfig({
        apiType: aiForm.apiType, apiKey: aiForm.apiKey,
        apiBaseUrl: aiForm.apiBaseUrl, modelName: aiForm.modelName,
        systemConfig: true,
      })
      ElMessage.success('已创建')
    }
    showAiDialog.value = false
    loadAiConfigs()
  } catch { ElMessage.error('操作失败') }
}

async function testAiConnection(id: number) {
  try {
    const res = await testAi(id)
    if ((res.data as any).success) ElMessage.success('连接测试成功')
    else ElMessage.error('连接测试失败: ' + ((res.data as any).error || ''))
    loadAiConfigs()
  } catch { ElMessage.error('测试请求失败') }
}

async function delAiConfig(id: number) {
  try { await deleteAiConfig(id); ElMessage.success('已删除'); loadAiConfigs() } catch { ElMessage.error('删除失败') }
}

watch(activeTab, (tab) => {
  if (tab === 'logs') loadLogs()
  if (tab === 'ai') loadAiConfigs()
})
</script>

<style scoped>
.section-header { display: flex; justify-content: space-between; align-items: center; font-weight: 500; }
</style>
