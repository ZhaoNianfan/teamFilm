<template>
  <div class="tag-input">
    <div class="selected-tags" v-if="modelValue.length > 0">
      <TagBadge v-for="tag in selectedTags" :key="tag.id" :tag="tag" :closable="true" @close="removeTag(tag.id)" />
    </div>
    <el-input v-model="searchText" placeholder="输入标签名搜索..." size="small" clearable @input="onSearchInput">
      <template #prefix><el-icon><Search /></el-icon></template>
    </el-input>
    <div v-if="showDropdown" class="tag-dropdown">
      <div v-for="tag in suggestionList" :key="tag.id" class="tag-option" @click="addTag(tag)">
        <TagBadge :tag="tag" size="small" :show-count="true" />
      </div>
      <div v-if="suggestionList.length === 0 && searchText" class="tag-option empty">无匹配标签，输入完整名称可创建新标签</div>
    </div>
    <div v-if="similarTags.length > 0" class="similar-warning">
      <span style="font-size:12px;color:#e6a23c">相似标签：</span>
      <TagBadge v-for="tag in similarTags" :key="tag.id" :tag="tag" size="small" style="cursor:pointer" @click.native="addTag(tag)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import type { TagItem } from '@/api/tag'
import { autocompleteTags, getSimilarTags, getTagList, createTag } from '@/api/tag'
import TagBadge from './TagBadge.vue'

const props = defineProps<{ modelValue: number[] }>()
const emit = defineEmits<{ 'update:modelValue': [value: number[]] }>()

const allTags = ref<TagItem[]>([])
const searchText = ref('')
const loading = ref(false)
const suggestions = ref<TagItem[]>([])
const similarTags = ref<TagItem[]>([])
const showDropdown = ref(false)

const selectedTags = computed(() => allTags.value.filter(t => props.modelValue.includes(t.id)))
const suggestionList = computed(() => {
  if (!searchText.value) return allTags.value.slice(0, 10)
  return suggestions.value
})

onMounted(async () => {
  try { const res = await getTagList(); allTags.value = res.data } catch { /* */ }
})
watch(() => props.modelValue, () => { searchText.value = ''; showDropdown.value = false })

async function onSearchInput(val: string | number) {
  const kw = String(val || '').trim()
  if (!kw) { showDropdown.value = false; similarTags.value = []; return }
  loading.value = true
  showDropdown.value = true
  try {
    const [autoRes, simRes] = await Promise.all([autocompleteTags(kw), getSimilarTags(kw)])
    suggestions.value = autoRes.data
    similarTags.value = simRes.data.filter(t => !props.modelValue.includes(t.id))
  } catch { suggestions.value = allTags.value.filter(t => t.tagName.includes(kw)) }
  finally { loading.value = false }
}

async function addTag(tag: TagItem) {
  const next = [...props.modelValue, tag.id]
  emit('update:modelValue', next)
  showDropdown.value = false
  searchText.value = ''
}

function removeTag(id: number) {
  emit('update:modelValue', props.modelValue.filter(i => i !== id))
}
</script>

<style scoped>
.tag-input { position: relative; }
.selected-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 8px; }
.tag-dropdown { position: absolute; z-index: 2000; background: #fff; border: 1px solid #e4e7ed; border-radius: 4px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); max-height: 200px; overflow-y: auto; width: 100%; margin-top: 2px; }
.tag-option { padding: 6px 10px; cursor: pointer; }
.tag-option:hover { background: #f5f7fa; }
.tag-option.empty { color: #909399; font-size: 12px; cursor: default; }
.similar-warning { margin-top: 8px; display: flex; flex-wrap: wrap; align-items: center; gap: 4px; }
</style>
