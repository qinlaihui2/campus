<template>
  <div class="md-page">
    <div class="md-container">
      <el-button text @click="$router.push('/market')" style="margin-bottom:16px">
        <el-icon><ArrowLeft /></el-icon> 返回市场
      </el-button>

      <div v-if="loading" class="loading-wrap"><el-icon class="is-loading" :size="32"><Loading /></el-icon></div>

      <template v-else-if="item">
        <!-- 商品详情 -->
        <div class="md-card">
          <div class="md-gallery">
            <img v-if="mainImage" :src="mainImage" class="main-img" />
            <div v-else class="main-img-placeholder">📦</div>
            <div class="thumb-list" v-if="allImages.length > 1">
              <img v-for="(url, i) in allImages" :key="i" :src="url" class="thumb" :class="{ active: i === selectedImage }" @click="selectedImage = i" />
            </div>
          </div>
          <div class="md-info">
            <div class="md-status">
              <el-tag v-if="item.status === 'ON_SALE'" type="success">在售</el-tag>
              <el-tag v-else-if="item.status === 'SOLD'" type="info">已售出</el-tag>
              <span class="md-category">{{ item.category }}</span>
              <span class="md-condition">{{ conditionLabel(item.condition) }}</span>
            </div>
            <h2 class="md-title">{{ item.title }}</h2>
            <div class="md-price">¥{{ item.price }}</div>
            <div class="md-original" v-if="item.originalPrice">原价 ¥{{ item.originalPrice }}</div>
            <div class="md-poster">
              <el-avatar :size="36" :src="getAvatarUrl(item.userAvatar)" />
              <span>{{ item.userNickname || '同学' }}</span>
            </div>
            <div class="md-desc">{{ item.description }}</div>
            <div class="md-meta">
              <span>{{ item.viewCount }} 次浏览</span>
              <span>{{ timeAgo(item.createdAt) }}</span>
            </div>

            <div class="md-actions">
              <el-button :type="item.liked ? 'danger' : 'default'" @click="handleLike">
                <el-icon :size="16"><CaretTop /></el-icon>
                {{ item.likeCount }}
              </el-button>
              <template v-if="isOwner && item.status === 'ON_SALE'">
                <el-button type="success" @click="handleMarkSold">标记已售</el-button>
              </template>
              <template v-if="!isOwner && item.status === 'ON_SALE'">
                <el-button type="warning" @click="showOfferDialog = true">出价</el-button>
              </template>
            </div>
          </div>
        </div>

        <!-- 出价列表（仅卖家和已出价买家可见） -->
        <div v-if="isOwner || myOffers.length > 0" class="offers-section">
          <h3 class="section-title">出价记录</h3>
          <div v-if="offers.length === 0 && myOffers.length === 0" class="no-offers">暂无出价</div>
          <div v-for="offer in [...offers, ...myOffers]" :key="offer.id" class="offer-item">
            <el-avatar :size="28" :src="getAvatarUrl(isOwner ? offer.buyerAvatar : offer.sellerAvatar)" />
            <span class="offer-user">{{ isOwner ? offer.buyerName : offer.sellerName }}</span>
            <span class="offer-price">¥{{ offer.price }}</span>
            <span v-if="offer.message" class="offer-msg">{{ offer.message }}</span>
            <el-tag :type="offerStatusTag(offer.status)" size="small">{{ offerStatusLabel(offer.status) }}</el-tag>
            <template v-if="isOwner && offer.status === 'PENDING'">
              <el-button size="small" type="success" @click="handleAccept(offer.id)">接受</el-button>
              <el-button size="small" @click="handleReject(offer.id)">拒绝</el-button>
            </template>
          </div>
        </div>

        <!-- 评论 -->
        <div class="comments-section">
          <h3 class="section-title">评论</h3>
          <div class="comment-input">
            <el-input v-model="commentText" type="textarea" :rows="2" :placeholder="replyTo ? `回复 ${replyTo.nickname}...` : '写评论...'" />
            <div class="comment-actions" v-if="replyTo">
              <el-button size="small" text @click="replyTo = null">取消回复</el-button>
            </div>
            <el-button size="small" type="primary" style="margin-top:8px" @click="handleAddComment">发表评论</el-button>
          </div>
          <div class="comment-list" v-if="comments.length > 0">
            <CommentItem
              v-for="c in comments"
              :key="c.id"
              :comment="c"
              :depth="0"
              :my-user-id="myUserId"
              @reply="onReply"
              @delete="onDeleteComment"
            />
          </div>
        </div>
      </template>

      <el-result v-else icon="error" title="商品不存在或已删除" />
    </div>

    <!-- 出价弹窗 -->
    <el-dialog v-model="showOfferDialog" title="出价" width="400px" top="20vh">
      <el-form>
        <el-form-item label="商品">
          <span>{{ item?.title }}</span>
        </el-form-item>
        <el-form-item label="出售价">
          <span class="current-price">¥{{ item?.price }}</span>
        </el-form-item>
        <el-form-item label="我的出价">
          <el-input-number v-model="offerPrice" :min="1" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="留言">
          <el-input v-model="offerMessage" placeholder="可选留言" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showOfferDialog = false">取消</el-button>
        <el-button type="primary" :loading="offering" @click="handleOffer">确认出价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star } from '@element-plus/icons-vue'
