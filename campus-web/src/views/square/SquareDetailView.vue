<template>
  <div class="detail-page">
    <div class="detail-container" v-if="post">
      <!-- 返回 -->
      <div class="back-bar">
        <el-button text @click="$router.push('/square')">
          <el-icon><ArrowLeft /></el-icon> 返回广场
        </el-button>
      </div>

      <!-- 帖子主体 -->
      <article class="detail-card">
        <!-- 用户信息 -->
        <div class="detail-header">
          <div class="poster-info">
            <el-avatar :size="40" :src="getAvatarUrl(post.userAvatar)">
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
            <div>
              <div class="poster-name">{{ post.userNickname || '同学' }}</div>
              <div class="poster-time">{{ formatTime(post.createdAt) }}</div>
            </div>
          </div>
          <el-tag v-if="post.category" size="small" effect="plain" type="info">
            {{ post.category }}
          </el-tag>
        </div>

        <!-- 标题 -->
        <h1 class="detail-title">{{ post.title }}</h1>

        <!-- 问题 -->
        <div class="question-block">
          <div class="block-label">
            <el-icon><QuestionFilled /></el-icon> 提问
          </div>
          <p class="question-text">{{ post.question }}</p>
        </div>

        <!-- 回答 -->
        <div class="answer-block">
          <div class="block-label">
            <el-icon><ChatDotRound /></el-icon> AI 回答
          </div>
          <div class="answer-text markdown-body" v-html="renderMarkdown(post.answer)" />

          <!-- 引用来源 -->
          <div class="references" v-if="references.length > 0">
            <div class="ref-label">参考来源</div>
            <div v-for="(ref, i) in references" :key="i" class="ref-item">
              <el-icon><Link /></el-icon>
              <span>{{ ref }}</span>
            </div>
          </div>
        </div>
      </article>

      <!-- 互动栏 -->
      <div class="action-bar">
        <div
          class="action-btn like-btn"
          :class="{ active: post.liked }"
          @click="handleLike"
        >
          <el-icon :size="20"><CaretTop /></el-icon>
          <span>{{ post.likeCount }}</span>
        </div>
        <div
          class="action-btn fav-btn"
          :class="{ active: post.favorited }"
          @click="handleFavorite"
        >
          <el-icon :size="20">
            <StarFilled v-if="post.favorited" />
            <Star v-else />
          </el-icon>
          <span>收藏</span>
        </div>
        <div class="action-btn">
          <el-icon :size="20"><View /></el-icon>
          <span>{{ post.viewCount }}</span>
        </div>
      </div>
    </div>

    <!-- 评论区 -->
    <div class="comment-section" v-if="post">
      <div class="comment-header">
        <h3 class="comment-title">评论 ({{ commentCount }})</h3>
        <div class="comment-sort">
          <span :class="{ active: sortBy === 'hot' }" @click="switchSort('hot')">热门</span>
          <span :class="{ active: sortBy === 'newest' }" @click="switchSort('newest')">最新</span>
          <span :class="{ active: sortBy === 'oldest' }" @click="switchSort('oldest')">最早</span>
        </div>
      </div>

      <div class="comment-post">
        <el-input
          v-model="commentText"
          type="textarea"
          :rows="2"
          :placeholder="replyTarget ? '回复 @' + replyTarget.nickname + '...' : '写下你的评论...'"
          resize="none"
        />
        <div class="comment-post-actions">
          <el-button v-if="replyTarget" size="small" text @click="cancelReply">取消回复</el-button>
          <el-button size="small" type="primary" :disabled="!commentText.trim()" @click="handleAddComment">发表</el-button>
        </div>
      </div>

      <div v-if="comments.length > 0" class="comment-list">
        <CommentItem
          v-for="c in comments"
          :key="c.id"
          :comment="c"
          :depth="0"
          :my-user-id="myUserId"
          @reply="startReply"
          @like="handleCommentLike"
          @delete="handleCommentDelete"
        />
      </div>
    </div>

    <!-- Loading -->
    <div class="loading-wrap" v-if="loading">
      <el-icon class="loading-icon" :size="32"><Loading /></el-icon>
    </div>

    <!-- 错误 -->
    <el-result
      v-if="!loading && !post"
      icon="warning"
      title="帖子不存在"
      sub-title="该帖子可能已被删除"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/square')">返回广场</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSquarePostDetail, likeSquarePost, favoriteSquarePost, getComments, addComment, likeComment, deleteComment, type SquarePost, type CommentVO } from '@/api/square'
import CommentItem from '@/components/CommentItem.vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const route = useRoute()
const md = new MarkdownIt({
  html: false,
  linkify: true,
  highlight(str: string, lang: string) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch { /* fallthrough */ }
    }
    return ''
  },
})

const post = ref<SquarePost | null>(null)
const loading = ref(true)
const references = ref<string[]>([])

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    const res = await getSquarePostDetail(id)
    post.value = res.data
    if (post.value.referencesJson) {
      try {
        const refs = JSON.parse(post.value.referencesJson)
        references.value = Array.isArray(refs)
          ? refs.map((r: any) => typeof r === 'string' ? r : (r.content || ''))
          : []
      } catch { references.value = [] }
    }
  } catch { /* handled */ }
  finally { loading.value = false }
  loadComments()
})

// 评论
const comments = ref<CommentVO[]>([])
const commentText = ref('')
const replyTarget = ref<{ id: number; nickname: string; userId: number } | null>(null)
const sortBy = ref<'hot' | 'newest' | 'oldest'>('hot')
const myUserId = ref(0)

