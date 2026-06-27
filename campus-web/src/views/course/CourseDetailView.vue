<template>
  <div class="course-page">
    <div class="course-container" v-if="course">
      <!-- 顶部返回 -->
      <div class="top-bar">
        <el-button text @click="$router.push('/courses')">
          <el-icon><ArrowLeft /></el-icon> 返回课程
        </el-button>
        <span class="top-title">{{ course.title }}</span>
      </div>

      <div class="course-layout">
        <!-- 左侧：播放器 + 信息 + 评论 -->
        <div class="course-left">
          <div class="player-box">
            <video v-if="currentVideo" :src="getVideoUrl(currentVideo.videoUrl)" controls class="player"
              autoplay :key="currentVideo.id" />
            <div v-else class="player-empty">
              <el-icon :size="56"><VideoCamera /></el-icon>
              <p>选择右侧视频开始学习</p>
            </div>
          </div>

          <!-- 当前视频信息 -->
          <div class="video-info" v-if="currentVideo">
            <h2 class="video-title">{{ currentVideo.title }}</h2>
            <div class="video-meta">
              <span>{{ course.instructor }}</span>
              <span>{{ formatNum(course.viewCount) }} 次播放</span>
            </div>
          </div>

          <!-- 课程信息 + 操作 -->
          <div class="course-desc-card">
            <p>{{ course.description || '暂无简介' }}</p>
            <div class="course-actions">
              <el-button :type="course.liked ? 'danger' : 'default'" size="small" @click="handleLike">
                <el-icon><CaretTop /></el-icon> 点赞 {{ course.likeCount }}
              </el-button>
              <el-button :type="course.favorited ? 'warning' : 'default'" size="small" @click="handleFavorite">
                <el-icon><Star /></el-icon> {{ course.favorited ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </div>

          <!-- 评论区 -->
          <div class="comment-box">
            <h3>评论 ({{ comments.length }})</h3>
            <div class="comment-input">
              <el-input v-model="commentText" type="textarea" :rows="2"
                :placeholder="replyTarget ? '回复 @' + replyTarget.nickname + '...' : '说点什么...'"
                resize="none" />
              <div class="comment-actions">
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

        <!-- 右侧：B站风格合集侧边栏 -->
        <div class="course-right">
          <div class="playlist-sidebar">
            <div class="playlist-header">
              <span>合集</span>
              <span class="playlist-count">{{ totalVideos }} 个视频</span>
            </div>
            <div class="playlist-body">
              <div v-for="(ch, ci) in course.chapters" :key="ch.id" class="chapter-group">
                <!-- 章节标题（可折叠） -->
                <div class="chapter-header" @click="toggleChapter(ci)">
                  <el-icon :size="14" class="fold-icon">
                    <ArrowRight v-if="collapsedChapters.has(ci)" />
                    <ArrowDown v-else />
                  </el-icon>
                  <span class="chapter-label">{{ ch.title }}</span>
                  <span class="chapter-count">{{ ch.videos?.length || 0 }}P</span>
                </div>

                <!-- 视频列表 -->
                <div v-if="!collapsedChapters.has(ci)" class="video-list">
                  <div
                    v-for="v in ch.videos"
                    :key="v.id"
                    class="video-item"
                    :class="{ playing: currentVideo?.id === v.id }"
                    @click="playVideo(v, ch, ci)"
                  >
                    <span class="vi-index">
                      <el-icon v-if="currentVideo?.id === v.id" :size="14"><VideoPlay /></el-icon>
                      <span v-else>{{ v.sortOrder || 1 }}</span>
                    </span>
                    <div class="vi-info">
                      <div class="vi-title">{{ v.title }}</div>
                      <div class="vi-duration" v-if="v.duration">{{ fmtDur(v.duration) }}</div>
                    </div>
                  </div>
                </div>
              </div>
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
import { assetUrl } from '@/utils/assetUrl'
import { useRoute } from 'vue-router'
import {
  getCourseDetail, likeCourse, favoriteCourse,
  getComments, addComment, likeComment, deleteComment,
  type CourseDetailVO, type CommentVO, type VideoVO, type ChapterVO
} from '@/api/course'
import CommentItem from '@/components/CommentItem.vue'

const route = useRoute()
const course = ref<CourseDetailVO | null>(null)
const loading = ref(true)
const currentVideo = ref<VideoVO | null>(null)
const comments = ref<CommentVO[]>([])
const commentText = ref('')
const replyTarget = ref<{ id: number; nickname: string; userId: number } | null>(null)
const sortBy = ref<'hot' | 'newest'>('hot')
const myUserId = ref(0)
const collapsedChapters = ref(new Set<number>())

const totalVideos = computed(() => {
  if (!course.value) return 0
  return course.value.chapters.reduce((sum, ch) => sum + (ch.videos?.length || 0), 0)
})

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
    // 自动播放第一个章节的第一个视频
    for (const ch of course.value.chapters) {
      if (ch.videos?.length > 0) {
        playVideo(ch.videos[0], ch, 0)
        break
      }
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
  loadComments()
})

function playVideo(v: VideoVO, _ch: ChapterVO, _ci: number) {
  currentVideo.value = v
}

function toggleChapter(ci: number) {
  const set = new Set(collapsedChapters.value)
  if (set.has(ci)) set.delete(ci)
  else set.add(ci)
  collapsedChapters.value = set
}

function getVideoUrl(url: string) {
  return assetUrl(`/api/files/${url}`)
}
function formatNum(n: number) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
function fmtDur(sec: number) {
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
.course-page { min-height: 100vh; background: #f5f6f7; padding-bottom: 40px; }
.course-container { max-width: 1400px; margin: 0 auto; padding: 0 20px; }

.top-bar {
  display: flex; align-items: center; gap: 12px; padding: 12px 0;
  border-bottom: 1px solid #e3e5e7; margin-bottom: 16px;
}
.top-title { font-size: 14px; color: #61666d; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.course-layout { display: flex; gap: 20px; align-items: flex-start; }

/* ===== 左侧 ===== */
.course-left { flex: 1; min-width: 0; }

.player-box {
  background: #000; border-radius: 10px; overflow: hidden;
  aspect-ratio: 16/9; display: flex; align-items: center; justify-content: center;
}
.player { width: 100%; height: 100%; outline: none; }
.player-empty { text-align: center; color: #666; }
.player-empty p { margin-top: 14px; font-size: 15px; }

.video-info { padding: 14px 0 8px; }
.video-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; color: #18191c; }
.video-meta { font-size: 13px; color: #9499a0; display: flex; gap: 16px; }

.course-desc-card {
  background: #fff; border-radius: 10px; padding: 16px 20px; margin-bottom: 16px;
}
.course-desc-card p { font-size: 13px; color: #61666d; line-height: 1.7; margin: 0 0 12px; }
.course-actions { display: flex; gap: 8px; }

.comment-box {
  background: #fff; border-radius: 10px; padding: 20px; margin-bottom: 16px;
}
.comment-box h3 { font-size: 15px; margin: 0 0 12px; }
.comment-input { margin-bottom: 10px; }
.comment-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
.comment-sort { display: flex; gap: 0; margin-bottom: 10px; }
.comment-sort span { font-size: 12px; color: #9499a0; cursor: pointer; padding: 4px 10px; border-radius: 4px; }
.comment-sort span.active { color: #00aeec; background: #e6f7ff; }
.comment-list { border-top: 1px solid #f0f0f0; padding-top: 10px; }

/* ===== 右侧合集侧边栏（B站风格） ===== */
.course-right { width: 340px; flex-shrink: 0; position: sticky; top: 12px; }

.playlist-sidebar {
  background: #fff; border-radius: 10px; overflow: hidden;
  max-height: calc(100vh - 40px); display: flex; flex-direction: column;
}
.playlist-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; border-bottom: 1px solid #f0f0f0;
  font-size: 14px; font-weight: 600;
}
.playlist-count { font-size: 12px; color: #9499a0; font-weight: 400; }
.playlist-body { overflow-y: auto; flex: 1; }

.chapter-header {
  display: flex; align-items: center; gap: 6px;
  padding: 12px 14px; cursor: pointer; font-size: 13px; font-weight: 600;
  background: #f9f9f9; border-bottom: 1px solid #f0f0f0;
  transition: background .15s;
}
.chapter-header:hover { background: #f0f0f0; }
.fold-icon { color: #9499a0; flex-shrink: 0; }
.chapter-label { flex: 1; color: #18191c; }
.chapter-count { font-size: 11px; color: #9499a0; font-weight: 400; }

.video-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px 10px 30px; cursor: pointer;
  border-bottom: 1px solid #fafafa;
  transition: background .12s;
}
.video-item:hover { background: #f5f6f7; }
.video-item.playing { background: #e6f7ff; }
.vi-index {
  width: 24px; text-align: center; font-size: 12px; color: #9499a0;
  flex-shrink: 0;
}
.video-item.playing .vi-index { color: #00aeec; }
.vi-info { flex: 1; min-width: 0; display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.vi-title {
  font-size: 13px; color: #18191c; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.video-item.playing .vi-title { color: #00aeec; font-weight: 600; }
.vi-duration { font-size: 11px; color: #c0c4cc; flex-shrink: 0; }

.loading-wrap { display: flex; justify-content: center; padding: 80px 0; }
.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

@media (max-width: 900px) {
  .course-layout { flex-direction: column; }
  .course-right { width: 100%; position: static; max-height: none; }
  .playlist-sidebar { max-height: 400px; }
}
</style>
