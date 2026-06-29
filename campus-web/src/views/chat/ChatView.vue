<template>
  <div class="chat-layout">
    <!-- 左侧边栏 -->
    <AppSidebar :show-new-chat="true" :unread-count="unreadCount" @new-chat="startNewChat">
      <template #conversations>
        <div class="sidebar-divider" />
        <div class="conversation-list">
          <div class="conv-list-title">历史会话</div>
          <div v-for="conv in conversations" :key="conv.id" class="conv-item"
            :class="{ active: currentConvId === conv.id }" @click="switchConversation(conv.id)">
            <div class="conv-title">{{ conv.title }}</div>
            <div class="conv-time">{{ formatTime(conv.updatedAt) }}</div>
            <el-button class="conv-delete" text size="small" @click.stop="handleDelete(conv.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <div v-if="conversations.length === 0" class="empty-conv">暂无对话</div>
        </div>
      </template>
    </AppSidebar>

    <!-- 主聊天区域 -->
    <main class="chat-main">
      <!-- 消息列表 -->
      <div ref="messageContainer" class="message-container">
        <div v-if="messages.length === 0" class="welcome">
          <h1>你好，我是小园 👋</h1>
          <p>我是小园，校圈的 AI 助手，可以帮你解答关于校园的任何问题</p>
          <div class="suggestions">
            <div v-for="q in suggestions" :key="q" class="suggestion-item" @click="sendMessage(q)">
              {{ q }}
            </div>
          </div>
        </div>

        <div v-for="msg in messages" :key="msg.id" class="message-item" :class="msg.role">
          <div class="message-avatar">
            <el-avatar v-if="msg.role === 'user'" :size="36" :src="getAvatarUrl(userStore.userInfo?.avatar)">
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
            <el-avatar v-else :size="36" style="background: #409eff">
              <span style="font-size:14px">AI</span>
            </el-avatar>
          </div>
          <div class="message-content">
            <div class="message-text" v-html="renderMarkdown(msg.content)" />
            <div class="message-actions" v-if="msg.role === 'assistant' && msg.id">
              <el-button text size="small" @click="handleFeedback(msg.id, 'like')">
                <el-icon><Select /></el-icon>
              </el-button>
              <el-button text size="small" @click="handleFeedback(msg.id, 'dislike')">
                <el-icon><CloseBold /></el-icon>
              </el-button>
              <el-divider direction="vertical" />
              <el-button text size="small" style="color:#ff4757" @click="openPublishDialog(msg)">
                <el-icon><Share /></el-icon> 发布到广场
              </el-button>
            </div>
          </div>
        </div>

        <!-- AI 工具调用卡片 -->
        <template v-for="(tc, idx) in toolCalls" :key="'tc' + idx">
          <div class="tool-call-card">
            <div class="tool-call-header" @click="tc.collapsed = !tc.collapsed">
              <span class="tool-call-icon">{{ toolIconMap[tc.name] || '🔧' }}</span>
              <span class="tool-call-label">正在查询{{ tc.label }}</span>
              <span class="tool-call-arrow">{{ tc.collapsed ? '▶' : '▼' }}</span>
            </div>
            <div class="tool-call-result" v-show="!tc.collapsed">
              <div class="tool-call-result-text">{{ tc.result }}</div>
            </div>
          </div>
        </template>

        <!-- 流式输出中的消息 -->
        <div v-if="streaming" class="message-item assistant">
          <div class="message-avatar">
            <el-avatar :size="36" style="background: #409eff">AI</el-avatar>
          </div>
          <div class="message-content">
            <div ref="streamingEl" class="message-text" /><span class="cursor">|</span>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <!-- 表情面板 -->
        <div class="emoji-panel" v-if="emojiPickerVisible">
          <EmojiPicker @select="insertEmoji" />
        </div>
        <el-input v-model="inputText" type="textarea" :rows="3" placeholder="输入你的问题，按 Enter 发送..."
          :disabled="streaming" @keyup.enter.exact="sendMessage()" resize="none" />
        <div class="input-actions">
          <el-button text class="emoji-btn" @click="emojiPickerVisible = !emojiPickerVisible">
            😊
          </el-button>
          <span class="input-hint">{{ streaming ? 'AI 正在回复...' : 'Enter 发送' }}</span>
          <el-button type="primary" :disabled="!inputText.trim() || streaming" @click="sendMessage()">
            <el-icon><Position /></el-icon> 发送
          </el-button>
          <el-button v-if="streaming" type="danger" @click="stopStreaming">停止</el-button>
        </div>
      </div>
    </main>

    <!-- 个人信息对话框 -->
    <el-dialog v-model="profileDialogVisible" title="个人信息" width="420px">
      <el-form :model="profileForm" label-width="80px">
        <!-- 头像 -->
        <el-form-item label="头像">
          <div class="avatar-section">
            <el-avatar :size="64" :src="getAvatarUrl(profileForm.avatar)">
              <el-icon :size="32"><UserFilled /></el-icon>
            </el-avatar>
            <el-upload
              :show-file-list="false"
              :before-upload="handleAvatarUpload"
              accept="image/*"
            >
              <el-button size="small" type="primary" plain>上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input :model-value="userStore.userInfo?.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag>{{ userStore.userInfo?.role }}</el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 发布到广场对话框 -->
    <el-dialog v-model="publishDialogVisible" title="发布到问答广场" width="460px">
      <el-form :model="publishForm" label-width="60px">
        <el-form-item label="标题">
          <el-input v-model="publishForm.title" maxlength="200" show-word-limit placeholder="给问答起个标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="publishForm.category" placeholder="选择分类" style="width:100%">
            <el-option label="规章制度" value="规章制度" />
            <el-option label="办事流程" value="办事流程" />
            <el-option label="课程学习" value="课程信息" />
            <el-option label="校园生活" value="校园生活" />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="publish-preview">
        <div class="preview-label">将要发布的内容</div>
        <div class="preview-question">{{ publishForm.question }}</div>
        <div class="preview-answer markdown-body" v-html="renderMarkdown(publishForm.answer.substring(0, 200))" />
      </div>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" style="background:#ff4757;border-color:#ff4757" @click="handlePublish" :loading="publishing">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import AppSidebar from '@/components/AppSidebar.vue'
