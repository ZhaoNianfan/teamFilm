<template>
  <div class="file-list-page">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>{{ isTeamSpace ? '团队共享空间' : '我的文件' }}</h2>
        <el-breadcrumb separator=">">
          <el-breadcrumb-item
            :to="{ path: isTeamSpace ? '/files/team' : '/files' }"
            @click="fileStore.navigateToFolder(undefined)"
          >
            根目录
          </el-breadcrumb-item>
          <el-breadcrumb-item
            v-for="(item, idx) in fileStore.breadcrumb"
            :key="item.id"
          >
            <a @click.prevent="fileStore.navigateToBreadcrumb(idx)">{{ item.name }}</a>
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="header-right">
        <el-button-group>
          <el-button
            :type="fileStore.viewMode === 'list' ? 'primary' : ''"
            size="small"
            @click="fileStore.viewMode = 'list'"
          >
            <el-icon><List /></el-icon>
          </el-button>
          <el-button
            :type="fileStore.viewMode === 'grid' ? 'primary' : ''"
            size="small"
            @click="fileStore.viewMode = 'grid'"
          >
            <el-icon><Grid /></el-icon>
          </el-button>
        </el-button-group>
      </div>
    </div>

    <el-divider />

    <el-container>
      <!-- Left: Folder tree -->
      <el-aside width="260px" class="folder-aside">
        <FolderTree ref="folderTreeRef" />
      </el-aside>

      <!-- Right: File content -->
      <el-main class="file-main">
        <!-- Upload area -->
        <FileUpload
          :storage-space="fileStore.currentStorageSpace"
          :folder-id="fileStore.currentFolderId"
          @uploaded="refreshList"
        />

        <!-- Toolbar -->
        <div class="toolbar">
          <div class="toolbar-left">
            <el-checkbox
              v-model="selectAll"
              :indeterminate="isIndeterminate"
              @change="handleSelectAll"
            />
            <span v-if="fileStore.selectedFileIds.length > 0" class="selected-count">
              已选 {{ fileStore.selectedFileIds.length }} 项
            </span>
          </div>
          <div class="toolbar-right">
            <el-button
              v-if="fileStore.selectedFileIds.length > 0"
              size="small"
              @click="handleBatchDownload"
            >
              <el-icon><Download /></el-icon>
              下载
            </el-button>
            <el-button
              v-if="fileStore.selectedFileIds.length === 1"
              size="small"
              @click="handleMove"
            >
              <el-icon><Rank /></el-icon>
              移动
            </el-button>
            <el-button
              v-if="fileStore.selectedFileIds.length > 0"
              size="small"
              type="danger"
              @click="handleBatchDelete"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </div>

        <!-- Grid view -->
        <div v-if="fileStore.viewMode === 'grid'" class="file-grid">
          <div
            v-for="file in fileStore.fileList"
            :key="file.id"
            class="file-card"
            :class="{ selected: fileStore.selectedFiles.has(file.id) }"
            @click="onFileClick(file, $event)"
            @dblclick="previewFile(file)"
            @contextmenu.prevent="onContextMenu($event, file)"
          >
            <div class="file-icon">
              <el-icon :size="40" :color="getFileColor(file)">
                <component :is="getFileIcon(file)" />
              </el-icon>
            </div>
            <div class="file-name" :title="file.originalName">
              {{ file.originalName }}
              <el-tag v-if="file.shared" type="warning" size="small" effect="plain">已共享</el-tag>
            </div>
            <div class="file-meta">{{ file.displaySize }}</div>
          </div>
          <el-empty v-if="!fileStore.loading && fileStore.fileList.length === 0" description="暂无文件" />
        </div>

        <!-- List view -->
        <el-table
          v-else
          :data="fileStore.fileList"
          v-loading="fileStore.loading"
          style="width: 100%"
          @selection-change="onSelectionChange"
          @row-dblclick="previewFile"
          @row-contextmenu="onRowContextMenu"
        >
          <el-table-column type="selection" width="40" />
          <el-table-column label="文件名" min-width="180">
            <template #default="{ row }">
              <div class="file-name-cell">
                <el-icon :size="20" :color="getFileColor(row)"><component :is="getFileIcon(row)" /></el-icon>
                <div class="file-name-info">
                  <span>{{ row.originalName }}</span>
                  <div class="file-tags" v-if="row.tags?.length">
                    <span v-for="t in row.tags.slice(0,3)" :key="t.id" class="mini-tag" :style="{borderColor:t.color,color:t.color}">{{ t.tagName }}</span>
                    <span v-if="row.tags.length > 3" style="font-size:11px;color:#909399">+{{ row.tags.length - 3 }}</span>
                  </div>
                </div>
                <el-tag v-if="row.shared" type="warning" size="small" effect="plain">共享</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="大小" width="90" prop="displaySize" />
          <el-table-column label="日期" width="150" prop="createdAt" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="previewFile(row)">预览</el-button>
              <el-button size="small" text type="primary" @click="downloadFile(row)">下载</el-button>
              <el-button size="small" text type="warning" @click="showTagDialog(row)">
                <el-icon><CollectionTag /></el-icon>
              </el-button>
              <el-dropdown trigger="click">
                <el-button size="small" text>
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="showRenameDialog(row)">
                      <el-icon><Edit /></el-icon> 重命名
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleCopy(row)">
                      <el-icon><CopyDocument /></el-icon> 复制
                    </el-dropdown-item>
                    <el-dropdown-item @click="showMoveDialog(row)">
                      <el-icon><Rank /></el-icon> 移动
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-if="row.storageSpace === 'PERSONAL'"
                      @click="handleMoveToTeam(row)"
                    >
                      <el-icon><Promotion /></el-icon> 移至团队空间
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="handleDelete(row)">
                      <el-icon color="#f56c6c"><Delete /></el-icon>
                      <span style="color:#f56c6c">删除</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="pagination-wrapper" v-if="fileStore.total > 50">
          <el-pagination
            :total="fileStore.total"
            :page-size="50"
            layout="total, prev, pager, next"
            @current-change="onPageChange"
          />
        </div>
      </el-main>
    </el-container>

    <!-- Context menu -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ top: contextMenu.top + 'px', left: contextMenu.left + 'px' }"
    >
      <div class="menu-item" @click="previewFile(contextMenu.file)">预览</div>
      <div class="menu-item" @click="downloadFile(contextMenu.file)">下载</div>
      <div class="menu-item" @click="showRenameDialog(contextMenu.file)">重命名</div>
      <div class="menu-item" @click="handleCopy(contextMenu.file!)">复制</div>
      <div class="menu-item" @click="showMoveDialog(contextMenu.file)">移动</div>
      <div v-if="contextMenu.file?.storageSpace === 'PERSONAL'" class="menu-item" @click="handleMoveToTeam(contextMenu.file!)">移至团队空间</div>
      <div class="menu-item danger" @click="handleDelete(contextMenu.file)">删除</div>
    </div>

    <!-- Rename dialog -->
    <el-dialog v-model="showRenameDialogVisible" title="重命名" width="400px">
      <el-input v-model="renameValue" placeholder="请输入新名称" @keyup.enter="handleRenameSubmit" />
      <template #footer>
        <el-button @click="showRenameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRenameSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Move dialog -->
    <el-dialog v-model="showMoveDialogVisible" title="移动到文件夹" width="400px">
      <el-tree
        :data="folderTreeForSelect"
        :props="{ children: 'children', label: 'folderName' }"
        node-key="id"
        highlight-current
        @node-click="onMoveTargetSelect"
      />
      <template #footer>
        <el-button @click="showMoveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMoveSubmit" :disabled="!moveTargetId && moveTargetId !== 0">
          移动
        </el-button>
      </template>
    </el-dialog>

    <!-- Tag dialog -->
    <el-dialog v-model="showTagDialogVisible" title="编辑标签" width="620px" top="2vh">
      <div style="min-height:240px;padding-bottom:20px">
        <TagInput v-model="tagEditIds" />
      </div>
      <template #footer>
        <el-button @click="showTagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTags">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FileItem } from '@/api/file'
