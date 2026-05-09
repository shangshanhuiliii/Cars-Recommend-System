<template>
  <el-container class="app-shell">
    <el-header class="topbar">
      <div class="topbar__inner">
        <AppLogo :to="brandTarget" />

        <el-menu
          class="nav-menu"
          mode="horizontal"
          router
          :ellipsis="false"
          :default-active="activeRoute"
        >
          <el-menu-item v-if="showHomeMenu" index="/">首页</el-menu-item>
          <template v-if="authStore.isAuthenticated">
            <el-menu-item v-for="item in visibleMenus" :key="item.code" :index="item.path">
              {{ menuLabel(item) }}
            </el-menu-item>
          </template>
        </el-menu>

        <div class="identity-box">
          <template v-if="authStore.isAuthenticated">
            <span class="identity-box__role">{{ roleLabel }}</span>
            <strong>{{ displayName }}</strong>
            <el-button link type="primary" @click="logout">退出</el-button>
          </template>
          <RouterLink v-else class="login-link" to="/login">登录</RouterLink>
        </div>
      </div>
    </el-header>

    <el-main class="main-shell">
      <RouterView />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppLogo from '@/components/AppLogo.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const labelMap = {
  recommend: '购车推荐',
  history: '推荐历史',
  favorites: '我的收藏',
  compare: '车型对比',
  'admin-cars': '车型管理',
  'admin-users': '用户管理',
  'admin-favorites': '收藏车型',
  'admin-feedbacks': '反馈记录',
  'admin-recommend-records': '推荐记录',
  'admin-dashboard': '运营概览',
  'admin-health': '系统健康检查',
  'algorithm-demo': '算法可视化',
}

const isAdmin = computed(() => authStore.principalType === 'ADMIN')
const brandTarget = computed(() => (isAdmin.value ? '/admin/cars' : '/'))
const showHomeMenu = computed(() => !authStore.isAuthenticated || !isAdmin.value)
const visibleMenus = computed(() =>
  (authStore.menus || []).filter((item) => item.code !== 'home' && (!isAdmin.value || item.path !== '/')),
)
const roleLabel = computed(() => (authStore.principalType === 'ADMIN' ? '管理员' : '用户'))
const displayName = computed(() => (isAdmin.value ? '管理员' : authStore.displayName))

const activeRoute = computed(() => {
  if (route.path.startsWith('/recommend/result')) {
    return '/recommend'
  }
  if (route.path.startsWith('/car/')) {
    return authStore.principalType === 'USER' ? '/recommend' : '/admin/cars'
  }
  return route.path
})

function menuLabel(item) {
  return labelMap[item.code] || item.label || item.code
}

async function logout() {
  await authStore.logout()
  await router.push('/login')
}
</script>
