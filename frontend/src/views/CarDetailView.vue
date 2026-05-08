<template>
  <section class="car-detail-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">车型详情</h1>
        <p class="page-subtitle">查看车型基础信息、配置参数和多维表现。</p>
      </div>
      <el-button v-if="recordId" @click="$router.push(`/recommend/result/${recordId}`)">返回推荐结果</el-button>
    </div>

    <div v-if="loading" class="panel">
      <div class="panel__body">
        <el-skeleton :rows="8" animated />
      </div>
    </div>

    <el-alert
      v-else-if="error"
      type="error"
      :closable="false"
      :title="error"
      show-icon
    />

    <template v-else-if="detail">
      <section class="detail-hero">
        <div class="detail-visual">
          <img :src="carImageSrc(car.imageUrl)" :alt="car.modelName" @error="fallbackCarImage" />
        </div>
        <aside class="detail-summary">
          <p v-if="recordId" class="context-line">来自本次推荐</p>
          <p class="brand-line">{{ car.brand }} · {{ car.series }}</p>
          <h2>{{ car.modelName }}</h2>
          <strong class="price-line">{{ formatWan(car.guidePrice) }}</strong>
          <div class="summary-tags">
            <span>{{ car.bodyType }}</span>
            <span>{{ car.energyType }}</span>
            <span>{{ car.seats }} 座</span>
          </div>
          <div class="detail-actions">
            <el-button
              :type="isInCompare(car.id) ? 'success' : 'default'"
              :loading="compareOperating"
              @click="addToCompare(car.id)"
            >
              {{ compareButtonText(car.id) }}
            </el-button>
            <el-button
              :type="favorited ? 'warning' : 'default'"
              :loading="favoriteOperating"
              @click="toggleFavorite"
            >
              {{ favorited ? '已收藏' : '收藏' }}
            </el-button>
            <el-button type="primary" plain :disabled="!compareIds.length" @click="goToCompare">
              查看对比（{{ compareIds.length }}/3）
            </el-button>
          </div>
          <p
            v-if="inlineMessage"
            class="inline-action-message"
            :class="`inline-action-message--${inlineMessageType}`"
          >
            {{ inlineMessage }}
          </p>
        </aside>
      </section>

      <section class="info-grid">
        <article v-for="item in overviewRows" :key="item.label" class="info-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </section>

      <section class="panel section-panel">
        <div class="panel__body">
          <div class="section-title">
            <h2>车辆参数</h2>
          </div>
          <template v-if="param">
            <div class="param-grid">
              <article v-for="item in paramRows" :key="item.label" class="param-item">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>
            <div class="feature-groups">
              <div>
                <h3>安全配置</h3>
                <div class="feature-list">
                  <span v-for="item in safetyFeatures" :key="item.label" :class="{ muted: !item.enabled }">
                    {{ item.label }}
                  </span>
                </div>
              </div>
              <div>
                <h3>智能配置</h3>
                <div class="feature-list">
                  <span v-for="item in intelligenceFeatures" :key="item.label" :class="{ muted: !item.enabled }">
                    {{ item.label }}
                  </span>
                </div>
              </div>
            </div>
          </template>
          <el-empty v-else description="该车型暂无参数信息" />
        </div>
      </section>

      <section class="panel section-panel">
        <div class="panel__body">
          <div class="section-title">
            <h2>特征评分</h2>
          </div>
          <template v-if="score">
            <div class="score-grid">
              <div v-for="row in scoreRows" :key="row.key" class="score-row">
                <span>{{ row.label }}</span>
                <el-progress
                  :percentage="scorePercent(row.value)"
                  :status="scoreStatus(row.value)"
                  :stroke-width="9"
                  :show-text="false"
                />
                <strong>{{ formatScore(row.value) }}</strong>
              </div>
            </div>
          </template>
          <el-empty v-else description="该车型暂无评分数据" />
        </div>
      </section>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchCarDetail } from '@/api/cars'
import { addFavorite, fetchFavoriteStatus, removeFavorite } from '@/api/favorites'
import { addUserCompare, fetchUserCompare } from '@/api/userCompare'
import { carImageSrc, fallbackCarImage } from '@/utils/carImage'
import { saveCompareReturn } from '@/utils/compareSelection'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const detail = ref(null)
const favorited = ref(false)
const favoriteOperating = ref(false)
const compareIds = ref([])
const compareOperating = ref(false)
const inlineMessage = ref('')
const inlineMessageType = ref('info')