import {
  renameFile,
  deleteFile,
  copyFile,
  moveFile as moveFileApi,
  moveToTeam,
  getDownloadUrl,
  getPreviewUrl,
  batchDownload,
} from '@/api/file'
import { getFolderTree } from '@/api/folder'
import type { FolderItem } from '@/api/folder'
import { useFileStore } from '@/stores/file'
import FileUpload from '@/components/file/FileUpload.vue'
import FolderTree from '@/components/folder/FolderTree.vue'
import TagInput from '@/components/tag/TagInput.vue'
import { addTagsToFile } from '@/api/tag'

const route = useRoute()
const router = useRouter()
const fileStore = useFileStore()

const isTeamSpace = computed(() => route.path.includes('team'))

// Selection
const selectAll = ref(false)
const isIndeterminate = ref(false)

function onSelectionChange(rows: FileItem[]) {
  fileStore.selectAll(rows.map((r) => r.id))
  selectAll.value = rows.length === fileStore.fileList.length
  isIndeterminate.value = rows.length > 0 && rows.length < fileStore.fileList.length
}

function handleSelectAll(val: boolean) {
  if (val) {
    fileStore.selectAll(fileStore.fileList.map((f) => f.id))
  } else {
    fileStore.clearSelection()
  }
  isIndeterminate.value = false
}

