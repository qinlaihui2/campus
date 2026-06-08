<template>
  <div class="message-layout">
    <!-- 左侧：会话列表 + 搜索 -->
    <aside class="msg-sidebar">
      <div class="sidebar-header">
        <h2>消息</h2>
      </div>

      <!-- 用户搜索 -->
      <div class="search-box">
        <el-input v-model="searchKeyword" placeholder="搜索用户名..." size="small" clearable
          @keyup.enter="handleSearch" @clear="searchResults = []">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <!-- 搜索结果 -->
      <div v-if="searchResults.length > 0" class="search-results">
        <div v-for="u in searchResults" :key="u.id" class="search-item" @click="startChat(u)">
          <el-avatar :size="36" :src="getAvatarUrl(u.avatar)">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <div class="search-info">
            <div class="search-name">{{ u.nickname || u.username }}</div>
            <div class="search-role">{{ u.role }}</div>
          </div>
        </div>
        <div class="search-cancel" @click="searchResults = []">取消</div>
      </div>

      <!-- 会话列表 -->
      <div class="conversation-list">
        <div v-for="conv in conversations" :key="conv.id" class="conv-item"
          :class="{ active: currentConv?.id === conv.id }" @click="openConversation(conv)">
          <el-avatar :size="40" :src="getAvatarUrl(conv.peerAvatar)">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <div class="conv-info">
            <div class="conv-end">{{ getPeerName(conv) }}</div>
            <div class="conv-preview">{{ conv.lastMessage || '' }}</div>
          </div>
          <div class="conv-meta">
            <div class="conv-time">{{ formatTime(conv.lastMessageAt) }}</div>
            <span v-if="getUnread(conv)" class="conv-badge">{{ getUnread(conv) }}</span>
          </div>
        </div>
        <el-empty v-if="conversations.length === 0 && searchResults.length === 0" description="暂无消息" :image-size="60" />
      </div>
    </aside>

    <!-- 右侧：聊天面板 -->
    <main class="msg-main">
      <template v-if="currentConv">
        <div class="chat-header">
          <span class="chat-peer">{{ getPeerName(currentConv) }}</span>
          <el-button text type="danger" size="small" @click="handleDelete">删除会话</el-button>
        </div>

        <div class="chat-messages" ref="chatContainer">
          <div v-for="msg in messages" :key="msg.id" class="chat-bubble"
            :class="msg.senderId === userId ? 'mine' : 'other'">
            <div class="bubble-text" v-if="msg.type === 'TEXT'">{{ msg.content }}</div>
            <img v-else :src="getImageUrl(msg.content)" class="bubble-image" @click="previewImage(msg.content)" />
          </div>
          <div v-if="messages.length === 0" class="empty-chat">发送第一条消息吧</div>
        </div>

        <div class="chat-input">
          <div class="emoji-panel" v-if="emojiPickerVisible">
            <EmojiPicker @select="insertEmoji" />
          </div>
          <el-button text class="emoji-btn" @click="emojiPickerVisible = !emojiPickerVisible">
            😊
          </el-button>
          <el-upload :show-file-list="false" :before-upload="handleImageSend" accept="image/*"
            style="display:inline-block">
            <el-button circle size="small"><el-icon><PictureFilled /></el-icon></el-button>
          </el-upload>
          <el-input v-model="inputText" placeholder="输入消息..." @keyup.enter.exact="handleSend" />
          <el-button type="primary" size="small" :disabled="!inputText.trim()" @click="handleSend">发送</el-button>
        </div>
      </template>
      <div v-else class="no-conversation">
        <el-icon :size="60" color="#ccc"><ChatLineSquare /></el-icon>
        <p>选择一个会话开始聊天</p>
        <p class="sub">或在左侧搜索用户发起新对话</p>
      </div>
    </main>

    <!-- 图片预览 -->
    <el-dialog v-model="previewVisible" width="600px" :show-close="true">
      <img :src="previewUrl" style="width:100%" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getConversations, getMessages, sendMessage, deleteConversation,
  searchUser, type Conversation, type Message, type UserBrief,
} from '@/api/message'
import { uploadImage } from '@/api/announcement'
import EmojiPicker from '@/components/EmojiPicker.vue'
import type { Emoji } from '@/api/emoji'

const userId = ref<number>(0)
const conversations = ref<Conversation[]>([])
const currentConv = ref<Conversation | null>(null)
const messages = ref<Message[]>([])
const inputText = ref('')
const searchKeyword = ref('')
const searchResults = ref<UserBrief[]>([])
const chatContainer = ref<HTMLElement>()
const previewVisible = ref(false)
const previewUrl = ref('')

let pollTimer: number | null = null

onMounted(() => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
      userId.value = payload.userId
    } catch { }
  }
  loadConversations()
  pollTimer = window.setInterval(loadConversations, 10000)
})

watch(() => messages.value.length, () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
})

async function loadConversations() {
  try {
    const res = await getConversations()
    conversations.value = res.data.records
  } catch { }
}

async function openConversation(conv: Conversation) {
  currentConv.value = conv
  try {
    const res = await getMessages(conv.id)
    messages.value = (res.data.records || []).reverse()
    await nextTick()
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  } catch { }
}

