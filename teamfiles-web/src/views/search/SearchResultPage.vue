<template>
  <div class="search-page">
    <div class="search-bar-area">
      <el-input
        v-model="searchForm.keyword"
        size="large"
        placeholder="输入关键词搜索文件..."
        clearable
        @keyup.enter="doSearch"
        @input="onKeywordInput"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button type="primary" @click="doSearch" :loading="loading">
            搜索
          </el-button>
        </template>
      </el-input>
      <div v-if="suggestions.length > 0 && showSuggestions" class="suggestions-dropdown">
        <div
          v-for="(s, idx) in suggestions"
          :key="idx"
          class="suggestion-item"
          @click="selectSuggestion(s)"
        >
          <el-icon><Search /></el-icon>
          <span>{{ s }}</span>
        </div>
      </div>
    </div>

    <div class="search-body">
      <el-aside width="220px" class="filter-aside">
        <div class="filter-section">
          <h4>文件类型</h4>
          <el-select v-model="searchForm.fileType" placeholder="全部类型" clearable style="width:100%">
            <el-option label="图片" value="IMAGE" />
            <el-option label="文档" value="DOCUMENT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </div>
        <div class="filter-section">
          <h4>搜索范围</h4>
          <el-select v-model="searchForm.storageSpace" placeholder="全部空间" clearable style="width:100%">
            <el-option label="个人空间" value="PERSONAL" />
            <el-option label="团队空间" value="TEAM" />
          </el-select>
        </div>
        <div class="filter-section">
          <h4>排序</h4>
          <el-select v-model="searchForm.sortBy" style="width:100%">
            <el-option label="相关度" value="_score" />
            <el-option label="上传时间" value="created_at" />
            <el-option label="文件大小" value="file_size" />
            <el-option label="文件名" value="file_name" />
          </el-select>
          <el-radio-group v-model="searchForm.sortDir" size="small" style="margin-top:6px">
            <el-radio-button value="desc">降序</el-radio-button>
            <el-radio-button value="asc">升序</el-radio-button>
          </el-radio-group>
        </div>
        <div class="filter-section">
          <el-button type="primary" @click="doSearch" style="width:100%">搜索</el-button>
        </div>

        <div class="filter-section" v-if="templates.length > 0">
          <h4>搜索模板</h4>
          <div v-for="tpl in templates" :key="tpl.id" class="template-item">
            <span @click="executeTemplate(tpl)">{{ tpl.templateName }}</span>
            <el-button size="small" text type="danger" @click="deleteTemplateClick(tpl.id)">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>
      </el-aside>

      <el-main class="result-main">
        <div v-if="!hasSearched && historyItems.length > 0" class="search-history">
          <div class="history-header">
            <h4>搜索历史</h4>
            <el-button size="small" text type="danger" @click="clearHistory">清空</el-button>
          </div>
          <div class="history-list">
            <el-tag
              v-for="item in historyItems"
              :key="item.id"
              closable
              @click="searchForm.keyword = item.keyword; doSearch()"
              @close="deleteHistory(item.id)"
            >
              {{ item.keyword }}
            </el-tag>
          </div>
        </div>

        <div v-if="hasSearched">
          <div class="result-header">
            <span>找到 <strong>{{ total }}</strong> 个结果</span>
            <div class="view-toggle">
              <el-button
                :type="viewMode === 'list' ? 'primary' : ''"
                size="small"
                @click="viewMode = 'list'"
              >
                <el-icon><List /></el-icon>
              </el-button>
              <el-button
                :type="viewMode === 'grid' ? 'primary' : ''"
                size="small"
                @click="viewMode = 'grid'"
              >
                <el-icon><Grid /></el-icon>
              </el-button>
              <el-button size="small" @click="showSaveTemplateDialog = true">
                <el-icon><Star /></el-icon>
                保存模板
              </el-button>
            </div>
          </div>

          <el-empty v-if="results.length === 0 && !loading" description="未找到匹配的文件" />

          <div v-if="viewMode === 'grid'" class="result-grid">
            <div
              v-for="item in results"
              :key="item.fileId"
              class="result-card"
              @click="previewFile(item)"
            >
              <div class="card-icon">
                <el-icon :size="36" :color="getFileColor(item.fileType)">
                  <PictureFilled v-if="item.fileType === 'IMAGE'" />
                  <Document v-else />
                </el-icon>
              </div>
              <div class="card-info">
                <div class="card-name" v-html="highlightName(item)"></div>
                <div class="card-meta">{{ item.displaySize }} · {{ item.uploadUsername }}</div>
              </div>
            </div>
          </div>

          <el-table v-else :data="results" v-loading="loading" style="width:100%">
            <el-table-column label="文件名" min-width="260">
              <template #default="{ row }">
                <div class="result-name" @click="previewFile(row)" style="cursor:pointer">
                  <span v-html="highlightName(row)"></span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="80" prop="fileType" />
            <el-table-column label="大小" width="100" prop="displaySize" />
            <el-table-column label="上传者" width="100" prop="uploadUsername" />
            <el-table-column label="时间" width="160" prop="createdAt" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="previewFile(row)">预览</el-button>
                <el-button size="small" text type="primary" @click="downloadFile(row)">下载</el-button>
                <el-button size="small" text type="primary" @click="findSimilar(row)">相似</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper" v-if="total > (searchForm.size || 20)">
            <el-pagination
              :total="total"
              :page-size="searchForm.size || 20"
              :current-page="searchForm.page || 1"
              layout="total, prev, pager, next"
              @current-change="onPageChange"
            />
          </div>
        </div>
      </el-main>
    </div>

    <el-dialog v-model="showSaveTemplateDialog" title="保存搜索模板" width="400px">
      <el-input v-model="templateName" placeholder="模板名称" />
      <template #footer>
        <el-button @click="showSaveTemplateDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { SearchParams, SearchResultItem, SearchHistoryItem, SearchTemplateItem } from '@/api/search'
