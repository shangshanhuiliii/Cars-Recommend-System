<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">车型对比</h1>
        <p class="page-subtitle">最多选择 3 款车型，横向查看基础信息、参数和八维评分，帮助你快速看清差异。</p>
      </div>
      <el-button type="primary" plain @click="returnToSource">返回推荐结果</el-button>
    </div>

    <div class="panel compare-toolbar">
      <div class="panel__body toolbar-body">
        <div>
          <p class="toolbar-label">当前对比</p>
          <div class="selected-tags">
            <el-tag
              v-for="id in selectedIds"
              :key="id"
              closable
              effect="light"
              @close="removeCar(id)"
            >
              车型 #{{ id }}
            </el-tag>
            <span v-if="!selectedIds.length" class="muted-text">暂未选择车型</span>
          </div>
        </div>
        <div class="add-control">
          <el-select
            v-model="pendingCarId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="搜索车型加入对比"
            :remote-method="searchCars"
            :loading="optionLoading"
          >
            <el-option
              v-for="option in carOptions"
              :key="option.id"
              :label="option.displayName"
              :value="option.id"
            />
          </el-select>
          <el-button type="primary" :loading="operating" @click="addSelectedCar">加入对比</el-button>
          <el-button plain :disabled="!selectedIds.length" :loading="operating" @click="clearSelectedCars">清空</el-button>
        </div>
        <p
          v-if="operationMessage"
          class="inline-state toolbar-state"
          :class="{ 'inline-state--error': operationMessageType === 'error' }"
        >
          {{ operationMessage }}
        </p>
      </div>
    </div>

    <div v-if="loading" class="panel">
      <div class="panel__body">
        <el-skeleton :rows="10" animated />
      </div>
    </div>

    <el-alert
      v-else-if="error"
      class="state-alert"
      type="error"
      :closable="false"
      :title="error"
      show-icon
    />

    <div v-else-if="!selectedIds.length" class="panel">
      <div class="panel__body">
        <el-empty description="暂未选择车型">
          <el-button type="primary" @click="returnToSource">返回推荐结果选择车型</el-button>
        </el-empty>
      </div>
    </div>

    <template v-else-if="comparison">
      <el-alert
        v-if="selectedIds.length === 1"
        class="state-alert"
        type="info"
        :closable="false"
        title="当前已选择 1 款车型，可继续从推荐结果或车型详情中加入更多车型进行对比。"
        show-icon
      />

      <div class="compare-grid">
        <article v-for="car in comparison.cars" :key="car.carId" class="compare-card">
          <div class="card-visual">
            <img :src="carImageSrc(car.imageUrl)" :alt="car.modelName" @error="fallbackCarImage" />
          </div>
          <div class="compare-card__body">
            <h2>{{ car.brand }} {{ car.modelName }}</h2>
            <p>{{ car.series }} · {{ car.bodyType }} · {{ car.energyType }} · {{ car.seats }} 座</p>
            <strong>{{ formatWan(car.guidePrice) }}</strong>
          </div>
        </article>
      </div>

      <div class="panel radar-panel">
        <div class="panel__body">
          <div class="section-head">
            <div>
              <h2>八维静态评分</h2>
              <p>价格不进入雷达图，价格只在基础信息中展示。</p>
            </div>
            <span>来自 car_feature_score</span>
          </div>

          <div v-if="hasAnyScore" class="radar-layout">
            <svg class="radar-svg" viewBox="0 0 420 420" role="img" aria-label="车型静态评分雷达图">
              <g v-for="level in [1, 2, 3, 4, 5]" :key="level">
                <polygon class="radar-grid-line" :points="gridPoints(level / 5)" />
              </g>
              <line
                v-for="axis in radarAxes"
                :key="axis.key"
                class="radar-axis"
                x1="210"
                y1="210"
                :x2="axis.x"
                :y2="axis.y"
              />
              <text
                v-for="axis in radarAxes"
                :key="`${axis.key}-label`"
                class="radar-label"
                :x="axis.labelX"
                :y="axis.labelY"
                text-anchor="middle"
              >
                {{ axis.label }}
              </text>
              <polygon
                v-for="series in radarSeries"
                :key="series.carId"
                class="radar-polygon"
                :points="series.points"
                :style="{ stroke: series.color, fill: series.color }"
              />
            </svg>

            <div class="legend-list">
              <div v-for="series in radarSeries" :key="series.carId" class="legend-item">
                <i :style="{ background: series.color }"></i>
                <span>{{ series.name }}</span>
              </div>
              <el-alert
                v-if="hasMissingScore"
                type="warning"
                :closable="false"
                title="部分车型暂无评分，请在管理端重算评分。"
              />
            </div>
          </div>
          <el-empty v-else description="暂无评分，请在管理端重算评分" />
        </div>
      </div>

      <div class="panel table-panel">
        <div class="panel__body">
          <h2>基础信息对比</h2>
          <CompareTable :rows="baseRows" :cars="comparison.cars" />
        </div>
      </div>

      <div class="panel table-panel">
        <div class="panel__body">
          <h2>参数对比</h2>
          <CompareTable :rows="paramRows" :cars="comparison.cars" />
        </div>
      </div>

      <div class="panel table-panel">
        <div class="panel__body">
          <h2>评分对比</h2>
          <CompareTable :rows="scoreRows" :cars="comparison.cars" />
        </div>
      </div>

      <div class="panel">
        <div class="panel__body">
          <h2>对比结论</h2>
          <div class="insight-list">
            <p v-for="insight in insights" :key="insight">{{ insight }}</p>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchCarOptions } from '@/api/cars'
