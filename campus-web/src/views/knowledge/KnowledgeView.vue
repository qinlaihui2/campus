<template>
  <div class="knowledge-layout">
    <header class="kb-header">
      <el-button text @click="$router.push('/')">
        <el-icon><ArrowLeft /></el-icon> 返回对话
      </el-button>
      <h2>知识库管理</h2>
      <el-button v-if="userStore.userInfo?.role === 'ADMIN'" type="primary" @click="showAddKBDialog">
        <el-icon><Plus /></el-icon> 新建分类
      </el-button>
    </header>

    <el-tabs v-model="activeKB" type="card">
      <el-tab-pane v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name" :name="String(kb.id)">
        <div class="kb-toolbar">
          <span class="kb-desc">{{ kb.description }}</span>
          <el-upload :action="uploadUrl" :headers="uploadHeaders" :data="{ knowledgeBaseId: kb.id }"
            :on-success="handleUploadSuccess" :before-upload="beforeUpload" :show-file-list="false"
            accept=".pdf,.docx,.md,.txt">
            <el-button type="primary" size="small">
              <el-icon><Upload /></el-icon> 上传文档
            </el-button>
          </el-upload>
        </div>

        <el-table :data="documents[kb.id] || []" v-loading="docLoading" style="width: 100%">
          <el-table-column prop="title" label="文档名称" min-width="250" />
          <el-table-column prop="fileType" label="类型" width="80" align="center" />
          <el-table-column label="大小" width="100" align="center">
            <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.parseStatus === 'COMPLETED'" type="success">已完成</el-tag>
              <el-tag v-else-if="row.parseStatus === 'PARSING'" type="warning">解析中</el-tag>
              <el-tag v-else-if="row.parseStatus === 'FAILED'" type="danger">失败</el-tag>
              <el-tag v-else type="info">等待中</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="chunkCount" label="切片数" width="80" align="center" />
          <el-table-column label="时间" width="180">
            <template #default="{ row }">
              {{ new Date(row.createdAt).toLocaleString('zh-CN') }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.isEnabled" :active-value="1" :inactive-value="0" size="small"
                @change="toggleDoc(row)" />
              <el-button text type="danger" size="small" @click="deleteDoc(row.id)" style="margin-left:8px">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建知识库对话框 -->
    <el-dialog v-model="kbDialogVisible" title="新建知识库分类" width="400px">
      <el-form :model="newKBForm">
        <el-form-item label="名称">
          <el-input v-model="newKBForm.name" placeholder="如：规章制度" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newKBForm.description" type="textarea" placeholder="分类描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="kbDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createKB">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const userStore = useUserStore()
const activeKB = ref('')
const knowledgeBases = ref<any[]>([])
const documents = ref<Record<number, any[]>>({})
const docLoading = ref(false)
const kbDialogVisible = ref(false)
const newKBForm = ref({ name: '', description: '' })

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
}))

const uploadUrl = '/api/document/upload'

onMounted(async () => {
  await userStore.fetchUserInfo()
  await loadKnowledgeBases()
})

watch(activeKB, async (newKbId) => {
  if (newKbId) {
    await loadDocuments(Number(newKbId))
  }
})

async function loadKnowledgeBases() {
  try {
    const res = await request.get('/knowledge-base/list')
    knowledgeBases.value = res.data
    if (knowledgeBases.value.length > 0) {
      activeKB.value = String(knowledgeBases.value[0].id)
      await loadDocuments(Number(activeKB.value))
    }
  } catch { }
}

async function loadDocuments(kbId: number) {
  docLoading.value = true
  try {
    const res = await request.get(`/document/list/${kbId}`, { params: { page: 1, size: 50 } })
    documents.value[kbId] = res.data.records
  } finally {
    docLoading.value = false
  }
}

function beforeUpload(file: File) {
  const allowed = ['pdf', 'docx', 'md', 'txt']
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !allowed.includes(ext)) {
    ElMessage.error('仅支持 PDF、DOCX、MD、TXT 格式')
    return false
  }
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  return true
}

function handleUploadSuccess(response: any) {
  if (response.code === 200) {
    ElMessage.success('上传成功，正在后台解析')
    loadDocuments(Number(activeKB.value))
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

async function toggleDoc(doc: any) {
  try {
    await request.put(`/document/${doc.id}/status`, null, { params: { enabled: doc.isEnabled } })
    ElMessage.success('更新成功')
  } catch { }
}

async function deleteDoc(id: number) {
  try {
    await request.delete(`/document/${id}`)
    ElMessage.success('删除成功')
    loadDocuments(Number(activeKB.value))
  } catch { }
}

async function createKB() {
  try {
    await request.post('/knowledge-base', newKBForm.value)
    ElMessage.success('创建成功')
    kbDialogVisible.value = false
    newKBForm.value = { name: '', description: '' }
    loadKnowledgeBases()
  } catch { }
}

function showAddKBDialog() {
  kbDialogVisible.value = true
}

function formatSize(bytes: number) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}
</script>

<style scoped>
.knowledge-layout {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px;
}

.kb-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.kb-header h2 {
  margin: 0;
  font-size: 20px;
  flex: 1;
}

.kb-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}

.kb-desc {
  color: #999;
  font-size: 13px;
}
</style>
