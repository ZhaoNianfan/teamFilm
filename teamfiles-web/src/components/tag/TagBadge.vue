<template>
  <span class="tag-badge" :style="badgeStyle" :title="tag.tagName + (tag.fileCount ? ` (${tag.fileCount} files)` : '')">
    <span class="tag-dot" :style="dotStyle"></span>
    <span class="tag-label">{{ tag.tagName }}</span>
    <span v-if="showCount && tag.fileCount" class="tag-count">{{ tag.fileCount }}</span>
    <el-icon v-if="closable" class="tag-close" @click.stop="$emit('close', tag.id)">
      <Close />
    </el-icon>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TagItem } from '@/api/tag'

const props = withDefaults(defineProps<{
  tag: TagItem
  showCount?: boolean
  closable?: boolean
  size?: 'small' | 'default'
}>(), {
  showCount: false,
  closable: false,
  size: 'default',
})

defineEmits<{
  close: [id: number]
}>()

const badgeStyle = computed(() => {
  const c = props.tag.color || '#409EFF'
  return {
    backgroundColor: '#fff',
    borderColor: c,
    color: '#303133',
    fontSize: props.size === 'small' ? '11px' : '12px',
    padding: props.size === 'small' ? '1px 6px' : '2px 10px',
  }
})

const dotStyle = computed(() => ({
  backgroundColor: props.tag.color,
}))
</script>

<style scoped>
.tag-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid;
  border-radius: 12px;
  font-weight: 500;
  cursor: default;
  white-space: nowrap;
}
.tag-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.tag-count {
  font-size: 10px;
  opacity: 0.7;
}
.tag-close {
  font-size: 12px;
  cursor: pointer;
  opacity: 0.7;
}
.tag-close:hover { opacity: 1; }
</style>
