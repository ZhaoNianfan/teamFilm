<template>
  <div class="file-upload">
    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      :action="uploadUrl"
      :headers="uploadHeaders"
      :data="uploadData"
      :multiple="true"
      :limit="20"
      :before-upload="beforeUpload"
      :on-success="onSuccess"
      :on-error="onError"
      :on-progress="onProgress"
      :show-file-list="true"
      drag
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">
        拖拽文件到此处或 <em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          单个文件最大 5GB，批量最多 20 个文件
        </div>
      </template>
    </el-upload>

    <!-- Tag selector for uploaded files -->
    <div class="upload-tags">
      <span class="tag-label">标签：</span>
      <TagInput v-model="tagIds" />
    </div>

    <!-- Chunk upload progress -->
    <div v-if="chunkProgress" class="chunk-progress">
      <el-progress :percentage="chunkProgress.percentage" :status="chunkProgress.status" />
      <span class="chunk-text">
        {{ chunkProgress.uploaded }} / {{ chunkProgress.total }} chunks
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadInstance, UploadProps } from 'element-plus'
import { initChunkUpload, uploadChunk, mergeChunks, cancelChunkUpload } from '@/api/file'
import { addTagsToFile } from '@/api/tag'
import { useFileStore } from '@/stores/file'
import TagInput from '@/components/tag/TagInput.vue'

const props = defineProps<{
  storageSpace: string
  folderId?: number
}>()

const emit = defineEmits<{
  uploaded: []
  cancelled: []
}>()

const fileStore = useFileStore()
const uploadRef = ref<UploadInstance>()
const fileList = ref<any[]>([])
const tagIds = ref<number[]>([])
const chunkProgress = ref<{
  uploadId: string
  total: number
  uploaded: number
  percentage: number
  status: 'success' | 'exception' | 'warning' | ''
} | null>(null)

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
}))

const uploadData = computed(() => ({
  storageSpace: props.storageSpace,
  folderId: props.folderId || '',
}))

const uploadUrl = computed(
  () => `${import.meta.env.VITE_API_BASE_URL || '/api/v1'}/files/upload`
)

const CHUNK_THRESHOLD = 5 * 1024 * 1024 // 5MB

const beforeUpload: UploadProps['beforeUpload'] = async (rawFile) => {
  const maxSize = 5 * 1024 * 1024 * 1024 // 5GB
  if (rawFile.size > maxSize) {
    ElMessage.error(`文件 ${rawFile.name} 超过 5GB 限制`)
    return false
  }

  // For large files, use chunked upload
  if (rawFile.size > CHUNK_THRESHOLD) {
    await doChunkUpload(rawFile)
    return false // Prevent default upload
  }

  return true
}

async function doChunkUpload(file: File) {
  try {
    const initRes = await initChunkUpload(
      file.name,
      file.size,
      undefined,
      props.storageSpace,
      props.folderId
    )
    const { uploadId, chunkCount, skipUpload } = initRes.data

    if (skipUpload) {
      ElMessage.success('文件已存在，跳过上传（秒传）')
      emit('uploaded')
      return
    }

    chunkProgress.value = {
      uploadId,
      total: chunkCount,
      uploaded: 0,
      percentage: 0,
      status: '',
    }

    const chunkSize = 5 * 1024 * 1024
    for (let i = 0; i < chunkCount; i++) {
      const start = i * chunkSize
      const end = Math.min(start + chunkSize, file.size)
      const chunk = file.slice(start, end)
      await uploadChunk(uploadId, i, chunk)
      chunkProgress.value.uploaded = i + 1
      chunkProgress.value.percentage = Math.round(((i + 1) / chunkCount) * 100)
    }

    const mergeRes = await mergeChunks(uploadId)
    chunkProgress.value.status = 'success'
    // Bind tags if selected
    const fileId = (mergeRes as any)?.data?.id
    if (fileId && tagIds.value.length > 0) {
      try { await addTagsToFile(fileId, tagIds.value) } catch { /* ignore */ }
    }
    ElMessage.success(`文件 ${file.name} 上传成功`)
    emit('uploaded')
  } catch (error) {
    chunkProgress.value!.status = 'exception'
    if (chunkProgress.value) {
      await cancelChunkUpload(chunkProgress.value.uploadId).catch(() => {})
    }
    ElMessage.error(`上传失败: ${file.name}`)
  } finally {
    setTimeout(() => {
      chunkProgress.value = null
    }, 2000)
  }
}

async function onSuccess(response: any, _file: any) {
  // Bind tags if selected
  const fileId = response?.data?.id
  if (fileId && tagIds.value.length > 0) {
    try { await addTagsToFile(fileId, tagIds.value) } catch { /* ignore */ }
  }
  fileStore.fetchFiles()
  emit('uploaded')
}

function onError(_error: any, _file: any) {
  ElMessage.error('上传失败')
}

function onProgress() {}
</script>

<style scoped>
.file-upload { margin-bottom: 16px; }
.chunk-progress { margin-top: 12px; display: flex; align-items: center; gap: 12px; }
.chunk-text { font-size: 13px; color: #909399; white-space: nowrap; }
</style>