// Dialogs
const showRenameDialogVisible = ref(false)
const renameValue = ref('')
const renameTarget = ref<FileItem | null>(null)

const showMoveDialogVisible = ref(false)
const showTagDialogVisible = ref(false)
const tagEditIds = ref<number[]>([])
const tagEditFileId = ref<number | null>(null)
const moveTargetId = ref<number | null>(null)
const moveFile = ref<FileItem | null>(null)
const folderTreeForSelect = ref<FolderItem[]>([])

// Context menu
const contextMenu = ref<{
  visible: boolean
  top: number
  left: number
  file: FileItem | null
}>({ visible: false, top: 0, left: 0, file: null })

onMounted(async () => {
  await loadData()
})

watch(
  () => route.path,
  async () => {
    const space = isTeamSpace.value ? 'TEAM' : 'PERSONAL'
    fileStore.switchSpace(space)
  }
)

async function loadData() {
  const space = isTeamSpace.value ? 'TEAM' : 'PERSONAL'
  fileStore.currentStorageSpace = space
  await Promise.all([fileStore.fetchFiles(), fileStore.fetchFolderTree(space)])
}

function refreshList() {
  fileStore.fetchFiles()
  fileStore.fetchFolderTree(fileStore.currentStorageSpace)
}

// File actions
function onFileClick(file: FileItem, event: MouseEvent) {
  if (event.ctrlKey || event.metaKey) {
    fileStore.toggleSelect(file.id)
  }
}

function previewFile(file: FileItem | null) {
  if (!file) return
  if (file.fileType === 'IMAGE' || file.fileType === 'DOCUMENT') {
    window.open(getPreviewUrl(file.id), '_blank')
  } else {
    downloadFile(file)
  }
}

function downloadFile(file: FileItem | null) {
  if (!file) return
  const a = document.createElement('a')
  a.href = getDownloadUrl(file.id)
  a.download = file.originalName
  a.click()
}

function handleBatchDownload() {
  if (fileStore.selectedFileIds.length === 0) return
  batchDownload(fileStore.selectedFileIds)
}

function showRenameDialog(file: FileItem | null) {
  if (!file) return
  renameTarget.value = file
  renameValue.value = file.originalName
  showRenameDialogVisible.value = true
  closeContextMenu()
}

async function handleRenameSubmit() {
  if (!renameTarget.value || !renameValue.value.trim()) return
  try {
    await renameFile(renameTarget.value.id, renameValue.value.trim())
    ElMessage.success('重命名成功')
    showRenameDialogVisible.value = false
    refreshList()
  } catch {
    ElMessage.error('重命名失败')
  }
}

async function handleCopy(file: FileItem) {
  if (!file) return
  try {
    await copyFile(file.id, fileStore.currentFolderId ?? 0)
    ElMessage.success('复制成功')
    refreshList()
  } catch {
    ElMessage.error('复制失败')
  }
}

async function showMoveDialog(file: FileItem | null) {
  if (!file) return
  moveFile.value = file
  moveTargetId.value = null
  try {
    const res = await getFolderTree(fileStore.currentStorageSpace)
    folderTreeForSelect.value = res.data
  } catch {
    folderTreeForSelect.value = []
  }
  showMoveDialogVisible.value = true
  closeContextMenu()
}

function onMoveTargetSelect(data: FolderItem) {
  moveTargetId.value = data.id
}

async function handleMoveSubmit() {
  if (!moveFile.value) return
  try {
    const fid = moveTargetId.value ?? 0
    await moveFileApi(moveFile.value.id, fid)
    ElMessage.success('移动成功')
    showMoveDialogVisible.value = false
    refreshList()
  } catch (e: any) {
    ElMessage.error('移动失败: ' + (e?.message || '未知错误'))
  }
}

