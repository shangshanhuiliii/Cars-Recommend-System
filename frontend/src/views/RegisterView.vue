<template>
  <section class="register-page">
    <div class="register-panel">
      <div class="register-copy">
        <p class="eyebrow">创建 USER 账号</p>
        <h1>注册后即可保存你的购车需求、推荐历史、收藏和反馈</h1>
        <p>注册只创建普通用户账号，不能创建管理员账号。密码会在后端使用 PBKDF2 哈希保存。</p>
      </div>

      <el-card class="register-card" shadow="never">
        <template #header>
          <div class="register-card__header">
            <strong>普通用户注册</strong>
            <RouterLink :to="{ path: '/login', query: loginQuery }">已有账号？返回登录</RouterLink>
          </div>
        </template>

        <el-form
          ref="formRef"
          class="register-form"
          :model="form"
          :rules="rules"
          label-position="top"
          @keyup.enter="submitRegister"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model.trim="form.username" autocomplete="username" placeholder="4-32 位字母、数字或下划线" />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model.trim="form.nickname" maxlength="32" show-word-limit placeholder="可选，默认使用用户名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model.trim="form.phone" autocomplete="tel" placeholder="可选，11 位手机号" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              autocomplete="new-password"
              placeholder="8-32 位，至少包含字母和数字"
              show-password
              type="password"
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              autocomplete="new-password"
              placeholder="再次输入密码"
              show-password
              type="password"
            />
          </el-form-item>

          <p v-if="error" class="inline-error">{{ error }}</p>

          <el-button class="register-submit" type="primary" :loading="loading" @click="submitRegister">
            注册并进入推荐
          </el-button>
        </el-form>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const error = ref('')
const form = reactive({
  username: '',
  nickname: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

const loginQuery = computed(() => (route.query.redirect ? { redirect: route.query.redirect } : {}))

const validatePassword = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }
  if (value.length < 8 || value.length > 32 || !/[A-Za-z]/.test(value) || !/\d/.test(value)) {
    callback(new Error('密码必须为 8-32 位且至少包含字母和数字'))
    return
  }
  callback()
}

const validateConfirmPassword = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const validatePhone = (_rule, value, callback) => {
  if (!value || /^1\d{10}$/.test(value) || /^\d{11,}$/.test(value)) {
    callback()
    return
  }
  callback(new Error('手机号格式不正确'))
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,32}$/, message: '用户名必须为 4-32 位字母、数字或下划线', trigger: 'blur' },
  ],
  nickname: [{ max: 32, message: '昵称最多 32 个字符', trigger: 'blur' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
}

async function submitRegister() {
  error.value = ''
  try {
    await formRef.value.validate()
  } catch {
    error.value = '请先修正注册表单。'
    return
  }
  loading.value = true
  try {
    await authStore.register({
      username: form.username,
      nickname: form.nickname,
      phone: form.phone,
      password: form.password,
      confirmPassword: form.confirmPassword,
    })
    await router.push(resolveTarget())
  } catch (requestError) {
    error.value = requestError?.response?.data?.message || requestError?.message || '注册失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function resolveTarget() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (redirect && redirect !== '/login' && redirect !== '/register' && !redirect.startsWith('/admin')) {
    return redirect
  }
  return '/recommend'
}
</script>

<style scoped>
.register-page {
  min-height: calc(100vh - 160px);
}

.register-panel {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(360px, 480px);
  gap: 28px;
  align-items: stretch;
}

.register-copy {
  overflow: hidden;
  min-height: 520px;
  padding: 46px;
  border-radius: 28px;
  background:
    radial-gradient(circle at 15% 18%, rgba(37, 99, 235, 0.24), transparent 30%),
    radial-gradient(circle at 82% 72%, rgba(22, 163, 74, 0.22), transparent 28%),
    linear-gradient(140deg, #082f49 0%, #0f172a 62%, #111827 100%);
  color: #fff;
  box-shadow: var(--shadow-card);
}

.eyebrow {
  margin: 0 0 18px;
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 0.22em;
}

.register-copy h1 {
  max-width: 620px;
  margin: 0;
  font-size: clamp(30px, 4vw, 50px);
  line-height: 1.12;
}

.register-copy p:not(.eyebrow) {
  max-width: 620px;
  margin: 22px 0 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 15px;
  line-height: 1.9;
}

.register-card {
  border: 1px solid var(--color-border);
  border-radius: 24px;
}

.register-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.register-card__header a {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
}

.register-form {
  margin-top: 4px;
}

.inline-error {
  margin: 0 0 14px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 13px;
}

.register-submit {
  width: 100%;
}

@media (max-width: 900px) {
  .register-panel {
    grid-template-columns: 1fr;
  }

  .register-copy {
    min-height: auto;
    padding: 32px;
  }
}
</style>
