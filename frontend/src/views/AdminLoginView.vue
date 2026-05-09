<template>
  <section class="admin-login-page">
    <div class="admin-copy">
      <p class="eyebrow">管理端登录</p>
      <h1>进入车型管理和运营概览</h1>
      <p>管理端用于维护车型数据、追溯推荐记录、查看收藏排行和反馈记录。</p>
    </div>

    <el-card class="login-card" shadow="never">
      <template #header>
        <div class="login-card__header">
          <strong>管理员登录</strong>
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
          <el-input v-model.trim="form.username" autocomplete="username" placeholder="请输入管理员用户名" />
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
          登录管理端
        </el-button>
      </el-form>
    </el-card>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
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

const rules = {
  username: [{ required: true, message: '请输入管理员用户名', trigger: 'blur' }],
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
    await authStore.login('ADMIN', {
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
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (redirect && redirect !== '/login' && redirect !== '/admin/login' && redirect.startsWith('/admin')) {
    return redirect
  }
  return '/admin/cars'
}
</script>

<style scoped>
.admin-login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) 420px;
  gap: 28px;
}

.admin-copy {
  min-height: 420px;
  padding: 44px;
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(17, 24, 39, 0.94), rgba(10, 132, 255, 0.24)),
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

.admin-copy h1 {
  max-width: 620px;
  margin: 0;
  font-size: 42px;
  line-height: 1.14;
}

.admin-copy p:not(.eyebrow) {
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

@media (max-width: 900px) {
  .admin-login-page {
    grid-template-columns: 1fr;
  }

  .admin-copy {
    min-height: auto;
    padding: 32px;
  }

  .admin-copy h1 {
    font-size: 32px;
  }
}
</style>
