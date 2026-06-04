<template>
  <el-container :class="['app-shell', { 'app-shell--admin': isAdminLayout }]" direction="vertical">
    <template v-if="isAdminLayout">
      <div class="admin-layout">
        <button
          v-if="adminSidebarOpen"
          class="admin-sidebar-backdrop"
          type="button"
          aria-label="关闭管理菜单"
          @click="adminSidebarOpen = false"
        />
        <aside :class="['admin-sidebar', { 'admin-sidebar--open': adminSidebarOpen }]">
          <div class="admin-sidebar__brand">
            <AppLogo class="app-logo--compact" :to="brandTarget" />
          </div>

          <div class="admin-sidebar__identity">
            <span>当前身份</span>
            <strong>管理员</strong>
          </div>

          <nav class="admin-menu" aria-label="管理端导航">
            <RouterLink
              v-for="item in adminVisibleMenus"
              :key="item.path"
              :to="item.path"
              :class="['admin-menu__item', { 'admin-menu__item--active': activeRoute === item.path }]"
              @click="adminSidebarOpen = false"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </RouterLink>
          </nav>

          <button class="admin-sidebar__logout" type="button" @click="logout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出登录</span>
          </button>
        </aside>

        <section class="admin-content">
          <header class="admin-content__topbar">
            <button class="admin-menu-toggle" type="button" aria-label="打开管理菜单" @click="adminSidebarOpen = true">
              <el-icon><Menu /></el-icon>
            </button>
            <strong class="admin-content__title">{{ activeAdminTitle }}</strong>
            <div class="admin-content__account">
              <span>管理员</span>
            </div>
          </header>

          <el-main class="admin-main-shell">
            <RouterView />
          </el-main>
        </section>
      </div>
    </template>

    <template v-else>
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
            <el-menu-item v-if="showCarLibraryMenu" index="/cars">车型库</el-menu-item>
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
                    <el-dropdown-item v-if="authStore.principalType === 'USER'" command="profile">我的</el-dropdown-item>
                    <el-dropdown-item divided command="logout">退出</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <button v-else class="login-link" type="button" @click="goLogin">登录 / 注册</button>
          </div>
        </div>
      </el-header>

      <el-main :class="['main-shell', { 'main-shell--home': route.path === '/' }]">
        <RouterView />
      </el-main>
    </template>
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
import {
  ChatLineSquare,
  Cpu,
  DataBoard,
  Menu,
  Monitor,
  Star,
  SwitchButton,
  Tickets,
  User,
  Van,
} from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'

import AuthDialog from '@/components/AuthDialog.vue'
import AppLogo from '@/components/AppLogo.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const authDialogOpen = ref(false)
const authDialogMode = ref('login')
const adminSidebarOpen = ref(false)

const labelMap = {
  recommend: '购车推荐',
  history: '推荐历史',
  favorites: '我的收藏',
  compare: '车型对比',
  profile: '我的',
  'admin-cars': '车型管理',
  'admin-users': '用户管理',
  'admin-favorites': '收藏车型',
  'admin-feedbacks': '反馈记录',
  'admin-recommend-records': '推荐记录',
  'admin-dashboard': '运营概览',
  'admin-health': '系统健康检查',
  'algorithm-demo': '算法可视化',
}

const adminMenuItems = [
  { path: '/admin/cars', label: '车型管理', permission: 'admin:cars', icon: Van },
  { path: '/admin/users', label: '用户管理', permission: 'admin:users', icon: User },
  { path: '/admin/favorites', label: '收藏车型', permission: 'admin:favorites', icon: Star },
  { path: '/admin/feedbacks', label: '反馈记录', permission: 'admin:feedbacks', icon: ChatLineSquare },
  { path: '/admin/recommend-records', label: '推荐记录', permission: 'admin:recommend-records', icon: Tickets },
  { path: '/admin/dashboard', label: '运营概览', permission: 'admin:dashboard', icon: DataBoard },
  { path: '/admin/health', label: '系统健康检查', permission: 'admin:health', icon: Monitor },
  { path: '/algorithm-demo', label: '算法可视化', permission: 'admin:algorithm-demo', icon: Cpu },
]

const isAdmin = computed(() => authStore.principalType === 'ADMIN')
const isAdminLayout = computed(() => isAdmin.value && (route.path.startsWith('/admin') || route.path === '/algorithm-demo'))
const brandTarget = computed(() => (isAdmin.value ? '/admin/cars' : '/'))
const showHomeMenu = computed(() => !authStore.isAuthenticated || !isAdmin.value)
const showCarLibraryMenu = computed(() => !isAdmin.value)
const visibleMenus = computed(() =>
  (authStore.menus || []).filter((item) => item.code !== 'home' && (!isAdmin.value || item.path !== '/')),
)
const adminVisibleMenus = computed(() =>
  adminMenuItems.filter((item) => authStore.hasPermission(item.permission) || hasBackendMenu(item.path)),
)
const roleLabel = computed(() => (authStore.principalType === 'ADMIN' ? '管理员' : '用户'))
const displayName = computed(() => (isAdmin.value ? '管理员' : authStore.displayName))

const activeRoute = computed(() => {
  if (route.path.startsWith('/recommend/result')) {
    return '/recommend'
  }
  if (route.path.startsWith('/car/')) {
    return authStore.principalType === 'ADMIN' ? '/admin/cars' : '/cars'
  }
  return route.path
})

const activeAdminTitle = computed(() => {
  const activeItem = adminMenuItems.find((item) => item.path === activeRoute.value)
  return activeItem?.label || '管理端'
})

function menuLabel(item) {
  return labelMap[item.code] || item.label || item.code
}

function hasBackendMenu(path) {
  return (authStore.menus || []).some((item) => item.path === path)
}

watch(
  () => route.query.auth,
  (value) => {
    if (value === 'login' || value === 'register' || value === 'admin') {
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
  if (!value && (route.query.auth === 'login' || route.query.auth === 'register' || route.query.auth === 'admin')) {
    clearAuthQuery()
  }
})

function goLogin() {
  openAuth('login')
}

function openAuth(mode = 'login') {
  router.push({
    path: route.path,
    query: {
      ...route.query,
      auth: mode,
      ...(route.path !== '/' ? { redirect: route.fullPath } : {}),
    },
    hash: route.hash,
  })
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
    hash: route.hash,
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
  router.replace({ path: route.path, query: rest, hash: route.hash })
}

function handleAccountCommand(command) {
  if (command === 'profile') {
    router.push('/me')
    return
  }
  if (command === 'logout') {
    logout()
  }
}

async function logout() {
  await authStore.logout()
  await router.push('/')
}
</script>