import CommentItem from '@/components/CommentItem.vue'
import type { MarketItemVO, MarketCommentVO, OfferVO } from '@/api/market'
import {
  getMarketItemDetail, likeMarketItem, getMarketComments, addMarketComment,
  deleteMarketComment, makeOffer, getReceivedOffers, getSentOffers,
  acceptOffer, rejectOffer, markItemSold,
} from '@/api/market'
import { assetUrl } from '@/utils/assetUrl'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

const item = ref<MarketItemVO | null>(null)
const loading = ref(true)
const selectedImage = ref(0)
const comments = ref<MarketCommentVO[]>([])
const offers = ref<OfferVO[]>([])
const myOffers = ref<OfferVO[]>([])
const commentText = ref('')
const replyTo = ref<{ commentId: number; userId: number; nickname: string } | null>(null)

// 出价弹窗
const showOfferDialog = ref(false)
const offerPrice = ref(0)
const offerMessage = ref('')
const offering = ref(false)

const myUserId = computed(() => {
  const token = localStorage.getItem('accessToken')
  if (!token) return 0
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return Number(payload.userId) || 0
  } catch { return 0 }
})

const isOwner = computed(() => myUserId.value > 0 && item.value?.userId === myUserId.value)

const allImages = computed(() => {
  if (!item.value) return []
  return (item.value.imageUrls || []).map(u => assetUrl(`/api/files/${u}`))
})
const mainImage = computed(() => allImages.value[selectedImage.value] || allImages.value[0])

function getAvatarUrl(avatar?: string | null) {
  if (!avatar) return ''
  return assetUrl(`/api/files/${avatar}`)
}

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getMarketItemDetail(id)
    item.value = res.data
    fetchComments()
    if (myUserId.value) {
      if (isOwner.value) fetchReceivedOffers()
      fetchSentOffers()
    }
  } finally {
    loading.value = false
  }
}

async function fetchComments() {
  try {
    const res = await getMarketComments(id)
    comments.value = res.data
  } catch { /* handled */ }
}

async function fetchReceivedOffers() {
  try {
    const res = await getReceivedOffers()
    offers.value = res.data.records.filter(o => o.itemId === id)
  } catch { /* handled */ }
}

async function fetchSentOffers() {
  try {
    const res = await getSentOffers()
    myOffers.value = res.data.records.filter(o => o.itemId === id)
  } catch { /* handled */ }
}

async function handleLike() {
  if (!item.value) return
  try {
    const res = await likeMarketItem(id)
    item.value.liked = res.data.liked
    item.value.likeCount += res.data.liked ? 1 : -1
  } catch { /* handled */ }
}

async function handleMarkSold() {
  try {
    await ElMessageBox.confirm('确认将此商品标记为已售？', '提示')
    await markItemSold(id)
    if (item.value) item.value.status = 'SOLD'
    ElMessage.success('已标记为已售')
  } catch { /* cancel */ }
}

function onReply(comment: { id: number; userId: number; userNickname: string }) {
  replyTo.value = { commentId: comment.id, userId: comment.userId, nickname: comment.userNickname || '同学' }
}

async function handleAddComment() {
  if (!commentText.value.trim()) return
  try {
    const parentId = replyTo.value?.commentId
    const replyToUserId = replyTo.value?.userId
    await addMarketComment(id, commentText.value.trim(), parentId, replyToUserId)
    commentText.value = ''
    replyTo.value = null
    fetchComments()
  } catch { /* handled */ }
}

