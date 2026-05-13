<template>
  <div class="folder-tree">
    <div class="tree-header">
      <el-button
        type="primary"
        size="small"
        text
        @click="showCreateDialog = true"
      >
        <el-icon><FolderAdd /></el-icon>
        新建文件夹
      </el-button>
    </div>

    <el-tree
      :data="treeData"
      :props="treeProps"
      node-key="id"
      :expand-on-click-node="true"
      :highlight-current="true"
      :current-node-key="fileStore.currentFolderId"
      @node-click="onNodeClick"
      @node-contextmenu="onContextMenu"
    >
      <template #default="{ node, data }">
        <span class="tree-node">
          <el-icon><Folder /></el-icon>
          <span class="node-label">{{ node.label }}</span>
          <span v-if="data.fileCount" class="node-count">({{ data.fileCount }})</span>
        </span>
      </template>
    </el-tree>

    <!-- Context menu -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ top: contextMenu.top + 'px', left: contextMenu.left + 'px' }"
    >
      <div class="menu-item" @click="handleRename">重命名</div>
      <div class="menu-item danger" @click="handleDelete">删除</div>
    </div>

    <!-- Create folder dialog -->
    <el-dialog v-model="showCreateDialog" title="新建文件夹" width="400px">
      <el-input v-model="newFolderName" placeholder="请输入文件夹名称" @keyup.enter="handleCreate" />
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :disabled="!newFolderName.trim()">
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- Rename dialog -->
    <el-dialog v-model="showRenameDialog" title="重命名文件夹" width="400px">
      <el-input v-model="renameValue" placeholder="请输入新名称" @keyup.enter="handleRenameSubmit" />
      <template #footer>
        <el-button @click="showRenameDialog = false">取消</el-button>
        <el-button type="primary" @click="handleRenameSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FolderItem } from '@/api/folder'
import { createFolder, renameFolder, deleteFolder, getFolderTree } from '@/api/folder'
import { useFileStore } from '@/stores/file'

const fileStore = useFileStore()

const showCreateDialog = ref(false)
const showRenameDialog = ref(false)
const newFolderName = ref('')
const renameValue = ref('')
const contextMenuFolder = ref<FolderItem | null>(null)

const contextMenu = ref({
  visible: false,
  top: 0,
  left: 0,
})

const treeProps = {
  children: 'children',
  label: 'folderName',
}

const treeData = ref<FolderItem[]>([])

async function loadTree() {
  try {
    const res = await getFolderTree(fileStore.currentStorageSpace)
    treeData.value = res.data
  } catch {
    treeData.value = []
  }
}

watch(() => fileStore.currentStorageSpace, loadTree, { immediate: true })

function onNodeClick(data: FolderItem) {
  fileStore.navigateToFolder(data.id, data.folderName)
}

function onContextMenu(event: MouseEvent, data: FolderItem) {
  event.preventDefault()
  contextMenuFolder.value = data
  contextMenu.value = {
    visible: true,
    top: event.clientY,
    left: event.clientX,
  }
  document.addEventListener('click', closeContextMenu, { once: true })
}

function closeContextMenu() {
  contextMenu.value.visible = false
}

function handleRename() {
  closeContextMenu()
  if (contextMenuFolder.value) {
    renameValue.value = contextMenuFolder.value.folderName
    showRenameDialog.value = true
  }
}

async function handleRenameSubmit() {
  if (!contextMenuFolder.value || !renameValue.value.trim()) return
  try {
    await renameFolder(contextMenuFolder.value.id, renameValue.value.trim())
    ElMessage.success('重命名成功')
    showRenameDialog.value = false
    loadTree()
    fileStore.fetchFiles()
  } catch {
    ElMessage.error('重命名失败')
  }
}

function handleDelete() {
  closeContextMenu()
  if (!contextMenuFolder.value) return
  ElMessageBox.confirm(
    `确定删除文件夹 "${contextMenuFolder.value.folderName}" 及其内容？`,
    '确认删除',
    { type: 'warning' }
  ).then(async () => {
    try {
      await deleteFolder(contextMenuFolder.value!.id)
      ElMessage.success('已删除')
      loadTree()
      fileStore.fetchFiles()
    } catch {
      ElMessage.error('删除失败')
    }
  })
}

async function handleCreate() {
  const name = newFolderName.value.trim()
  if (!name) return
  try {
    await createFolder({
      folderName: name,
      parentId: fileStore.currentFolderId,
      storageSpace: fileStore.currentStorageSpace,
    })
    ElMessage.success('文件夹已创建')
    showCreateDialog.value = false
    newFolderName.value = ''
    loadTree()
  } catch {
    ElMessage.error('创建失败')
  }
}

// Expose for parent refresh
defineExpose({ loadTree })
</script>

<style scoped>
.folder-tree { padding: 8px 0; }
.tree-header { padding: 0 8px 8px; }
.tree-node { display: flex; align-items: center; gap: 4px; font-size: 14px; }
.node-label { flex: 1; }
.node-count { color: #909399; font-size: 12px; }
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
