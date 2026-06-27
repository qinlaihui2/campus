<template>
  <aside class="app-sidebar">
    <div class="sidebar-header">
      <h2 @click="$router.push('/')" style="cursor:pointer">校圈</h2>
      <el-button v-if="showNewChat" type="primary" size="small" @click="$emit('new-chat')">
        <el-icon><Plus /></el-icon> 新对话
      </el-button>
    </div>

    <nav class="sidebar-nav">
      <router-link to="/" class="nav-item" :class="{ active: $route.path === '/' }">
        <el-icon><ChatDotRound /></el-icon>
        <span>AI 对话</span>
      </router-link>
      <router-link to="/courses" class="nav-item" :class="{ active: $route.path.startsWith('/courses') }">
        <el-icon><VideoCamera /></el-icon>
        <span>课程学习</span>
      </router-link>
      <router-link to="/square" class="nav-item" :class="{ active: $route.path.startsWith('/square') }">
        <el-icon><Grid /></el-icon>
        <span>问答广场</span>
      </router-link>
      <router-link to="/lost-found" class="nav-item" :class="{ active: $route.path.startsWith('/lost-found') }">
        <el-icon><Search /></el-icon>
        <span>失物招领</span>
      </router-link>
      <router-link to="/market" class="nav-item" :class="{ active: $route.path.startsWith('/market') }">
        <el-icon><ShoppingCart /></el-icon>
        <span>二手交易</span>
      </router-link>
      <router-link to="/announcements" class="nav-item" :class="{ active: $route.path.startsWith('/announcements') }">
        <el-icon><Notification /></el-icon>
        <span>公告通知</span>
      </router-link>
      <router-link to="/my" class="nav-item" :class="{ active: $route.path.startsWith('/my') }">
        <el-icon><User /></el-icon>
        <span>我的</span>
      </router-link>
      <router-link to="/messages" class="nav-item" :class="{ active: $route.path === '/messages' }">
        <el-icon><ChatLineSquare /></el-icon>
        <span>消息</span>
        <span v-if="(unreadCount || 0) > 0" class="nav-badge">{{ unreadCount }}</span>
      </router-link>
      <router-link to="/notifications" class="nav-item" :class="{ active: $route.path.startsWith('/notifications') }">
        <el-icon><Bell /></el-icon>
        <span>消息通知</span>
      </router-link>
      <template v-if="isAdmin">
        <div class="nav-divider" />
        <router-link to="/admin/courses" class="nav-item admin-item" :class="{ active: $route.path.startsWith('/admin/courses') }">
          <el-icon><Setting /></el-icon>
          <span>课程管理</span>
        </router-link>
        <router-link to="/admin/announcements" class="nav-item admin-item" :class="{ active: $route.path === '/admin/announcements' }">
          <el-icon><Setting /></el-icon>
          <span>公告管理</span>
        </router-link>
        <router-link to="/knowledge" class="nav-item admin-item" :class="{ active: $route.path.startsWith('/knowledge') }">
          <el-icon><FolderOpened /></el-icon>
          <span>知识库管理</span>
        </router-link>
      </template>
    </nav>

    <!-- Conversation list slot for ChatView -->
    <slot name="conversations" />

    <div class="sidebar-footer">
      <el-dropdown @command="handleCommand">
        <span class="user-info">
          <el-avatar :size="32" :src="avatarUrl">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <span class="username">{{ nickname }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人信息</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { assetUrl } from '@/utils/assetUrl'

const props = defineProps<{
  showNewChat?: boolean
  unreadCount?: number
}>()

defineEmits<{
  'new-chat': []
}>()

const router = useRouter()

const isAdmin = computed(() => {
  try {
    const token = localStorage.getItem('accessToken')
    if (!token) return false
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.role === 'ADMIN'
  } catch { return false }
})

const nickname = computed(() => {
  try {
    const token = localStorage.getItem('accessToken')
    if (!token) return '未登录'
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.nickname || payload.username || '用户'
  } catch { return '用户' }
})

const avatarUrl = computed(() => {
  try {
    const token = localStorage.getItem('accessToken')
    if (!token) return ''
    const payload = JSON.parse(atob(token.split('.')[1]))
    return assetUrl(`/api/files/${payload.avatar}`)
  } catch { return '' }
})

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' }).then(() => {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      router.push('/login')
    }).catch(() => {})
  } else if (cmd === 'profile') {
    router.push('/my')
  }
}
</script>

<style scoped>
.app-sidebar {
  width: 260px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #ebeef5;
  flex-shrink: 0;
  overflow-y: auto;
}
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
}
.sidebar-header h2 {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}
.sidebar-nav {
  padding: 0 12px;
  flex-shrink: 0;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  color: #606266;
  text-decoration: none;
  font-size: 14px;
  transition: background 0.2s;
  margin-bottom: 2px;
}
.nav-item:hover { background: #f5f7fa; color: #303133; }
.nav-item.active { background: #ecf5ff; color: #409eff; font-weight: 500; }
.nav-item .el-icon { font-size: 18px; }
.nav-badge {
  margin-left: auto;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}
.nav-divider {
  height: 1px;
  background: #ebeef5;
  margin: 8px 12px;
}
.admin-item { font-size: 13px; opacity: 0.8; }
.sidebar-footer {
  margin-top: auto;
  padding: 12px 20px;
  border-top: 1px solid #ebeef5;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.username {
  font-size: 13px;
  color: #606266;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
