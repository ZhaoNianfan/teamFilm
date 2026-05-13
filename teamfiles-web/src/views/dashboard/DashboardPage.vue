<template>
  <div class="dashboard">
    <h2>首页概览</h2>

    <!-- Stat cards -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">存储空间</div>
          <div class="stat-value">{{ formatSize(data.storage.used) }} / {{ formatSize(data.storage.quota) }}</div>
          <el-progress :percentage="data.storage.percentage" :color="progressColor" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">文件总数</div>
          <div class="stat-value">{{ data.totalFiles }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">文件夹</div>
          <div class="stat-value">{{ data.totalFolders }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">今日上传</div>
          <div class="stat-value">{{ data.todayUploads }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <!-- File type chart -->
      <el-col :span="10">
        <el-card>
          <template #header>文件类型分布</template>
          <v-chart v-if="chartOption" :option="chartOption" style="height:280px" autoresize />
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
      <!-- Hot tags -->
      <el-col :span="7">
        <el-card>
          <template #header>常用标签</template>
          <div v-if="data.hotTags.length > 0" class="tag-cloud">
            <span
              v-for="tag in data.hotTags"
              :key="tag.id"
              class="tag-item"
              :style="{ color: tag.color, fontSize: Math.min(12 + tag.count * 2, 28) + 'px' }"
            >
              {{ tag.name }}
            </span>
          </div>
          <el-empty v-else description="暂无标签" />
        </el-card>
      </el-col>
      <!-- Recent files -->
      <el-col :span="7">
        <el-card>
          <template #header>最近文件</template>
          <div v-if="data.recentFiles.length > 0" class="recent-list">
            <div v-for="f in data.recentFiles" :key="f.id" class="recent-item" @click="openFile(f.id)">
              <el-icon :color="f.type === 'IMAGE' ? '#67c23a' : '#409eff'">
                <PictureFilled v-if="f.type === 'IMAGE'" />
                <Document v-else />
              </el-icon>
              <span class="recent-name">{{ f.name }}</span>
              <span class="recent-time">{{ f.createdAt }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无文件" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboard } from '@/api/statistics'
import type { DashboardData } from '@/api/statistics'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([PieChart, TitleComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const router = useRouter()

const data = ref<DashboardData>({
  storage: { used: 0, quota: 5 * 1024 * 1024 * 1024, percentage: 0 },
  totalFiles: 0,
  totalFolders: 0,
  fileTypeDistribution: [],
  recentFiles: [],
  hotTags: [],
  todayUploads: 0,
})

const progressColor = computed(() => {
  const p = data.value.storage.percentage
  if (p > 90) return '#f56c6c'
  if (p > 70) return '#e6a23c'
  return '#409eff'
})

const chartOption = computed(() => {
  const items = data.value.fileTypeDistribution
  if (!items || items.length === 0) return null
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['50%', '45%'],
      data: items.map(i => ({ name: typeLabel(i.type), value: i.count })),
      label: { show: true, formatter: '{b}\n{d}%' },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' } },
    }],
  }
})

function typeLabel(t: string): string {
  if (t === 'IMAGE') return '图片'
  if (t === 'DOCUMENT') return '文档'
  return '其他'
}

function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

function openFile(id: number) {
  window.open(`/files/${id}/preview`, '_blank')
}

onMounted(async () => {
  try {
    const res = await getDashboard()
    data.value = res.data
  } catch { /* ignore */ }
})
</script>

<style scoped>
.stat-row { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: bold; color: #303133; margin-bottom: 8px; }
.tag-cloud { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; min-height: 60px; }
.tag-item { cursor: pointer; font-weight: 500; }
.tag-item:hover { opacity: 0.7; }
.recent-list { display: flex; flex-direction: column; gap: 6px; max-height: 260px; overflow-y: auto; }
.recent-item {
  display: flex; align-items: center; gap: 8px; padding: 6px 4px;
  border-radius: 4px; cursor: pointer; font-size: 13px;
}
.recent-item:hover { background: #f5f7fa; }
.recent-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.recent-time { font-size: 12px; color: #909399; white-space: nowrap; }
</style>
