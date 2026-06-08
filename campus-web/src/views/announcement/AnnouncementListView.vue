<template>
  <div class="announcement-list-page">
    <div class="announcement-container">
      <!-- 轮播图 -->
      <AnnouncementCarousel :slides="carouselData" @detail="goDetail" />

      <!-- 分类 tabs -->
      <div class="category-tabs">
        <span
          v-for="cat in categories"
          :key="cat.value"
          :class="{ active: activeCategory === cat.value }"
          @click="switchCategory(cat.value)"
        >
          {{ cat.label }}
        </span>
      </div>

      <!-- 公告列表 -->
      <div class="announcement-grid">
        <div v-for="item in list" :key="item.id" class="announcement-card" @click="goDetail(item.id)">
          <div class="card-cover" :style="{ backgroundImage: `url(${getImageUrl(item.coverImage)})` }">
            <el-tag v-if="item.priority === 2" type="danger" size="small" class="priority-badge">紧急</el-tag>
          </div>
          <div class="card-body">
            <div class="card-meta">
              <el-tag :type="categoryTag(item.category)" size="small">{{ categoryLabel(item.category) }}</el-tag>
              <span class="card-date">{{ formatDate(item.publishedAt) }}</span>
            </div>
            <h3 class="card-title">{{ item.title }}</h3>
            <p class="card-summary">{{ item.summary || '暂无摘要' }}</p>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          background
          layout="prev, pager, next"
          @current-change="fetchList"
        />
      </div>

      <el-empty v-if="!loading && list.length === 0" description="暂无公告" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAnnouncements, getCarousel, type Announcement } from '@/api/announcement'
import AnnouncementCarousel from '@/components/AnnouncementCarousel.vue'

const router = useRouter()

const categories = [
  { label: '全部', value: '' },
  { label: '通知公告', value: 'NOTICE' },
  { label: '学术活动', value: 'ACTIVITY' },
  { label: '考试信息', value: 'EXAM' },
  { label: '选课通知', value: 'COURSE' },
]

const activeCategory = ref('')
const list = ref<Announcement[]>([])
const carouselData = ref<Announcement[]>([])
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

onMounted(() => {
  fetchCarousel()
  fetchList()
})

async function fetchCarousel() {
  try {
    const res = await getCarousel()
    carouselData.value = res.data || []
  } catch { /* ignore */ }
}

async function fetchList() {
  loading.value = true
  try {
    const category = activeCategory.value || undefined
    const res = await getAnnouncements(category, currentPage.value, pageSize)
    list.value = res.data.records
    total.value = res.data.total
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function switchCategory(cat: string) {
  activeCategory.value = cat
  currentPage.value = 1
  fetchList()
}

function goDetail(id: number) {
  router.push(`/announcements/${id}`)
}

function getImageUrl(coverImage: string) {
  if (coverImage.startsWith('http')) return coverImage
  return `/api/files/${coverImage}`
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
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
.announcement-list-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px 0 60px;
}
.announcement-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 20px;
}
.category-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
.category-tabs span {
  flex: 1;
  text-align: center;
  padding: 14px 0;
  cursor: pointer;
  color: #666;
  font-size: 15px;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}
.category-tabs span:hover { color: #409eff; }
.category-tabs span.active {
  color: #409eff;
  border-bottom-color: #409eff;
  font-weight: 500;
}
.announcement-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}
.announcement-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.announcement-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}
.card-cover {
  height: 160px;
  background-size: cover;
  background-position: center;
  background-color: #e0e6ed;
  position: relative;
}
.priority-badge { position: absolute; top: 8px; right: 8px; }
.card-body { padding: 16px; }
.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-date { color: #999; font-size: 12px; }
.card-title {
  font-size: 16px;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-summary {
  font-size: 13px;
  color: #888;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
