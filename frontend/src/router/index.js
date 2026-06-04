import { createRouter, createWebHistory } from 'vue-router'

import AdminCarsView from '@/views/AdminCarsView.vue'
import AdminDashboardView from '@/views/AdminDashboardView.vue'
import AdminFavoritesView from '@/views/AdminFavoritesView.vue'
import AdminFeedbacksView from '@/views/AdminFeedbacksView.vue'
import AdminHealthView from '@/views/AdminHealthView.vue'
import AdminRecommendRecordsView from '@/views/AdminRecommendRecordsView.vue'
import AdminUsersView from '@/views/AdminUsersView.vue'
import AlgorithmDemoView from '@/views/AlgorithmDemoView.vue'
import CarListView from '@/views/CarListView.vue'
import CarDetailView from '@/views/CarDetailView.vue'
import CarCompareView from '@/views/CarCompareView.vue'
import FavoritesView from '@/views/FavoritesView.vue'
import FeatureShowcaseView from '@/views/FeatureShowcaseView.vue'
import HistoryView from '@/views/HistoryView.vue'
import HomeView from '@/views/HomeView.vue'
import RecommendDemandView from '@/views/RecommendDemandView.vue'
import RecommendResultView from '@/views/RecommendResultView.vue'
import UnauthorizedView from '@/views/UnauthorizedView.vue'
import UserProfileView from '@/views/UserProfileView.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { public: true },
    },
    {
      path: '/login',
      name: 'login',
      redirect: (to) => ({
        path: safeRedirect(to.query.redirect) || '/',
        query: buildAuthQuery('login', to.query.redirect),
      }),
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      redirect: (to) => ({
        path: safeRedirect(to.query.redirect) || '/admin/cars',
        query: buildAuthQuery('admin', to.query.redirect || '/admin/cars'),
      }),
    },
    {
      path: '/register',
      name: 'register',
      redirect: (to) => ({
        path: safeRedirect(to.query.redirect) || '/',
        query: buildAuthQuery('register', to.query.redirect),
      }),
    },
    {
      path: '/unauthorized',
      name: 'unauthorized',
      component: UnauthorizedView,
    },
    {
      path: '/recommend',
      name: 'recommend-demand',
      component: RecommendDemandView,
      meta: { requiredRole: 'USER', requiredPermission: 'user:recommend' },
    },
    {
      path: '/recommend/result/:recordId',
      name: 'recommend-result',
      component: RecommendResultView,
      meta: { requiredRole: 'USER', requiredPermission: 'user:recommend' },
    },
    {
      path: '/cars',
      name: 'car-list',
      component: CarListView,
      meta: { public: true },
    },
    {
      path: '/car/:id',
      name: 'car-detail',
      component: CarDetailView,
      meta: { public: true },
    },
    {
      path: '/features',
      name: 'features',
      component: FeatureShowcaseView,
      meta: { requiredRole: 'USER', authPromptOnly: true },
    },
    {
      path: '/compare',
      name: 'car-compare',
      component: CarCompareView,
      meta: { requiredRole: 'USER', requiredPermission: 'user:compare' },
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: FavoritesView,
      meta: { requiredRole: 'USER', requiredPermission: 'user:favorites' },
    },
    {
      path: '/history',
      name: 'history',
      component: HistoryView,
      meta: { requiredRole: 'USER', requiredPermission: 'user:history' },
    },
    {
      path: '/me',
      name: 'user-profile',
      component: UserProfileView,
      meta: { requiredRole: 'USER' },
    },
    {
      path: '/algorithm-demo',
      name: 'algorithm-demo',
      component: AlgorithmDemoView,
      meta: { requiredRole: 'ADMIN', requiredPermission: 'admin:algorithm-demo' },
    },
    {
      path: '/admin/cars',
      name: 'admin-cars',
      component: AdminCarsView,
      meta: { requiredRole: 'ADMIN', requiredPermission: 'admin:cars' },
    },
    {
      path: '/admin/recommend-records',
      name: 'admin-recommend-records',
      component: AdminRecommendRecordsView,
      meta: { requiredRole: 'ADMIN', requiredPermission: 'admin:recommend-records' },
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: AdminUsersView,
      meta: { requiredRole: 'ADMIN', requiredPermission: 'admin:users' },
    },
    {
      path: '/admin/favorites',
      name: 'admin-favorites',
      component: AdminFavoritesView,
      meta: { requiredRole: 'ADMIN', requiredPermission: 'admin:favorites' },
    },
    {
      path: '/admin/feedbacks',
      name: 'admin-feedbacks',
      component: AdminFeedbacksView,
      meta: { requiredRole: 'ADMIN', requiredPermission: 'admin:feedbacks' },
    },
    {
      path: '/admin/dashboard',
      name: 'admin-dashboard',
      component: AdminDashboardView,
      meta: { requiredRole: 'ADMIN', requiredPermission: 'admin:dashboard' },
    },
    {
      path: '/admin/health',
      name: 'admin-health',
      component: AdminHealthView,
      meta: { requiredRole: 'ADMIN', requiredPermission: 'admin:health' },
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.name === 'home' && authStore.isAuthenticated && authStore.principalType === 'ADMIN') {
    return '/admin/cars'
  }
  if ((to.name === 'car-list' || to.name === 'car-detail') && authStore.isAuthenticated && authStore.principalType === 'ADMIN') {
    return '/admin/cars'
  }
  if ((to.name === 'login' || to.name === 'admin-login' || to.name === 'register') && authStore.isAuthenticated) {
    return authStore.principalType === 'ADMIN' ? '/admin/cars' : '/'
  }
  if (to.meta.public || to.name === 'unauthorized') {
    return true
  }
  if (!authStore.isAuthenticated) {
    if (to.query.auth === 'login' || to.query.auth === 'register' || to.query.auth === 'admin') {
      return true
    }
    return {
      path: to.path,
      query: {
        ...to.query,
        ...buildAuthQuery(to.meta.requiredRole === 'ADMIN' ? 'admin' : 'login', to.fullPath),
      },
      hash: to.hash,
    }
  }
  if (!authStore.hasRole(to.meta.requiredRole) || !authStore.hasPermission(to.meta.requiredPermission)) {
    return {
      path: '/unauthorized',
      query: { from: to.fullPath },
    }
  }
  return true
})

function buildAuthQuery(auth, redirect) {
  const query = { auth }
  const safe = safeRedirect(redirect)
  if (safe) {
    query.redirect = safe
  }
  return query
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

export default router
