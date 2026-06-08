<template>
  <div class="history-layout">
    <header class="history-header">
      <el-button text @click="$router.push('/')">
        <el-icon><ArrowLeft /></el-icon> 返回对话
      </el-button>
      <h2>历史会话</h2>
    </header>

    <div class="history-list">
      <el-table :data="conversations" style="width: 100%" v-loading="loading" @row-click="viewConversation">
        <el-table-column prop="title" label="会话标题" min-width="300" />
        <el-table-column prop="messageCount" label="消息数" width="100" align="center" />
        <el-table-column label="时间" width="200">
          <template #default="{ row }">
            {{ new Date(row.updatedAt).toLocaleString('zh-CN') }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button text type="danger" size="small" @click.stop="handleDelete(row.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 会话详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="selectedConv?.title" size="600px">
      <div class="conv-messages">
        <div v-for="msg in convMessages" :key="msg.id" class="conv-msg-item" :class="msg.role">
          <div class="msg-role">{{ msg.role === 'user' ? '我' : 'AI助手' }}</div>
          <div class="msg-content" v-html="renderMarkdown(msg.content)" />
          <div class="msg-time">{{ new Date(msg.createdAt).toLocaleString('zh-CN') }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getConversations, getMessages, deleteConversation, type Conversation, type Message } from '@/api/chat'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

const router = useRouter()
const loading = ref(false)
const conversations = ref<Conversation[]>([])

const drawerVisible = ref(false)
const selectedConv = ref<Conversation | null>(null)
const convMessages = ref<Message[]>([])

const md = new MarkdownIt({ breaks: true, linkify: true })

function renderMarkdown(text: string) {
  if (!text) return ''
  return md.render(text)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getConversations(1, 50)
    conversations.value = res.data.records
  } finally {
    loading.value = false
  }
})

async function viewConversation(conv: Conversation) {
  selectedConv.value = conv
  try {
    const res = await getMessages(conv.id)
    convMessages.value = res.data
    drawerVisible.value = true
  } catch { }
}

async function handleDelete(convId: number) {
  try {
    await deleteConversation(convId)
    conversations.value = conversations.value.filter(c => c.id !== convId)
    ElMessage.success('已删除')
  } catch { }
}
</script>

<style scoped>
.history-layout {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
}

.history-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.history-header h2 {
  margin: 0;
  font-size: 20px;
}

.conv-msg-item {
  padding: 16px;
  margin-bottom: 12px;
  border-radius: 8px;
}

.conv-msg-item.user {
  background: #f0f5ff;
}

.conv-msg-item.assistant {
  background: #f5f7fa;
}

.msg-role {
  font-weight: bold;
  margin-bottom: 8px;
  color: #666;
}

.msg-content {
  line-height: 1.6;
}

.msg-time {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
</style>