import { getConversations, getMessages, deleteConversation, feedbackMessage, type Conversation, type Message } from '@/api/chat'
import { getUnreadCount } from '@/api/message'
import { uploadImage } from '@/api/announcement'
import { publishToSquare } from '@/api/square'
import EmojiPicker from '@/components/EmojiPicker.vue'
import type { Emoji } from '@/api/emoji'
import { assetUrl, apiBaseUrl } from '@/utils/assetUrl'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const userStore = useUserStore()

// 会话列表
const conversations = ref<Conversation[]>([])
const currentConvId = ref<number | null>(null)
const messages = ref<Message[]>([])

// 输入
const inputText = ref('')
const streaming = ref(false)
const streamingText = ref('')
const stoppedByUser = ref(false)
const toolCalls = ref<{ name: string; label: string; result: string; collapsed: boolean }[]>([])
const messageContainer = ref<HTMLElement>()
const streamingEl = ref<HTMLElement>()
let abortController: AbortController | null = null

const suggestions = [
  '奖学金申请需要什么条件？',
  '如何办理校园卡挂失？',
  '图书馆借书流程是什么？',
  '补考和重修的区别？',
]

const md: MarkdownIt = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true,
  highlight(str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre><code class="hljs language-${lang}">${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch { }
    }
    return `<pre><code class="hljs">${md.utils.escapeHtml(str)}</code></pre>`
  },
})

function renderMarkdown(text: string) {
  if (!text) return ''
  return md.render(text)
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const unreadCount = ref(0)

onMounted(async () => {
  await userStore.fetchUserInfo()
  await loadConversations()
  fetchUnread()
  setInterval(fetchUnread, 15000)
})

async function fetchUnread() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data || 0
  } catch { }
}

async function loadConversations() {
  try {
    const res = await getConversations()
    conversations.value = res.data.records
  } catch { }
}