const carId = computed(() => route.params.id)
const recordId = computed(() => route.query.recordId)
const car = computed(() => detail.value?.carModel || {})
const param = computed(() => detail.value?.carParam || null)
const score = computed(() => detail.value?.carFeatureScore || null)

const scoreConfig = [
  ['spaceScore', '空间'],
  ['safetyScore', '安全'],
  ['energyScore', '能耗'],
  ['intelligenceScore', '智能'],
  ['comfortScore', '舒适'],
  ['powerScore', '动力'],
  ['reputationScore', '口碑'],
  ['popularityScore', '热度'],
]

const overviewRows = computed(() => [
  { label: '品牌', value: car.value.brand || '暂无' },
  { label: '车系', value: car.value.series || '暂无' },
  { label: '指导价', value: formatWan(car.value.guidePrice) },
  { label: '车型级别', value: car.value.bodyType || '暂无' },
  { label: '动力类型', value: car.value.energyType || '暂无' },
  { label: '座位数', value: car.value.seats ? `${car.value.seats} 座` : '暂无' },
  { label: '上市年份', value: car.value.launchYear || '暂无' },
  { label: '用户评分', value: car.value.userRating ? `${car.value.userRating} / 5` : '暂无' },
  { label: '销量', value: Number(car.value.salesVolume || 0).toLocaleString('zh-CN') },
])

const scoreRows = computed(() =>
  scoreConfig.map(([key, label]) => ({
    key,
    label,
    value: Number(score.value?.[key] || 0),
  })),
)

const paramRows = computed(() => {
  const value = param.value
  if (!value) return []
  return [
    ['尺寸', `${value.lengthMm} / ${value.widthMm} / ${value.heightMm} mm`],
    ['轴距', `${value.wheelbaseMm} mm`],
    ['燃油油耗', value.fuelConsumption ? `${value.fuelConsumption} L/100km` : '不适用'],
    ['电耗', value.electricConsumption ? `${value.electricConsumption} kWh/100km` : '不适用'],
    ['纯电续航', value.electricRangeKm ? `${value.electricRangeKm} km` : '不适用'],
    ['综合续航', value.totalRangeKm ? `${value.totalRangeKm} km` : '不适用'],
    ['百公里加速', value.acceleration100 ? `${value.acceleration100} s` : '暂无'],
    ['气囊数量', `${value.airbagCount || 0} 个`],
    ['屏幕尺寸', value.screenSize ? `${value.screenSize} 英寸` : '暂无'],
    ['辅助驾驶', value.assistDriveLevel || '暂无'],
  ].map(([label, itemValue]) => ({ label, value: itemValue }))
})

const safetyFeatures = computed(() => {
  const value = param.value
  if (!value) return []
  return [
    ['ABS', value.hasAbs],
    ['ESP', value.hasEsp],
    ['主动刹车', value.hasActiveBrake],
    ['车道保持', value.hasLaneKeep],
    ['自适应巡航', value.hasAdaptiveCruise],
    ['并线辅助', value.hasBlindSpot],
  ].map(([label, enabled]) => ({ label, enabled }))
})

const intelligenceFeatures = computed(() => {
  const value = param.value
  if (!value) return []
  return [
    ['倒车影像', value.hasReverseCamera],
    ['360 全景', value.has360Camera],
    ['OTA', value.hasOta],
    ['语音交互', value.hasVoiceControl],
    ['自动泊车', value.hasAutoParking],
  ].map(([label, enabled]) => ({ label, enabled }))
})

onMounted(loadCar)
watch(carId, loadCar)

async function loadCar() {
  if (!carId.value) return
  loading.value = true
  error.value = ''
  setInlineMessage('')
  try {
    const response = await fetchCarDetail(carId.value)
    detail.value = response.data
    loadCompareList()
    loadFavoriteStatus()
  } catch (requestError) {
    detail.value = null
    if (requestError?.response?.status === 404) {
      error.value = '车型不存在或已删除。'
    } else {
      error.value = requestError?.response?.data?.message || requestError?.message || '车型详情加载失败。'
    }
  } finally {
    loading.value = false
  }
}

async function loadCompareList() {
  try {
    const response = await fetchUserCompare()
    compareIds.value = response.data?.carIds || []
  } catch {
    compareIds.value = []
    setInlineMessage('对比列表加载失败，不影响车型详情展示。', 'error')
  }
}

async function loadFavoriteStatus() {
  favorited.value = false
  try {
    const response = await fetchFavoriteStatus([Number(carId.value)])
    favorited.value = Boolean(response.data?.[0]?.favorited)
  } catch {
    setInlineMessage('收藏状态加载失败，不影响车型详情展示。', 'error')
  }
}