import { addUserCompare, clearUserCompare, fetchUserCompare, removeUserCompare } from '@/api/userCompare'
import { carImageSrc, fallbackCarImage } from '@/utils/carImage'
import { readCompareReturn } from '@/utils/compareSelection'

const CompareTable = defineComponent({
  props: {
    rows: { type: Array, required: true },
    cars: { type: Array, required: true },
  },
  setup(props) {
    return () =>
      h('div', { class: 'compare-table-wrap' }, [
        h('table', { class: 'compare-table' }, [
          h('thead', [
            h('tr', [
              h('th', '项目'),
              ...props.cars.map((car) => h('th', `${car.brand} ${car.modelName}`)),
            ]),
          ]),
          h(
            'tbody',
            props.rows.map((row) =>
              h('tr', { key: row.label }, [
                h('td', { class: 'row-label' }, row.label),
                ...props.cars.map((car) =>
                  h('td', { class: { highlight: row.bestCarId && row.bestCarId === car.carId } }, row.values[car.carId] || '暂无'),
                ),
              ]),
            ),
          ),
        ]),
      ])
  },
})

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const comparison = ref(null)
const selectedIds = ref([])
const pendingCarId = ref(null)
const optionLoading = ref(false)
const carOptions = ref([])
const operationMessage = ref('')
const operationMessageType = ref('info')
const operating = ref(false)

const dimensions = computed(() => comparison.value?.dimensions || [])
const colors = ['#2563EB', '#0891B2', '#16A34A']
const hasAnyScore = computed(() => (comparison.value?.cars || []).some((car) => !!car.scores))
const hasMissingScore = computed(() => (comparison.value?.cars || []).some((car) => !car.scores))

const radarAxes = computed(() =>
  dimensions.value.map((dimension, index) => {
    const point = radarPoint(1, index)
    const labelPoint = radarPoint(1.16, index)
    return {
      ...dimension,
      x: point.x,
      y: point.y,
      labelX: labelPoint.x,
      labelY: labelPoint.y,
    }
  }),
)

const radarSeries = computed(() =>
  (comparison.value?.cars || [])
    .filter((car) => car.scores)
    .map((car, index) => ({
      carId: car.carId,
      name: `${car.brand} ${car.modelName}`,
      color: colors[index % colors.length],
      points: pointsFor(car.scores),
    })),
)

const baseRows = computed(() => [
  rowOf('指导价', (car) => formatWan(car.guidePrice), (car) => Number(car.guidePrice || 0), 'min'),
  rowOf('车型类型', (car) => car.bodyType),
  rowOf('动力类型', (car) => car.energyType),
  rowOf('座位数', (car) => `${car.seats || '暂无'} 座`, (car) => Number(car.seats || 0), 'max'),
  rowOf('上市年份', (car) => car.launchYear || '暂无', (car) => Number(car.launchYear || 0), 'max'),
])

const paramRows = computed(() => [
  rowOf('车身尺寸', (car) => (car.param ? `${car.param.lengthMm} / ${car.param.widthMm} / ${car.param.heightMm} mm` : '暂无')),
  rowOf('轴距', (car) => formatMetric(car.param?.wheelbaseMm, 'mm'), (car) => Number(car.param?.wheelbaseMm || 0), 'max'),
  rowOf('燃油油耗', (car) => formatMetric(car.param?.fuelConsumption, 'L/100km'), (car) => Number(car.param?.fuelConsumption || 0), 'minPositive'),
  rowOf('电耗', (car) => formatMetric(car.param?.electricConsumption, 'kWh/100km'), (car) => Number(car.param?.electricConsumption || 0), 'minPositive'),
  rowOf('纯电续航', (car) => formatMetric(car.param?.electricRangeKm, 'km'), (car) => Number(car.param?.electricRangeKm || 0), 'max'),
  rowOf('综合续航', (car) => formatMetric(car.param?.totalRangeKm, 'km'), (car) => Number(car.param?.totalRangeKm || 0), 'max'),
  rowOf('百公里加速', (car) => formatMetric(car.param?.acceleration100, 's'), (car) => Number(car.param?.acceleration100 || 0), 'minPositive'),
  rowOf('气囊数量', (car) => formatMetric(car.param?.airbagCount, '个'), (car) => Number(car.param?.airbagCount || 0), 'max'),
  rowOf('辅助驾驶', (car) => car.param?.assistDriveLevel || '暂无'),
])

