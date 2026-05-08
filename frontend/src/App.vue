<template>
  <el-container class="app-shell">
    <el-header class="topbar">
      <div class="topbar__inner">
        <RouterLink class="brand" to="/">
          <span class="brand__mark">CR</span>
          <span>
            <strong>汽车推荐系统</strong>
            <small>可解释购车决策</small>
          </span>
        </RouterLink>

        <el-menu
          class="nav-menu"
          mode="horizontal"
          router
          :ellipsis="false"
          :default-active="activeRoute"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <template v-if="authStore.isAuthenticated">
            <el-menu-item v-for="item in visibleMenus" :key="item.code" :index="item.path">
              {{ menuLabel(item) }}
            </el-menu-item>
          </template>
          <el-menu-item v-else index="/login">登录</el-menu-item>
        </el-menu>

        <div class="identity-box">
          <template v-if="authStore.isAuthenticated">
            <span class="identity-box__role">{{ roleLabel }}</span>
            <strong>{{ authStore.displayName }}</strong>
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
  'admin-recommend-records': '推荐记录',
  'admin-dashboard': '统计仪表盘',
  'admin-health': '系统健康检查',
  'algorithm-demo': '算法可视化',
}

const visibleMenus = computed(() => (authStore.menus || []).filter((item) => item.code !== 'home'))
const roleLabel = computed(() => (authStore.principalType === 'ADMIN' ? '管理员' : '用户'))

const activeRoute = computed(() => {
  if (route.path.startsWith('/recommend/result')) {
    return '/recommend'
  }
  if (route.path.startsWith('/car/')) {
    return authStore.principalType === 'USER' ? '/recommend' : '/'
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