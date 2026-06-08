<template>
  <div class="admin-page">
    <div class="admin-container">
      <div class="admin-header">
        <h2>公告管理</h2>
        <el-button type="primary" @click="openEditor()">发布公告</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
        </el-table-column>
        <el-table-column label="优先级" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.priority === 2" type="danger" size="small">紧急</el-tag>
            <el-tag v-else-if="row.priority === 1" type="warning" size="small">重要</el-tag>
            <span v-else class="text-muted">普通</span>
          </template>
        </el-table-column>
        <el-table-column label="轮播" width="70">
          <template #default="{ row }">
            <el-switch
              :model-value="row.isCarousel === 1"
              @change="(val: boolean) => handleToggleCarousel(row, val)"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ row.publishedAt?.substring(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openEditor(row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button text type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          background
          layout="prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑公告' : '发布公告'"
      width="800px"
      top="5vh"
      destroy-on-close
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="简短摘要（列表展示用）" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category">
            <el-option label="通知公告" value="NOTICE" />
            <el-option label="学术活动" value="ACTIVITY" />
            <el-option label="考试信息" value="EXAM" />
            <el-option label="选课通知" value="COURSE" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="form.priority">
            <el-radio :value="0">普通</el-radio>
            <el-radio :value="1">重要</el-radio>
            <el-radio :value="2">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="封面图">
          <div class="cover-upload">
            <el-input v-model="form.coverImage" placeholder="封面图URL（MinIO路径）" />
            <el-upload
              :show-file-list="false"
              :before-upload="handleCoverUpload"
              accept="image/*"
              style="display: inline-block; margin-left: 8px;"
            >
              <el-button type="primary" plain>上传</el-button>
            </el-upload>
          </div>
          <img v-if="form.coverImage" :src="getImageUrl(form.coverImage)" class="cover-preview" />
        </el-form-item>
        <el-form-item label="正文">
          <div class="editor-toolbar">
            <el-upload
              :show-file-list="false"
              :before-upload="handleContentImageUpload"
              accept="image/*"
              style="display: inline-block;"
            >
              <el-button size="small">插入图片</el-button>
            </el-upload>
          </div>
          <el-input v-model="form.content" type="textarea" :rows="12" placeholder="富文本HTML内容（可用编辑器生成）" />
        </el-form-item>
        <el-form-item label="附件">
          <el-upload
            :before-upload="handleAttachmentUpload"
            :show-file-list="false"
          >
            <el-button size="small">上传附件</el-button>
          </el-upload>
          <div v-if="editAttachments.length > 0" class="attachment-list">
            <div v-for="att in editAttachments" :key="att.id" class="att-item">
              <span>{{ att.fileName }}</span>
              <el-button text type="danger" size="small" @click="removeAttachment(att.id)">删除</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          {{ isEdit ? '保存' : '发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAnnouncements, createAnnouncement, updateAnnouncement, deleteAnnouncement,
  toggleCarousel, uploadAttachment, deleteAttachment, uploadImage, getAttachments,
  type Announcement,
} from '@/api/announcement'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<Announcement[]>([])
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const editAttachments = ref<any[]>([])

const form = ref({
  title: '',
  summary: '',
  content: '',
  coverImage: '',
  category: 'NOTICE',
  priority: 0,
})

onMounted(() => fetchList())

async function fetchList() {
  loading.value = true
  try {
    // fetch all categories for admin
    const res = await getAnnouncements(undefined, currentPage.value, pageSize)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function openEditor(row?: Announcement) {
  isEdit.value = !!row
  editingId.value = row?.id || null
  if (row) {
    form.value = {
      title: row.title,
      summary: row.summary || '',
      content: row.content || '',
      coverImage: row.coverImage || '',
      category: row.category,
      priority: row.priority,
    }
    getAttachments(row.id).then(res => {
      editAttachments.value = res.data || []
    })
  } else {
    form.value = { title: '', summary: '', content: '', coverImage: '', category: 'NOTICE', priority: 0 }
    editAttachments.value = []
  }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (isEdit.value && editingId.value) {
      await updateAnnouncement(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await createAnnouncement(form.value)
      ElMessage.success('发布成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await deleteAnnouncement(id)
  ElMessage.success('删除成功')
  fetchList()
}

async function handleToggleCarousel(row: Announcement, val: boolean) {
  await toggleCarousel(row.id, val ? 1 : 0, row.carouselSort || 0)
  row.isCarousel = val ? 1 : 0
  ElMessage.success(val ? '已加入轮播' : '已移出轮播')
}

async function handleCoverUpload(file: File) {
  const res = await uploadImage(file)
  form.value.coverImage = res.data
  ElMessage.success('封面上传成功')
  return false
}

async function handleContentImageUpload(file: File) {
  const res = await uploadImage(file)
  const url = `/api/files/${res.data}`
  form.value.content += `<img src="${url}" alt="" style="max-width:100%;" />`
  ElMessage.success('图片已插入')
  return false
}

async function handleAttachmentUpload(file: File) {
  if (!editingId.value) {
    ElMessage.warning('请先保存公告后再上传附件')
    return false
  }
  await uploadAttachment(editingId.value, file)
  ElMessage.success('附件上传成功')
  // refresh attachment list
  const res = await getAttachments(editingId.value)
  editAttachments.value = res.data || []
  return false
}

async function removeAttachment(attId: number) {
  if (!editingId.value) return
  await deleteAttachment(editingId.value, attId)
  editAttachments.value = editAttachments.value.filter(a => a.id !== attId)
  ElMessage.success('附件已删除')
}

function getImageUrl(coverImage: string) {
  if (coverImage.startsWith('http')) return coverImage
  return `/api/files/${coverImage}`
}

function categoryLabel(category: string) {
  const map: Record<string, string> = { NOTICE: '通知公告', ACTIVITY: '学术活动', EXAM: '考试信息', COURSE: '选课通知' }
  return map[category] || category
}
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px 0 60px;
}
.admin-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 20px;
}
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.admin-header h2 { margin: 0; }
.cover-upload { display: flex; align-items: center; }
.cover-preview { max-height: 120px; margin-top: 8px; border-radius: 6px; }
.editor-toolbar { margin-bottom: 8px; }
.attachment-list { margin-top: 8px; }
.att-item { display: flex; align-items: center; justify-content: space-between; padding: 4px 0; }
.text-muted { color: #bbb; font-size: 12px; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
</style>
