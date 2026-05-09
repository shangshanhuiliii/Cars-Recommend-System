<template>
  <section class="login-page">
    <div class="login-hero">
      <p class="eyebrow">用户登录</p>
      <h1>继续你的可解释购车推荐</h1>
      <p>
        登录后可以保存购车需求、推荐历史、收藏车型、用户反馈和车型对比列表。
        每次推荐都保留评分、理由和不足提醒，便于后续回看。
      </p>
    </div>

    <el-card class="login-card" shadow="never">
      <template #header>
        <div class="login-card__header">
          <strong>普通用户登录</strong>
        </div>
      </template>

      <el-form
        ref="formRef"
        class="login-form"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="submitLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" autocomplete="username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            autocomplete="current-password"
            placeholder="请输入密码"
            show-password
            type="password"
          />
        </el-form-item>

        <p v-if="error" class="inline-error">{{ error }}</p>
        <p v-if="route.query.reason === 'forbidden'" class="inline-warning">当前账号无权访问目标页面，请切换账号。</p>

        <el-button class="login-submit" type="primary" :loading="loading" @click="submitLogin">
          登录
        </el-button>

        <p class="login-switch">
          没有账号？
          <RouterLink :to="{ path: '/register', query: registerQuery }">立即注册</RouterLink>
        </p>
      </el-form>
    </el-card>

    <RouterLink class="admin-login-entry" :to="{ path: '/admin/login', query: adminLoginQuery }">管理员登录</RouterLink>
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
  password: '',
})
const registerQuery = computed(() => (route.query.redirect ? { redirect: route.query.redirect } : {}))
const adminLoginQuery = computed(() => (route.query.redirect?.startsWith?.('/admin') ? { redirect: route.query.redirect } : {}))

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submitLogin() {
  error.value = ''
  try {
    await formRef.value.validate()
  } catch {
    error.value = '请先填写用户名和密码。'
    return
  }
  loading.value = true
  try {
    await authStore.login('USER', {
      username: form.username,
      password: form.password,
    })
    await router.push(resolveTarget())
  } catch (requestError) {
    error.value = requestError?.response?.data?.message || requestError?.message || '登录失败，请检查账号和密码。'
  } finally {
    loading.value = false
  }
}

function resolveTarget() {
  return '/'
}
</script>

<style scoped>
.login-page {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) 420px;
  align-items: stretch;
  gap: 28px;
}

.login-hero {
  position: relative;
  overflow: hidden;
  min-height: 460px;
  padding: 44px;
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(17, 24, 39, 0.94), rgba(10, 132, 255, 0.28)),
    url("/images/cars/default-car.svg") right 12% bottom 10% / min(520px, 78%) no-repeat,
    #111827;
  color: #fff;
  box-shadow: var(--shadow-card);
}

.eyebrow {
  margin: 0 0 18px;
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 0.18em;
}

.login-hero h1 {
  max-width: 620px;
  margin: 0;
  font-size: 46px;
  line-height: 1.12;
}

.login-hero p:not(.eyebrow) {
  max-width: 620px;
  margin: 22px 0 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 15px;
  line-height: 1.9;
}

.login-card {
  border: 1px solid var(--color-border);
  border-radius: 24px;
}

.login-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.login-form {
  margin-top: 4px;
}

.inline-error,
.inline-warning {
  margin: 0 0 14px;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 13px;
}

.inline-error {
  background: #fef2f2;
  color: #b91c1c;
}

.inline-warning {
  background: #fffbeb;
  color: #92400e;
}

.login-submit {
  width: 100%;
}

.login-switch {
  margin: 16px 0 0;
  color: var(--color-muted);
  font-size: 13px;
  text-align: center;
}

.login-switch a,
.admin-login-entry {
  color: var(--color-primary);
  font-weight: 700;
}

.admin-login-entry {
  position: absolute;
  right: 0;
  bottom: -34px;
  font-size: 13px;
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-hero {
    min-height: auto;
    padding: 32px;
  }

  .login-hero h1 {
    font-size: 34px;
  }
}
</style>
