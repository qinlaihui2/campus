<template>
  <div class="lf-detail-page">
    <div class="detail-container">
      <el-button text @click="$router.push('/lost-found')" style="margin-bottom:16px;">
        <el-icon><ArrowLeft /></el-icon> 返回列表
      </el-button>

      <div class="detail-card" v-loading="loading">
        <!-- 图片 -->
        <div class="detail-images" v-if="images.length > 0">
          <img v-for="(url, i) in images" :key="i" :src="getImageUrl(url)" @click="preview(i)" />
        </div>

        <div class="detail-body">
          <div class="detail-top">
            <el-tag :type="detail.type === 'LOST' ? 'danger' : 'success'">{{ detail.type === 'LOST' ? '寻物' : '拾到' }}</el-tag>
            <el-tag v-if="detail.status === 'RESOLVED'" type="info">已完结</el-tag>
            <el-tag>{{ detail.category }}</el-tag>
          </div>
          <h1>{{ detail.title }}</h1>
          <div class="detail-meta">
            <span><el-icon><Location /></el-icon> {{ detail.location || '未知地点' }}</span>
            <span>{{ formatDate(detail.publishedAt) }}</span>
          </div>
          <p class="detail-desc">{{ detail.description }}</p>

          <!-- 发布者 -->
          <div class="detail-publisher">
            <el-avatar :size="40" :src="getImageUrl(detail.publisherAvatar || '')">
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
            <span>{{ detail.publisherName }}</span>
          </div>

          <!-- 操作 -->
          <div class="detail-actions">
            <el-button v-if="!isOwner && detail.status === 'OPEN'" type="primary" @click="contactPublisher">
              <el-icon><ChatDotRound /></el-icon> 联系TA
            </el-button>
            <el-button v-if="isOwner && detail.status === 'OPEN'" type="success" @click="handleResolve">
              标记已完结
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览 -->
    <el-image-viewer v-if="previewVisible" :url-list="images.map(getImageUrl)" :initial-index="previewIndex" @close="previewVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPostDetail, resolvePost, type LostFoundPost } from '@/api/lostfound'
import { sendMessage } from '@/api/message'

const route = useRoute()
const router = useRouter()
const detail = ref<LostFoundPost>({} as LostFoundPost)
const loading = ref(true)
const previewVisible = ref(false)
const previewIndex = ref(0)

const userId = computed(() => {
  try {
    const token = localStorage.getItem('accessToken')
    if (!token) return 0
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.userId || 0
  } catch { return 0 }
})
const isOwner = computed(() => detail.value.publisherId === userId.value)
const images = computed(() => {
  if (!detail.value.imageUrls) return []
  return detail.value.imageUrls.split(',').filter(Boolean)
})

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    const res = await getPostDetail(id)
    detail.value = res.data
  } finally { loading.value = false }
})

async function contactPublisher() {
  try {
    await sendMessage(detail.value.publisherId, `你好，我在失物招领看到了你发布的"${detail.value.title}"`)
    ElMessage.success('已发送消息')
    router.push('/messages')
  } catch { ElMessage.error('发送失败') }
}

async function handleResolve() {
  try {
    await ElMessageBox.confirm('确定标记为已完结？', '提示', { type: 'info' })
    await resolvePost(detail.value.id)
    detail.value.status = 'RESOLVED'
    ElMessage.success('已标记完结')
  } catch { /* cancel */ }
}

function getImageUrl(url: string) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return `/api/files/${url}`
}

function formatDate(d: string) {
  if (!d) return ''
  return d.substring(0, 16)
}

function preview(i: number) {
  previewIndex.value = i
  previewVisible.value = true
}
</script>

<style scoped>
.lf-detail-page { min-height: 100vh; background: #f5f7fa; padding: 24px 0 60px; }
.detail-container { max-width: 700px; margin: 0 auto; padding: 0 20px; }
.detail-card { background: #fff; border-radius: 12px; overflow: hidden; }
.detail-images { display: flex; gap: 8px; overflow-x: auto; padding: 16px 20px 0; }
.detail-images img { width: 160px; height: 120px; object-fit: cover; border-radius: 8px; cursor: pointer; }
.detail-body { padding: 20px 24px 24px; }
.detail-top { display: flex; gap: 6px; margin-bottom: 12px; }
.detail-body h1 { font-size: 22px; margin: 0 0 10px; }
.detail-meta { display: flex; gap: 16px; color: #999; font-size: 13px; margin-bottom: 16px; }
.detail-desc { font-size: 15px; line-height: 1.8; color: #333; }
.detail-publisher { display: flex; align-items: center; gap: 10px; padding: 16px 0; border-top: 1px solid #eee; margin-top: 20px; }
.detail-actions { margin-top: 16px; display: flex; gap: 10px; }
</style>
