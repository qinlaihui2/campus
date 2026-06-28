<template>
  <div class="square-page">
    <div class="square-container">
      <!-- 顶部搜索区 -->
      <div class="square-hero">
        <h1 class="hero-title">问答广场</h1>
        <p class="hero-subtitle">发现校园里的好问题，让知识流动起来</p>
        <el-button type="primary" class="publish-btn" @click="showPublishDialog = true">发布问题</el-button>
        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="keyword"
            class="search-input"
            placeholder="搜索你感兴趣的问题..."
            @keyup.enter="handleSearch"
          />
          <el-button v-if="keyword" class="search-clear" text @click="keyword = ''; handleSearch()">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 分类标签 -->
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

      <!-- Tab 切换：最新 / 热门 -->
      <div class="sort-tabs">
        <span :class="{ active: sortBy === 'latest' }" @click="switchSort('latest')">最新</span>
        <span :class="{ active: sortBy === 'hot' }" @click="switchSort('hot')">热门</span>
      </div>

      <!-- 卡片瀑布流 -->
      <div class="card-grid">
        <div v-for="post in posts" :key="post.id" class="square-card" @click="goDetail(post.id)">
          <div class="card-header">
            <div class="card-user">
              <el-avatar :size="28" :src="getAvatarUrl(post.userAvatar)">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <span class="card-nickname">{{ post.userNickname || '同学' }}</span>
            </div>
            <el-tag v-if="post.category" size="small" type="info" effect="plain">
              {{ categoryLabel(post.category) }}
            </el-tag>
          </div>
          <h3 class="card-title">{{ post.title }}</h3>
          <div class="card-preview" v-html="renderPreview(post.answer)" />
          <div class="card-footer">
            <div class="footer-stat">
              <el-icon><View /></el-icon>
              <span>{{ formatCount(post.viewCount) }}</span>
            </div>
            <div class="footer-stat" :class="{ liked: post.liked }" @click.stop="handleLike(post)">
              <el-icon><StarFilled v-if="post.liked" /><Star v-else /></el-icon>
              <span>{{ formatCount(post.likeCount) }}</span>
            </div>
            <span class="card-time">{{ timeAgo(post.createdAt) }}</span>
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
          @current-change="fetchPosts"
        />
      </div>

      <!-- 发布弹窗 -->
      <el-dialog v-model="showPublishDialog" title="发布问题" width="600px" top="5vh" destroy-on-close>
        <el-form label-width="80px">
          <el-form-item label="标题">
            <el-input v-model="publishForm.title" placeholder="简洁概括你的问题" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="问题描述">
            <el-input v-model="publishForm.question" type="textarea" :rows="3" placeholder="详细描述你的问题" />
          </el-form-item>
          <el-form-item label="你的回答">
            <el-input v-model="publishForm.answer" type="textarea" :rows="4" placeholder="你对这个问题的看法或解答" />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="publishForm.category" placeholder="选择分类（可选）" clearable>
              <el-option v-for="c in categories.filter(x => x.value)" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showPublishDialog = false">取消</el-button>
          <el-button type="primary" :loading="publishing" :disabled="!publishForm.title || !publishForm.question || !publishForm.answer" @click="handlePublish">发布</el-button>
        </template>
      </el-dialog>

      <el-empty v-if="!loading && posts.length === 0" description="还没有帖子，快去对话中分享你的问答吧" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { assetUrl } from '@/utils/assetUrl'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSquarePosts, getHotPosts, likeSquarePost, publishToSquare, type SquarePost } from '@/api/square'
import MarkdownIt from 'markdown-it'

const router = useRouter()
const md = new MarkdownIt({ html: false, linkify: true })

const categories = [
  { label: '全部', value: '' },
  { label: '规章制度', value: '规章制度' },
  { label: '办事流程', value: '办事流程' },
  { label: '课程学习', value: '课程信息' },
  { label: '校园生活', value: '校园生活' },
]

const activeCategory = ref('')
const sortBy = ref<'latest' | 'hot'>('latest')
const keyword = ref('')
const posts = ref<SquarePost[]>([])
const showPublishDialog = ref(false)
const publishing = ref(false)
const publishForm = ref({ title: '', question: '', answer: '', category: '' as string })

const currentPage = ref(1)
const pageSize = 12
const total = ref(0)
const loading = ref(false)

onMounted(() => fetchPosts())

