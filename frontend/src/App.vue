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
          <el-sub-menu index="/admin">
            <template #title>管理端</template>
            <el-menu-item index="/admin/cars">车型管理</el-menu-item>
            <el-menu-item index="/admin/recommend-records">推荐记录</el-menu-item>
            <el-menu-item index="/admin/dashboard">统计仪表盘</el-menu-item>
          </el-sub-menu>
        </el-menu>

        <el-tag class="demo-tag" type="info" effect="plain">演示用户 ID 1</el-tag>
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