async function handleMoveToTeam(file: FileItem) {
  try {
    await moveToTeam(file.id)
    ElMessage.success('已移至团队空间')
    refreshList()
  } catch {
    ElMessage.error('移动至团队空间失败')
  }
}

function handleMove() {
  const id = fileStore.selectedFileIds[0]
  const file = fileStore.fileList.find((f) => f.id === id)
  if (file) showMoveDialog(file)
}

async function handleDelete(file: FileItem | null) {
  if (!file) return
  closeContextMenu()
  try {
    await ElMessageBox.confirm(`确定删除 "${file.originalName}"？`, '确认删除', { type: 'warning' })
    await deleteFile(file.id)
    ElMessage.success('已删除')
    refreshList()
  } catch {
    // cancelled
  }
}

async function handleBatchDelete() {
  if (fileStore.selectedFileIds.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定删除 ${fileStore.selectedFileIds.length} 个文件？`,
      '确认批量删除',
      { type: 'warning' }
    )
    for (const id of fileStore.selectedFileIds) {
      await deleteFile(id)
    }
    ElMessage.success('已删除')
    fileStore.clearSelection()
    selectAll.value = false
    refreshList()
  } catch {
    // cancelled
  }
}

function onContextMenu(event: MouseEvent, file: FileItem) {
  contextMenu.value = {
    visible: true,
    top: event.clientY,
    left: event.clientX,
    file,
  }
  document.addEventListener('click', closeContextMenu, { once: true })
}

function onRowContextMenu(row: FileItem, _column: any, event: MouseEvent) {
  onContextMenu(event, row)
}

function closeContextMenu() {
  contextMenu.value.visible = false
}

function showTagDialog(file: FileItem) {
  tagEditFileId.value = file.id
  tagEditIds.value = []
  showTagDialogVisible.value = true
}

async function saveTags() {
  if (tagEditFileId.value == null) return
  try {
    await addTagsToFile(tagEditFileId.value, tagEditIds.value)
    ElMessage.success('标签已更新')
    showTagDialogVisible.value = false
    refreshList()
  } catch { ElMessage.error('标签更新失败') }
}

function onPageChange(page: number) {
  fileStore.fetchFiles({ page })
}

function getFileIcon(file: FileItem) {
  if (file.fileType === 'IMAGE') return 'PictureFilled'
  if (file.fileType === 'DOCUMENT') return 'Document'
  return 'Document'
}

function getFileColor(file: FileItem) {
  if (file.fileType === 'IMAGE') return '#67c23a'
  if (file.fileType === 'DOCUMENT') return '#409eff'
  return '#909399'
}
</script>

<style scoped>
.file-list-page { height: 100%; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.page-header h2 { margin: 0; font-size: 20px; }
.header-left { display: flex; flex-direction: column; gap: 8px; }
.folder-aside {
  border-right: 1px solid #e6e6e6;
  min-height: calc(100vh - 200px);
  overflow-y: auto;
}
.file-main { padding: 0 0 0 16px; }
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  min-height: 36px;
}
.toolbar-left { display: flex; align-items: center; gap: 8px; }
.selected-count { color: #409eff; font-size: 13px; }
.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
  padding: 8px 0;
}
.file-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}
.file-card:hover { border-color: #409eff; box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15); }
.file-card.selected { border-color: #409eff; background: #ecf5ff; }
.file-icon { margin-bottom: 8px; }
.file-name {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-meta { font-size: 12px; color: #909399; margin-top: 4px; }
.file-name-cell { display: flex; align-items: center; gap: 8px; }
.file-name-info { flex:1; min-width:0; }
.file-tags { display:flex; flex-wrap:wrap; gap:2px; margin-top:2px; }
.mini-tag { display:inline-block; border:1px solid; border-radius:8px; padding:0 5px; font-size:11px; background:#fff; }
.pagination-wrapper { padding: 16px 0; display: flex; justify-content: center; }
.context-menu {
  position: fixed;
  z-index: 9999;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  min-width: 140px;
}
.menu-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 13px;
}
.menu-item:hover { background: #f5f7fa; }
.menu-item.danger { color: #f56c6c; }
.menu-item.danger:hover { background: #fef0f0; }
</style>
