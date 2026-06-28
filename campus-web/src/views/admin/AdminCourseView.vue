<template>
  <div class="admin-page">
    <div class="admin-container">
      <div class="admin-header">
        <h2>课程管理</h2>
        <el-button type="primary" @click="openCourseDialog">创建课程</el-button>
      </div>

      <!-- 课程列表 -->
      <div class="course-table" v-loading="loading">
        <el-table :data="courses" stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="title" label="课程名称" min-width="180" />
          <el-table-column prop="instructor" label="讲师" width="100" />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" @click="openChapterDialog(row)">章节</el-button>
              <el-button size="small" @click="editCourse(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteCourse(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 章节管理弹窗 -->
      <el-dialog v-model="chapterDialogVisible" :title="`章节管理 - ${currentCourse?.title}`" width="800px" top="3vh" destroy-on-close>
        <div class="chapter-list" v-if="currentCourse">
          <div v-for="ch in chapters" :key="ch.id" class="chapter-block">
            <div class="ch-head">
              <span class="ch-label">{{ ch.title }}</span>
              <div>
                <el-button size="small" @click="openVideoDialog(ch)">添加视频</el-button>
                <el-button size="small" type="danger" @click="deleteChapter(currentCourse.id, ch.id)">删除</el-button>
              </div>
            </div>
            <!-- 视频列表 -->
            <div class="video-mini-list" v-if="ch.videos?.length > 0">
              <div v-for="v in ch.videos" :key="v.id" class="video-mini-item">
                <el-icon :size="14"><VideoPlay /></el-icon>
                <span class="v-title">{{ v.title }}</span>
                <span class="v-dur" v-if="v.duration">{{ v.duration }}秒</span>
                <el-button size="small" text type="danger" @click="deleteVideo(currentCourse.id, ch.id, v.id)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
            <div v-else class="no-video">暂无视频</div>
          </div>
        </div>

        <div class="add-chapter">
          <el-input v-model="newChapterTitle" placeholder="新章节标题" size="small" style="width:200px" />
          <el-button size="small" type="primary" :disabled="!newChapterTitle.trim()" @click="handleAddChapter">添加章节</el-button>
        </div>
      </el-dialog>

      <!-- 视频上传/添加弹窗 -->
      <el-dialog v-model="videoDialogVisible" :title="`添加视频到 ${currentChapter?.title}`" width="560px" top="10vh" destroy-on-close>
        <el-form label-width="80px">
          <el-form-item label="视频标题">
            <el-input v-model="videoForm.title" placeholder="如：第一课" />
          </el-form-item>
          <el-form-item label="视频文件">
            <el-upload
              ref="uploadRef"
              :show-file-list="true"
              :before-upload="beforeUpload"
              :http-request="handleUpload"
              :limit="1"
              accept="video/mp4,video/webm,video/quicktime,video/x-msvideo,video/x-matroska"
            >
              <el-button type="primary">
                <el-icon><Upload /></el-icon> 选择视频
              </el-button>
              <template #tip>
                <div class="upload-tip">支持 MP4、WebM、MOV、AVI、MKV，最大 500MB</div>
              </template>
            </el-upload>
          </el-form-item>
          <el-form-item label="上传进度" v-if="uploading">
            <el-progress :percentage="uploadPercent" :status="uploadPercent === 100 ? 'success' : ''" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="videoDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="uploading" :disabled="!videoForm.title">
            {{ uploading ? '上传中...' : '确认' }}
          </el-button>
        </template>
      </el-dialog>

      <!-- 课程创建/编辑弹窗 -->
      <el-dialog v-model="courseDialogVisible" :title="editingCourse ? '编辑课程' : '创建课程'" width="500px" top="8vh">
        <el-form label-width="80px">
          <el-form-item label="课程名称">
            <el-input v-model="courseForm.title" />
          </el-form-item>
          <el-form-item label="讲师">
            <el-input v-model="courseForm.instructor" />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="courseForm.category">
              <el-option label="计算机" value="计算机" />
              <el-option label="外语" value="外语" />
              <el-option label="数学" value="数学" />
              <el-option label="文学" value="文学" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="封面图">
            <div class="cover-upload">
              <el-input v-model="courseForm.coverImage" placeholder="封面图URL（MinIO路径）" />
              <el-upload
                :show-file-list="false"
                :before-upload="handleCoverUpload"
                accept="image/*"
                style="display:inline-block;margin-left:8px"
              >
                <el-button type="primary" plain>上传</el-button>
              </el-upload>
            </div>
            <img v-if="courseForm.coverImage" :src="getImageUrl(courseForm.coverImage)" class="cover-preview" />
          </el-form-item>
          <el-form-item label="简介">
            <el-input v-model="courseForm.description" type="textarea" :rows="3" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="courseDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveCourse">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadProps } from 'element-plus'