async function switchConversation(convId: number) {
  stopStreamingSilently()
  currentConvId.value = convId
  toolCalls.value = []
  try {
    const res = await getMessages(convId)
    messages.value = res.data
    await nextTick()
    scrollToBottom()
  } catch { }
}

function startNewChat() {
  stopStreamingSilently()
  currentConvId.value = null
  messages.value = []
  streamingText.value = ''
  inputText.value = ''
  toolCalls.value = []
}

async function sendMessage(presetQuestion?: string) {
  const question = presetQuestion || inputText.value.trim()
  if (!question || streaming.value) return

  // 添加用户消息到界面
  messages.value.push({
    id: 0,
    conversationId: currentConvId.value || 0,
    role: 'user',
    content: question,
    createdAt: new Date().toISOString(),
  })
  inputText.value = ''
  streaming.value = true
  streamingText.value = ''
  toolCalls.value = []
  stoppedByUser.value = false
  await nextTick()
  scrollToBottom()

  const token = localStorage.getItem('accessToken')
  const params = new URLSearchParams()
  if (currentConvId.value) params.append('conversationId', String(currentConvId.value))
  params.append('question', question)

  abortController = new AbortController()

  try {
    const base = apiBaseUrl()
    const url = `${base}/api/agent/chat?${params.toString()}`
    const headers: Record<string, string> = { Authorization: `Bearer ${token}` }
    if (base) headers['ngrok-skip-browser-warning'] = 'true'

    const response = await fetch(url, {
      method: 'POST',
      headers,
      signal: abortController.signal,
    })

    const reader = response.body?.getReader()
    if (!reader) throw new Error('No reader')

    const decoder = new TextDecoder()
    let buffer = ''
    let metaReceived = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      let eventType = ''
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.substring(6).trim()
          continue
        }
        if (line.startsWith('data:')) {
          const data = line.substring(5).trim()
          if (eventType === 'tool_call') {
            handleToolCall(JSON.parse(data))
          } else if (eventType === 'meta') {
            const parsed = JSON.parse(data)
            if (parsed.conversationId) {
              currentConvId.value = parsed.conversationId
            }
          } else if (eventType === 'error') {
            ElMessage.error(data)
          } else if (data !== 'completed' && data !== '[DONE]') {
            // 流式文本：追加到 reactive + 直接操作 DOM
            streamingText.value += data
            if (streamingEl.value) {
              streamingEl.value.innerHTML = renderMarkdown(streamingText.value)
            }
            scrollToBottom()
            await new Promise(r => setTimeout(r, 25)) // 逐字视觉间隔
          }
          eventType = ''
        }
      }
    }
  } catch (err: any) {
    // 如果已经有 AI 回复内容，只是连接关闭，不弹错误
    if (err.name !== 'AbortError' && !streamingText.value) {
      ElMessage.error('对话失败: ' + err.message)
    }
  } finally {
    try {
      if (currentConvId.value && !stoppedByUser.value) {
        await loadConversations()
        const res = await getMessages(currentConvId.value)
        messages.value = res.data
        await nextTick()
        scrollToBottom()
      }
    } catch { }
    streaming.value = false
    streamingText.value = ''
  }
}

function processSSEEvent(data: string) {
  if (data === 'completed' || data === '[DONE]') return
  try {
    const parsed = JSON.parse(data)
    if (typeof parsed === 'object' && parsed !== null) {
      if (Array.isArray(parsed)) return
      if (parsed.conversationId) {
        currentConvId.value = parsed.conversationId
        return
      }
    }
    streamingText.value += data
    scrollToBottom()
  } catch {
    streamingText.value += data
    scrollToBottom()
  }
  // 直接操作 DOM，绕过 Vue 批量渲染，实现逐字流式
  if (streamingEl.value) {
    streamingEl.value.innerHTML = renderMarkdown(streamingText.value)
  }
}

const toolNameMap: Record<string, string> = {
  searchCourses: '课程',
  searchItems: '二手商品',
  searchLostFound: '失物招领',
  searchSquarePosts: '广场帖子',
  getHotPosts: '热门帖子',
  searchAnnouncements: '公告',
  searchKnowledge: '知识库',
}

