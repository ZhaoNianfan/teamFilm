import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { FileItem, FilePageParams } from '@/api/file'
import { getFileList } from '@/api/file'
import type { FolderItem } from '@/api/folder'
import { getFolderTree } from '@/api/folder'

export const useFileStore = defineStore('file', () => {
  const currentPath = ref('')
  const viewMode = ref<'list' | 'grid'>('list')
  const selectedFiles = ref<Set<number>>(new Set())
  const fileList = ref<FileItem[]>([])
  const folderTree = ref<FolderItem[]>([])
  const total = ref(0)
  const loading = ref(false)
  const currentFolderId = ref<number | undefined>(undefined)
  const currentStorageSpace = ref('PERSONAL')
  const breadcrumb = ref<{ id: number; name: string }[]>([])

  const selectedFileIds = computed(() => Array.from(selectedFiles.value))

  function toggleSelect(id: number) {
    const next = new Set(selectedFiles.value)
    if (next.has(id)) {
      next.delete(id)
    } else {
      next.add(id)
    }
    selectedFiles.value = next
  }

  function selectAll(ids: number[]) {
    selectedFiles.value = new Set(ids)
  }

  function clearSelection() {
    selectedFiles.value = new Set()
  }

  async function fetchFiles(params?: FilePageParams) {
    loading.value = true
    try {
      const p = {
        storageSpace: currentStorageSpace.value,
        folderId: currentFolderId.value,
        page: 1,
        size: 50,
        ...params,
      }
      const res = await getFileList(p)
      fileList.value = res.data.records
      total.value = res.data.total
    } finally {
      loading.value = false
    }
  }

  async function fetchFolderTree(space?: string) {
    const sp = space || currentStorageSpace.value
    try {
      const res = await getFolderTree(sp)
      folderTree.value = res.data
    } catch {
      folderTree.value = []
    }
  }

  function navigateToFolder(folderId: number | undefined, folderName?: string) {
    currentFolderId.value = folderId
    if (folderId && folderName) {
      breadcrumb.value.push({ id: folderId, name: folderName })
    } else if (!folderId) {
      breadcrumb.value = []
    }
    fetchFiles()
  }

  function navigateToBreadcrumb(index: number) {
    if (index < 0) {
      currentFolderId.value = undefined
      breadcrumb.value = []
    } else {
      const item = breadcrumb.value[index]
      currentFolderId.value = item.id
      breadcrumb.value = breadcrumb.value.slice(0, index + 1)
    }
    fetchFiles()
  }

  function switchSpace(space: string) {
    currentStorageSpace.value = space
    currentFolderId.value = undefined
    breadcrumb.value = []
    fetchFiles()
    fetchFolderTree(space)
  }

  return {
    currentPath,
    viewMode,
    selectedFiles,
    selectedFileIds,
    fileList,
    folderTree,
    total,
    loading,
    currentFolderId,
    currentStorageSpace,
    breadcrumb,
    toggleSelect,
    selectAll,
    clearSelection,
    fetchFiles,
    fetchFolderTree,
    navigateToFolder,
    navigateToBreadcrumb,
    switchSpace,
  }
})
