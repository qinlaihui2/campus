<template>
  <div class="course-page">
    <div class="course-container">
      <div class="page-header">
        <h1>课程学习</h1>
        <p>精选优质课程，随时随地学习新知识</p>
      </div>

      <!-- 分类 -->
      <div class="category-tabs">
        <span v-for="c in categories" :key="c.value"
          :class="{ active: activeCategory === c.value }" @click="switchCategory(c.value)">
          {{ c.label }}
        </span>
      </div>

      <!-- 课程卡片网格 -->
      <div class="course-grid">
        <div v-for="course in courses" :key="course.id" class="course-card" @click="goDetail(course.id)">
          <div class="card-cover">
            <img v-if="course.coverImage" :src="getImageUrl(course.coverImage)" />
            <div v-else class="cover-placeholder">
              <el-icon :size="40"><VideoCamera /></el-icon>
            </div>
            <div class="cover-duration" v-if="course.chapterCount">{{ course.chapterCount }}课时</div>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ course.title }}</h3>
            <p class="card-desc">{{ course.description || '暂无简介' }}</p>
            <div class="card-meta">
              <span class="meta-instructor">{{ course.instructor || '未知讲师' }}</span>
              <span class="meta-stats">
                <el-icon><View /></el-icon> {{ formatNum(course.viewCount) }}
                <el-icon style="margin-left:8px"><Star /></el-icon> {{ formatNum(course.likeCount) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total"
          background layout="prev, pager, next" @current-change="fetchCourses" />
      </div>

      <el-empty v-if="!loading && courses.length === 0" description="暂无课程" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCourses, type CourseVO } from '@/api/course'
import { assetUrl } from '@/utils/assetUrl'

const router = useRouter()
const categories = [
  { label: '全部', value: '' },
  { label: '计算机', value: '计算机' },
  { label: '外语', value: '外语' },
  { label: '数学', value: '数学' },
  { label: '文学', value: '文学' },
  { label: '其他', value: '其他' },
]

const activeCategory = ref('')
const courses = ref<CourseVO[]>([])
const currentPage = ref(1)
const pageSize = 12
const total = ref(0)
const loading = ref(false)

onMounted(() => fetchCourses())

async function fetchCourses() {
  loading.value = true
  try {
    const res = await getCourses({
      category: activeCategory.value || undefined,
      page: currentPage.value,
      size: pageSize,
    })
    courses.value = res.data.records
    total.value = res.data.total
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function switchCategory(cat: string) {
  activeCategory.value = cat
  currentPage.value = 1
  fetchCourses()
}

function goDetail(id: number) {
  router.push(`/courses/${id}`)
}

function getImageUrl(url: string) {
  return assetUrl(`/api/files/${url}`)
}

function formatNum(n: number) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
</script>

<style scoped>
.course-page {
  min-height: 100vh;
  background: #f5f6f7;
  padding-bottom: 60px;
}
.course-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}
.page-header {
  text-align: center;
  padding: 32px 0 20px;
}
.page-header h1 { font-size: 26px; color: #18191c; margin: 0 0 6px; }
.page-header p { font-size: 14px; color: #9499a0; margin: 0; }

.category-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
.category-tabs span {
  flex: 1; text-align: center; padding: 14px 0; cursor: pointer;
  color: #61666d; font-size: 14px; border-bottom: 2px solid transparent; transition: all .2s;
}
.category-tabs span:hover { color: #00aeec; }
.category-tabs span.active { color: #00aeec; border-bottom-color: #00aeec; font-weight: 500; }

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.course-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform .2s, box-shadow .2s;
}
.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,.08);
}

.card-cover {
  position: relative;
  height: 160px;
  background: #e3e5e7;
  overflow: hidden;
}
.card-cover img {
  width: 100%; height: 100%; object-fit: cover;
}
.cover-placeholder {
  display: flex; align-items: center; justify-content: center;
  height: 100%; color: #c9cdd4;
}
.cover-duration {
  position: absolute; bottom: 8px; right: 8px;
  background: rgba(0,0,0,.6); color: #fff; font-size: 12px;
  padding: 2px 8px; border-radius: 4px;
}

.card-body { padding: 14px; }
.card-title {
  font-size: 15px; font-weight: 600; color: #18191c;
  margin: 0 0 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.card-desc {
  font-size: 12px; color: #9499a0; line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  margin: 0 0 10px;
}
.card-meta { display: flex; justify-content: space-between; align-items: center; }
.meta-instructor { font-size: 12px; color: #9499a0; }
.meta-stats { font-size: 12px; color: #9499a0; display: flex; align-items: center; gap: 2px; }

.pagination-wrap { display: flex; justify-content: center; margin-top: 32px; }
</style>