import { getCourses, uploadVideo, type CourseVO, type ChapterVO } from '@/api/course'
import { uploadImage } from '@/api/announcement'
import { assetUrl } from '@/utils/assetUrl'
import request from '@/api/request'

// ===== 课程列表 =====
const courses = ref<CourseVO[]>([])
const loading = ref(false)

async function fetchCourses() {
  loading.value = true
  try {
    const res = await getCourses({ size: 100 })
    courses.value = res.data.records
  } finally { loading.value = false }
}

// ===== 课程 CRUD =====
const courseDialogVisible = ref(false)
const editingCourse = ref<CourseVO | null>(null)
const courseForm = ref({ title: '', instructor: '', category: '', description: '', coverImage: '' })

function openCourseDialog() {
  editingCourse.value = null
  courseForm.value = { title: '', instructor: '', category: '', description: '', coverImage: '' }
  courseDialogVisible.value = true
}
function editCourse(row: CourseVO & { coverImage?: string }) {
  editingCourse.value = row
  courseForm.value = {
    title: row.title,
    instructor: row.instructor,
    category: row.category,
    description: row.description,
    coverImage: row.coverImage || '',
  }
  courseDialogVisible.value = true
}
async function handleCoverUpload(file: File) {
  try {
    const res = await uploadImage(file)
    courseForm.value.coverImage = res.data
    ElMessage.success('封面上传成功')
  } catch { /* ignore */ }
  return false
}
function getImageUrl(coverImage: string) {
  if (!coverImage) return ''
  return assetUrl(`/api/files/${coverImage}`)
}