import {
  search as searchApi,
  getSuggestions,
  getSearchHistory,
  deleteSearchHistory,
  clearSearchHistory,
  getSearchTemplates,
  saveSearchTemplate,
  deleteSearchTemplate,
  executeSearchTemplate,
  getSimilarFiles,
} from '@/api/search'
import { getPreviewUrl, getDownloadUrl } from '@/api/file'

const route = useRoute()

const loading = ref(false)
const hasSearched = ref(false)
const results = ref<SearchResultItem[]>([])
const total = ref(0)
const viewMode = ref<'list' | 'grid'>('list')
const suggestions = ref<string[]>([])
const showSuggestions = ref(false)
const historyItems = ref<SearchHistoryItem[]>([])
const templates = ref<SearchTemplateItem[]>([])
const showSaveTemplateDialog = ref(false)
const templateName = ref('')

const searchForm = reactive<SearchParams>({
  keyword: (route.query.q as string) || '',
  fileType: undefined,
  storageSpace: undefined,
  sortBy: '_score',
  sortDir: 'desc',
  page: 1,
  size: 20,
})

onMounted(async () => {
  if (searchForm.keyword) doSearch()
  await Promise.all([loadHistory(), loadTemplates()])
})

async function loadHistory() {
  try { const res = await getSearchHistory(); historyItems.value = res.data || [] } catch { /* */ }
}

async function loadTemplates() {
  try { const res = await getSearchTemplates(); templates.value = res.data || [] } catch { /* */ }
}

async function doSearch() {
  if (!searchForm.keyword?.trim()) return
  loading.value = true
  showSuggestions.value = false
  try {
    const res = await searchApi(searchForm)
    results.value = res.data.records
    total.value = res.data.total
    hasSearched.value = true
    loadHistory()
  } catch { ElMessage.error('搜索失败') }
  finally { loading.value = false }
}

async function onKeywordInput(val: string | number) {
  const kw = String(val || '').trim()
  if (kw.length < 1) { showSuggestions.value = false; return }
  try {
    const res = await getSuggestions(kw)
    suggestions.value = res.data || []
    showSuggestions.value = suggestions.value.length > 0
  } catch { showSuggestions.value = false }
}