async function onDeleteComment(comment: { id: number }) {
  try {
    await deleteMarketComment(comment.id)
    fetchComments()
  } catch { /* handled */ }
}

async function handleOffer() {
  if (offerPrice.value <= 0) { ElMessage.warning('请输入有效出价'); return }
  offering.value = true
  try {
    await makeOffer(id, offerPrice.value, offerMessage.value)
    ElMessage.success('出价成功')
    showOfferDialog.value = false
    fetchReceivedOffers()
  } finally {
    offering.value = false
  }
}

async function handleAccept(offerId: number) {
  try {
    await ElMessageBox.confirm('确认接受此出价？商品将标记为已售。', '提示')
    await acceptOffer(offerId)
    ElMessage.success('已接受出价')
    if (item.value) item.value.status = 'SOLD'
    fetchReceivedOffers()
  } catch { /* cancel */ }
}

async function handleReject(offerId: number) {
  try {
    await rejectOffer(offerId)
    ElMessage.success('已拒绝出价')
    fetchReceivedOffers()
  } catch { /* handled */ }
}

function conditionLabel(c: string) {
  const map: Record<string, string> = { NEW: '全新', LIKE_NEW: '几乎全新', USED: '使用过', OLD: '老旧' }
  return map[c] || c
}
function offerStatusLabel(s: string) {
  const map: Record<string, string> = { PENDING: '待处理', ACCEPTED: '已接受', REJECTED: '已拒绝', CANCELLED: '已取消' }
  return map[s] || s
}
function offerStatusTag(s: string) {
  const map: Record<string, string> = { PENDING: 'warning', ACCEPTED: 'success', REJECTED: 'danger', CANCELLED: 'info' }
  return map[s] || 'info'
}
function timeAgo(dateStr: string) {
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins} 分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  return new Date(dateStr).toLocaleDateString()
}

onMounted(fetchDetail)
</script>

<style scoped>
.md-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32px 0;
}
.md-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}
.loading-wrap { text-align: center; padding: 80px 0; color: #909399; }
.md-card {
  display: flex;
  gap: 32px;
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
}
.md-gallery { width: 360px; flex-shrink: 0; }
.main-img { width: 100%; height: 300px; object-fit: cover; border-radius: 8px; }
.main-img-placeholder {
  width: 100%; height: 300px; display: flex; align-items: center; justify-content: center;
  background: #f0f0f0; border-radius: 8px; font-size: 64px;
}
.thumb-list { display: flex; gap: 8px; margin-top: 8px; }
.thumb { width: 56px; height: 56px; object-fit: cover; border-radius: 6px; cursor: pointer; border: 2px solid transparent; }
.thumb.active { border-color: #409eff; }
.md-info { flex: 1; min-width: 0; }
.md-status { display: flex; gap: 8px; align-items: center; margin-bottom: 12px; }
.md-category, .md-condition { font-size: 12px; color: #909399; }
.md-title { font-size: 22px; font-weight: 700; color: #303133; margin-bottom: 12px; }
.md-price { font-size: 28px; font-weight: 700; color: #ff4757; }
.md-original { font-size: 14px; color: #c0c4cc; text-decoration: line-through; margin-bottom: 12px; }
.md-poster { display: flex; align-items: center; gap: 8px; margin: 16px 0; }
.md-desc { font-size: 14px; color: #606266; line-height: 1.8; white-space: pre-wrap; margin-bottom: 16px; }
.md-meta { display: flex; gap: 16px; font-size: 12px; color: #c0c4cc; margin-bottom: 20px; }
.md-actions { display: flex; gap: 12px; }

.offers-section, .comments-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
}
.section-title { font-size: 17px; font-weight: 600; margin-bottom: 16px; color: #303133; }
.no-offers { color: #c0c4cc; font-size: 14px; padding: 12px 0; }
.offer-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.offer-item:last-child { border: none; }
.offer-user { font-weight: 500; min-width: 60px; }
.offer-price { font-weight: 700; color: #ff4757; min-width: 80px; }
.offer-msg { color: #909399; flex: 1; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.comment-input { margin-bottom: 20px; }
.comment-actions { margin-top: 6px; }
.comment-list { margin-top: 16px; }

@media (max-width: 768px) {
  .md-card { flex-direction: column; }
  .md-gallery { width: 100%; }
}
</style>
