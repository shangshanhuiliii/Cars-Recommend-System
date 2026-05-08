<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">运营概览</h1>
        <p class="page-subtitle">汇总用户、车型、推荐、收藏和反馈数据，帮助管理员观察推荐业务运行情况。</p>
      </div>
      <el-button type="primary" plain :loading="loading" @click="loadOverview">刷新概览</el-button>
    </div>

    <el-alert v-if="error" class="state-alert" type="error" :closable="false" :title="error" show-icon />

    <div v-if="loading" class="panel">
      <div class="panel__body">
        <el-skeleton :rows="10" animated />
      </div>
    </div>

    <template v-else>
      <div class="metric-grid">
        <div v-for="metric in metrics" :key="metric.label" class="metric-card">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <p>{{ metric.source }}</p>
        </div>
      </div>

      <div class="section-band">
        <div>
          <p class="eyebrow">推荐业务</p>
          <h2>推荐量、状态分布和热门推荐车型</h2>
        </div>
      </div>
      <div class="chart-grid">
        <ChartCard
          title="推荐状态分布"
          source="recommend_record.recommend_status"
          :items="overview.recommendStatusDistribution"
        />
        <ChartCard
          title="热门推荐车型 TOP 10"
          source="recommend_item + car_model"
          :items="overview.popularCars"
        />
      </div>

      <div class="section-band">
        <div>
          <p class="eyebrow">用户偏好</p>
          <h2>预算、车型、动力、场景和显式关注因素</h2>
        </div>
      </div>
      <div class="chart-grid">
        <ChartCard title="预算区间分布" source="user_demand.budget_min / budget_max" :items="overview.budgetDistribution" />
        <ChartCard title="车型偏好分布" source="user_demand.body_types" :items="overview.bodyTypeDistribution" />
        <ChartCard title="动力偏好分布" source="user_demand.energy_types" :items="overview.energyTypeDistribution" />
        <ChartCard title="使用场景分布" source="user_demand.scenes" :items="overview.sceneDistribution" />
        <ChartCard title="显式关注因素" source="user_demand.factor_weights" :items="overview.focusFactorDistribution" />
      </div>

      <div class="section-band">
        <div>
          <p class="eyebrow">收藏与反馈</p>
          <h2>收藏排行、满意度和反馈原因</h2>
        </div>
      </div>
      <div class="chart-grid">
        <ChartCard title="收藏最多车型 TOP 10" source="user_favorite + car_model" :items="overview.favoriteTopCars" />
        <ChartCard title="满意度分布" source="recommend_feedback.satisfaction_score" :items="overview.satisfactionDistribution" />
        <ChartCard title="反馈原因标签" source="recommend_feedback.reason_tags" :items="overview.feedbackReasonDistribution" />
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref } from 'vue'

import { fetchAdminStatOverview } from '@/api/adminStats'

const ChartCard = defineComponent({
  props: {
    title: { type: String, required: true },
    source: { type: String, required: true },
    items: { type: Array, default: () => [] },
  },
  setup(props) {
    const maxValue = computed(() => Math.max(...props.items.map((item) => Number(item.value || 0)), 0))
    function barWidth(value) {
      if (!maxValue.value) return '0%'
      return `${(Number(value || 0) / maxValue.value) * 100}%`
    }
    return () =>
      h('article', { class: 'panel chart-card' }, [
        h('div', { class: 'panel__body' }, [
          h('div', { class: 'chart-head' }, [
            h('div', [h('h2', props.title), h('p', props.source)]),
            h('span', `${props.items.length} 项`),
          ]),
          props.items.length
            ? h(
                'div',
                { class: 'bar-list' },
                props.items.map((item) =>
                  h('div', { class: 'bar-row', key: item.name }, [
                    h('div', { class: 'bar-row__label' }, [
                      h('span', item.name),
                      h('strong', String(item.value ?? 0)),
                    ]),
                    h('div', { class: 'bar-track' }, [h('i', { style: { width: barWidth(item.value) } })]),
                  ]),
                ),
              )
            : h('div', { class: 'empty-chart' }, '暂无数据'),
        ]),
      ])
  },
})

const loading = ref(false)
const error = ref('')
const overview = ref(emptyOverview())

const metrics = computed(() => [
  { label: '用户总数', value: overview.value.userCount || 0, source: 'app_user' },
  { label: '启用用户', value: overview.value.activeUserCount || 0, source: 'ACTIVE' },
  { label: '禁用用户', value: overview.value.disabledUserCount || 0, source: 'DISABLED' },
  { label: '车型总数', value: overview.value.carCount || 0, source: 'car_model' },
  { label: '推荐记录', value: overview.value.recommendRecordCount || 0, source: 'recommend_record' },
  { label: '今日推荐', value: overview.value.todayRecommendRecordCount || 0, source: '自然日' },
  { label: '近 7 天推荐', value: overview.value.recentRecommendRecordCount || 0, source: '滚动窗口' },
  { label: '收藏总数', value: overview.value.favoriteCount || 0, source: 'user_favorite' },
  { label: '反馈总数', value: overview.value.feedbackCount || 0, source: 'recommend_feedback' },
  { label: '平均满意度', value: formatAverage(overview.value.averageSatisfaction), source: '1-5 分' },
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
    error.value = requestError?.response?.data?.message || requestError?.message || '运营概览加载失败。'
  } finally {
    loading.value = false
  }
}

function emptyOverview() {
  return {
    userCount: 0,
    activeUserCount: 0,
    disabledUserCount: 0,
    carCount: 0,
    recommendRecordCount: 0,
    todayRecommendRecordCount: 0,
    recentRecommendRecordCount: 0,
    favoriteCount: 0,
    budgetDistribution: [],
    sceneDistribution: [],
    focusFactorDistribution: [],
    popularCars: [],
    favoriteTopCars: [],
    recommendStatusDistribution: [],
    energyTypeDistribution: [],
    bodyTypeDistribution: [],
    satisfactionDistribution: [],
    feedbackReasonDistribution: [],
    feedbackCount: 0,
    averageSatisfaction: null,
  }
}

function formatAverage(value) {
  if (value === null || value === undefined) return '暂无'
  return Number(value || 0).toFixed(2)
}
</script>

<style scoped>
.state-alert {
  margin-bottom: 18px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.metric-card {
  min-height: 112px;
  padding: 18px;
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
  margin: 9px 0 6px;
  color: var(--color-primary-dark);
  font-size: 30px;
}

.metric-card p {
  margin: 0;
}

.section-band {
  margin: 24px 0 14px;
}

.section-band h2 {
  margin: 4px 0 0;
  color: var(--color-primary-dark);
  font-size: 18px;
}

.eyebrow {
  margin: 0;
  color: var(--color-muted);
  font-size: 12px;
  letter-spacing: 0.16em;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.chart-card {
  min-height: 300px;
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
  min-height: 200px;
  place-items: center;
  color: var(--color-muted);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-sm);
  margin-top: 18px;
}

@media (max-width: 1080px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