const scoreRows = computed(() =>
  dimensions.value.map((dimension) =>
    rowOf(
      dimension.label,
      (car) => (car.scores ? formatScore(car.scores[dimension.key]) : '暂无评分，请在管理端重算评分'),
      (car) => Number(car.scores?.[dimension.key] || 0),
      'max',
    ),
  ),
)

const insights = computed(() => {
  const cars = comparison.value?.cars || []
  if (!cars.length) return ['暂无可对比车型。']
  return dimensions.value
    .map((dimension) => {
      const best = cars
        .filter((car) => car.scores)
        .map((car) => ({ car, score: Number(car.scores?.[dimension.key] || 0) }))
        .sort((left, right) => right.score - left.score)[0]
      if (!best || best.score <= 0) return null
      return `${dimension.label}维度：${best.car.brand} ${best.car.modelName} 当前最高，为 ${best.score.toFixed(2)} 分。`
    })
    .filter(Boolean)
    .slice(0, 4)
})

onMounted(loadCompare)

async function loadCompare() {
  loading.value = true
  error.value = ''
  try {
    const response = await fetchUserCompare()
    comparison.value = response.data
    selectedIds.value = response.data?.carIds || []
  } catch (requestError) {
    comparison.value = null
    selectedIds.value = []
    error.value = requestError?.response?.data?.message || requestError?.message || '车型对比加载失败。'
  } finally {
    loading.value = false
  }
}

async function searchCars(keyword) {
  optionLoading.value = true
  setOperationMessage('')
  try {
    const response = await fetchCarOptions({ keyword, limit: 20 })
    carOptions.value = response.data || []
  } catch (requestError) {
    setOperationMessage(requestError?.response?.data?.message || requestError?.message || '车型搜索失败。', 'error')
  } finally {
    optionLoading.value = false
  }
}

async function addSelectedCar() {
  if (!pendingCarId.value) {
    setOperationMessage('请先选择车型。', 'error')
    return
  }
  operating.value = true
  try {
    const response = await addUserCompare(pendingCarId.value)
    comparison.value = response.data
    selectedIds.value = response.data?.carIds || []
    pendingCarId.value = null
    setOperationMessage('该车型已加入对比。')
  } catch (requestError) {
    setOperationMessage(requestError?.response?.data?.message || requestError?.message || '加入对比失败。', 'error')
  } finally {
    operating.value = false
  }
}

async function removeCar(carId) {
  operating.value = true
  try {
    const response = await removeUserCompare(carId)
    comparison.value = response.data
    selectedIds.value = response.data?.carIds || []
    setOperationMessage(selectedIds.value.length ? '已从对比中移出。' : '已清空对比车型。')
  } catch (requestError) {
    setOperationMessage(requestError?.response?.data?.message || requestError?.message || '移出对比失败。', 'error')
  } finally {
    operating.value = false
  }
}

async function clearSelectedCars() {
  operating.value = true
  try {
    const response = await clearUserCompare()
    comparison.value = response.data
    selectedIds.value = []
    setOperationMessage('已清空对比车型。')
  } catch (requestError) {
    setOperationMessage(requestError?.response?.data?.message || requestError?.message || '清空对比失败。', 'error')
  } finally {
    operating.value = false
  }
}

function returnToSource() {
  const saved = readCompareReturn()
  const target = saved?.path && saved.path !== route.fullPath ? saved.path : '/history'
  router.push(target)
}

function setOperationMessage(text, type = 'info') {
  operationMessage.value = text
  operationMessageType.value = type
}

function rowOf(label, formatter, ranker, mode) {
  const cars = comparison.value?.cars || []
  const values = Object.fromEntries(cars.map((car) => [car.carId, formatter(car)]))
  return {
    label,
    values,
    bestCarId: bestCarId(cars, ranker, mode),
  }
}