try {
  const token = localStorage.getItem('accessToken')
  if (token) {
    const payload = JSON.parse(atob(token.split('.')[1]))
    myUserId.value = payload.userId || 0
  }
} catch { }

// count all nested comments recursively
function countAll(list: CommentVO[]): number {
  let n = list.length
  for (const item of list) {
    if (item.replies) n += countAll(item.replies)
  }
  return n
}
const commentCount = computed(() => countAll(comments.value))

async function loadComments() {
  const id = Number(route.params.id)
  try {
    const res = await getComments(id, sortBy.value)
    comments.value = res.data || []
  } catch { comments.value = [] }
}

function switchSort(s: 'hot' | 'newest' | 'oldest') {
  sortBy.value = s
  loadComments()
}

function startReply(c: CommentVO) {
  replyTarget.value = { id: c.id, nickname: c.userNickname, userId: c.userId }
  commentText.value = ''
}

function cancelReply() {
  replyTarget.value = null
  commentText.value = ''
}

async function handleAddComment() {
  if (!commentText.value.trim()) return
  const id = Number(route.params.id)
  try {
    await addComment(id, commentText.value.trim(), replyTarget.value?.id, replyTarget.value?.userId)
    commentText.value = ''
    replyTarget.value = null
    await loadComments()
  } catch { /* handled */ }
}

async function handleCommentLike(c: CommentVO) {
  try {
    const res = await likeComment(c.id)
    c.liked = res.data.liked
    c.likeCount += res.data.liked ? 1 : -1
  } catch { /* handled */ }
}

async function handleCommentDelete(c: CommentVO) {
  try {
    await deleteComment(c.id)
    await loadComments()
  } catch { /* handled */ }
}

function renderMarkdown(text: string) {
  return md.render(text || '')
}

async function handleLike() {
  if (!post.value) return
  try {
    const res = await likeSquarePost(post.value.id)
    post.value.liked = res.data.liked
    post.value.likeCount += res.data.liked ? 1 : -1
  } catch { /* handled */ }
}

async function handleFavorite() {
  if (!post.value) return
  try {
    const res = await favoriteSquarePost(post.value.id)
    post.value.favorited = res.data.favorited
    ElMessage.success(post.value.favorited ? '已收藏' : '已取消收藏')
  } catch { /* handled */ }
}

function getAvatarUrl(avatar: string) {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return `/api/files/${avatar}`
}

function formatTime(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #fafafa;
  padding: 24px 0 60px;
}
.detail-container {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 20px;
}

.back-bar {
  margin-bottom: 16px;
}

/* 帖子卡片 */
.detail-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  border: 1px solid #f0f0f0;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.poster-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.poster-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}
.poster-time {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.detail-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 24px;
  line-height: 1.4;
}

/* 问题 */
.question-block {
  background: #fff5f5;
  border-left: 4px solid #ff4757;
  border-radius: 0 12px 12px 0;
  padding: 16px 20px;
  margin-bottom: 24px;
}
.block-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #ff4757;
  margin-bottom: 8px;
}
.question-text {
  font-size: 15px;
  color: #444;
  line-height: 1.7;
  margin: 0;
}

/* 回答 */
.answer-block {
  margin-bottom: 8px;
}
.answer-block > .block-label {
  color: #409eff;
  margin-bottom: 12px;
}
.answer-text {
  font-size: 15px;
  color: #303133;
  line-height: 1.8;
}
.answer-text :deep(pre) {
  background: #f6f8fa;
  border-radius: 8px;
  padding: 16px;
  overflow-x: auto;
}
.answer-text :deep(code) {
  font-family: 'Fira Code', monospace;
  font-size: 13px;
}
.answer-text :deep(blockquote) {
  border-left: 3px solid #e4e7ed;
  padding-left: 16px;
  color: #888;
  margin: 12px 0;
}
.answer-text :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}
.answer-text :deep(th), .answer-text :deep(td) {
  border: 1px solid #e4e7ed;
  padding: 8px 12px;
  text-align: left;
}
.answer-text :deep(th) {
  background: #f6f8fa;
}

/* 引用来源 */
.references {
  margin-top: 20px;
  background: #f8f9fb;
  border-radius: 10px;
  padding: 16px;
}
.ref-label {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}
.ref-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  padding: 4px 0;
}
.ref-item .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

/* 互动栏 */
.action-bar {
  display: flex;
  gap: 24px;
  justify-content: center;
  padding: 20px 0;
  margin-top: 20px;
}
.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 22px;
  color: #b0b3bb;
  padding: 12px 24px;
  background: #fff;
  border-radius: 16px;
  cursor: pointer;
  border: 1px solid #f0f0f0;
  transition: all 0.2s;
}
.action-btn span {
  font-size: 13px;
}
.action-btn:hover {
  color: #666;
  border-color: #e4e7ed;
}
.like-btn.active {
  color: #ff4757;
  border-color: #ffcdd2;
  background: #fff5f5;
}
.fav-btn.active {
  color: #ffa502;
  border-color: #ffe4b5;
  background: #fffaf0;
}

/* 评论区 */
.comment-section {
  max-width: 720px;
  margin: 20px auto 0;
  background: #fff;
  border-radius: 16px;
  padding: 24px 28px;
  border: 1px solid #f0f0f0;
}
.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.comment-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}
.comment-sort {
  display: flex;
  gap: 0;
}
.comment-sort span {
  font-size: 12px;
  color: #999;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 4px;
}
.comment-sort span.active {
  color: #ff4757;
  font-weight: 500;
  background: #fff5f5;
}
.comment-post {
  margin-bottom: 16px;
}
.comment-post-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
.comment-list {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}
.reply-to {
  color: #409eff;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}
.loading-icon {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
