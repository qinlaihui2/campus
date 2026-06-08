<template>
  <div class="detail-page">
    <div class="detail-container" v-if="course">
      <div class="back-bar">
        <el-button text @click="$router.push('/courses')">
          <el-icon><ArrowLeft /></el-icon> 返回课程
        </el-button>
      </div>

      <div class="detail-layout">
        <!-- 左侧：视频 + 大纲 -->
        <div class="detail-left">
          <div class="video-box">
            <video v-if="currentVideo" :src="getVideoUrl(currentVideo)" controls class="video-player"
              autoplay />
            <div v-else class="video-empty">
              <el-icon :size="48"><VideoCamera /></el-icon>
              <p>请选择章节开始学习</p>
            </div>
          </div>

          <div class="course-info-bar">
            <h2 class="course-title">{{ course.title }}</h2>
            <div class="course-stats">
              <span>{{ course.instructor }}</span>
              <span>{{ formatNum(course.viewCount) }} 次播放</span>
              <span>{{ course.chapterCount }} 课时</span>
            </div>
          </div>

          <!-- 章节大纲 -->
          <div class="chapter-list">
            <h3>课程大纲</h3>
            <div v-for="(ch, i) in course.chapters" :key="ch.id" class="chapter-item"
              :class="{ active: currentIndex === i }" @click="playChapter(ch, i)">
              <span class="ch-index">{{ i + 1 }}</span>
              <div class="ch-info">
                <div class="ch-title">{{ ch.title }}</div>
                <div class="ch-meta" v-if="ch.description">{{ ch.description }}</div>
              </div>
              <span class="ch-duration" v-if="ch.duration">{{ formatDuration(ch.duration) }}</span>
            </div>
          </div>

          <!-- 评论区 -->
          <div class="comment-section">
            <h3>评论</h3>
            <div class="comment-post">
              <el-input v-model="commentText" type="textarea" :rows="2"
                :placeholder="replyTarget ? '回复 @' + replyTarget.nickname + '...' : '写下你的评论...'"
                resize="none" />
              <div class="comment-post-actions">
                <el-button v-if="replyTarget" size="small" text @click="cancelReply">取消回复</el-button>
                <el-button size="small" type="primary" :disabled="!commentText.trim()" @click="handleAddComment">发表</el-button>
              </div>
            </div>
            <div class="comment-sort">
              <span :class="{ active: sortBy === 'hot' }" @click="switchSort('hot')">热门</span>
              <span :class="{ active: sortBy === 'newest' }" @click="switchSort('newest')">最新</span>
            </div>
            <div class="comment-list" v-if="comments.length > 0">
              <CommentItem v-for="c in comments" :key="c.id" :comment="c" :depth="0"
                :my-user-id="myUserId" @reply="startReply" @like="handleCommentLike" @delete="handleCommentDelete" />
            </div>
          </div>
        </div>

        <!-- 右侧：课程信息 + 操作 -->
        <div class="detail-right">
          <div class="side-card">
            <div class="side-cover">
              <img v-if="course.coverImage" :src="getImageUrl(course.coverImage)" />
              <div v-else class="cover-placeholder">
                <el-icon :size="40"><VideoCamera /></el-icon>
              </div>
            </div>
            <p class="side-desc">{{ course.description || '暂无简介' }}</p>
            <div class="side-actions">
              <el-button :type="course.liked ? 'danger' : 'default'" @click="handleLike">
                <el-icon><CaretTop /></el-icon> 点赞 {{ course.likeCount }}
              </el-button>
              <el-button :type="course.favorited ? 'warning' : 'default'" @click="handleFavorite">
                <el-icon><Star /></el-icon> {{ course.favorited ? '已收藏' : '收藏' }} {{ course.favoriteCount }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="loading-wrap" v-if="loading">
      <el-icon class="loading-icon" :size="32"><Loading /></el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  getCourseDetail, likeCourse, favoriteCourse,
  getComments, addComment, likeComment, deleteComment,
  type CourseDetailVO, type CommentVO
} from '@/api/course'
import CommentItem from '@/components/CommentItem.vue'

const route = useRoute()
const course = ref<CourseDetailVO | null>(null)
const loading = ref(true)
const currentVideo = ref<string | null>(null)
const currentIndex = ref(-1)
const comments = ref<CommentVO[]>([])
const commentText = ref('')
const replyTarget = ref<{ id: number; nickname: string; userId: number } | null>(null)
const sortBy = ref<'hot' | 'newest'>('hot')
const myUserId = ref(0)

try {
  const token = localStorage.getItem('accessToken')
  if (token) {
    const payload = JSON.parse(atob(token.split('.')[1]))
    myUserId.value = payload.userId || 0
  }
} catch { }

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    const res = await getCourseDetail(id)
    course.value = res.data
    if (course.value.chapters?.length > 0) {
      playChapter(course.value.chapters[0], 0)
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
  loadComments()
})

function playChapter(ch: { videoUrl: string }, i: number) {
  currentVideo.value = ch.videoUrl
  currentIndex.value = i
}