async function toggleFavorite() {
  favoriteOperating.value = true
  setInlineMessage('')
  try {
    if (favorited.value) {
      await removeFavorite(carId.value)
      favorited.value = false
    } else {
      await addFavorite(carId.value)
      favorited.value = true
    }
  } catch (requestError) {
    setInlineMessage(requestError?.response?.data?.message || requestError?.message || '收藏操作失败，请稍后重试。', 'error')
  } finally {
    favoriteOperating.value = false
  }
}

async function addToCompare(id) {
  compareOperating.value = true
  setInlineMessage('')
  try {
    const response = await addUserCompare(id)
    compareIds.value = response.data?.carIds || []
    setInlineMessage('该车型已加入对比。')
  } catch (requestError) {
    setInlineMessage(requestError?.response?.data?.message || requestError?.message || '加入对比失败，请稍后重试。', 'error')
  } finally {
    compareOperating.value = false
  }
}

function goToCompare() {
  const returnPath = recordId.value ? `/recommend/result/${recordId.value}` : route.fullPath
  saveCompareReturn(returnPath, 0)
  router.push('/compare')
}

function isInCompare(id) {
  return compareIds.value.includes(Number(id))
}

function compareButtonText(id) {
  if (isInCompare(id)) return '已加入对比'
  if (compareIds.value.length >= 3) return '对比已满'
  return '加入对比'
}

function setInlineMessage(text, type = 'info') {
  inlineMessage.value = text
  inlineMessageType.value = type
}

function scoreStatus(value) {
  const scoreValue = Number(value || 0)
  if (scoreValue >= 85) return 'success'
  if (scoreValue < 60) return 'exception'
  return undefined
}

function scorePercent(value) {
  return Math.max(0, Math.min(100, Number(value || 0)))
}

function formatScore(value) {
  return Number(value || 0).toFixed(1)
}

function formatWan(value) {
  return `${(Number(value || 0) / 10000).toFixed(1).replace(/\.0$/, '')} 万`
}
</script>

<style scoped>
.car-detail-page {
  display: grid;
  gap: 20px;
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.75fr);
  gap: 20px;
  align-items: stretch;
}

.detail-visual {
  overflow: hidden;
  aspect-ratio: 16 / 9;
  border-radius: 24px;
  background: #eef2f7;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.1);
}

.detail-visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-summary {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 28px;
  border: 1px solid rgba(226, 232, 240, 0.96);
  border-radius: 24px;
  background:
    radial-gradient(circle at 100% 0%, rgba(37, 99, 235, 0.12), transparent 32%),
    #fff;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.07);
}

.context-line,
.brand-line {
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
}

.context-line {
  color: var(--color-accent);
  font-weight: 700;
}

.detail-summary h2 {
  margin: 10px 0 14px;
  color: var(--color-primary-dark);
  font-size: clamp(30px, 4vw, 48px);
  line-height: 1.1;
}

.price-line {
  color: var(--color-primary);
  font-size: 24px;
}

.summary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.summary-tags span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f1f5f9;
  color: var(--color-primary-dark);
  font-size: 12px;
  font-weight: 700;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 26px;
}

.inline-action-message {
  margin: 10px 0 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.6;
  text-align: right;
}

.inline-action-message--error {
  color: var(--color-danger);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.info-card,
.param-item {
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  background: #fff;
}

.info-card span,
.info-card strong,
.param-item span,
.param-item strong {
  display: block;
}

.info-card span,
.param-item span {
  color: var(--color-muted);
  font-size: 12px;
}

.info-card strong,
.param-item strong {
  margin-top: 7px;
  color: var(--color-primary-dark);
  font-size: 16px;
}

.section-panel {
  margin-top: 0;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-title h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 20px;
}

.param-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.feature-groups {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.feature-groups h3 {
  margin: 0 0 10px;
  color: var(--color-primary-dark);
  font-size: 16px;
}

.feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.feature-list span {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(22, 163, 74, 0.1);
  color: var(--color-success);
  font-size: 12px;
  font-weight: 700;
}

.feature-list span.muted {
  background: #f3f4f6;
  color: var(--color-muted);
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 18px;
}

.score-row {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) 52px;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.score-row strong {
  text-align: right;
}

@media (max-width: 980px) {
  .detail-hero,
  .info-grid,
  .param-grid,
  .feature-groups,
  .score-grid {
    grid-template-columns: 1fr;
  }
}
</style>
