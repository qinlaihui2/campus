<template>
  <div class="announcement-detail-page">
    <div class="detail-container">
      <el-button text @click="$router.push('/announcements')" style="margin-bottom: 16px;">
        <el-icon><ArrowLeft /></el-icon> 返回公告列表
      </el-button>

      <div class="detail-card" v-loading="loading">
        <!-- 封面图 -->
        <div class="detail-cover" v-if="detail.coverImage">
          <img :src="getImageUrl(detail.coverImage)" :alt="detail.title" />
        </div>

        <!-- 头部信息 -->
        <div class="detail-header">
          <h1>{{ detail.title }}</h1>
          <div class="detail-meta">
            <el-tag :type="categoryTag(detail.category)">{{ categoryLabel(detail.category) }}</el-tag>
            <el-tag v-if="detail.priority === 2" type="danger">紧急</el-tag>
            <el-tag v-if="detail.priority === 1" type="warning">重要</el-tag>
            <span class="detail-date">发布于 {{ formatDate(detail.publishedAt) }}</span>
          </div>
          <p class="detail-summary" v-if="detail.summary">{{ detail.summary }}</p>
        </div>

        <!-- 正文 -->
        <div class="detail-content" v-html="detail.content"></div>

        <!-- 附件 -->
        <div class="detail-attachments" v-if="attachments.length > 0">
          <h3>相关附件</h3>
          <div v-for="att in attachments" :key="att.id" class="attachment-item">
            <el-icon><Document /></el-icon>
            <span>{{ att.fileName }}</span>
            <span class="file-size">{{ formatSize(att.fileSize) }}</span>
            <a :href="`/api/announcements/attachments/${att.id}/download`" download>下载</a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getAnnouncementDetail, getAttachments, type Announcement, type AnnouncementAttachment } from '@/api/announcement'

const route = useRoute()
const detail = ref<Announcement>({} as Announcement)
const attachments = ref<AnnouncementAttachment[]>([])
const loading = ref(true)

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    const res = await getAnnouncementDetail(id)
    detail.value = res.data
    const attRes = await getAttachments(id)
    attachments.value = attRes.data || []
  } finally {
    loading.value = false
  }
})

function getImageUrl(coverImage: string) {
  if (coverImage.startsWith('http')) return coverImage
  return `/api/files/${coverImage}`
}
function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}
function formatSize(bytes: number) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
function categoryLabel(category: string) {
  const map: Record<string, string> = { NOTICE: '通知公告', ACTIVITY: '学术活动', EXAM: '考试信息', COURSE: '选课通知' }
  return map[category] || category
}
function categoryTag(category: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, string> = { NOTICE: '', ACTIVITY: 'success', EXAM: 'danger', COURSE: 'warning' }
  return (map[category] || 'info') as any
}
</script>

<style scoped>
.announcement-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px 0 60px;
}
.detail-container {
  max-width: 860px;
  margin: 0 auto;
  padding: 0 20px;
}
.detail-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}
.detail-cover img {
  width: 100%;
  max-height: 400px;
  object-fit: cover;
}
.detail-header {
  padding: 30px 30px 20px;
}
.detail-header h1 {
  font-size: 26px;
  margin: 0 0 12px;
}
.detail-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.detail-date { color: #999; font-size: 13px; }
.detail-summary {
  color: #666;
  font-size: 15px;
  line-height: 1.6;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border-left: 3px solid #409eff;
}
.detail-content {
  padding: 0 30px 30px;
  font-size: 15px;
  line-height: 1.8;
  color: #333;
}
.detail-content :deep(img) { max-width: 100%; }
.detail-attachments {
  padding: 20px 30px;
  border-top: 1px solid #eee;
}
.detail-attachments h3 { margin: 0 0 12px; font-size: 16px; }
.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f9fafb;
  border-radius: 6px;
  margin-bottom: 6px;
}
.file-size { color: #999; font-size: 12px; flex: 1; }
.attachment-item a { color: #409eff; text-decoration: none; font-size: 13px; }
</style>
