<template>
  <div class="my-page">
    <div class="my-container">
      <!-- Header -->
      <div class="my-header">
        <h1>我的</h1>
      </div>

      <!-- Tab Switch -->
      <div class="tab-bar">
        <div class="tab-item" :class="{ active: activeTab === 'likes' }" @click="switchTab('likes')">
          <span>点赞</span>
          <div class="tab-indicator" v-if="activeTab === 'likes'" />
        </div>
        <div class="tab-item" :class="{ active: activeTab === 'favorites' }" @click="switchTab('favorites')">
          <span>收藏</span>
          <div class="tab-indicator" v-if="activeTab === 'favorites'" />
        </div>
      </div>

      <!-- Waterfall Grid -->
      <div class="waterfall-grid" v-loading="loading">
        <div v-for="item in items" :key="item.id" class="card-item" @click="goDetail(item)">
          <!-- 有封面时显示图片卡片 -->
          <template v-if="getCoverImage(item)">
            <div class="card-cover">
              <img :src="getImageUrl(getCoverImage(item)!)" />
              <div class="cover-overlay">
                <div class="overlay-like">
                  <el-icon :size="14"><CaretTop /></el-icon>
                  <span>{{ formatNum(item.likeCount) }}</span>
                </div>
              </div>
            </div>
            <div class="card-info">
              <div class="card-title">{{ item.title }}</div>
              <div class="card-desc">{{ item.description }}</div>
            </div>
          </template>
          <!-- 无封面时显示纯文字卡片（小红书风格） -->
          <template v-else>
            <div class="card-text-only" :style="{ background: textCardBg(item.id) }">
              <div class="text-card-title">{{ item.title }}</div>
              <div class="text-card-desc">{{ item.description }}</div>
              <div class="text-card-meta">
                <span>{{ formatNum(item.likeCount) }} 赞</span>
              </div>
            </div>
          </template>
        </div>
      </div>

      <!-- Load More -->
      <div class="load-more" v-if="hasMore">
        <el-button text :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>

      <el-empty v-if="!loading && items.length === 0" :description="activeTab === 'likes' ? '还没有点赞过任何内容' : '还没有收藏过任何内容'" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyLikes, getMyFavorites, type MyItemVO } from '@/api/my'
import { assetUrl } from '@/utils/assetUrl'

const router = useRouter()
const activeTab = ref<'likes' | 'favorites'>('likes')
const items = ref<MyItemVO[]>([])
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)
const loadingMore = ref(false)

onMounted(() => fetchData(true))

function switchTab(tab: 'likes' | 'favorites') {
  if (activeTab.value === tab) return
  activeTab.value = tab
  page.value = 1
  items.value = []
  hasMore.value = true
  fetchData(true)
}

async function fetchData(reset = false) {
  if (reset) loading.value = true
  else loadingMore.value = true
  try {
    const fn = activeTab.value === 'likes' ? getMyLikes : getMyFavorites
    const res = await fn(page.value, 12)
    const data = res.data || []
    if (reset) items.value = data
    else items.value.push(...data)
    hasMore.value = data.length >= 12
  } catch { /* ignore */ }
  finally { loading.value = false; loadingMore.value = false }
}

async function loadMore() {
  page.value++
  await fetchData(false)
}

function goDetail(item: MyItemVO) {
  if (item.type === 'course') {
    router.push(`/courses/${item.targetId}`)
  } else if (item.type === 'square_post') {
    router.push(`/square/${item.targetId}`)
  } else if (item.type === 'market') {
    router.push(`/market/${item.targetId}`)
  }
}

function getCoverImage(item: MyItemVO): string | null {
  if (!item.coverImage) return null
  // market 的 images 是 JSON 数组，取第一张
  if (item.type === 'market') {
    try {
      const arr = JSON.parse(item.coverImage)
      return arr.length > 0 ? arr[0] : null
    } catch { return null }
  }
  return item.coverImage
}

const TEXT_CARD_COLORS = [
  '#fef0ef', '#fdf6ec', '#eef7ee', '#eaf3fc', '#f3eefc',
  '#fef9e7', '#eaf7f7', '#fce4ec', '#e8f5e9', '#fff3e0',
]
function textCardBg(id: number) {
  return TEXT_CARD_COLORS[id % TEXT_CARD_COLORS.length]
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
.my-page { min-height: 100vh; background: #f5f6f7; padding-bottom: 60px; }
.my-container { max-width: 600px; margin: 0 auto; padding: 0 16px; }

.my-header { padding: 24px 0 4px; }
.my-header h1 { font-size: 22px; color: #18191c; margin: 0; font-weight: 700; }

/* Tab */
.tab-bar {
  display: flex; gap: 24px; margin-bottom: 16px; padding: 8px 0;
  border-bottom: 1px solid #eee; position: sticky; top: 0; background: #f5f6f7; z-index: 10;
}
.tab-item {
  position: relative; cursor: pointer; padding: 6px 0;
  font-size: 15px; color: #9499a0; transition: color .2s; font-weight: 500;
}
.tab-item.active { color: #18191c; font-weight: 700; }
.tab-indicator {
  position: absolute; bottom: -9px; left: 50%; transform: translateX(-50%);
  width: 20px; height: 3px; background: #18191c; border-radius: 2px;
}

/* Grid */
.waterfall-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.card-item {
  background: #fff; border-radius: 12px; overflow: hidden;
  cursor: pointer; transition: transform .15s;
}
.card-item:active { transform: scale(0.97); }

.card-cover {
  position: relative; width: 100%; aspect-ratio: 3 / 4;
  background: #e3e5e7; overflow: hidden;
}
.card-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder {
  display: flex; align-items: center; justify-content: center;
  height: 100%; color: #c9cdd4;
}
.cover-overlay {
  position: absolute; bottom: 0; left: 0; right: 0;
  padding: 20px 8px 6px;
  background: linear-gradient(transparent, rgba(0,0,0,.5));
}
.overlay-like {
  display: inline-flex; align-items: center; gap: 2px;
  color: #fff; font-size: 12px;
}

.card-info { padding: 10px; }
.card-title {
  font-size: 13px; font-weight: 600; color: #18191c;
  overflow: hidden; text-overflow: ellipsis;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  line-height: 1.4; margin-bottom: 4px;
}
.card-desc {
  font-size: 11px; color: #9499a0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

/* 文字卡片（无封面） */
.card-text-only {
  display: flex; flex-direction: column; justify-content: space-between;
  padding: 14px; min-height: 120px;
}
.text-card-title {
  font-size: 14px; font-weight: 600; color: #18191c;
  line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical;
  overflow: hidden; margin-bottom: 6px;
}
.text-card-desc {
  font-size: 12px; color: #9499a0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  margin-bottom: 8px;
}
.text-card-meta {
  font-size: 11px; color: #c0c4cc;
}

.load-more { text-align: center; padding: 24px 0; }
</style>
