<template>
  <div class="lf-page">
    <div class="lf-container">
      <div class="lf-header">
        <h2>失物招领</h2>
        <el-button type="primary" @click="openPublish">发布信息</el-button>
      </div>

      <!-- 筛选栏 -->
      <div class="lf-toolbar">
        <div class="type-tabs">
          <span :class="{ active: activeType === '' }" @click="activeType = ''; fetchList()">全部</span>
          <span :class="{ active: activeType === 'LOST' }" @click="activeType = 'LOST'; fetchList()">寻物</span>
          <span :class="{ active: activeType === 'FOUND' }" @click="activeType = 'FOUND'; fetchList()">拾到</span>
        </div>
        <el-select v-model="activeCategory" placeholder="分类" clearable size="small" style="width:120px" @change="fetchList">
          <el-option label="证件" value="证件" />
          <el-option label="电子数码" value="电子数码" />
          <el-option label="衣物饰品" value="衣物饰品" />
          <el-option label="书籍文具" value="书籍文具" />
          <el-option label="钥匙钱包" value="钥匙钱包" />
          <el-option label="其他" value="其他" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索..." size="small" clearable style="width:200px" @keyup.enter="fetchList" @clear="fetchList" />
      </div>

      <!-- 卡片列表 -->
      <div class="lf-grid" v-loading="loading">
        <div v-for="item in list" :key="item.id" class="lf-card" @click="goDetail(item.id)">
          <div class="card-img" :style="{ backgroundImage: `url(${getFirstImage(item.imageUrls)})` }">
            <el-tag :type="item.type === 'LOST' ? 'danger' : 'success'" size="small" class="type-tag">
              {{ item.type === 'LOST' ? '寻物' : '拾到' }}
            </el-tag>
            <el-tag v-if="item.status === 'RESOLVED'" type="info" size="small" class="status-tag">已完结</el-tag>
          </div>
          <div class="card-body">
            <div class="card-title">{{ item.title }}</div>
            <div class="card-loc"><el-icon><Location /></el-icon> {{ item.location || '未知地点' }}</div>
            <div class="card-meta">
              <span>{{ item.publisherName }}</span>
              <span>{{ formatDate(item.publishedAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && list.length === 0" description="暂无信息" />

      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total"
          background layout="prev, pager, next" @current-change="fetchList" />
      </div>
    </div>

    <!-- 发布弹窗 -->
    <el-dialog v-model="dialogVisible" title="发布失物招领" width="560px" top="5vh" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio value="LOST">寻物</el-radio>
            <el-radio value="FOUND">拾到</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="物品名称">
          <el-input v-model="form.title" placeholder="例如：校园卡、AirPods" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option label="证件" value="证件" />
            <el-option label="电子数码" value="电子数码" />
            <el-option label="衣物饰品" value="衣物饰品" />
            <el-option label="书籍文具" value="书籍文具" />
            <el-option label="钥匙钱包" value="钥匙钱包" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.location" placeholder="捡到或丢失的地点" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="详细描述..." />
        </el-form-item>
        <el-form-item label="图片">
          <div class="upload-imgs">
            <div v-for="(url, i) in form.imageUrls" :key="i" class="upload-preview">
              <img :src="getImageUrl(url)" />
              <el-icon class="remove-btn" @click="removeImage(i)"><CircleClose /></el-icon>
            </div>
            <el-upload v-if="form.imageUrls.length < 6" :show-file-list="false" :before-upload="handleUpload" accept="image/*">
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPosts, publishPost, type LostFoundPost } from '@/api/lostfound'
import { uploadImage } from '@/api/announcement'

const router = useRouter()
const list = ref<LostFoundPost[]>([])
const activeType = ref('')
const activeCategory = ref('')
const keyword = ref('')
const currentPage = ref(1)
const pageSize = 12
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const publishing = ref(false)

const form = ref({
  type: 'LOST',
  title: '',
  category: '',
  location: '',
  description: '',
  imageUrls: [] as string[],
})

onMounted(() => fetchList())

async function fetchList() {
  loading.value = true
  try {
    const res = await getPosts(activeType.value || undefined, activeCategory.value || undefined, keyword.value || undefined, currentPage.value, pageSize)
    list.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function openPublish() {
  form.value = { type: 'LOST', title: '', category: '', location: '', description: '', imageUrls: [] }
  dialogVisible.value = true
}

async function handleUpload(file: File) {
  try {
    const res = await uploadImage(file)
    form.value.imageUrls.push(res.data)
    ElMessage.success('上传成功')
  } catch { ElMessage.error('上传失败') }
  return false
}

function removeImage(i: number) { form.value.imageUrls.splice(i, 1) }

async function handlePublish() {
  if (!form.value.title.trim()) { ElMessage.warning('请输入物品名称'); return }
  publishing.value = true
  try {
    await publishPost({
      ...form.value,
      imageUrls: form.value.imageUrls.join(','),
    } as any)
    ElMessage.success('发布成功')
    dialogVisible.value = false
    fetchList()
  } finally { publishing.value = false }
}

function goDetail(id: number) { router.push(`/lost-found/${id}`) }

function getFirstImage(urls: string) {
  if (!urls) return ''
  const first = urls.split(',')[0]
  if (first.startsWith('http')) return first
  return `/api/files/${first}`
}

function getImageUrl(url: string) {
  if (url.startsWith('http')) return url
  return `/api/files/${url}`
}

function formatDate(d: string) {
  if (!d) return ''
  const now = new Date()
  const date = new Date(d)
  const diff = now.getTime() - date.getTime()
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.substring(0, 10)
}
</script>

<style scoped>
.lf-page { min-height: 100vh; background: #f5f7fa; padding: 24px 0 60px; }
.lf-container { max-width: 1040px; margin: 0 auto; padding: 0 20px; }
.lf-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.lf-header h2 { margin: 0; font-size: 22px; }
.lf-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.type-tabs { display: flex; background: #fff; border-radius: 8px; overflow: hidden; }
.type-tabs span { padding: 8px 20px; cursor: pointer; color: #666; transition: all 0.2s; }
.type-tabs span:hover { color: #409eff; }
.type-tabs span.active { background: #409eff; color: #fff; }
.lf-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.lf-card { background: #fff; border-radius: 10px; overflow: hidden; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }
.lf-card:hover { transform: translateY(-3px); box-shadow: 0 6px 16px rgba(0,0,0,0.08); }
.card-img { height: 140px; background-size: contain; background-position: center; background-repeat: no-repeat; background-color: #f0f0f0; position: relative; }
.type-tag { position: absolute; top: 8px; left: 8px; }
.status-tag { position: absolute; top: 8px; right: 8px; }
.card-body { padding: 12px; }
.card-title { font-size: 15px; font-weight: 500; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-loc { font-size: 12px; color: #999; display: flex; align-items: center; gap: 4px; margin-bottom: 8px; }
.card-meta { display: flex; justify-content: space-between; font-size: 12px; color: #bbb; }
.upload-imgs { display: flex; flex-wrap: wrap; gap: 8px; }
.upload-preview { width: 80px; height: 80px; border-radius: 6px; overflow: hidden; position: relative; }
.upload-preview img { width: 100%; height: 100%; object-fit: cover; }
.upload-preview .remove-btn { position: absolute; top: 0; right: 0; color: #f56c6c; cursor: pointer; background: #fff; border-radius: 50%; }
.upload-add { width: 80px; height: 80px; border: 2px dashed #ddd; border-radius: 6px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #ccc; }
.upload-add:hover { border-color: #409eff; color: #409eff; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 24px; }
</style>
