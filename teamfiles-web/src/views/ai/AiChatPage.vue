<template>
  <div class="ai-chat-page">
    <div class="chat-layout">
      <!-- Left: conversations -->
      <div class="conv-sidebar">
        <div class="sidebar-header">
          <span>对话列表</span>
          <el-button size="small" type="primary" @click="startNewChat">
            <el-icon><Plus /></el-icon> 新对话
          </el-button>
        </div>
        <div class="conv-list">
          <div
            v-for="conv in conversations"
            :key="conv.id"
            class="conv-item"
            :class="{ active: conv.id === activeConversation }"
            @click="switchConversation(conv.id)"
          >
            <div class="conv-preview">{{ getPreview(conv.lastMessage) }}</div>
            <div class="conv-meta">{{ conv.messageCount }} 条消息</div>
            <el-button
              size="small" text type="danger" class="conv-delete"
              @click.stop="deleteConv(conv.id)"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <el-empty v-if="conversations.length === 0" description="暂无对话" :image-size="60" />
        </div>
      </div>

      <!-- Right: chat -->
      <div class="chat-main">
        <div class="chat-messages" ref="msgListRef">
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="message-row"
            :class="msg.role"
          >
            <div class="msg-avatar">
              <el-icon :size="24"><UserFilled v-if="msg.role === 'user'" /><Service v-else /></el-icon>
            </div>
            <div class="msg-bubble" v-html="renderMarkdown(msg.content)"></div>
          </div>
          <div v-if="thinking" class="message-row assistant">
            <div class="msg-avatar"><el-icon :size="24"><Service /></el-icon></div>
            <div class="msg-bubble thinking-dots"><span>.</span><span>.</span><span>.</span></div>
          </div>
          <div v-if="!hasMessages && !thinking" class="chat-welcome">
            <h3>AI 助手</h3>
            <p>我可以帮助您：</p>
            <ul>
              <li>分析文件内容并生成摘要</li>
              <li>根据文件内容回答问题</li>
              <li>搜索和管理您的文件</li>
            </ul>
            <p class="hint">请先在系统设置中配置 AI 服务（支持 OpenAI/通义千问/智谱GLM 等）</p>
          </div>
        </div>

        <!-- File refs -->
        <div v-if="selectedFiles.length > 0" class="file-refs">
          <el-tag
            v-for="f in selectedFiles"
            :key="f.id"
            closable
            size="small"
            @close="removeFile(f.id)"
          >
            <el-icon><Document /></el-icon> {{ f.name }}
          </el-tag>
        </div>

        <!-- Input -->
        <div class="chat-input">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            placeholder="输入消息，Enter 发送，Shift+Enter 换行"
            :disabled="thinking"
            @keydown.enter.exact="sendMessage"
          />
          <div class="input-actions">
            <el-button @click="openFilePicker" :disabled="thinking" size="small">
              <el-icon><FolderOpened /></el-icon> 引用文件
            </el-button>
            <el-button
              type="primary"
              size="small"
              @click="sendMessage"
              :disabled="!inputText.trim() || thinking"
              :loading="thinking"
            >
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- File picker -->
    <el-dialog v-model="showFilePicker" title="选择引用文件" width="500px">
      <el-input v-model="fileSearch" placeholder="搜索文件..." size="small" style="margin-bottom:12px" />
      <div class="file-pick-list">
        <div
          v-for="f in filteredFiles"
          :key="f.id"
          class="file-pick-item"
          :class="{ picked: selectedFiles.some(sf => sf.id === f.id) }"
          @click="toggleFile(f)"
        >
          <el-icon><Document /></el-icon>
          <span>{{ f.originalName }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showFilePicker = false">完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import type { Conversation, ChatMessage } from '@/api/ai'
import { getConversations, getConversation, deleteConversation, chatSync } from '@/api/ai'
import { getFileList } from '@/api/file'
import type { FileItem } from '@/api/file'

const conversations = ref<Conversation[]>([])
const activeConversation = ref<string | null>(null)
const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const thinking = ref(false)
const msgListRef = ref<HTMLElement | null>(null)

const selectedFiles = ref<{ id: number; name: string }[]>([])
const showFilePicker = ref(false)
const fileSearch = ref('')
const allFiles = ref<FileItem[]>([])

const hasMessages = computed(() => messages.value.length > 0)

onMounted(() => loadConversations())

async function loadConversations() {
  try {
    const res = await getConversations()
    conversations.value = res.data || []
  } catch { /* */ }
}

function startNewChat() {
  activeConversation.value = null
  messages.value = []
  selectedFiles.value = []
}

async function switchConversation(id: string) {
  activeConversation.value = id
  try {
    const res = await getConversation(id)
    messages.value = res.data.messages || []
    scrollBottom()
  } catch { messages.value = [] }
}

async function deleteConv(id: string) {
  await deleteConversation(id)
  if (activeConversation.value === id) startNewChat()
  loadConversations()
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || thinking.value) return
  inputText.value = ''
  thinking.value = true

  messages.value.push({ role: 'user', content: text, time: new Date().toISOString() })
  scrollBottom()

  try {
    const res = await chatSync(text, activeConversation.value, selectedFiles.value.map(f => f.id))
    const data = res.data
    if (!activeConversation.value) activeConversation.value = data.conversationId
    messages.value.push({ role: 'assistant', content: data.reply, time: new Date().toISOString() })
  } catch (e: any) {
    messages.value.push({ role: 'assistant', content: 'AI 请求失败: ' + (e.message || '网络错误'), time: new Date().toISOString() })
  } finally {
    thinking.value = false
    loadConversations()
    scrollBottom()
  }
}