const toolIconMap: Record<string, string> = {
  searchCourses: '📚',
  searchItems: '🛒',
  searchLostFound: '🎒',
  searchSquarePosts: '💬',
  getHotPosts: '🔥',
  searchAnnouncements: '📢',
  searchKnowledge: '📖',
}

function handleToolCall(data: any) {
  const label = toolNameMap[data.name] || data.name
  const result = data.result || '查询完成'
  toolCalls.value.push({
    name: data.name,
    label,
    result,
    collapsed: false,
  })
  scrollToBottom()
}

function stopStreamingSilently() {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  streaming.value = false
}

function stopStreaming() {
  stoppedByUser.value = true
  stopStreamingSilently()
  // Save the partial response
  if (streamingText.value) {
    messages.value.push({
      id: 0,
      conversationId: currentConvId.value || 0,
      role: 'assistant',
      content: streamingText.value,
      createdAt: new Date().toISOString(),
    })
    streamingText.value = ''
  }
}

async function handleDelete(convId: number) {
  try {
    await deleteConversation(convId)
    conversations.value = conversations.value.filter(c => c.id !== convId)
    if (currentConvId.value === convId) startNewChat()
    ElMessage.success('已删除')
  } catch { }
}

async function handleFeedback(msgId: number, feedback: string) {
  if (!msgId) return
  try {
    await feedbackMessage(msgId, feedback)
    ElMessage.success('感谢反馈')
  } catch { }
}

const profileDialogVisible = ref(false)
const profileForm = ref({ nickname: '', email: '', phone: '', avatar: '' })

function handleCommand(cmd: string) {
  if (cmd === 'profile') {
    profileForm.value.nickname = userStore.userInfo?.nickname || ''
    profileForm.value.email = userStore.userInfo?.email || ''
    profileForm.value.phone = userStore.userInfo?.phone || ''
    profileForm.value.avatar = userStore.userInfo?.avatar || ''
    profileDialogVisible.value = true
  }
  if (cmd === 'logout') userStore.logout()
}

async function handleAvatarUpload(file: File) {
  try {
    const res = await uploadImage(file)
    profileForm.value.avatar = res.data
    ElMessage.success('头像上传成功')
  } catch { ElMessage.error('上传失败') }
  return false
}

function getAvatarUrl(avatar: string | undefined | null) {
  if (!avatar) return ''
  return assetUrl(`/api/files/${avatar}`)
}

async function saveProfile() {
  try {
    await userStore.updateProfile(profileForm.value)
    ElMessage.success('保存成功')
    profileDialogVisible.value = false
  } catch { }
}

// 发布到广场
const publishDialogVisible = ref(false)
const publishing = ref(false)
const publishForm = ref({ title: '', question: '', answer: '', referencesJson: '', category: '' })

function openPublishDialog(msg: Message) {
  // 找到前一条用户消息
  const msgIndex = messages.value.indexOf(msg)
  const prevMsg = msgIndex > 0 ? messages.value[msgIndex - 1] : null
  publishForm.value = {
    title: prevMsg?.content?.substring(0, 50) || '来自问答广场',
    question: prevMsg?.content || '',
    answer: msg.content,
    referencesJson: msg.referencesJson || '',
    category: '',
  }
  publishDialogVisible.value = true
}

