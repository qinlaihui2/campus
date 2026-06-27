<template>
  <div class="market-page">
    <div class="market-container">
      <div class="market-header">
        <h2>二手交易</h2>
        <el-button type="primary" @click="openPublish">发布商品</el-button>
      </div>

      <!-- 筛选栏 -->
      <div class="market-toolbar">
        <div class="toolbar-left">
          <el-select v-model="filters.category" placeholder="全部分类" clearable size="small" style="width:120px" @change="fetchList">
            <el-option label="电子数码" value="电子数码" />
            <el-option label="书籍教材" value="书籍教材" />
            <el-option label="衣物鞋包" value="衣物鞋包" />
            <el-option label="生活用品" value="生活用品" />
            <el-option label="运动健身" value="运动健身" />
            <el-option label="其他" value="其他" />
          </el-select>
          <el-select v-model="filters.condition" placeholder="全部成色" clearable size="small" style="width:110px" @change="fetchList">
            <el-option label="全新" value="NEW" />
            <el-option label="几乎全新" value="LIKE_NEW" />
            <el-option label="使用过" value="USED" />
            <el-option label="老旧" value="OLD" />
          </el-select>
          <el-input v-model="filters.keyword" placeholder="搜索商品..." size="small" clearable style="width:200px" @keyup.enter="fetchList" @clear="fetchList" />
        </div>
        <div class="toolbar-right">
          <el-radio-group v-model="filters.sort" size="small" @change="fetchList">
            <el-radio-button value="newest">最新</el-radio-button>
            <el-radio-button value="price_asc">价格↑</el-radio-button>
            <el-radio-button value="price_desc">价格↓</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- 卡片网格 -->
      <div class="market-grid" v-loading="loading">
        <div v-for="item in items" :key="item.id" class="market-card" @click="goDetail(item.id)">
          <div class="card-img" :style="{ backgroundImage: `url(${getFirstImage(item)})` }">
            <el-tag v-if="item.status === 'SOLD'" type="info" size="small" class="sold-tag">已售出</el-tag>
          </div>
          <div class="card-body">
            <div class="card-title">{{ item.title }}</div>
            <div class="card-desc">{{ item.description?.slice(0, 60) }}</div>
            <div class="card-bottom">
              <span class="card-price">¥{{ item.price }}</span>
              <span class="card-meta">{{ item.viewCount }} 次浏览</span>
            </div>
            <div class="card-poster">{{ item.userNickname || '同学' }}</div>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && items.length === 0" description="暂无商品" />

      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total"
          background layout="prev, pager, next" @current-change="fetchList" />
      </div>
    </div>

    <!-- 发布弹窗 -->
    <el-dialog v-model="dialogVisible" title="发布商品" width="580px" top="5vh" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="商品名称" maxlength="200" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述一下商品..." />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :min="0" :precision="2" placeholder="售价" style="width:200px" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" placeholder="可选" style="width:200px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option label="电子数码" value="电子数码" />
            <el-option label="书籍教材" value="书籍教材" />
            <el-option label="衣物鞋包" value="衣物鞋包" />
            <el-option label="生活用品" value="生活用品" />
            <el-option label="运动健身" value="运动健身" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="成色">
          <el-select v-model="form.condition" placeholder="选择成色">
            <el-option label="全新" value="NEW" />
            <el-option label="几乎全新" value="LIKE_NEW" />
            <el-option label="使用过" value="USED" />
            <el-option label="老旧" value="OLD" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片">
          <div class="upload-imgs">
            <div v-for="(url, i) in form.images" :key="i" class="upload-preview">
              <img :src="getImageUrl(url)" />
              <el-icon class="remove-btn" @click="removeImage(i)"><CircleClose /></el-icon>
            </div>
            <el-upload v-if="form.images.length < 6" :show-file-list="false" :before-upload="handleUpload" accept="image/*">
              <div class="upload-add"><el-icon :size="24"><Plus /></el-icon></div>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="handlePublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMarketItems, publishMarketItem, type MarketItemVO } from '@/api/market'
import request from '@/api/request'
import { assetUrl } from '@/utils/assetUrl'

const router = useRouter()
const items = ref<MarketItemVO[]>([])
const currentPage = ref(1)
const pageSize = 12
const total = ref(0)
const loading = ref(false)

const filters = reactive({
  category: '',
  condition: '',
  keyword: '',
  sort: 'newest',
})

async function fetchList() {
  loading.value = true
  try {
    const res = await getMarketItems({
      ...filters,
      page: currentPage.value,
      size: pageSize,
    })
    items.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/market/${id}`)
}

function getFirstImage(item: MarketItemVO) {
  const urls = item.imageUrls || []
  if (urls.length > 0) {
    return assetUrl(`/api/files/${urls[0]}`)
  }
  return 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><rect fill="%23f0f0f0" width="200" height="200"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%23c0c4cc" font-size="40">📦</text></svg>'
}

function getImageUrl(url: string) {
  return assetUrl(`/api/files/${url}`)
}

// --- 发布 ---
const dialogVisible = ref(false)
const publishing = ref(false)
const form = reactive({
  title: '',
  description: '',
  price: 0,
  originalPrice: undefined as number | undefined,
  category: '',
  condition: 'NEW' as string,
  images: [] as string[],
})

function openPublish() {
  form.title = ''
  form.description = ''
  form.price = 0
  form.originalPrice = undefined
  form.category = ''
  form.condition = 'NEW'
  form.images = []
  dialogVisible.value = true
}

function removeImage(i: number) {
  form.images.splice(i, 1)
}

async function handleUpload(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await request.post<any, { code: number; data: string }>('/upload/image', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    form.images.push(res.data)
  } catch { /* handled */ }
  return false
}

async function handlePublish() {
  if (!form.title || !form.description || !form.price || !form.category) {
    ElMessage.warning('请填写必要信息')
    return
  }
  publishing.value = true
  try {
    await publishMarketItem({
      title: form.title,
      description: form.description,
      price: form.price,
      originalPrice: form.originalPrice,
      category: form.category,
      condition: form.condition,
      images: form.images,
    })
    ElMessage.success('发布成功')
    dialogVisible.value = false
    currentPage.value = 1
    fetchList()
  } finally {
    publishing.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.market-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32px 0;
}
.market-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 20px;
}
.market-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.market-header h2 {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
.market-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar-left {
  display: flex;
  gap: 12px;
}
.market-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.market-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.market-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}
.card-img {
  width: 100%;
  height: 180px;
  background: #f0f0f0 center/cover no-repeat;
  position: relative;
}
.sold-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}
.card-body {
  padding: 12px 14px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-desc {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-price {
  font-size: 18px;
  font-weight: 700;
  color: #ff4757;
}
.card-meta { font-size: 12px; color: #c0c4cc; }
.card-poster { font-size: 12px; color: #606266; margin-top: 4px; }
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
.upload-imgs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.upload-preview {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}
.upload-preview img { width: 100%; height: 100%; object-fit: cover; }
.remove-btn {
  position: absolute;
  top: -4px;
  right: -4px;
  color: #ff4757;
  cursor: pointer;
  background: #fff;
  border-radius: 50%;
  font-size: 16px;
}
.upload-add {
  width: 80px;
  height: 80px;
  border: 1px dashed #c0c4cc;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #c0c4cc;
  transition: border-color 0.2s;
}
.upload-add:hover { border-color: #409eff; color: #409eff; }
</style>
