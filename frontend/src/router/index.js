import { createRouter, createWebHistory } from 'vue-router'

import AdminCarsView from '@/views/AdminCarsView.vue'
import AdminDashboardView from '@/views/AdminDashboardView.vue'
import AdminRecommendRecordsView from '@/views/AdminRecommendRecordsView.vue'
import AlgorithmDemoView from '@/views/AlgorithmDemoView.vue'
import CarDetailView from '@/views/CarDetailView.vue'
import CarCompareView from '@/views/CarCompareView.vue'
import FavoritesView from '@/views/FavoritesView.vue'
import HistoryView from '@/views/HistoryView.vue'
import HomeView from '@/views/HomeView.vue'
import RecommendDemandView from '@/views/RecommendDemandView.vue'
import RecommendResultView from '@/views/RecommendResultView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/recommend',
      name: 'recommend-demand',
      component: RecommendDemandView,
    },
    {
      path: '/recommend/result/:recordId',
      name: 'recommend-result',
      component: RecommendResultView,
    },
    {
      path: '/car/:id',
      name: 'car-detail',
      component: CarDetailView,
    },
    {
      path: '/compare',
      name: 'car-compare',
      component: CarCompareView,
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: FavoritesView,
    },
    {
      path: '/history',
      name: 'history',
      component: HistoryView,
    },
    {
      path: '/algorithm-demo',
      name: 'algorithm-demo',
      component: AlgorithmDemoView,
    },
    {
      path: '/admin/cars',
      name: 'admin-cars',
      component: AdminCarsView,
    },
    {
      path: '/admin/recommend-records',
      name: 'admin-recommend-records',
      component: AdminRecommendRecordsView,
    },
    {
      path: '/admin/dashboard',
      name: 'admin-dashboard',
      component: AdminDashboardView,
    },
  ],
})

export default router