async function handlePublish() {
  if (!publishForm.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  publishing.value = true
  try {
    await publishToSquare({
      title: publishForm.value.title,
      question: publishForm.value.question,
      answer: publishForm.value.answer,
      referencesJson: publishForm.value.referencesJson,
      category: publishForm.value.category || undefined,
    })
    ElMessage.success('发布成功！已分享到问答广场')
    publishDialogVisible.value = false
  } catch { /* handled by interceptor */ }
  finally { publishing.value = false }
}

// 表情包
const emojiPickerVisible = ref(false)
function insertEmoji(emoji: Emoji) {
  if (emoji.emojiChar) {
    inputText.value += emoji.emojiChar
  } else if (emoji.imageUrl) {
    const url = assetUrl(`/api/files/${emoji.imageUrl}`)
    inputText.value += `![${emoji.name}](${url})`
  }
  emojiPickerVisible.value = false
}

function scrollToBottom() {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100vh;
}

.sidebar-divider {
  height: 1px;
  background: #e4e7ed;
  margin: 8px 16px;
}

.conv-list-title {
  font-size: 12px;
  color: #999;
  padding: 8px 12px 4px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conv-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  margin-bottom: 4px;
}

.conv-item:hover,
.conv-item.active {
  background: #e6f0ff;
}

.conv-title {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 24px;
}

.conv-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.conv-delete {
  position: absolute;
  right: 4px;
  top: 8px;
  opacity: 0;
}

.conv-item:hover .conv-delete {
  opacity: 1;
}

.empty-conv {
  text-align: center;
  color: #999;
  padding: 40px 16px;
  font-size: 14px;
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid #e4e7ed;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #303133;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.message-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.welcome {
  text-align: center;
  padding-top: 120px;
}

.welcome h1 {
  font-size: 32px;
  color: #303133;
  margin-bottom: 12px;
}

.welcome p {
  color: #999;
  font-size: 16px;
  margin-bottom: 32px;
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  max-width: 600px;
  margin: 0 auto;
}

.suggestion-item {
  padding: 10px 20px;
  background: #f0f5ff;
  border: 1px solid #d0e0ff;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  color: #409eff;
  transition: all 0.2s;
}

.suggestion-item:hover {
  background: #409eff;
  color: #fff;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  max-width: 800px;
}

.message-item.user {
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-content {
  max-width: 70%;
}

.message-item.user .message-text {
  background: #409eff;
  color: #fff;
  border-radius: 12px 12px 4px 12px;
  padding: 12px 16px;
}

.message-item.assistant .message-text {
  background: #f5f7fa;
  border-radius: 12px 12px 12px 4px;
  padding: 12px 16px;
}

.message-text :deep(pre) {
  background: #282c34;
  color: #abb2bf;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.message-text :deep(code) {
  font-family: 'Fira Code', monospace;
  font-size: 13px;
}

.message-text :deep(p) {
  margin: 4px 0;
}

.message-actions {
  margin-top: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-item:hover .message-actions {
  opacity: 1;
}

.input-area {
  padding: 16px 24px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
}

.input-area :deep(.el-textarea__inner) {
  border-radius: 12px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.input-hint {
  font-size: 12px;
  color: #999;
  margin-right: auto;
}

.cursor {
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.emoji-btn {
  font-size: 22px;
  padding: 4px 8px;
  line-height: 1;
}
.emoji-panel {
  position: absolute;
  bottom: 100%;
  left: 12px;
  margin-bottom: 8px;
  z-index: 1000;
}
.input-area {
  position: relative;
}
.publish-preview {
  background: #fafafa;
  border-radius: 10px;
  padding: 14px;
  margin-top: 8px;
}
.preview-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}
.preview-question {
  font-size: 14px;
  color: #303133;
  margin-bottom: 10px;
  line-height: 1.5;
}
.preview-answer {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  max-height: 120px;
  overflow: hidden;
}

/* ========== Agent 工具调用卡片 ========== */
.tool-call-card {
  margin: 0 0 12px 48px;
  max-width: 600px;
  border: 1px solid #e0e7ff;
  border-radius: 10px;
  background: #f8faff;
  overflow: hidden;
  animation: toolSlideIn 0.3s ease;
}

@keyframes toolSlideIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.tool-call-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
}

.tool-call-header:hover {
  background: #eef2ff;
}

.tool-call-icon {
  font-size: 16px;
}

.tool-call-label {
  flex: 1;
  font-size: 13px;
  color: #4a6cf7;
  font-weight: 500;
}

.tool-call-arrow {
  font-size: 11px;
  color: #999;
}

.tool-call-result {
  padding: 0 14px 12px;
  animation: toolExpand 0.2s ease;
}

@keyframes toolExpand {
  from { opacity: 0; max-height: 0; }
  to { opacity: 1; max-height: 300px; }
}

.tool-call-result-text {
  font-size: 12px;
  color: #666;
  line-height: 1.6;
  background: #fff;
  border-radius: 6px;
  padding: 10px 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}
</style>