async function handleSearch() {
  if (!searchKeyword.value.trim()) return
  try {
    const res = await searchUser(searchKeyword.value.trim())
    searchResults.value = res.data || []
  } catch { }
}

async function startChat(u: UserBrief) {
  // Create conversation by sending first message... no, findOrCreate then open
  // We'll just open it via the send endpoint - first send a placeholder, then load
  // Actually the API findOrCreateConversation & send are separate.
  // For now, just navigate: send a small message to initiate
  searchResults.value = []
  searchKeyword.value = ''
  try {
    await sendMessage(u.id, '你好，我想和你聊一下')
    await loadConversations()
    // Open the newly created conversation
    const conv = conversations.value.find(
      c => (c.user1Id === u.id && c.user2Id === userId.value) ||
           (c.user2Id === u.id && c.user1Id === userId.value)
    )
    if (conv) openConversation(conv)
  } catch { }
}

async function handleSend() {
  if (!inputText.value.trim() || !currentConv.value) return
  const peerId = currentConv.value.user1Id === userId.value
    ? currentConv.value.user2Id : currentConv.value.user1Id
  try {
    await sendMessage(peerId, inputText.value.trim())
    inputText.value = ''
    await openConversation(currentConv.value)
  } catch { }
}

async function handleImageSend(file: File) {
  try {
    const res = await uploadImage(file)
    const url = res.data
    if (currentConv.value) {
      const peerId = currentConv.value.user1Id === userId.value
        ? currentConv.value.user2Id : currentConv.value.user1Id
      await sendMessage(peerId, url, 'IMAGE')
      await openConversation(currentConv.value)
    }
  } catch { ElMessage.error('图片发送失败') }
  return false
}

async function handleDelete() {
  if (!currentConv.value) return
  try {
    await deleteConversation(currentConv.value.id)
    currentConv.value = null
    messages.value = []
    ElMessage.success('已删除')
    await loadConversations()
  } catch { }
}

function getAvatarUrl(avatar: string | null | undefined) {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return `/api/files/${avatar}`
}

function getPeerName(conv: Conversation) {
  return conv.peerName || `用户${getPeerId(conv)}`
}

function getPeerId(conv: Conversation) {
  return conv.user1Id === userId.value ? conv.user2Id : conv.user1Id
}

function getUnread(_conv: Conversation) {
  // Can't know from current data model - would need server to include it
  return null
}

function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function getImageUrl(url: string) {
  if (url.startsWith('http')) return url
  return `/api/files/${url}`
}

function previewImage(url: string) {
  previewUrl.value = getImageUrl(url)
  previewVisible.value = true
}

// 表情包
const emojiPickerVisible = ref(false)
function insertEmoji(emoji: Emoji) {
  if (emoji.emojiChar) {
    inputText.value += emoji.emojiChar
  } else if (emoji.imageUrl) {
    const url = emoji.imageUrl.startsWith('http') ? emoji.imageUrl : `/api/files/${emoji.imageUrl}`
    inputText.value += `[${emoji.name}](${url})`
  }
  emojiPickerVisible.value = false
}
</script>

<style scoped>
.message-layout {
  display: flex;
  height: 100vh;
  background: #fff;
}
.msg-sidebar {
  width: 300px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}
.sidebar-header {
  padding: 16px 16px 8px;
}
.sidebar-header h2 { font-size: 18px; margin: 0; }
.search-box { padding: 8px 12px; }
.search-results {
  border-bottom: 1px solid #e4e7ed;
  padding: 4px 8px;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
}
.search-item:hover { background: #e6f0ff; }
.search-info { flex: 1; }
.search-name { font-size: 14px; }
.search-role { font-size: 12px; color: #999; }
.search-cancel {
  text-align: center;
  padding: 6px;
  color: #999;
  cursor: pointer;
  font-size: 13px;
}
.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}
.conv-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
}
.conv-item:hover, .conv-item.active { background: #e6f0ff; }
.conv-info { flex: 1; overflow: hidden; }
.conv-end { font-size: 14px; }
.conv-preview {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv-meta { text-align: right; }
.conv-time { font-size: 11px; color: #bbb; }
.conv-badge {
  display: inline-block;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
}
.msg-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}
.chat-peer { font-size: 16px; font-weight: 500; }
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.chat-bubble { display: flex; }
.chat-bubble.mine { justify-content: flex-end; }
.bubble-text {
  max-width: 60%;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
}
.chat-bubble.mine .bubble-text {
  background: #409eff;
  color: #fff;
}
.chat-bubble.other .bubble-text {
  background: #f0f0f0;
  color: #333;
}
.bubble-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  cursor: pointer;
}
.empty-chat { text-align: center; color: #ccc; padding-top: 80px; }
.emoji-btn {
  font-size: 22px;
  padding: 4px 6px;
  line-height: 1;
}
.emoji-panel {
  position: absolute;
  bottom: 100%;
  left: 12px;
  margin-bottom: 8px;
  z-index: 1000;
}
.chat-input {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid #eee;
}
.no-conversation {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  gap: 8px;
}
.no-conversation .sub { font-size: 13px; }
</style>