function bestCarId(cars, ranker, mode) {
  if (!ranker || !mode) return null
  const ranked = cars
    .map((car) => ({ carId: car.carId, value: ranker(car) }))
    .filter((item) => (mode === 'minPositive' ? item.value > 0 : item.value >= 0))
  if (!ranked.length) return null
  ranked.sort((left, right) => (mode === 'max' ? right.value - left.value : left.value - right.value))
  return ranked[0].value > 0 ? ranked[0].carId : null
}

function radarPoint(scale, index) {
  const total = dimensions.value.length || 1
  const angle = -Math.PI / 2 + (Math.PI * 2 * index) / total
  const radius = 140 * scale
  return {
    x: 210 + Math.cos(angle) * radius,
    y: 210 + Math.sin(angle) * radius,
  }
}

function gridPoints(scale) {
  return dimensions.value.map((_, index) => {
    const point = radarPoint(scale, index)
    return `${point.x},${point.y}`
  }).join(' ')
}

function pointsFor(scores) {
  return dimensions.value.map((dimension, index) => {
    const point = radarPoint(Math.max(0, Math.min(100, Number(scores?.[dimension.key] || 0))) / 100, index)
    return `${point.x},${point.y}`
  }).join(' ')
}

function formatScore(value) {
  return `${Number(value || 0).toFixed(2)} 分`
}

function formatWan(value) {
  return `${(Number(value || 0) / 10000).toFixed(1).replace(/\.0$/, '')} 万`
}

function formatMetric(value, unit) {
  if (!value) return '暂无'
  return `${value} ${unit}`
}
</script>

<style scoped>
.compare-toolbar,
.state-alert,
.compare-grid,
.radar-panel,
.table-panel {
  margin-bottom: 18px;
}

.toolbar-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 18px;
  align-items: end;
}

.toolbar-state {
  grid-column: 1 / -1;
}

.toolbar-label {
  margin: 0 0 8px;
  color: var(--color-muted);
  font-size: 13px;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
  align-items: center;
}

.muted-text {
  color: var(--color-muted);
  font-size: 13px;
}

.inline-state {
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.6;
}

.inline-state--error {
  color: var(--color-danger);
}

.add-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px 72px;
  gap: 10px;
}

.compare-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.compare-card {
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--shadow-card);
}

.card-visual {
  height: 150px;
  background: #eef2f7;
}

.card-visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.compare-card__body {
  padding: 16px;
}

.compare-card h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 18px;
}

.compare-card p {
  margin: 8px 0 12px;
  color: var(--color-muted);
  font-size: 13px;
}

.compare-card strong {
  color: var(--color-primary);
  font-size: 18px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.section-head h2,
.table-panel h2,
.panel h2 {
  margin: 0 0 8px;
  color: var(--color-primary-dark);
  font-size: 18px;
}

.section-head p,
.section-head span {
  margin: 0;
  color: var(--color-muted);
  font-size: 12px;
}

.radar-layout {
  display: grid;
  grid-template-columns: 420px minmax(0, 1fr);
  gap: 22px;
  align-items: center;
}

.radar-svg {
  width: 100%;
  max-width: 420px;
}

.radar-grid-line {
  fill: none;
  stroke: #dbe3ee;
  stroke-width: 1;
}

.radar-axis {
  stroke: #e5e7eb;
  stroke-width: 1;
}

.radar-label {
  fill: var(--color-muted);
  font-size: 12px;
}

.radar-polygon {
  fill-opacity: 0.14;
  stroke-width: 2;
}

.legend-list {
  display: grid;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-primary-dark);
  font-size: 13px;
}

.legend-item i {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

:deep(.compare-table-wrap) {
  overflow-x: auto;
}

:deep(.compare-table) {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

:deep(.compare-table th),
:deep(.compare-table td) {
  padding: 12px;
  border: 1px solid var(--color-border);
  text-align: left;
  font-size: 13px;
}

:deep(.compare-table th) {
  background: #f8fafc;
  color: var(--color-primary-dark);
}

:deep(.row-label) {
  width: 120px;
  color: var(--color-muted);
  font-weight: 700;
}

:deep(.highlight) {
  background: rgba(22, 163, 74, 0.08);
  color: var(--color-success);
  font-weight: 700;
}

.insight-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.insight-list p {
  margin: 0;
  padding: 12px;
  border: 1px solid rgba(8, 145, 178, 0.18);
  border-radius: var(--radius-sm);
  background: rgba(8, 145, 178, 0.06);
  color: var(--color-primary-dark);
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .toolbar-body,
  .compare-grid,
  .radar-layout,
  .insight-list {
    grid-template-columns: 1fr;
  }

  .add-control {
    grid-template-columns: 1fr;
  }
}
</style>
