<template>
  <div class="preview-page">
    <div class="preview-toolbar">
      <el-button @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <span class="file-name">{{ file?.originalName }}</span>
      <div class="toolbar-actions">
        <el-button @click="zoomOut" :disabled="scale <= 0.2">
          <el-icon><ZoomOut /></el-icon>
        </el-button>
        <span>{{ Math.round(scale * 100) }}%</span>
        <el-button @click="zoomIn" :disabled="scale >= 5">
          <el-icon><ZoomIn /></el-icon>
        </el-button>
        <el-button @click="rotateLeft">
          <el-icon><RefreshLeft /></el-icon>
        </el-button>
        <el-button @click="rotateRight">
          <el-icon><RefreshRight /></el-icon>
        </el-button>
        <el-button @click="downloadCurrent">
          <el-icon><Download /></el-icon>
        </el-button>
      </div>
    </div>
    <div class="preview-content">
      <!-- Image preview -->
      <div v-if="isImage" class="image-preview" @wheel.prevent="onWheel">
        <img
          :src="previewUrl"
          :style="{
            transform: `scale(${scale}) rotate(${rotation}deg)`,
            transition: 'transform 0.2s',
          }"
          draggable="false"
        />
      </div>

      <!-- PDF preview -->
      <div v-else-if="isPdf" class="pdf-preview">
        <iframe :src="previewUrl" class="pdf-frame" />
      </div>

      <!-- Office preview -->
      <div v-else-if="isOffice" class="office-preview">
        <p class="preview-note">Office 文档预览需部署 KKFileView</p>
        <el-empty description="请使用外部程序打开" />
      </div>

      <!-- Text preview -->
      <div v-else-if="isText" class="text-preview">
        <pre>{{ textContent }}</pre>
      </div>

      <!-- Other file types -->
      <div v-else class="other-preview">
        <el-empty description="此文件类型暂不支持预览">
          <el-button type="primary" @click="downloadCurrent">下载文件</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import type { FileItem } from '@/api/file'
import { getFileDetail, getPreviewUrl, getDownloadUrl } from '@/api/file'

const route = useRoute()
const fileId = Number(route.params.id)

const file = ref<FileItem | null>(null)
const scale = ref(1)
const rotation = ref(0)
const textContent = ref('')

const previewUrl = computed(() => getPreviewUrl(fileId))

const isImage = computed(() => file.value?.fileType === 'IMAGE')
const isPdf = computed(() => file.value?.fileExtension === 'pdf')
const isOffice = computed(() =>
  ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(file.value?.fileExtension || '')
)
const isText = computed(() => file.value?.fileExtension === 'txt')

onMounted(async () => {
  try {
    const res = await getFileDetail(fileId)
    file.value = res.data

    if (isText.value) {
      const response = await fetch(previewUrl.value)
      textContent.value = await response.text()
    }
  } catch {
    // show error
  }
})

function zoomIn() {
  scale.value = Math.min(scale.value + 0.25, 5)
}

function zoomOut() {
  scale.value = Math.max(scale.value - 0.25, 0.2)
}

function onWheel(event: WheelEvent) {
  if (event.deltaY < 0) zoomIn()
  else zoomOut()
}

function rotateLeft() {
  rotation.value -= 90
}

function rotateRight() {
  rotation.value += 90
}

function downloadCurrent() {
  const a = document.createElement('a')
  a.href = getDownloadUrl(fileId)
  a.download = file.value?.originalName || 'file'
  a.click()
}
</script>

<style scoped>
.preview-page { height: 100vh; display: flex; flex-direction: column; background: #1a1a2e; color: #fff; }
.preview-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  background: #16213e;
  border-bottom: 1px solid #0f3460;
}
.file-name { flex: 1; text-align: center; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.toolbar-actions { display: flex; align-items: center; gap: 8px; }
.preview-content { flex: 1; overflow: auto; display: flex; align-items: center; justify-content: center; }
.image-preview { display: flex; align-items: center; justify-content: center; max-width: 100%; max-height: 100%; overflow: auto; }
.image-preview img { max-width: 90%; max-height: 90vh; object-fit: contain; }
.pdf-frame { width: 100%; height: calc(100vh - 60px); border: none; }
.office-preview, .other-preview { text-align: center; }
.preview-note { color: #909399; margin-bottom: 16px; }
.text-preview { width: 90%; max-width: 900px; max-height: 80vh; overflow: auto; background: #f5f7fa; color: #333; padding: 20px; border-radius: 8px; }
.text-preview pre { white-space: pre-wrap; word-wrap: break-word; font-family: monospace; font-size: 13px; }
</style>
