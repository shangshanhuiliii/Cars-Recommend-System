<template>
  <section class="profile-page">
    <div class="profile-toolbar">
      <el-button type="primary" plain :loading="loading" @click="loadProfile">刷新资料</el-button>
    </div>

    <el-alert
      v-if="message"
      class="state-alert"
      :type="messageType"
      :closable="false"
      :title="message"
      show-icon
    />

    <div class="profile-layout">
      <section class="panel profile-card">
        <div class="panel__body">
          <el-skeleton v-if="loading" :rows="8" animated />
          <template v-else>
            <div class="profile-card__head">
              <div class="avatar-mark">{{ avatarText }}</div>
              <div>
                <p class="eyebrow">当前账号</p>
                <h2>{{ form.nickname || profile.username || '用户' }}</h2>
                <span>{{ profile.username }}</span>
              </div>
            </div>

            <div class="profile-facts">
              <div>
                <span>账号状态</span>
                <strong>{{ statusLabel(profile.status) }}</strong>
              </div>
              <div>
                <span>注册时间</span>
                <strong>{{ formatDate(profile.createTime) }}</strong>
              </div>
              <div>
                <span>最近更新</span>
                <strong>{{ formatDate(profile.updateTime) }}</strong>
              </div>
            </div>
          </template>
        </div>
      </section>

      <section class="panel profile-form-panel">
        <div class="panel__body">
          <div class="section-title">
            <h2>个人信息</h2>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="saveProfile">
            <el-form-item label="用户名">
              <el-input :model-value="profile.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model.trim="form.nickname" maxlength="32" show-word-limit placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model.trim="form.email" autocomplete="email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model.trim="form.phone" autocomplete="tel" placeholder="可选，11 位手机号" />
            </el-form-item>

            <div class="profile-actions">
              <el-button :disabled="saving || loading" @click="resetForm">重置</el-button>
              <el-button type="primary" :loading="saving" :disabled="loading" @click="saveProfile">保存修改</el-button>
            </div>
          </el-form>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import { fetchUserProfile, updateUserProfile } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const messageType = ref('success')
const profile = ref({})
const form = reactive({
  nickname: '',
  email: '',
  phone: '',
})

const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 32, message: '昵称最多 32 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!emailPattern.test(String(value || '').trim())) {
          callback(new Error('请输入有效邮箱'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  phone: [
    {
      validator: (_rule, value, callback) => {
        const text = String(value || '').trim()
        if (text && !/^1\d{10}$/.test(text) && !/^\d{11,}$/.test(text)) {
          callback(new Error('请输入有效手机号'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const avatarText = computed(() => {
  const source = form.nickname || profile.value.username || 'U'
  return String(source).trim().slice(0, 1).toUpperCase()
})

onMounted(loadProfile)

async function loadProfile() {
  loading.value = true
  message.value = ''
  try {
    const response = await fetchUserProfile()
    applyProfile(response.data || {})
  } catch (error) {
    setMessage(readErrorMessage(error, '个人信息加载失败。'), 'error')
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  message.value = ''
  try {
    await formRef.value?.validate()
  } catch {
    setMessage('请先修正表单中的信息。', 'error')
    return
  }
  saving.value = true
  try {
    const response = await updateUserProfile({
      nickname: form.nickname,
      email: form.email,
      phone: form.phone || null,
    })
    applyProfile(response.data || {})
    await authStore.refreshMe()
    setMessage('个人信息已保存。', 'success')
  } catch (error) {
    setMessage(readErrorMessage(error, '保存失败，请稍后重试。'), 'error')
  } finally {
    saving.value = false
  }
}

function resetForm() {
  fillForm(profile.value)
  formRef.value?.clearValidate?.()
  message.value = ''
}

function applyProfile(data) {
  profile.value = data
  fillForm(data)
}

function fillForm(data) {
  form.nickname = data.nickname || data.username || ''
  form.email = data.email || ''
  form.phone = data.phone || ''
}

function setMessage(text, type = 'success') {
  message.value = text
  messageType.value = type
}

function readErrorMessage(error, fallback) {
  const messageText = error?.response?.data?.message || error?.message || fallback
  if (messageText === 'api endpoint is not registered for access') {
    return '个人资料接口未注册访问权限，请重启后端服务后再试。'
  }
  return messageText
}

function statusLabel(value) {
  if (value === 'ACTIVE') return '启用'
  if (value === 'DISABLED') return '禁用'
  return value || '未知'
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 20px;
}

.profile-toolbar {
  display: flex;
  justify-content: flex-end;
}

.state-alert {
  margin-bottom: 0;
}

.profile-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.profile-card__head {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-mark {
  display: grid;
  width: 68px;
  height: 68px;
  place-items: center;
  border-radius: 18px;
  background: linear-gradient(135deg, #0a84ff, #2f855a);
  color: #fff;
  font-size: 30px;
  font-weight: 850;
}

.eyebrow {
  margin: 0;
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 800;
}

.profile-card h2,
.section-title h2 {
  margin: 4px 0 0;
  color: var(--color-primary-dark);
}

.profile-card__head span {
  display: block;
  margin-top: 6px;
  color: var(--color-muted);
  font-size: 13px;
}

.profile-facts {
  display: grid;
  gap: 10px;
  margin-top: 22px;
}

.profile-facts div {
  padding: 13px 14px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
}

.profile-facts span {
  display: block;
  color: var(--color-muted);
  font-size: 12px;
}

.profile-facts strong {
  display: block;
  margin-top: 5px;
  color: var(--color-primary-dark);
}

.section-title {
  margin-bottom: 18px;
}

.profile-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}
</style>
