<template>
  <div class="carousel-wrapper" v-if="slides.length > 0">
    <el-carousel :interval="5000" arrow="hover" height="320px" indicator-position="outside">
      <el-carousel-item v-for="item in slides" :key="item.id" @click="goDetail(item.id)">
        <div class="carousel-slide" :style="{ backgroundImage: `url(${getImageUrl(item.coverImage)})` }">
          <div class="carousel-overlay">
            <div class="carousel-category">
              <el-tag :type="categoryTag(item.category)" size="small">{{ categoryLabel(item.category) }}</el-tag>
              <el-tag v-if="item.priority === 2" type="danger" size="small">紧急</el-tag>
              <el-tag v-if="item.priority === 1" type="warning" size="small">重要</el-tag>
            </div>
            <h2>{{ item.title }}</h2>
            <p>{{ item.summary }}</p>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>
  </div>
</template>

<script setup lang="ts">
import type { Announcement } from '@/api/announcement'

defineProps<{ slides: Announcement[] }>()

const emit = defineEmits<{ (e: 'detail', id: number): void }>()

function goDetail(id: number) {
  emit('detail', id)
}

function getImageUrl(coverImage: string) {
  // If it's already a full URL, return as-is; otherwise prepend API proxy path
  if (coverImage.startsWith('http')) return coverImage
  return `/api/files/${coverImage}`
}

function categoryLabel(category: string) {
  const map: Record<string, string> = {
    NOTICE: '通知公告',
    ACTIVITY: '学术活动',
    EXAM: '考试信息',
    COURSE: '选课通知',
  }
  return map[category] || category
}

function categoryTag(category: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, string> = {
    NOTICE: '',
    ACTIVITY: 'success',
    EXAM: 'danger',
    COURSE: 'warning',
  }
  return (map[category] || 'info') as any
}
</script>

<style scoped>
.carousel-wrapper {
  margin-bottom: 30px;
  border-radius: 12px;
  overflow: hidden;
}
.carousel-slide {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  cursor: pointer;
  position: relative;
}
.carousel-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 60px 40px 30px;
  background: linear-gradient(transparent, rgba(0,0,0,0.7));
  color: #fff;
}
.carousel-overlay h2 {
  margin: 8px 0 4px;
  font-size: 22px;
}
.carousel-overlay p {
  font-size: 14px;
  opacity: 0.85;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.carousel-category {
  display: flex;
  gap: 6px;
}
</style>
