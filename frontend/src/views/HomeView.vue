<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">基于多维偏好建模的汽车购买推荐系统</h1>
        <p class="page-subtitle">
          阶段 0 已建立前后端工程骨架，后续阶段会沿车型参数、特征评分、用户画像、推荐解释和记录追溯主链路逐步落地。
        </p>
      </div>
      <el-button type="primary" size="large" @click="$router.push('/recommend')">进入购车推荐</el-button>
    </div>

    <div class="status-grid">
      <div class="status-card">
        <p class="status-card__label">后端接口</p>
        <p class="status-card__value">{{ health.status.backend }}</p>
      </div>
      <div class="status-card">
        <p class="status-card__label">数据库配置</p>
        <p class="status-card__value">{{ health.status.database }}</p>
      </div>
      <div class="status-card">
        <p class="status-card__label">健康检查</p>
        <p class="status-card__value">{{ health.loading ? 'checking' : health.loaded ? 'ready' : 'pending' }}</p>
      </div>
    </div>

    <el-alert
      v-if="health.error"
      class="health-alert"
      type="warning"
      :closable="false"
      :title="`后端连接失败：${health.error}`"
    />

    <div class="panel home-panel">
      <div class="panel__body">
        <div class="placeholder-list">
          <div class="placeholder-item">
            <h3>推荐主链路</h3>
            <p>车型参数、特征评分、用户画像、多维匹配、解释与追溯将在后续阶段按顺序实现。</p>
          </div>
          <div class="placeholder-item">
            <h3>统一工程约定</h3>
            <p>当前已固定 API 前缀、通用响应结构、基础枚举、路由表和本地敏感配置规则。</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'

import { useHealthStore } from '@/stores/health'

const health = useHealthStore()

onMounted(() => {
  health.load()
})
</script>

<style scoped>
.home-panel {
  margin-top: 24px;
}

.health-alert {
  margin-top: 16px;
}
</style>