function selectSuggestion(s: string) {
  searchForm.keyword = s
  showSuggestions.value = false
  doSearch()
}

async function deleteHistory(id: number) { await deleteSearchHistory(id); loadHistory() }

async function clearHistory() { await clearSearchHistory(); loadHistory() }

async function saveTemplate() {
  if (!templateName.value.trim()) return
  try {
    await saveSearchTemplate({
      templateName: templateName.value.trim(),
      searchCondition: JSON.stringify({ ...searchForm }),
    })
    ElMessage.success('模板已保存')
    showSaveTemplateDialog.value = false
    templateName.value = ''
    loadTemplates()
  } catch { ElMessage.error('保存失败') }
}

async function executeTemplate(tpl: SearchTemplateItem) {
  try {
    const res = await executeSearchTemplate(tpl.id)
    results.value = res.data.records
    total.value = res.data.total
    hasSearched.value = true
  } catch { ElMessage.error('执行模板失败') }
}

async function deleteTemplateClick(id: number) { await deleteSearchTemplate(id); loadTemplates() }

async function findSimilar(item: SearchResultItem) {
  try {
    const res = await getSimilarFiles(item.fileId)
    results.value = res.data
    total.value = res.data.length
    hasSearched.value = true
  } catch { ElMessage.error('查找相似文件失败') }
}

function previewFile(item: SearchResultItem) {
  window.open(getPreviewUrl(item.fileId), '_blank')
}

function downloadFile(item: SearchResultItem) {
  const a = document.createElement('a')
  a.href = getDownloadUrl(item.fileId)
  a.download = item.originalName
  a.click()
}

function onPageChange(page: number) { searchForm.page = page; doSearch() }

function highlightName(item: SearchResultItem): string {
  if (item.highlights?.originalName?.[0]) return item.highlights.originalName[0]
  return item.originalName
}

function getFileColor(fileType: string): string {
  if (fileType === 'IMAGE') return '#67c23a'
  if (fileType === 'DOCUMENT') return '#409eff'
  return '#909399'
}
</script>

<style scoped>
.search-page { padding: 20px; }
.search-bar-area { position: relative; max-width: 700px; margin-bottom: 20px; }
.suggestions-dropdown {
  position: absolute; top: 100%; left: 0; right: 0; z-index: 999;
  background: #fff; border: 1px solid #e4e7ed; border-radius: 0 0 8px 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1); max-height: 240px; overflow-y: auto;
}
.suggestion-item {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; cursor: pointer; font-size: 14px;
}
.suggestion-item:hover { background: #f5f7fa; }
.search-body { display: flex; gap: 20px; }
.filter-aside { flex-shrink: 0; }
.filter-section { margin-bottom: 16px; }
.filter-section h4 { margin: 0 0 8px; font-size: 14px; color: #606266; }
.template-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 4px 0; font-size: 13px; cursor: pointer;
}
.template-item:hover { color: #409eff; }
.result-main { flex: 1; padding: 0; }
.search-history { margin-bottom: 20px; }
.history-header { display: flex; justify-content: space-between; align-items: center; }
.history-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.history-list .el-tag { cursor: pointer; }
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; color: #606266; }
.view-toggle { display: flex; gap: 4px; align-items: center; }
.result-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px; }
.result-card {
  border: 1px solid #e4e7ed; border-radius: 8px; padding: 20px 16px;
  text-align: center; cursor: pointer; transition: all 0.2s;
}
.result-card:hover { border-color: #409eff; box-shadow: 0 2px 8px rgba(64,158,255,0.15); }
.card-icon { margin-bottom: 8px; }
.card-name { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-meta { font-size: 12px; color: #909399; margin-top: 4px; }
.result-name { color: #303133; }
.result-name :deep(em) { color: #f56c6c; font-style: normal; font-weight: bold; }
.pagination-wrapper { padding-top: 16px; display: flex; justify-content: center; }
</style>
