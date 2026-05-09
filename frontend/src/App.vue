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
            <el-dropdown trigger="click" @command="handleAccountCommand">
              <button class="account-pill" type="button">
                <span class="account-pill__role">{{ roleLabel }}</span>
                <strong>{{ displayName }}</strong>
                <span class="account-pill__chevron">⌄</span>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <div class="account-menu__identity">
                    <strong>{{ displayName }}</strong>
                    <span>{{ roleLabel }}</span>
                  </div>
                  <el-dropdown-item divided command="logout">退出</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <button v-else class="login-link" type="button" @click="openAuth('login')">登录 / 注册</button>
        </div>
      </div>
    </el-header>

    <el-main class="main-shell">
      <RouterView />
    </el-main>
    <AuthDialog
      v-model="authDialogOpen"
      :initial-mode="authDialogMode"
      @mode-change="syncAuthMode"
      @success="handleAuthSuccess"
    />
  </el-container>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AuthDialog from '@/components/AuthDialog.vue'
import AppLogo from '@/components/AppLogo.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const authDialogOpen = ref(false)
const authDialogMode = ref('login')

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

watch(
  () => route.query.auth,
  (value) => {
    if (value === 'login' || value === 'register') {
      if (authStore.isAuthenticated) {
        authDialogOpen.value = false
        clearAuthQuery()
        return
      }
      authDialogMode.value = value
      authDialogOpen.value = true
      return
    }
    authDialogOpen.value = false
  },
  { immediate: true },
)

watch(authDialogOpen, (value) => {
  if (!value && (route.query.auth === 'login' || route.query.auth === 'register')) {
    clearAuthQuery()
  }
})

function openAuth(mode = 'login') {
  const query = { auth: mode }
  if (route.path !== '/') {
    query.redirect = route.fullPath
  }
  router.push({ path: '/', query })
}

function syncAuthMode(mode) {
  if (route.query.auth === mode) {
    return
  }
  router.replace({
    path: route.path,
    query: {
      ...route.query,
      auth: mode,
    },
  })
}

async function handleAuthSuccess(data) {
  authDialogOpen.value = false
  const target = resolvePostAuthTarget(data?.principal?.principalType)
  await router.replace(target)
}

function resolvePostAuthTarget(principalType) {
  const redirect = safeRedirect(route.query.redirect)
  if (principalType === 'ADMIN') {
    return isAdminRedirect(redirect) ? redirect : '/admin/cars'
  }
  return isUserRedirect(redirect) ? redirect : '/'
}

function safeRedirect(value) {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) {
    return ''
  }
  if (value.startsWith('/login') || value.startsWith('/admin/login') || value.startsWith('/register')) {
    return ''
  }
  return value
}

function isAdminRedirect(value) {
  return value.startsWith('/admin') || value.startsWith('/algorithm-demo')
}

function isUserRedirect(value) {
  return Boolean(value) && !value.startsWith('/admin') && !value.startsWith('/algorithm-demo')
}

function clearAuthQuery() {
  const { auth, redirect, ...rest } = route.query
  router.replace({ path: route.path, query: rest })
}

function handleAccountCommand(command) {
  if (command === 'logout') {
    logout()
  }
}

async function logout() {
  await authStore.logout()
  await router.push('/')
}
</script>
