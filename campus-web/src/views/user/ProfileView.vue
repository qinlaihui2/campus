<template>
  <div class="profile-page">
    <div class="profile-container">
      <h2>个人信息</h2>

      <div class="profile-card">
        <!-- 头像 -->
        <div class="avatar-section">
          <el-avatar :size="80" :src="avatarUrl">
            <el-icon :size="40"><UserFilled /></el-icon>
          </el-avatar>
          <el-upload
            :show-file-list="false"
            :before-upload="handleAvatarUpload"
            accept="image/*"
          >
            <el-button type="primary" size="small" :loading="uploading">更换头像</el-button>
          </el-upload>
        </div>

        <!-- 表单 -->
        <el-form :model="form" label-width="80px" class="profile-form">
          <el-form-item label="用户名">
            <el-input v-model="form.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="你的昵称" maxlength="20" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" placeholder="可选" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="可选" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
            <el-button @click="$router.push('/')">返回</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserProfile, updateUserProfile, type UserInfo } from '@/api/auth'
import request from '@/api/request'
import { assetUrl } from '@/utils/assetUrl'

const form = ref<UserInfo>({
  id: 0, username: '', nickname: '', email: '', phone: '', avatar: '', role: ''
})
const saving = ref(false)
const uploading = ref(false)

const avatarUrl = computed(() => {
  if (!form.value.avatar) return ''
  return assetUrl(`/api/files/${form.value.avatar}`)
})

onMounted(async () => {
  try {
    const res = await getUserProfile()
    form.value = res.data
  } catch { ElMessage.error('加载用户信息失败') }
})

async function handleAvatarUpload(file: File) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await request.post<any, { code: number; data: string }>('/upload/image', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    form.value.avatar = res.data
    await updateUserProfile({ avatar: res.data })
    ElMessage.success('头像已更新')
  } catch { ElMessage.error('上传失败') }
  finally { uploading.value = false }
  return false
}

async function handleSave() {
  saving.value = true
  try {
    await updateUserProfile({
      nickname: form.value.nickname,
      email: form.value.email,
      phone: form.value.phone,
    })
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32px 0;
}
.profile-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 20px;
}
.profile-container h2 {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
}
.profile-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #ebeef5;
}
.profile-form {
  max-width: 400px;
}
</style>
