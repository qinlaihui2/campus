<template>
  <div class="emoji-picker">
    <!-- 搜索栏 -->
    <div class="picker-search">
      <el-input
        v-model="keyword"
        size="small"
        placeholder="搜索表情..."
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 表情网格 -->
    <div class="picker-grid" v-if="!keyword">
      <div
        v-for="emoji in currentEmojis"
        :key="emoji.id"
        class="emoji-item"
        :title="emoji.name"
        @click="$emit('select', emoji)"
      >
        <span v-if="emoji.emojiChar" class="emoji-char">{{ emoji.emojiChar }}</span>
        <img v-else-if="emoji.imageUrl" :src="getImageUrl(emoji.imageUrl)" class="emoji-img" />
      </div>
    </div>

    <!-- 搜索结果 -->
    <div class="picker-grid" v-else>
      <div
        v-for="emoji in searchResults"
        :key="emoji.id"
        class="emoji-item"
        :title="emoji.name"
        @click="$emit('select', emoji)"
      >
        <span v-if="emoji.emojiChar" class="emoji-char">{{ emoji.emojiChar }}</span>
        <img v-else-if="emoji.imageUrl" :src="getImageUrl(emoji.imageUrl)" class="emoji-img" />
      </div>
      <el-empty v-if="searchResults.length === 0" description="没找到相关表情" :image-size="40" />
    </div>

    <!-- 底部标签 -->
    <div class="picker-tabs" v-if="!keyword">
      <span
        v-for="tab in tabs"
        :key="tab.key"
        :class="{ active: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getEmojis, searchEmojis as searchApi, getFavoriteEmojis, type Emoji } from '@/api/emoji'

defineEmits<{ select: [emoji: Emoji] }>()

const tabs = [
  { key: '默认', label: '默认' },
  { key: '热门', label: '热门' },
  { key: '收藏', label: '收藏' },
]

const activeTab = ref('默认')
const keyword = ref('')
const currentEmojis = ref<Emoji[]>([])
const searchResults = ref<Emoji[]>([])
const allEmojis = ref<Record<string, Emoji[]>>({})

onMounted(async () => {
  try {
    const res = await getEmojis()
    for (const emoji of res.data) {
      const cat = emoji.category || '默认'
      if (!allEmojis.value[cat]) allEmojis.value[cat] = []
      allEmojis.value[cat].push(emoji)
    }
    currentEmojis.value = allEmojis.value['默认'] || []
  } catch { /* ignore */ }
})

function switchTab(key: string) {
  activeTab.value = key
  if (key === '收藏') {
    loadFavorites()
  } else {
    currentEmojis.value = allEmojis.value[key] || []
  }
}

async function loadFavorites() {
  try {
    const res = await getFavoriteEmojis()
    currentEmojis.value = res.data
  } catch { currentEmojis.value = [] }
}

let searchTimer: number
function handleSearch() {
  clearTimeout(searchTimer)
  if (!keyword.value) {
    searchResults.value = []
    return
  }
  searchTimer = window.setTimeout(async () => {
    try {
      const res = await searchApi(keyword.value)
      searchResults.value = res.data
    } catch { searchResults.value = [] }
  }, 200)
}

function getImageUrl(url: string) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return `/api/files/${url}`
}
</script>

<style scoped>
.emoji-picker {
  width: 360px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.12);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.picker-search {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.picker-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 2px;
  padding: 8px 4px;
  max-height: 260px;
  overflow-y: auto;
  flex: 1;
}

.emoji-item {
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 1;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s;
}
.emoji-item:hover {
  background: #f0f0f0;
}

.emoji-char {
  font-size: 22px;
  line-height: 1;
}

.emoji-img {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.picker-tabs {
  display: flex;
  border-top: 1px solid #f0f0f0;
}
.picker-tabs span {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  transition: all 0.15s;
  border-top: 2px solid transparent;
}
.picker-tabs span:hover {
  color: #333;
}
.picker-tabs span.active {
  color: #ff4757;
  border-top-color: #ff4757;
}
</style>
