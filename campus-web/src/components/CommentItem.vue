<template>
  <div class="comment-node">
    <div class="comment-row">
      <el-avatar :size="depth === 0 ? 32 : 28" :src="getAvatarUrl(comment.userAvatar)">
        <el-icon><UserFilled /></el-icon>
      </el-avatar>
      <div class="comment-main">
        <div class="comment-head">
          <span class="comment-name">{{ comment.userNickname }}</span>
          <span class="comment-time">{{ timeAgo(comment.createdAt) }}</span>
        </div>
        <div class="comment-text">
          <template v-if="comment.replyToUserNickname">
            回复 <span class="reply-to">@{{ comment.replyToUserNickname }}</span>：
          </template>
          {{ comment.content }}
        </div>
        <div class="comment-foot">
          <span class="foot-action like-action" :class="{ liked: comment.liked }" @click="$emit('like', comment)">
            <el-icon :size="14"><CaretTop /></el-icon>
            <span>{{ comment.likeCount || '赞' }}</span>
          </span>
          <span class="foot-action" @click="$emit('reply', comment)">回复</span>
          <span v-if="comment.userId === myUserId" class="foot-action delete" @click="$emit('delete', comment)">删除</span>
        </div>
        <!-- 递归渲染子回复 -->
        <div v-if="comment.replies && comment.replies.length > 0" class="nested-replies">
          <CommentItem
            v-for="child in comment.replies"
            :key="child.id"
            :comment="child"
            :depth="depth + 1"
            :my-user-id="myUserId"
            @reply="$emit('reply', $event)"
            @like="$emit('like', $event)"
            @delete="$emit('delete', $event)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 通用评论组件，兼容 square 和 course 两种 CommentVO（结构相同）
interface GenericComment {
  id: number
  userId: number
  userNickname: string
  userAvatar: string
  replyToUserId: number | null
  replyToUserNickname: string | null
  content: string
  likeCount: number
  liked: boolean
  createdAt: string
  replies: GenericComment[] | null
}

defineProps<{
  comment: GenericComment
  depth: number
  myUserId: number
}>()

defineEmits<{
  reply: [comment: GenericComment]
  like: [comment: GenericComment]
  delete: [comment: GenericComment]
}>()

import { assetUrl } from '@/utils/assetUrl'

function getAvatarUrl(avatar: string) {
  if (!avatar) return ''
  return assetUrl(`/api/files/${avatar}`)
}

function timeAgo(dateStr: string) {
  const diff = Date.now() - new Date(dateStr).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return m + '分钟前'
  const h = Math.floor(m / 60)
  if (h < 24) return h + '小时前'
  return dateStr.substring(0, 10)
}
</script>

<style scoped>
.comment-node { margin-bottom: 2px; }
.comment-row { display: flex; gap: 8px; }
.comment-main { flex: 1; min-width: 0; }
.comment-head { margin-bottom: 2px; }
.comment-name { font-size: 13px; font-weight: 500; color: #606266; }
.comment-time { font-size: 11px; color: #c0c4cc; margin-left: 6px; }
.comment-text { font-size: 14px; color: #303133; line-height: 1.6; word-break: break-word; }
.reply-to { color: #409eff; }
.comment-foot { display: flex; gap: 14px; margin-top: 4px; }
.foot-action {
  font-size: 12px; color: #999; cursor: pointer; display: inline-flex; align-items: center; gap: 2px;
}
.foot-action:hover { color: #666; }
.foot-action.liked { color: #ff4757; font-weight: 600; }
.foot-action.like-action:hover { color: #ff4757; }
.foot-action.delete { color: #ccc; }
.foot-action.delete:hover { color: #f56c6c; }
.nested-replies {
  margin-top: 6px;
  padding-left: 16px;
  border-left: 2px solid #f0f0f0;
}
</style>