async function fetchPosts() {
  loading.value = true
  try {
    const params = {
      category: activeCategory.value || undefined,
      keyword: keyword.value || undefined,
      page: currentPage.value,
      size: pageSize,
    }
    const fetcher = sortBy.value === 'hot' ? getHotPosts : getSquarePosts
    const res = await fetcher(params)
    posts.value = res.data.records
    total.value = res.data.total
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}

function switchCategory(cat: string) {
  activeCategory.value = cat
  currentPage.value = 1
  fetchPosts()
}

function switchSort(sort: 'latest' | 'hot') {
  sortBy.value = sort
  currentPage.value = 1
  fetchPosts()
}

function handleSearch() {
  currentPage.value = 1
  fetchPosts()
}

async function handlePublish() {
  if (!publishForm.value.title || !publishForm.value.question || !publishForm.value.answer) return
  publishing.value = true
  try {
    await publishToSquare({
      title: publishForm.value.title,
      question: publishForm.value.question,
      answer: publishForm.value.answer,
      category: publishForm.value.category || undefined,
    })
    ElMessage.success('发布成功')
    showPublishDialog.value = false
    publishForm.value = { title: '', question: '', answer: '', category: '' }
    fetchPosts()
  } catch { /* handled */ }
  finally { publishing.value = false }
}

function goDetail(id: number) {
  router.push(`/square/${id}`)
}

async function handleLike(post: SquarePost) {
  try {
    const res = await likeSquarePost(post.id)
    post.liked = res.data.liked
    post.likeCount += res.data.liked ? 1 : -1
  } catch { /* handled by interceptor */ }
}

function getAvatarUrl(avatar: string) {
  if (!avatar) return ''
  return assetUrl(`/api/files/${avatar}`)
}

function renderPreview(markdown: string) {
  const html = md.render(markdown || '')
  return html.replace(/<[^>]+>/g, '').substring(0, 120)
}

function categoryLabel(cat: string) {
  const map: Record<string, string> = {
    '规章制度': '规章制度',
    '办事流程': '办事流程',
    '课程信息': '课程学习',
    '校园生活': '校园生活',
  }
  return map[cat] || cat
}

function formatCount(n: number) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

function timeAgo(dateStr: string) {
  const diff = Date.now() - new Date(dateStr).getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  return dateStr.substring(0, 10)
}
</script>

<style scoped>
.square-page {
  min-height: 100vh;
  background: #fafafa;
  padding-bottom: 60px;
}
.square-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 20px;
}

/* Hero */
.square-hero {
  text-align: center;
  padding: 36px 0 24px;
}
.hero-title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 8px;
}
.hero-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0 0 14px;
}
.publish-btn { margin-bottom: 6px; }

/* Search */
.search-box {
  position: relative;
  max-width: 480px;
  margin: 0 auto;
}
.search-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #c0c4cc;
  font-size: 18px;
}
.search-input {
  width: 100%;
  height: 44px;
  border: 2px solid #e4e7ed;
  border-radius: 22px;
  padding: 0 40px 0 42px;
  font-size: 14px;
  outline: none;
  background: #fff;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.search-input:focus {
  border-color: #ff4757;
}
.search-input::placeholder {
  color: #c0c4cc;
}
.search-clear {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
}

/* Category tabs */
.category-tabs {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-bottom: 16px;
}
.category-tabs span {
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  background: #fff;
  border: 1px solid #ebeef5;
  transition: all 0.2s;
}
.category-tabs span:hover {
  color: #ff4757;
  border-color: #ffcdd2;
}
.category-tabs span.active {
  background: #ff4757;
  color: #fff;
  border-color: #ff4757;
}

/* Sort tabs */
.sort-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  font-size: 14px;
}
.sort-tabs span {
  padding: 6px 16px;
  cursor: pointer;
  color: #999;
  border-radius: 4px;
  transition: all 0.2s;
}
.sort-tabs span.active {
  color: #303133;
  font-weight: 600;
}
.sort-tabs span:first-child {
  border-right: 1px solid #e4e7ed;
  border-radius: 0;
  padding-right: 16px;
}

/* Card grid */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 16px;
}

/* Card — Xiaohongshu style */
.square-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid #f0f0f0;
}
.square-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.06);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.card-user {
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-nickname {
  font-size: 12px;
  color: #999;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-preview {
  font-size: 13px;
  color: #8a8f99;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 14px;
}
.card-footer {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}
.footer-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #b0b3bb;
}
.footer-stat.liked {
  color: #ff4757;
}
.card-time {
  margin-left: auto;
  font-size: 11px;
  color: #c0c4cc;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
