<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">统计仪表盘</h1>
        <p class="page-subtitle">聚焦推荐系统运行情况：需求分布、降级状态和热门推荐车型，数据全部来自当前数据库。</p>
      </div>
      <el-button type="primary" plain :loading="loading" @click="loadOverview">刷新统计</el-button>
    </div>

    <el-alert
      v-if="error"
      class="state-alert"
      type="error"
      :closable="false"
      :title="error"
      show-icon
    />

    <div v-if="loading" class="panel">
      <div class="panel__body">
        <el-skeleton :rows="10" animated />
      </div>
    </div>

    <template v-else>
      <div class="metric-grid">
        <div class="metric-card">
          <span>需求样本</span>
          <strong>{{ totalOf(overview.budgetDistribution) }}</strong>
          <p>来自 user_demand</p>
        </div>
        <div class="metric-card">
          <span>推荐记录</span>
          <strong>{{ totalOf(overview.recommendStatusDistribution) }}</strong>
          <p>来自 recommend_record</p>
        </div>
        <div class="metric-card">
          <span>推荐明细引用</span>
          <strong>{{ totalOf(overview.popularCars) }}</strong>
          <p>来自 recommend_item</p>
        </div>
      </div>

      <div class="chart-grid">
        <article v-for="chart in chartCards" :key="chart.key" class="panel chart-card">
          <div class="panel__body">
            <div class="chart-head">
              <div>
                <h2>{{ chart.title }}</h2>
                <p>{{ chart.source }}</p>
              </div>
              <span>{{ chart.items.length }} 项</span>
            </div>

            <div v-if="chart.items.length" class="bar-list">
              <div v-for="item in chart.items" :key="item.name" class="bar-row">
                <div class="bar-row__label">
                  <span>{{ item.name }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
                <div class="bar-track">
                  <i :style="{ width: barWidth(item.value, chart.items) }"></i>
                </div>
              </div>
            </div>

            <div v-else class="empty-chart">暂无数据</div>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'

import { fetchAdminStatOverview } from '@/api/adminStats'

const loading = ref(false)
const error = ref('')
const overview = ref(emptyOverview())

const chartCards = computed(() => [
  {
    key: 'budget',
    title: '预算区间分布',
    source: 'user_demand.budget_min / budget_max',
    items: overview.value.budgetDistribution,
  },
  {
    key: 'scene',
    title: '使用场景分布',
    source: 'user_demand.scene',
    items: overview.value.sceneDistribution,
  },
  {
    key: 'popularCars',
    title: '热门推荐车型',
    source: 'recommend_item + car_model',
    items: overview.value.popularCars,
  },
  {
    key: 'focus',
    title: '关注因素分布',
    source: 'user_demand.focus_factors',
    items: overview.value.focusFactorDistribution,
  },
  {
    key: 'status',
    title: '推荐状态分布',
    source: 'recommend_record.recommend_status',
    items: overview.value.recommendStatusDistribution,
  },
  {
    key: 'energy',
    title: '动力偏好分布',
    source: 'user_demand.energy_type',
    items: overview.value.energyTypeDistribution,
  },
  {
    key: 'body',
    title: '车型偏好分布',
    source: 'user_demand.body_type',
    items: overview.value.bodyTypeDistribution,
  },
])

onMounted(loadOverview)

async function loadOverview() {
  loading.value = true
  error.value = ''
  try {
    const response = await fetchAdminStatOverview()
    overview.value = {
      ...emptyOverview(),
      ...(response.data || {}),
    }
  } catch (requestError) {
    overview.value = emptyOverview()
    error.value = requestError?.response?.data?.message || requestError?.message || '统计数据加载失败。'
  } finally {
    loading.value = false
  }
}

function emptyOverview() {
  return {
    budgetDistribution: [],
    sceneDistribution: [],
    focusFactorDistribution: [],
    popularCars: [],
    recommendStatusDistribution: [],
    energyTypeDistribution: [],
    bodyTypeDistribution: [],
    satisfactionDistribution: [],
    feedbackReasonDistribution: [],
  }
}

function totalOf(items = []) {
  return items.reduce((sum, item) => sum + Number(item.value || 0), 0)
}

function barWidth(value, items) {
  const maxValue = Math.max(...items.map((item) => Number(item.value || 0)), 0)
  if (!maxValue) return '0%'
  return `${(Number(value || 0) / maxValue) * 100}%`
}
</script>

<style scoped>
.state-alert {
  margin-bottom: 18px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.metric-card {
  min-height: 128px;
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--shadow-card);
}

.metric-card span,
.metric-card p {
  color: var(--color-muted);
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin: 10px 0 8px;
  color: var(--color-primary-dark);
  font-size: 34px;
}

.metric-card p {
  margin: 0;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.chart-card {
  min-height: 320px;
}

.chart-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.chart-head h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 18px;
}

.chart-head p {
  margin: 6px 0 0;
  color: var(--color-muted);
  font-size: 12px;
}

.chart-head span {
  color: var(--color-muted);
  font-size: 12px;
}

.bar-list {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.bar-row__label {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 7px;
  font-size: 13px;
}

.bar-row__label span {
  color: var(--color-primary-dark);
}

.bar-row__label strong {
  color: var(--color-primary);
}

.bar-track {
  height: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef2f7;
}

.bar-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
}

.empty-chart {
  display: grid;
  min-height: 210px;
  place-items: center;
  color: var(--color-muted);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-sm);
  margin-top: 18px;
}

@media (max-width: 980px) {
  .metric-grid,
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
