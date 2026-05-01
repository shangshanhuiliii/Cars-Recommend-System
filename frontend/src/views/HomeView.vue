<template>
  <section>
    <div class="home-hero">
      <div>
        <p class="eyebrow">可解释汽车购买推荐系统</p>
        <h1>用多维评分，找到更适合你的车。</h1>
        <p>
          系统基于预算、场景、关注因素和车型特征评分生成推荐结果，并展示匹配状态、推荐理由、不足提醒和历史快照。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/recommend')">开始购车推荐</el-button>
          <el-button size="large" @click="$router.push('/history')">查看推荐历史</el-button>
        </div>
      </div>
      <div class="hero-metrics">
        <div>
          <span>推荐主链路</span>
          <strong>8 阶段</strong>
        </div>
        <div>
          <span>可解释输出</span>
          <strong>理由 / 不足 / 标签</strong>
        </div>
        <div>
          <span>演示用户</span>
          <strong>ID 1</strong>
        </div>
      </div>
    </div>

    <div class="entry-grid">
      <button class="entry-card" type="button" @click="$router.push('/recommend')">
        <span>01</span>
        <h2>填写购车需求</h2>
        <p>提交预算、车型、动力、场景和关注因素，生成用户画像权重。</p>
      </button>
      <button class="entry-card" type="button" @click="$router.push('/history')">
        <span>02</span>
        <h2>回看推荐记录</h2>
        <p>查看历史推荐状态、车型摘要，并进入完整推荐快照。</p>
      </button>
      <button class="entry-card" type="button" @click="$router.push('/admin/cars')">
        <span>03</span>
        <h2>维护车型数据</h2>
        <p>进入管理端维护车型基础信息、参数和特征评分。</p>
      </button>
    </div>

    <div class="status-grid home-status">
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
.home-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 28px;
  align-items: stretch;
  padding: 34px;
  border-radius: var(--radius-lg);
  color: #fff;
  background: linear-gradient(135deg, #0f172a, #1d4ed8 58%, #0891b2);
  box-shadow: var(--shadow-card);
}

.eyebrow {
  margin: 0 0 12px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}

.home-hero h1 {
  max-width: 680px;
  margin: 0;
  font-size: 42px;
  line-height: 1.18;
}

.home-hero p {
  max-width: 720px;
  margin: 16px 0 0;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.hero-metrics {
  display: grid;
  gap: 12px;
}

.hero-metrics div {
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.08);
}

.hero-metrics span,
.hero-metrics strong {
  display: block;
}

.hero-metrics span {
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.hero-metrics strong {
  margin-top: 8px;
  font-size: 20px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 24px;
}

.entry-card {
  min-height: 180px;
  padding: 22px;
  text-align: left;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--shadow-card);
  cursor: pointer;
}

.entry-card span {
  color: var(--color-accent);
  font-size: 13px;
  font-weight: 700;
}

.entry-card h2 {
  margin: 16px 0 10px;
  color: var(--color-primary-dark);
  font-size: 20px;
}

.entry-card p {
  margin: 0;
  color: var(--color-muted);
  line-height: 1.7;
}

.home-status {
  margin-top: 24px;
}

.health-alert {
  margin-top: 16px;
}

@media (max-width: 900px) {
  .home-hero,
  .entry-grid {
    grid-template-columns: 1fr;
  }

  .home-hero h1 {
    font-size: 32px;
  }
}
</style>