async function handleSaveCourse() {
  try {
    if (editingCourse.value) {
      await request.put(`/admin/courses/${editingCourse.value.id}`, courseForm.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/admin/courses', courseForm.value)
      ElMessage.success('创建成功')
    }
    courseDialogVisible.value = false
    fetchCourses()
  } catch { /* handled */ }
}
async function deleteCourse(id: number) {
  try {
    await ElMessageBox.confirm('确认删除此课程？', '提示', { type: 'warning' })
    await request.delete(`/admin/courses/${id}`)
    ElMessage.success('删除成功')
    fetchCourses()
  } catch { /* cancel */ }
}

// ===== 章节管理 =====
const chapterDialogVisible = ref(false)
const currentCourse = ref<CourseVO | null>(null)
const chapters = ref<ChapterVO[]>([])
const newChapterTitle = ref('')

async function openChapterDialog(course: CourseVO) {
  currentCourse.value = course
  newChapterTitle.value = ''
  chapterDialogVisible.value = true
  await fetchChapters(course.id)
}
async function fetchChapters(courseId: number) {
  try {
    // 通过课程详情获取章节和视频
    const res = await request.get<any, { code: number; data: { chapters: ChapterVO[] } }>(`/courses/${courseId}`)
    chapters.value = res.data.chapters || []
  } catch { chapters.value = [] }
}
async function handleAddChapter() {
  if (!currentCourse.value || !newChapterTitle.value.trim()) return
  try {
    await request.post(`/admin/courses/${currentCourse.value.id}/chapters`, {
      title: newChapterTitle.value.trim(),
      sortOrder: chapters.value.length + 1,
    })
    ElMessage.success('章节已添加')
    newChapterTitle.value = ''
    await fetchChapters(currentCourse.value.id)
  } catch { /* handled */ }
}
async function deleteChapter(courseId: number, chapterId: number) {
  try {
    await ElMessageBox.confirm('删除章节会同时删除所有视频，确认？', '提示', { type: 'warning' })
    await request.delete(`/admin/courses/${courseId}/chapters/${chapterId}`)
    ElMessage.success('已删除')
    await fetchChapters(courseId)
  } catch { /* cancel */ }
}

// ===== 视频上传 =====
const videoDialogVisible = ref(false)
const currentChapter = ref<ChapterVO | null>(null)
const videoForm = ref({ title: '' })
const uploading = ref(false)
const uploadPercent = ref(0)

function openVideoDialog(chapter: ChapterVO) {
  currentChapter.value = chapter
  videoForm.value = { title: '' }
  uploading.value = false
  uploadPercent.value = 0
  videoDialogVisible.value = true
}

function beforeUpload(file: File) {
  const allowed = ['video/mp4', 'video/webm', 'video/quicktime', 'video/x-msvideo', 'video/x-matroska']
  if (!allowed.includes(file.type)) {
    ElMessage.error('不支持的视频格式，支持 MP4、WebM、MOV、AVI、MKV')
    return false
  }
  if (file.size > 500 * 1024 * 1024) {
    ElMessage.error('文件不能超过 500MB')
    return false
  }
  return true
}

async function handleUpload(options: { file: File; onProgress: (e: { percent: number }) => void }) {
  uploading.value = true
  uploadPercent.value = 0

  try {
    const res = await uploadVideo(options.file)
    const objectName = res.data

    // 视频上传成功，保存到章节
    if (currentChapter.value && currentCourse.value) {
      await request.post(
        `/admin/courses/${currentCourse.value.id}/chapters/${currentChapter.value.id}/videos`,
        {
          title: videoForm.value.title || options.file.name,
          videoUrl: objectName,
          duration: 0,
          sortOrder: (currentChapter.value.videos?.length || 0) + 1,
        }
      )
      ElMessage.success('视频上传成功')
    }

    uploadPercent.value = 100
    videoDialogVisible.value = false
    if (currentCourse.value) await fetchChapters(currentCourse.value.id)
  } catch (e: any) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

async function deleteVideo(courseId: number, chapterId: number, videoId: number) {
  try {
    await request.delete(`/admin/courses/${courseId}/chapters/${chapterId}/videos/${videoId}`)
    ElMessage.success('已删除')
    await fetchChapters(courseId)
  } catch { /* handled */ }
}

onMounted(fetchCourses)
</script>

<style scoped>
.admin-page { min-height: 100vh; background: #f5f6f7; padding: 32px 0; }
.admin-container { max-width: 1100px; margin: 0 auto; padding: 0 20px; }
.admin-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.admin-header h2 { font-size: 22px; font-weight: 600; }

.course-table { background: #fff; border-radius: 10px; padding: 12px; }

.chapter-block {
  border: 1px solid #f0f0f0; border-radius: 8px; padding: 12px 14px; margin-bottom: 12px;
}
.ch-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.ch-label { font-weight: 600; font-size: 15px; }

.video-mini-list { margin-left: 12px; }
.video-mini-item {
  display: flex; align-items: center; gap: 8px; padding: 6px 8px;
  border-radius: 6px; font-size: 13px;
}
.video-mini-item:hover { background: #f5f6f7; }
.v-title { flex: 1; }
.v-dur { color: #9499a0; font-size: 12px; }
.no-video { color: #c0c4cc; font-size: 12px; margin-left: 12px; padding: 8px 0; }

.add-chapter { display: flex; gap: 10px; margin-top: 16px; }

.upload-tip { font-size: 12px; color: #9499a0; margin-top: 4px; }
.cover-upload { display: flex; align-items: center; }
.cover-preview { max-height: 120px; margin-top: 8px; border-radius: 6px; }
</style>
