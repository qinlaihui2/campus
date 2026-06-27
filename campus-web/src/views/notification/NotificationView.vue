<template>
  <div class="notif-page">
    <div class="notif-container">
      <div class="notif-header">
        <h2>消息通知</h2>
        <el-button v-if="unreadCount > 0" text type="primary" @click="handleReadAll">全部已读</el-button>
      </div>

      <div class="notif-list" v-loading="loading">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notif-item"
          :class="{ unread: !item.isRead }"
          @click="handleClick(item)"
        >
          <div class="notif-icon">
            <el-icon v-if="item.type === 'LIKE'" color="#ff4757"><StarFilled /></el-icon>
            <el-icon v-else-if="item.type === 'COMMENT'" color="#409eff"><ChatDotSquare /></el-icon>
            <el-icon v-else-if="item.type === 'REPLY'" color="#2ed573"><ChatLineSquare /></el-icon>
            <el-icon v-else-if="item.type === 'OFFER'" color="#ffa502"><Money /></el-icon>
            <el-icon v-else color="#909399"><Bell /></el-icon>
          </div>
          <div class="notif-body">
            <div class="notif-title">
              <span v-if="!item.isRead" class="unread-dot" />
              {{ item.title }}
            </div>
            <div class="notif-content">{{ item.content }}</div>
            <div class="notif-time">{{ timeAgo(item.createdAt) }}</div>
          </div>
          <el-button
            class="notif-delete"
            text
            size="small"
            @click.stop="handleDelete(item)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>

        <el-empty v-if="!loading && notifications.length === 0" description="暂无通知" />
      </div>

      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          background
          layout="prev, pager, next"
          @current-change="fetchNotifications"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { NotificationVO } from '@/api/notification'
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead, deleteNotification } from '@/api/notification'

const router = useRouter()
const notifications = ref<NotificationVO[]>([])
const unreadCount = ref(0)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const loading = ref(false)

async function fetchNotifications() {
  loading.value = true
  try {
    const res = await getNotifications(currentPage.value, pageSize)
    notifications.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function fetchUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data.count
  } catch { /* handled */ }
}

async function handleClick(item: NotificationVO) {
  if (!item.isRead) {
    await markAsRead(item.id)
    item.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  // Navigate based on target type
  const routes: Record<string, string> = {
    POST: '/square',
    MARKET: '/market',
    MESSAGE: '/messages',
    COURSE: '/courses',
  }
  const base = routes[item.targetType]
  if (base && item.targetId) {
    router.push(`${base}/${item.targetId}`)
  }
}

async function handleReadAll() {
  await markAllAsRead()
  notifications.value.forEach(n => (n.isRead = true))
  unreadCount.value = 0
}

async function handleDelete(item: NotificationVO) {
  await deleteNotification(item.id)
  notifications.value = notifications.value.filter(n => n.id !== item.id)
  total.value--
}

function timeAgo(dateStr: string) {
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins} 分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  return new Date(dateStr).toLocaleDateString()
}

onMounted(() => {
  fetchNotifications()
  fetchUnreadCount()
})
</script>

<style scoped>
.notif-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 40px 0;
}
.notif-container {
  max-width: 680px;
  margin: 0 auto;
  padding: 0 20px;
}
.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.notif-header h2 {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
.notif-list {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.notif-item {
  display: flex;
  align-items: flex-start;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f0f0f0;
}
.notif-item:last-child { border-bottom: none; }
.notif-item:hover { background: #fafafa; }
.notif-item.unread { background: #f0f5ff; }
.notif-icon {
  margin-right: 14px;
  margin-top: 2px;
  font-size: 20px;
}
.notif-body { flex: 1; min-width: 0; }
.notif-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  flex-shrink: 0;
}
.notif-content {
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notif-time {
  font-size: 12px;
  color: #c0c4cc;
}
.notif-delete {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
}
.notif-item:hover .notif-delete { opacity: 1; }
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
