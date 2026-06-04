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
      <section class="dashboard-hero">
        <div>
          <p class="eyebrow">运营概览</p>
          <h2>推荐业务快照</h2>
          <p>把核心数据压缩成可扫描的指标、趋势和分布，优先观察推荐链路是否活跃。</p>
          <div class="hero-pills">
            <span>今日推荐 <strong>{{ overview.todayRecommendRecordCount || 0 }}</strong></span>
            <span>近 7 天 <strong>{{ overview.recentRecommendRecordCount || 0 }}</strong></span>
            <span>反馈 <strong>{{ overview.feedbackCount || 0 }}</strong></span>
            <span>满意度 <strong>{{ formatAverage(overview.averageSatisfaction) }}</strong></span>
          </div>
        </div>
        <div class="trend-panel" aria-label="运营指标曲线快照">
          <svg viewBox="0 0 420 150" role="img" aria-label="运营指标曲线快照">
            <defs>
              <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#0A84FF" stop-opacity="0.28" />
                <stop offset="100%" stop-color="#0A84FF" stop-opacity="0" />
              </linearGradient>
              <linearGradient id="trendStroke" x1="0" y1="0" x2="1" y2="0">
                <stop offset="0%" stop-color="#0A84FF" />
                <stop offset="55%" stop-color="#5AC8FA" />
                <stop offset="100%" stop-color="#2F855A" />
              </linearGradient>
            </defs>
            <line v-for="line in trendGridLines" :key="line" class="trend-grid-line" x1="24" x2="396" :y1="line" :y2="line" />
            <path class="trend-area" :d="trendAreaPath" />
            <polyline class="trend-line" :points="trendLinePoints" />
            <g v-for="point in trendPoints" :key="point.label">
              <circle :cx="point.x" :cy="point.y" r="4" />
              <text class="trend-value" :x="point.x" :y="point.y - 10">{{ point.display }}</text>
              <text class="trend-label" :x="point.x" y="132">{{ point.label }}</text>
            </g>
          </svg>
        </div>
      </section>

      <div class="metric-grid">
        <div v-for="metric in metrics" :key="metric.label" class="metric-card">
          <div class="metric-card__top">
            <span>{{ metric.label }}</span>
            <el-icon :class="['metric-icon', `metric-icon--${metric.tone}`]">
              <component :is="metric.icon" />
            </el-icon>
          </div>
          <strong>{{ metric.value }}</strong>
          <p>{{ metric.source }}</p>
        </div>
      </div>

      <div class="section-band">
        <div>
          <p class="eyebrow">推荐业务</p>
          <h2>状态分布与热门车型</h2>
        </div>
      </div>
      <div class="chart-grid">
        <ChartCard
          title="推荐状态分布"
          source="推荐记录状态"
          :items="overview.recommendStatusDistribution"
        />
        <ChartCard
          title="热门推荐车型 TOP 10"
          source="推荐明细与车型"
          :items="overview.popularCars"
        />
      </div>

      <div class="section-band">
        <div>
          <p class="eyebrow">用户偏好</p>
          <h2>预算、车型、动力、场景和关注因素</h2>
        </div>
      </div>
      <div class="chart-grid">
        <ChartCard title="预算区间分布" source="用户需求预算" :items="overview.budgetDistribution" />
        <ChartCard title="车型偏好分布" source="用户需求车型" :items="overview.bodyTypeDistribution" />
        <ChartCard title="动力偏好分布" source="用户需求动力" :items="overview.energyTypeDistribution" />
        <ChartCard title="使用场景分布" source="用户需求场景" :items="overview.sceneDistribution" />
        <ChartCard title="显式关注因素" source="用户显式权重" :items="overview.focusFactorDistribution" />
      </div>

      <div class="section-band">
        <div>
          <p class="eyebrow">收藏与反馈</p>
          <h2>收藏排行、满意度和反馈原因</h2>
        </div>
      </div>
      <div class="chart-grid">
        <ChartCard title="收藏最多车型 TOP 10" source="收藏车型排行" :items="overview.favoriteTopCars" />
        <ChartCard title="满意度分布" source="反馈满意度" :items="overview.satisfactionDistribution" />
        <ChartCard title="反馈原因标签" source="反馈原因标签" :items="overview.feedbackReasonDistribution" />
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import {
  Calendar,
  ChatLineSquare,
  CircleCheck,
  CircleClose,
  Collection,
  Star,
  Tickets,
  TrendCharts,
  User,
  Van,
} from '@element-plus/icons-vue'

