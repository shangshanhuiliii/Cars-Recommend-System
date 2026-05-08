<template>
  <section class="unauthorized-page panel">
    <div class="panel__body">
      <p class="eyebrow">403</p>
      <h1>当前账号无权访问该页面</h1>
      <p>
        用户端和管理端使用不同角色权限。请返回可访问的页面，或退出后切换到拥有对应权限的账号。
      </p>
      <div class="actions">
        <el-button type="primary" @click="goHome">返回首页</el-button>
        <el-button @click="switchAccount">切换账号</el-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function goHome() {
  router.push('/')
}

async function switchAccount() {
  await authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.unauthorized-page {
  max-width: 720px;
  margin: 72px auto;
  text-align: center;
}

.eyebrow {
  margin: 0;
  color: var(--color-danger);
  font-weight: 700;
  letter-spacing: 0.18em;
}

h1 {
  margin: 10px 0;
  color: var(--color-primary-dark);
}

p {
  margin: 0 auto;
  max-width: 520px;
  color: var(--color-muted);
  line-height: 1.8;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}
</style>
