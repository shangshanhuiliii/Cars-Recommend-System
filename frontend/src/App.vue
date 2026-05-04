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
          <el-menu-item index="/recommend">购车推荐</el-menu-item>
          <el-menu-item index="/history">推荐历史</el-menu-item>
          <el-menu-item index="/favorites">我的收藏</el-menu-item>
          <el-menu-item index="/compare">车型对比</el-menu-item>
          <el-sub-menu index="/admin">
            <template #title>管理端入口</template>
            <el-menu-item index="/admin/cars">车型管理</el-menu-item>
            <el-menu-item index="/admin/recommend-records">推荐记录</el-menu-item>
            <el-menu-item index="/admin/dashboard">统计仪表盘</el-menu-item>
            <el-menu-item index="/admin/health">系统健康检查</el-menu-item>
            <el-menu-item index="/algorithm-demo">算法可视化</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>
    </el-header>

    <el-main class="main-shell">
      <RouterView />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeRoute = computed(() => {
  if (route.path.startsWith('/recommend/result')) {
    return '/recommend'
  }
  if (route.path.startsWith('/car/')) {
    return '/recommend'
  }
  return route.path
})
</script>