function scrollBottom() {
  nextTick(() => {
    const el = msgListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function openFilePicker() {
  showFilePicker.value = true
  if (allFiles.value.length === 0) {
    try {
      const res = await getFileList({ size: 200 })
      allFiles.value = res.data.records
    } catch { /* */ }
  }
}

const filteredFiles = computed(() => {
  if (!fileSearch.value) return allFiles.value.slice(0, 30)
  const kw = fileSearch.value.toLowerCase()
  return allFiles.value.filter(f => f.originalName.toLowerCase().includes(kw)).slice(0, 30)
})

function toggleFile(file: FileItem) {
  const idx = selectedFiles.value.findIndex(sf => sf.id === file.id)
  if (idx >= 0) selectedFiles.value.splice(idx, 1)
  else selectedFiles.value.push({ id: file.id, name: file.originalName })
}

function removeFile(id: number) {
  selectedFiles.value = selectedFiles.value.filter(f => f.id !== id)
}

function getPreview(msg: string): string {
  if (!msg) return '(空)'
  return msg.length > 30 ? msg.substring(0, 30) + '...' : msg
}

function renderMarkdown(text: string): string {
  // Simple markdown: bold, italic, code, line breaks
  return text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.ai-chat-page { height: calc(100vh - 120px); }
.chat-layout { display: flex; height: 100%; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden; }
.conv-sidebar { width: 240px; border-right: 1px solid #e4e7ed; display: flex; flex-direction: column; background: #fafafa; }
.sidebar-header { display: flex; justify-content: space-between; align-items: center; padding: 12px; border-bottom: 1px solid #e4e7ed; }
.conv-list { flex: 1; overflow-y: auto; }
.conv-item {
  padding: 10px 12px; border-bottom: 1px solid #f0f0f0; cursor: pointer; position: relative;
}
.conv-item:hover { background: #f0f5ff; }
.conv-item.active { background: #ecf5ff; }
.conv-preview { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; padding-right: 20px; }
.conv-meta { font-size: 11px; color: #909399; margin-top: 4px; }
.conv-delete { position: absolute; right: 4px; top: 8px; opacity: 0; }
.conv-item:hover .conv-delete { opacity: 1; }
.chat-main { flex: 1; display: flex; flex-direction: column; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; }
.chat-welcome { text-align: center; padding: 60px 20px; color: #909399; }
.chat-welcome h3 { color: #303133; }
.chat-welcome ul { text-align: left; max-width: 300px; margin: 12px auto; }
.chat-welcome .hint { font-size: 12px; margin-top: 24px; }
.message-row { display: flex; gap: 10px; margin-bottom: 16px; }
.message-row.user { flex-direction: row-reverse; }
.msg-avatar { flex-shrink: 0; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: #f0f0f0; }
.message-row.user .msg-avatar { background: #409eff; color: #fff; }
.msg-bubble { max-width: 70%; padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.6; }
.message-row.user .msg-bubble { background: #409eff; color: #fff; border-bottom-right-radius: 4px; }
.message-row.assistant .msg-bubble { background: #f5f7fa; border-bottom-left-radius: 4px; }
.msg-bubble :deep(code) { background: rgba(0,0,0,0.08); padding: 2px 5px; border-radius: 3px; font-size: 13px; }
.file-refs { padding: 6px 16px; display: flex; flex-wrap: wrap; gap: 6px; border-top: 1px solid #e4e7ed; }
.chat-input { padding: 12px; border-top: 1px solid #e4e7ed; }
.input-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
.file-pick-list { max-height: 300px; overflow-y: auto; }
.file-pick-item { display: flex; align-items: center; gap: 8px; padding: 8px; cursor: pointer; border-radius: 4px; }
.file-pick-item:hover { background: #f5f7fa; }
.file-pick-item.picked { background: #ecf5ff; color: #409eff; }
.thinking-dots span { display: inline-block; animation: dot-bounce 1.4s infinite both; font-size: 24px; line-height: 1; }
.thinking-dots span:nth-child(1) { animation-delay: 0s; }
.thinking-dots span:nth-child(2) { animation-delay: 0.2s; }
.thinking-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes dot-bounce { 0%,80%,100% { opacity: 0; transform: translateY(0); } 40% { opacity: 1; transform: translateY(-4px); } }
</style>
