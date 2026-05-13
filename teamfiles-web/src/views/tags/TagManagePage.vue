<template>
  <div class="tag-manage-page">
    <div class="page-header">
      <h2>标签管理</h2>
      <div class="header-actions">
        <el-button type="primary" size="small" @click="openCreateTag">
          <el-icon><Plus /></el-icon> 新建标签
        </el-button>
        <el-button size="small" @click="openCreateGroup">
          <el-icon><FolderAdd /></el-icon> 新建标签组
        </el-button>
      </div>
    </div>

    <div class="tag-layout">
      <!-- Left: groups sidebar -->
      <div class="group-sidebar">
        <div class="sidebar-title">标签组</div>
        <div class="group-list">
          <div class="group-item" :class="{active:activeGroup==='all'}" @click="activeGroup='all'">全部标签</div>
          <div v-for="g in groups" :key="g.id" class="group-item" :class="{active:activeGroup===g.id}" @click="activeGroup = activeGroup===g.id ? 'all' : g.id">
            <span>{{ g.groupName }}</span>
            <span class="g-count">{{ g.tagCount }}</span>
            <el-icon :size="12" class="g-del" @click.stop="deleteGroupClick(g.id)"><Close /></el-icon>
          </div>
        </div>
        <el-button size="small" text @click="openCreateGroup" style="margin-top:8px">+ 新建组</el-button>
      </div>

      <!-- Right: tag content -->
      <div class="tag-main">
        <div class="toolbar-row">
          <el-input v-model="tagSearch" placeholder="搜索标签..." size="small" clearable style="width:180px" prefix-icon="Search" />
          <el-button size="small" v-if="activeGroup!=='all'" @click="showAddToGroup=true">添加标签到此组</el-button>
        </div>

        <!-- Tag cloud -->
        <div class="tag-cloud">
      <div
        v-for="tag in filteredTags"
        :key="tag.id"
        class="tag-chip"
        :class="{ selected: selectedTagIds.has(tag.id) }"
        :style="{ borderColor: tag.color, color: tag.color, backgroundColor: selectedTagIds.has(tag.id) ? tag.color + '15' : '#fff' }"
        @click="toggleTagSelect(tag)"
      >
        <span class="chip-dot" :style="{ backgroundColor: tag.color }"></span>
        <span class="chip-name">{{ tag.tagName }}</span>
        <span class="chip-count">私有{{ tag.personalFileCount || 0 }} 共享{{ tag.sharedFileCount || 0 }}</span>
        <span class="chip-actions">
          <el-icon :size="14" @click.stop="editTag(tag)" title="编辑"><Edit /></el-icon>
          <el-icon :size="14" @click.stop="deleteTagClick(tag)" title="删除"><Close /></el-icon>
        </span>
      </div>
      <el-empty v-if="filteredTags.length === 0" description="暂无标签" :image-size="40" />
    </div>

    <el-divider v-if="selectedTagIds.size > 0" />

    <!-- Files for selected tags -->
    <div v-if="selectedTagIds.size > 0" class="tag-files">
      <div class="files-header">
        <h4>
          已选标签:
          <span v-for="id in [...selectedTagIds]" :key="id" class="sel-tag-name" :style="{color: tags.find(t=>t.id===id)?.color}">
            {{ tags.find(t=>t.id===id)?.tagName }}
          </span>
          ({{ selectedTagIds.size > 1 ? 'AND' : '' }})
          <el-button size="small" text @click="selectedTagIds.clear(); tagFiles=[]">清除</el-button>
        </h4>
      </div>
      <el-table :data="tagFiles" style="width:100%" max-height="300" v-if="tagFiles.length > 0">
        <el-table-column label="文件名" prop="originalName" min-width="200" />
        <el-table-column label="大小" width="100" prop="displaySize" />
        <el-table-column label="空间" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.storageSpace === 'TEAM' ? 'warning' : ''">{{ row.storageSpace === 'TEAM' ? '共享' : '个人' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="日期" width="160" prop="createdAt" />
      </el-table>
      <el-empty v-else description="未找到同时关联这些标签的文件" :image-size="50" />
    </div>

    <!-- Create/Edit Tag Dialog -->
    <el-dialog v-model="showTagDialog" :title="editingTag ? '编辑标签' : '新建标签'" width="420px">
      <el-form label-position="top">
        <el-form-item label="名称">
          <el-input v-model="tagForm.tagName" placeholder="请输入标签名称" @input="onTagNameInput" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="tagForm.color" />
        </el-form-item>
      </el-form>
      <el-alert v-if="tagSimilarWarnings.length > 0" type="warning" :closable="false" show-icon style="margin-bottom:8px">
        <template #title>
          发现相似标签（双击添加）：
          <span v-for="t in tagSimilarWarnings" :key="t.id" class="similar-tag" :style="{borderColor:t.color,color:t.color}" @dblclick="addSimilarTag(t)">{{ t.tagName }}</span>
        </template>
      </el-alert>
      <template #footer>
        <el-button @click="showTagDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTag" :disabled="!tagForm.tagName.trim()">
          {{ editingTag ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Group Dialog -->
    <el-dialog v-model="showGroupDialog" :title="editingGroup ? '编辑标签组' : '新建标签组'" width="460px">
      <el-form label-position="top">
        <el-form-item label="名称">
          <el-input v-model="groupForm.groupName" placeholder="请输入组名称" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="groupForm.color" />
        </el-form-item>
        <el-form-item v-if="editingGroup">
          <template #label>组内标签</template>
          <div style="display:flex;flex-wrap:wrap;gap:6px;margin-bottom:8px">
            <el-tag v-for="t in groupFormTags" :key="t.id" :color="t.color" size="small" closable @close="removeTagFromGroup(t.id)">{{ t.tagName }}</el-tag>
          </div>
          <el-select v-model="addTagToGroupIds" multiple filterable placeholder="添加标签..." style="width:100%" @change="onAddTagsToGroup">
            <el-option v-for="t in availableTagsForGroup" :key="t.id" :label="t.tagName" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGroupDialog = false">取消</el-button>
        <el-button type="primary" @click="saveGroup" :disabled="!groupForm.groupName.trim()">{{ editingGroup ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- Add tags to current group -->
    <el-dialog v-model="showAddToGroup" title="添加标签到组" width="400px">
      <el-input v-model="addGroupSearch" placeholder="搜索标签..." size="small" style="margin-bottom:8px" @input="onAddGroupSearch" />
      <div style="max-height:250px;overflow-y:auto">
        <div v-for="t in addGroupTagList" :key="t.id" class="add-tag-row" @click="toggleAddGroupTag(t.id)">
          <el-checkbox :model-value="addToGroupTagIds.includes(t.id)" />
          <TagBadge :tag="t" size="small" />
        </div>
        <div v-if="addGroupTagList.length===0" style="color:#909399;font-size:13px;text-align:center;padding:16px">无匹配标签</div>
      </div>
      <template #footer>
        <el-button @click="showAddToGroup=false">取消</el-button>
        <el-button type="primary" @click="addTagsToCurrentGroup" :disabled="!addToGroupTagIds.length">添加选中 ({{addToGroupTagIds.length}})</el-button>
      </template>
    </el-dialog>
      </div><!-- /tag-main -->
    </div><!-- /tag-layout -->
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TagItem, TagGroupItem } from '@/api/tag'
import {
  getTagList, createTag, updateTag, deleteTag, getFilesByTag, getSimilarTags,
  getTagGroupList, createTagGroup, updateTagGroup, deleteTagGroup,
  removeTagFromGroup as removeTagFromGroupApi, addTagsToGroup
} from '@/api/tag'

const tags = ref<TagItem[]>([])
const groups = ref<TagGroupItem[]>([])
const activeGroup = ref<string | number>('all')
const selectedTagIds = ref<Set<number>>(new Set())
const tagSearch = ref('')

const showTagDialog = ref(false); const editingTag = ref<TagItem | null>(null)
const tagForm = ref({ tagName: '', color: '#409EFF' }); const tagSimilarWarnings = ref<TagItem[]>([])

const showGroupDialog = ref(false); const editingGroup = ref<TagGroupItem | null>(null)
const showAddToGroup = ref(false); const addToGroupTagIds = ref<number[]>([]); const addGroupSearch = ref('')
const addGroupTagList = ref<TagItem[]>([])
const groupForm = ref({ groupName: '', color: '#409EFF' })
const groupFormTags = ref<TagItem[]>([]); const addTagToGroupIds = ref<number[]>([])

const tagFiles = ref<any[]>([])

const filteredTags = computed(() => {
  let list = tags.value
  if (activeGroup.value !== 'all') {
    const g = groups.value.find(g => g.id === activeGroup.value)
    list = g?.tags || []
  }
  if (tagSearch.value.trim()) {
    const kw = tagSearch.value.trim().toLowerCase()
    list = list.filter(t => t.tagName.toLowerCase().includes(kw))
  }
  return list
})

const availableTagsForGroup = computed(() => {
  if (!editingGroup.value) return tags.value
  const existing = new Set(editingGroup.value.tags.map(t => t.id))
  return tags.value.filter(t => !existing.has(t.id))
})

onMounted(() => loadData())

async function loadData() {
  try {
    const [t, g] = await Promise.all([getTagList(), getTagGroupList()])
    tags.value = t.data; groups.value = g.data
  } catch { /* */ }
}

function openCreateTag() {
  editingTag.value = null; tagForm.value = { tagName: '', color: '#409EFF' }
  tagSimilarWarnings.value = []; showTagDialog.value = true
}
function editTag(tag: TagItem) {
  editingTag.value = tag; tagForm.value = { tagName: tag.tagName, color: tag.color }
  tagSimilarWarnings.value = []; showTagDialog.value = true
}
async function addSimilarTag(tag: TagItem) {
  try {
    await createTag({ tagName: tag.tagName, color: tag.color })
    ElMessage.success('已添加标签: ' + tag.tagName)
    tagSimilarWarnings.value = tagSimilarWarnings.value.filter(t => t.id !== tag.id)
    loadData()
  } catch { ElMessage.error('添加失败') }
}

async function onTagNameInput() {
  if (tagForm.value.tagName.trim().length < 2) { tagSimilarWarnings.value = []; return }
  try {
    const res = await getSimilarTags(tagForm.value.tagName.trim())
    tagSimilarWarnings.value = (res.data || []).filter(t => !editingTag.value || t.id !== editingTag.value.id)
  } catch { tagSimilarWarnings.value = [] }
}
async function saveTag() {
  try {
    if (editingTag.value) {
      await updateTag(editingTag.value.id, { tagName: tagForm.value.tagName.trim(), color: tagForm.value.color })
      ElMessage.success('已更新')
    } else {
      await createTag({ tagName: tagForm.value.tagName.trim(), color: tagForm.value.color })
      ElMessage.success('已创建')
    }
    showTagDialog.value = false; editingTag.value = null; loadData()
  } catch { ElMessage.error('操作失败') }
}
async function deleteTagClick(tag: TagItem) {
  try {
    await ElMessageBox.confirm(`确定删除标签 "${tag.tagName}"？关联文件不受影响。`, '确认', { type: 'warning' })
    await deleteTag(tag.id)
    selectedTagIds.value.delete(tag.id)
    selectedTagIds.value = new Set(selectedTagIds.value)
    if (selectedTagIds.value.size === 0) tagFiles.value = []
    ElMessage.success('已删除'); loadData()
  } catch { /* */ }
}

async function toggleTagSelect(tag: TagItem) {
  if (selectedTagIds.value.has(tag.id)) {
    selectedTagIds.value.delete(tag.id)
    selectedTagIds.value = new Set(selectedTagIds.value)
  } else {
    selectedTagIds.value.add(tag.id)
    selectedTagIds.value = new Set(selectedTagIds.value)
  }
  if (selectedTagIds.value.size > 0) {
    await loadFilesForTags()
  } else {
    tagFiles.value = []
  }
}

async function loadFilesForTags() {
  const ids = [...selectedTagIds.value]
  try {
    // Load files for first tag, then filter by subsequent tags
    const res = await getFilesByTag(ids[0])
    let files = (res.data || []) as any[]
    for (let i = 1; i < ids.length; i++) {
      const r = await getFilesByTag(ids[i])
      const nextIds = new Set((r.data || []).map((f: any) => f.id))
      files = files.filter(f => nextIds.has(f.id))
    }
    tagFiles.value = files
  } catch { tagFiles.value = [] }
}

function openCreateGroup() {
  editingGroup.value = null; groupForm.value = { groupName: '', color: '#409EFF' }
  groupFormTags.value = []; showGroupDialog.value = true
}
function editGroup() {
  if (!editingGroup.value) return
  groupForm.value = { groupName: editingGroup.value.groupName, color: editingGroup.value.color }
  groupFormTags.value = [...(editingGroup.value.tags || [])]
  showGroupDialog.value = true
}
async function saveGroup() {
  try {
    if (editingGroup.value) {
      await updateTagGroup(editingGroup.value.id, { groupName: groupForm.value.groupName.trim(), color: groupForm.value.color })
      ElMessage.success('已更新')
    } else {
      await createTagGroup({ groupName: groupForm.value.groupName.trim(), color: groupForm.value.color })
      ElMessage.success('已创建')
    }
    showGroupDialog.value = false; editingGroup.value = null; loadData()
  } catch { ElMessage.error('操作失败') }
}
async function removeTagFromGroup(tagId: number) {
  if (!editingGroup.value) return
  await removeTagFromGroupApi(editingGroup.value.id, tagId)
  groupFormTags.value = groupFormTags.value.filter(t => t.id !== tagId); loadData()
}
async function onAddTagsToGroup(ids: number[]) {
  if (!editingGroup.value || !ids.length) return
  await addTagsToGroup(editingGroup.value.id, ids); addTagToGroupIds.value = []; loadData()
}
async function deleteGroupClick(id: number) {
  try {
    await ElMessageBox.confirm('确定删除此标签组？标签不会被删除。', '确认', { type: 'warning' })
    await deleteTagGroup(id)
    if (activeGroup.value === id) activeGroup.value = 'all'
    ElMessage.success('已删除'); loadData()
  } catch { /* */ }
}
async function addTagsToCurrentGroup() {
  if (activeGroup.value === 'all' || !addToGroupTagIds.value.length) return
  await addTagsToGroup(activeGroup.value as number, addToGroupTagIds.value)
  ElMessage.success('已添加'); addToGroupTagIds.value = []; addGroupSearch.value = ''; showAddToGroup.value = false; loadData()
}
function toggleAddGroupTag(id: number) {
  const idx = addToGroupTagIds.value.indexOf(id)
  if (idx >= 0) addToGroupTagIds.value.splice(idx, 1)
  else addToGroupTagIds.value.push(id)
}
function onAddGroupSearch() {
  const kw = addGroupSearch.value.trim().toLowerCase()
  addGroupTagList.value = kw ? tags.value.filter(t => t.tagName.toLowerCase().includes(kw)) : tags.value
}

// Init add-to-group list when dialog opens
watch(showAddToGroup, (v) => {
  if (v) { addGroupTagList.value = [...tags.value]; addToGroupTagIds.value = []; addGroupSearch.value = '' }
})
</script>

<style scoped>
.tag-manage-page { height: 100%; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-header h2 { margin: 0; }
.header-actions { display: flex; gap: 8px; }
.group-tabs { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
.group-tab { cursor: pointer; }
.tag-cloud { display: flex; flex-wrap: wrap; gap: 8px; align-items: flex-start; padding: 8px 0; }
.tag-chip { display:inline-flex; align-items:center; gap:6px; border:1px solid; border-radius:16px; padding:4px 12px; cursor:pointer; font-size:13px; transition:all 0.15s; }
.tag-chip:hover { box-shadow:0 2px 6px rgba(0,0,0,0.1); }
.tag-chip.selected { box-shadow:0 0 0 2px inset; }
.chip-name { font-weight: 500; }
.chip-count { margin-left: 6px; font-size: 11px; opacity: 0.8; }
.tag-files { margin-top: 8px; }
.files-header { display: flex; justify-content: space-between; align-items: center; }
.files-header h4 { margin: 0 0 8px; }
.similar-tag { display:inline-block; border:1px solid; border-radius:10px; padding:1px 8px; margin:2px; cursor:pointer; font-size:12px; background:#fff; }
.similar-tag:hover { opacity: 0.7; }
.sel-tag-name { margin:0 4px; font-weight:500; }
.tag-layout { display:flex; gap:16px; }
.group-sidebar { width:180px; flex-shrink:0; border-right:1px solid #e4e7ed; padding-right:12px; }
.sidebar-title { font-weight:500; margin-bottom:8px; font-size:14px; }
.group-list { display:flex; flex-direction:column; gap:2px; }
.group-item { display:flex; align-items:center; gap:6px; padding:6px 10px; font-size:13px; cursor:pointer; border-radius:4px; }
.group-item:hover { background:#f5f7fa; }
.group-item.active { background:#ecf5ff; color:#409eff; }
.g-count { margin-left:auto; font-size:11px; color:#909399; }
.g-del { opacity:0; color:#f56c6c; }
.group-item:hover .g-del { opacity:1; }
.tag-main { flex:1; min-width:0; }
.add-tag-row { display:flex; align-items:center; gap:8px; padding:6px 8px; cursor:pointer; border-radius:4px; }
.add-tag-row:hover { background:#f5f7fa; }
</style>