function getVideoUrl(url: string) {
  if (url.startsWith('http')) return url
  return `/api/files/${url}`
}
function getImageUrl(url: string) {
  if (url.startsWith('http')) return url
  return `/api/files/${url}`
}
function formatNum(n: number) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
function formatDuration(sec: number) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

async function handleLike() {
  if (!course.value) return
  try {
    const res = await likeCourse(course.value.id)
    course.value.liked = res.data.liked
    course.value.likeCount += res.data.liked ? 1 : -1
  } catch { /* ignore */ }
}

async function handleFavorite() {
  if (!course.value) return
  try {
    const res = await favoriteCourse(course.value.id)
    course.value.favorited = res.data.favorited
    course.value.favoriteCount += res.data.favorited ? 1 : -1
  } catch { /* ignore */ }
}

// 评论
async function loadComments() {
  const id = Number(route.params.id)
  try {
    const res = await getComments(id, sortBy.value)
    comments.value = res.data || []
  } catch { comments.value = [] }
}
function switchSort(s: 'hot' | 'newest') { sortBy.value = s; loadComments() }
function startReply(c: CommentVO) {
  replyTarget.value = { id: c.id, nickname: c.userNickname, userId: c.userId }
  commentText.value = ''
}
function cancelReply() { replyTarget.value = null; commentText.value = '' }
async function handleAddComment() {
  if (!commentText.value.trim()) return
  const id = Number(route.params.id)
  try {
    await addComment(id, commentText.value.trim(), replyTarget.value?.id, replyTarget.value?.userId)
    commentText.value = ''
    replyTarget.value = null
    await loadComments()
  } catch { /* ignore */ }
}
async function handleCommentLike(c: CommentVO) {
  try {
    const res = await likeComment(c.id)
    c.liked = res.data.liked
    c.likeCount += res.data.liked ? 1 : -1
  } catch { /* ignore */ }
}
async function handleCommentDelete(c: CommentVO) {
  try {
    await deleteComment(c.id)
    await loadComments()
  } catch { /* ignore */ }
}
</script>

<style scoped>
.detail-page { min-height: 100vh; background: #f5f6f7; padding-bottom: 40px; }
.detail-container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }
.back-bar { padding: 16px 0; }
.detail-layout { display: flex; gap: 20px; }
.detail-left { flex: 1; min-width: 0; }
.detail-right { width: 300px; flex-shrink: 0; }

.video-box {
  background: #000; border-radius: 12px; overflow: hidden;
  aspect-ratio: 16/9; display: flex; align-items: center; justify-content: center;
}
.video-player { width: 100%; height: 100%; }
.video-empty { text-align: center; color: #666; }
.video-empty p { margin-top: 12px; }

.course-info-bar { padding: 16px 0; }
.course-title { font-size: 20px; margin: 0 0 6px; }
.course-stats { font-size: 13px; color: #9499a0; display: flex; gap: 16px; }

.chapter-list {
  background: #fff; border-radius: 10px; padding: 16px 20px; margin-bottom: 16px;
}
.chapter-list h3 { font-size: 16px; margin: 0 0 12px; }
.chapter-item {
  display: flex; align-items: center; gap: 12px; padding: 10px 12px;
  border-radius: 8px; cursor: pointer; transition: background .15s;
}
.chapter-item:hover { background: #f5f6f7; }
.chapter-item.active { background: #e6f7ff; }
.ch-index {
  width: 24px; height: 24px; border-radius: 50%; background: #e3e5e7;
  display: flex; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0;
}
.chapter-item.active .ch-index { background: #00aeec; color: #fff; }
.ch-info { flex: 1; min-width: 0; }
.ch-title { font-size: 14px; color: #18191c; }
.ch-meta { font-size: 12px; color: #9499a0; margin-top: 2px; }
.ch-duration { font-size: 12px; color: #9499a0; flex-shrink: 0; }

/* 评论区 */
.comment-section {
  background: #fff; border-radius: 10px; padding: 20px; margin-bottom: 16px;
}
.comment-section h3 { font-size: 16px; margin: 0 0 12px; }
.comment-post { margin-bottom: 12px; }
.comment-post-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
.comment-sort { display: flex; gap: 0; margin-bottom: 12px; }
.comment-sort span { font-size: 12px; color: #999; cursor: pointer; padding: 4px 10px; border-radius: 4px; }
.comment-sort span.active { color: #00aeec; font-weight: 500; background: #e6f7ff; }
.comment-list { border-top: 1px solid #f0f0f0; padding-top: 12px; }

/* 右侧 */
.side-card { background: #fff; border-radius: 10px; overflow: hidden; position: sticky; top: 20px; }
.side-cover { height: 170px; background: #e3e5e7; overflow: hidden; }
.side-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder { display: flex; align-items: center; justify-content: center; height: 100%; color: #c9cdd4; }
.side-desc { padding: 14px 16px; font-size: 13px; color: #61666d; line-height: 1.6; margin: 0; }
.side-actions { padding: 0 16px 16px; display: flex; flex-direction: column; gap: 8px; }

.loading-wrap { display: flex; justify-content: center; padding: 80px 0; }
.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
</style>
