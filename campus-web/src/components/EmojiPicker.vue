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
import { assetUrl } from '@/utils/assetUrl'

defineEmits<{ select: [emoji: Emoji] }>()

// 内置表情包，不依赖后端数据库
const BUILTIN: Emoji[] = [
  ...['😀','😃','😄','😁','😅','😂','🤣','😊','😇','🙂','😉','😌','😍','🥰','😘','😗'].map((c,i) => ({ id: i+1, name: '笑脸', emojiChar: c, imageUrl: '', tags: '笑', category: '默认', sortOrder: i })),
  ...['😋','😛','😜','🤪','😝','🤑','🤗','🤭','🤫','🤔','🤐','🤨','😐','😑','😶','😏'].map((c,i) => ({ id: i+101, name: '搞怪', emojiChar: c, imageUrl: '', tags: '搞怪', category: '默认', sortOrder: i })),
  ...['😒','🙄','😬','😮‍💨','🤥','😪','😮','😯','😲','😳','🥺','😢','😭','😤','😡','🤬'].map((c,i) => ({ id: i+201, name: '情绪', emojiChar: c, imageUrl: '', tags: '情绪', category: '默认', sortOrder: i })),
  ...['😈','👿','👹','👺','💀','👻','👽','🤖','🎃','😺','😸','😹','😻','😼','😽','🙀'].map((c,i) => ({ id: i+301, name: '搞怪2', emojiChar: c, imageUrl: '', tags: '搞怪', category: '热门', sortOrder: i })),
  ...['💋','💌','💘','💝','💖','💗','💓','💞','💕','💟','❣️','❤️','🧡','💛','💚','💙'].map((c,i) => ({ id: i+401, name: '爱心', emojiChar: c, imageUrl: '', tags: '爱心', category: '热门', sortOrder: i })),
  ...['💜','🤎','🖤','🤍','💯','💢','💥','💫','💦','💨','🕳️','💣','💬','👁️‍🗨️','🗨️','🗯️'].map((c,i) => ({ id: i+501, name: '符号', emojiChar: c, imageUrl: '', tags: '符号', category: '热门', sortOrder: i })),
  ...['💭','🕶️','👓','🥽','🥼','🦺','👔','👕','👖','🧣','🧤','🧥','🧦','👗','👘','🥻'].map((c,i) => ({ id: i+601, name: '服饰', emojiChar: c, imageUrl: '', tags: '服饰', category: '常用', sortOrder: i })),
  ...['🩱','🩲','🩳','👙','👚','👛','👜','👝','🎒','👞','👟','🥾','🥿','👠','👡','🩰'].map((c,i) => ({ id: i+701, name: '穿搭', emojiChar: c, imageUrl: '', tags: '穿搭', category: '常用', sortOrder: i })),
]

// 分类：内置的 + 后端查到的合并
const ALL: Record<string, Emoji[]> = {}
for (const e of BUILTIN) {
  const cat = e.category || '默认'
  if (!ALL[cat]) ALL[cat] = []
  ALL[cat].push(e)
}

const activeTab = ref('默认')
const keyword = ref('')
const currentEmojis = ref<Emoji[]>(ALL['默认'] || [])
const searchResults = ref<Emoji[]>([])
const favEmojis = ref<Emoji[]>([])

onMounted(async () => {
  // 尝试从后端加载更多表情，合并到已有分类
  try {
    const res = await getEmojis()
    for (const emoji of res.data) {
      const cat = emoji.category || '默认'
      if (!ALL[cat]) ALL[cat] = []
      // 避免重复
      if (!ALL[cat].some(e => e.id === emoji.id)) {
        ALL[cat].push(emoji)
      }
    }
  } catch { /* 后端不可用时用内置的 */ }
  currentEmojis.value = ALL[activeTab.value] || ALL['默认'] || []
})

function switchTab(key: string) {
  activeTab.value = key
  if (key === '收藏') {
    loadFavorites()
  } else {
    currentEmojis.value = ALL[key] || []
  }
}

async function loadFavorites() {
  try {
    const res = await getFavoriteEmojis()
    favEmojis.value = res.data
    currentEmojis.value = favEmojis.value.length > 0 ? favEmojis.value : []
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
    // 先在内置表情里搜
    const kw = keyword.value.toLowerCase()
    const local = BUILTIN.filter(e =>
      e.name.includes(kw) || e.tags.includes(kw) || e.emojiChar.includes(kw)
    )
    // 同时搜后端
    try {
      const res = await searchApi(keyword.value)
      searchResults.value = [...local, ...res.data]
    } catch {
      searchResults.value = local
    }
  }, 200)
}

function getImageUrl(url: string) {
  if (!url) return ''
  return assetUrl(`/api/files/${url}`)
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