import { fetchAdminStatOverview } from '@/api/adminStats'

const ChartCard = defineComponent({
  props: {
    title: { type: String, required: true },
    source: { type: String, required: true },
    items: { type: Array, default: () => [] },
  },
  setup(props) {
    const maxValue = computed(() => Math.max(...props.items.map((item) => Number(item.value || 0)), 0))
    const totalValue = computed(() => props.items.reduce((sum, item) => sum + Number(item.value || 0), 0))
    const visibleItems = computed(() => props.items)
    function barWidth(value) {
      if (!maxValue.value) return '0%'
      return `${(Number(value || 0) / maxValue.value) * 100}%`
    }
    function rankClass(index) {
      if (index === 0) return 'bar-rank bar-rank--gold'
      if (index === 1) return 'bar-rank bar-rank--blue'
      if (index === 2) return 'bar-rank bar-rank--green'
      return 'bar-rank'
    }
    return () =>
      h('article', { class: 'panel chart-card' }, [
        h('div', { class: 'panel__body' }, [
          h('div', { class: 'chart-head' }, [
            h('div', [h('h2', props.title), h('p', props.source)]),
            h('span', `${props.items.length} 项`),
          ]),
          h('div', { class: 'stat-table-toolbar' }, [
            h('span', [h('small', '总量'), h('strong', String(totalValue.value))]),
            h('span', [h('small', '峰值'), h('strong', String(maxValue.value))]),
          ]),
          props.items.length
            ? h(
                'table',
                { class: 'stat-table' },
                [
                  h('thead', [
                    h('tr', [
                      h('th', '排名'),
                      h('th', '名称'),
                      h('th', '数量'),
                      h('th', '占比'),
                    ]),
                  ]),
                  h(
                    'tbody',
                    visibleItems.value.map((item, index) =>
                      h('tr', { key: item.name }, [
                        h('td', [h('span', { class: rankClass(index) }, String(index + 1))]),
                        h('td', [h('span', { class: 'stat-name' }, item.name)]),
                        h('td', [h('strong', String(item.value ?? 0))]),
                        h('td', [
                          h('div', { class: 'stat-progress' }, [
                            h('i', { style: { width: barWidth(item.value) } }),
                          ]),
                        ]),
                      ]),
                    ),
                  ),
                ],
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
  { label: '用户总数', value: overview.value.userCount || 0, source: '用户表', icon: User, tone: 'blue' },
  { label: '启用用户', value: overview.value.activeUserCount || 0, source: '启用状态', icon: CircleCheck, tone: 'green' },
  { label: '禁用用户', value: overview.value.disabledUserCount || 0, source: '禁用状态', icon: CircleClose, tone: 'orange' },
  { label: '车型总数', value: overview.value.carCount || 0, source: '车型表', icon: Van, tone: 'cyan' },
  { label: '推荐记录', value: overview.value.recommendRecordCount || 0, source: '推荐记录表', icon: Tickets, tone: 'blue' },
  { label: '今日推荐', value: overview.value.todayRecommendRecordCount || 0, source: '自然日', icon: Calendar, tone: 'cyan' },
  { label: '近 7 天推荐', value: overview.value.recentRecommendRecordCount || 0, source: '滚动窗口', icon: TrendCharts, tone: 'green' },
  { label: '收藏总数', value: overview.value.favoriteCount || 0, source: '收藏表', icon: Collection, tone: 'orange' },
  { label: '反馈总数', value: overview.value.feedbackCount || 0, source: '反馈表', icon: ChatLineSquare, tone: 'blue' },
  { label: '平均满意度', value: formatAverage(overview.value.averageSatisfaction), source: '1-5 分', icon: Star, tone: 'green' },
])

const trendPoints = computed(() => {
  const values = [
    { label: '用户', value: overview.value.activeUserCount || overview.value.userCount || 0 },
    { label: '车型', value: overview.value.carCount || 0 },
    { label: '推荐', value: overview.value.recentRecommendRecordCount || overview.value.recommendRecordCount || 0 },
    { label: '收藏', value: overview.value.favoriteCount || 0 },
    { label: '反馈', value: overview.value.feedbackCount || 0 },
    { label: '满意', value: Number(overview.value.averageSatisfaction || 0) * 10 },
  ]
  const max = Math.max(...values.map((item) => Math.log1p(Number(item.value || 0))), 1)
  return values.map((item, index) => ({
    label: item.label,
    display: item.label === '满意' ? formatAverage(overview.value.averageSatisfaction) : String(item.value || 0),
    x: 42 + index * 67,
    y: 94 - (Math.log1p(Number(item.value || 0)) / max) * 58,
  }))
})

const trendLinePoints = computed(() => trendPoints.value.map((point) => `${point.x},${point.y}`).join(' '))
const trendAreaPath = computed(() => {
  const points = trendPoints.value
  if (!points.length) return ''
  return `M ${points[0].x} 106 L ${points.map((point) => `${point.x} ${point.y}`).join(' L ')} L ${points[points.length - 1].x} 106 Z`
})
const trendGridLines = [36, 58, 80, 102]

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

.dashboard-hero {
  display: grid;
  grid-template-columns: minmax(0, 0.72fr) minmax(430px, 1.28fr);
  gap: 18px;
  align-items: center;
  margin-bottom: 18px;
  padding: 22px;
  border: 1px solid rgba(10, 132, 255, 0.12);
  border-radius: var(--radius-md);
  background:
    radial-gradient(circle at 82% 12%, rgba(90, 200, 250, 0.22), transparent 32%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(239, 248, 255, 0.9));
  box-shadow: var(--shadow-card);
}

.dashboard-hero h2 {
  margin: 6px 0 8px;
  color: var(--color-primary-dark);
  font-size: 28px;
}

.dashboard-hero p:last-child {
  max-width: 520px;
  margin: 0;
  color: var(--color-muted);
  line-height: 1.7;
}

.hero-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.hero-pills span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  border: 1px solid rgba(10, 132, 255, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  color: var(--color-muted);
  font-size: 12px;
}

.hero-pills strong {
  color: var(--color-primary-dark);
}

.trend-panel {
  min-height: 178px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(18px);
}

.trend-panel svg {
  display: block;
  width: 100%;
  height: 156px;
}

.trend-grid-line {
  stroke: rgba(102, 112, 133, 0.12);
  stroke-dasharray: 4 6;
}

.trend-area {
  fill: url("#trendFill");
}

.trend-line {
  fill: none;
  stroke: url("#trendStroke");
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 4;
}

.trend-panel circle {
  fill: #fff;
  stroke: var(--color-primary);
  stroke-width: 2.5;
}

.trend-value,
.trend-label {
  text-anchor: middle;
  user-select: none;
}

.trend-value {
  fill: var(--color-primary-dark);
  font-size: 11px;
  font-weight: 800;
}

.trend-label {
  fill: var(--color-muted);
  font-size: 11px;
}

.stat-table-toolbar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 16px;
  margin-bottom: 12px;
}

.stat-table-toolbar span {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  background: #f6f9fd;
}

.stat-table-toolbar small {
  color: var(--color-muted);
  font-size: 12px;
}

.stat-table-toolbar strong {
  color: var(--color-primary-dark);
  font-size: 18px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.metric-card {
  position: relative;
  overflow: hidden;
  min-height: 112px;
  padding: 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background:
    radial-gradient(circle at 88% 8%, rgba(10, 132, 255, 0.09), transparent 32%),
    #fff;
  box-shadow: var(--shadow-card);
}

.metric-card::after {
  position: absolute;
  right: -24px;
  bottom: -28px;
  width: 88px;
  height: 88px;
  content: "";
  border-radius: 50%;
  background: rgba(10, 132, 255, 0.05);
}

.metric-card__top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-card span,
.metric-card p {
  color: var(--color-muted);
  font-size: 13px;
}

.metric-card strong {
  position: relative;
  z-index: 1;
  display: block;
  margin: 9px 0 6px;
  color: var(--color-primary-dark);
  font-size: 30px;
}

.metric-card p {
  position: relative;
  z-index: 1;
  margin: 0;
}

.metric-icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  font-size: 18px;
}

.metric-icon--blue {
  background: rgba(10, 132, 255, 0.1);
  color: var(--color-primary);
}

.metric-icon--green {
  background: rgba(47, 133, 90, 0.12);
  color: var(--color-success);
}

.metric-icon--orange {
  background: rgba(183, 121, 31, 0.12);
  color: var(--color-warning);
}

.metric-icon--cyan {
  background: rgba(90, 200, 250, 0.14);
  color: #0284c7;
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
  min-height: 340px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.98));
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

.stat-table {
  width: 100%;
  margin-top: 18px;
  border-collapse: collapse;
  overflow: hidden;
  border: 1px solid #dfe5ee;
  background: #fff;
  table-layout: fixed;
}

.stat-table th,
.stat-table td {
  padding: 12px 14px;
  border: 1px solid #e6ebf2;
  text-align: left;
  vertical-align: middle;
}

.stat-table th {
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

.stat-table tbody tr:nth-child(even) {
  background: #fbfdff;
}

.stat-table tbody tr:hover {
  background: #f3f7fc;
}

.stat-table th:nth-child(1),
.stat-table td:nth-child(1) {
  width: 58px;
}

.stat-table th:nth-child(3),
.stat-table td:nth-child(3) {
  width: 82px;
  text-align: right;
}

.stat-table th:nth-child(4),
.stat-table td:nth-child(4) {
  width: 34%;
}

.stat-name {
  display: block;
  color: var(--color-primary-dark);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 650;
}

.stat-table td strong {
  color: var(--color-primary);
  font-size: 13px;
}

.bar-rank {
  display: inline-grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 8px;
  background: #eef2f7;
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 800;
}

.bar-rank--gold {
  background: rgba(183, 121, 31, 0.14);
  color: var(--color-warning);
}

.bar-rank--blue {
  background: rgba(10, 132, 255, 0.12);
  color: var(--color-primary);
}

.bar-rank--green {
  background: rgba(47, 133, 90, 0.12);
  color: var(--color-success);
}

.stat-progress {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #edf2f7;
  box-shadow: inset 0 0 0 1px rgba(17, 24, 39, 0.04);
}

.stat-progress i {
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

:global(.stat-table) {
  width: 100%;
  margin-top: 18px;
  border: 1px solid #cfd8e3;
  border-collapse: collapse;
  background: #fff;
  table-layout: fixed;
}

:global(.stat-table th),
:global(.stat-table td) {
  padding: 12px 14px;
  border: 1px solid #d9e1ec;
  text-align: left;
  vertical-align: middle;
}

:global(.stat-table th) {
  background: #f3f6fa;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

:global(.stat-table tbody tr:nth-child(even)) {
  background: #fbfdff;
}

:global(.stat-table tbody tr:hover) {
  background: #f3f7fc;
}

:global(.stat-table th:nth-child(1)),
:global(.stat-table td:nth-child(1)) {
  width: 58px;
}

:global(.stat-table th:nth-child(3)),
:global(.stat-table td:nth-child(3)) {
  width: 82px;
  text-align: right;
}

:global(.stat-table th:nth-child(4)),
:global(.stat-table td:nth-child(4)) {
  width: 34%;
}

:global(.stat-name) {
  display: block;
  color: var(--color-primary-dark);
  overflow: hidden;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.stat-table td strong) {
  color: var(--color-primary);
  font-size: 13px;
}

:global(.stat-progress) {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #edf2f7;
  box-shadow: inset 0 0 0 1px rgba(17, 24, 39, 0.05);
}

:global(.stat-progress i) {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
}

@media (max-width: 1080px) {
  .dashboard-hero {
    grid-template-columns: 1fr;
  }

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
