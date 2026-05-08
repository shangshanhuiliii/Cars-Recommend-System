<template>
  <section class="login-page">
    <div class="login-hero">
      <p class="eyebrow">身份认证</p>
      <h1>登录后继续使用可解释购车推荐</h1>
      <p>
        用户端用于生成需求、推荐、收藏和反馈；管理端用于车型维护、推荐记录追溯、
        统计看板和算法可视化。菜单展示只用于体验，后端接口会强制校验角色权限。
      </p>
      <div class="account-hints">
        <span>本地用户：demo_user / demo123456</span>
        <span>本地管理员：demo_admin / admin123456</span>
      </div>
    </div>

    <el-card class="login-card" shadow="never">
      <template #header>
        <div class="login-card__header">
          <strong>登录</strong>
          <span>{{ loginType === 'USER' ? '普通用户' : '管理员' }}</span>
        </div>
      </template>

      <el-radio-group v-model="loginType" class="login-type">
        <el-radio-button label="USER">普通用户</el-radio-button>
        <el-radio-button label="ADMIN">管理员</el-radio-button>
      </el-radio-group>

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
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const error = ref('')
const loginType = ref(route.query.type === 'admin' ? 'ADMIN' : 'USER')
const form = reactive(defaultForm(loginType.value))
const registerQuery = computed(() => (route.query.redirect ? { redirect: route.query.redirect } : {}))

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

watch(loginType, (value) => {
  Object.assign(form, defaultForm(value))
  error.value = ''
})

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
    const session = await authStore.login(loginType.value, {
      username: form.username,
      password: form.password,
    })
    await router.push(resolveTarget(session?.principal?.principalType))
  } catch (requestError) {
    error.value = requestError?.response?.data?.message || requestError?.message || '登录失败，请检查账号和密码。'
  } finally {
    loading.value = false
  }
}

function resolveTarget(principalType) {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (redirect && redirect !== '/login') {
    return redirect
  }
  return principalType === 'ADMIN' ? '/admin/dashboard' : '/recommend'
}

function defaultForm(type) {
  return type === 'ADMIN'
    ? { username: 'demo_admin', password: '' }
    : { username: 'demo_user', password: '' }
}
</script>

<style scoped>
.login-page {
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
  border-radius: 28px;
  background:
    radial-gradient(circle at 10% 20%, rgba(8, 145, 178, 0.24), transparent 28%),
    radial-gradient(circle at 80% 15%, rgba(245, 158, 11, 0.22), transparent 26%),
    linear-gradient(135deg, #0f172a 0%, #164e63 54%, #0f766e 100%);
  color: #fff;
  box-shadow: var(--shadow-card);
}

.login-hero::after {
  position: absolute;
  right: -90px;
  bottom: -120px;
  width: 310px;
  height: 310px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 50%;
  content: "";
}

.eyebrow {
  margin: 0 0 18px;
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 0.22em;
}

.login-hero h1 {
  max-width: 620px;
  margin: 0;
  font-size: clamp(34px, 5vw, 58px);
  line-height: 1.08;
}

.login-hero p:not(.eyebrow) {
  max-width: 620px;
  margin: 22px 0 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 15px;
  line-height: 1.9;
}

.account-hints {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 34px;
}

.account-hints span {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.10);
  color: rgba(255, 255, 255, 0.86);
  font-size: 12px;
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

.login-card__header span {
  color: var(--color-muted);
  font-size: 13px;
}

.login-type {
  width: 100%;
  margin-bottom: 22px;
}

.login-type :deep(.el-radio-button) {
  width: 50%;
}

.login-type :deep(.el-radio-button__inner) {
  width: 100%;
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

.login-switch a {
  color: var(--color-primary);
  font-weight: 700;
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-hero {
    min-height: auto;
    padding: 32px;
  }
}
</style>
