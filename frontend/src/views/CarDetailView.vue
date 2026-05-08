<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">车型详情</h1>
        <p class="page-subtitle">用户端车型详情读取 `GET /api/car/{id}`，展示基础信息、参数和已保存的特征评分。</p>
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
      <div class="detail-hero">
        <div class="detail-visual">
          <img :src="carImageSrc(car.imageUrl)" :alt="car.modelName" @error="fallbackCarImage" />
        </div>
        <div class="panel detail-summary">
          <div class="panel__body">
            <p v-if="recordId" class="context-line">来自推荐记录 #{{ recordId }}</p>
            <h2>{{ car.brand }} {{ car.modelName }}</h2>
            <p>{{ car.series }} · {{ car.bodyType }} · {{ car.energyType }} · {{ car.seats }} 座</p>
            <div class="hero-metrics">
              <div>
                <span>指导价</span>
                <strong>{{ formatWan(car.guidePrice) }}</strong>
              </div>
              <div>
                <span>上市年份</span>
                <strong>{{ car.launchYear || '未知' }}</strong>
              </div>
              <div>
                <span>口碑评分</span>
                <strong>{{ car.userRating || '暂无' }}</strong>
              </div>
            </div>
            <div class="detail-actions">
              <el-button
                :type="isInCompare(car.id) ? 'success' : 'default'"
                :loading="compareOperating"
                @click="addToCompare(car.id)"
              >
                {{ compareButtonText(car.id) }}
              </el-button>
              <el-button type="primary" plain :disabled="!compareIds.length" @click="goToCompare">
                查看对比（{{ compareIds.length }}/3）
              </el-button>
              <el-button
                :type="favorited ? 'warning' : 'default'"
                :loading="favoriteOperating"
                @click="toggleFavorite"
              >
                {{ favorited ? '已收藏' : '收藏' }}
              </el-button>
              <el-button v-if="!recordId" type="primary" plain @click="$router.push('/recommend')">开始推荐</el-button>
            </div>
            <p
              v-if="inlineMessage"
              class="inline-action-message"
              :class="`inline-action-message--${inlineMessageType}`"
            >
              {{ inlineMessage }}
            </p>
          </div>
        </div>
      </div>

      <div class="detail-grid">
        <div class="panel">
          <div class="panel__body">
            <h3 class="section-title">车型基础信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="品牌">{{ car.brand }}</el-descriptions-item>
              <el-descriptions-item label="车系">{{ car.series }}</el-descriptions-item>
              <el-descriptions-item label="车型">{{ car.modelName }}</el-descriptions-item>
              <el-descriptions-item label="指导价">{{ formatYuan(car.guidePrice) }}</el-descriptions-item>
              <el-descriptions-item label="车型类型">{{ car.bodyType }}</el-descriptions-item>
              <el-descriptions-item label="动力类型">{{ car.energyType }}</el-descriptions-item>
              <el-descriptions-item label="座位数">{{ car.seats }}</el-descriptions-item>
              <el-descriptions-item label="销量">{{ car.salesVolume || 0 }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>

        <div class="panel">
          <div class="panel__body">
            <h3 class="section-title">特征评分</h3>
            <template v-if="score">
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
              <p class="score-version">评分版本：{{ score.scoreVersion }} · {{ formatDate(score.calculatedTime) }}</p>
            </template>
            <el-empty v-else description="该车型暂无特征评分" />
          </div>
        </div>
      </div>

      <div class="panel param-panel">
        <div class="panel__body">
          <h3 class="section-title">车型参数</h3>
          <template v-if="param">
            <div class="param-grid">
              <div v-for="item in paramRows" :key="item.label" class="param-item">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
            <div class="feature-list">
              <span v-for="item in featureRows" :key="item.label" :class="{ muted: !item.enabled }">
                {{ item.label }} {{ item.enabled ? '已配备' : '未配置' }}
              </span>
            </div>
          </template>
          <el-empty v-else description="该车型暂无参数信息" />
        </div>
      </div>

      <div class="panel source-panel">
        <div class="panel__body">
          <h3 class="section-title">评分来源说明</h3>
          <div class="source-grid">
            <p>空间分由轴距、车长、座位数和车型类型计算得到。</p>
            <p>安全分由气囊、ABS、ESP 和主动安全配置计算得到。</p>
            <p>能耗分由油耗、电耗、纯电续航和综合续航计算得到。</p>
            <p>智能分由辅助驾驶、摄像头、OTA、语音和自动泊车配置计算得到。</p>
          </div>
        </div>
      </div>
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
    ['车身尺寸', `${value.lengthMm} / ${value.widthMm} / ${value.heightMm} mm`],
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

const featureRows = computed(() => {
  const value = param.value
  if (!value) return []
  return [
    ['ABS', value.hasAbs],
    ['ESP', value.hasEsp],
    ['主动刹车', value.hasActiveBrake],
    ['车道保持', value.hasLaneKeep],
    ['自适应巡航', value.hasAdaptiveCruise],
    ['并线辅助', value.hasBlindSpot],
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
  return Number(value || 0).toFixed(2)
}

function formatWan(value) {
  return `${(Number(value || 0) / 10000).toFixed(1).replace(/\.0$/, '')} 万`
}

function formatYuan(value) {
  return `${Number(value || 0).toLocaleString('zh-CN')} 元`
}

function formatDate(value) {
  if (!value) return '时间未知'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.detail-hero,
.detail-grid {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
  margin-bottom: 18px;
}

.detail-visual {
  min-height: 260px;
  overflow: hidden;
  border-radius: var(--radius-md);
  background: #eef2f7;
  box-shadow: var(--shadow-card);
}

.detail-visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-summary h2 {
  margin: 6px 0 8px;
  color: var(--color-primary-dark);
  font-size: 28px;
}

.detail-summary p {
  margin: 0;
  color: var(--color-muted);
}

.context-line {
  color: var(--color-accent) !important;
  font-size: 13px;
  font-weight: 700;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 28px;
}

.hero-metrics div {
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f9fafb;
}

.hero-metrics span,
.hero-metrics strong {
  display: block;
}

.hero-metrics span {
  color: var(--color-muted);
  font-size: 12px;
}

.hero-metrics strong {
  margin-top: 8px;
  color: var(--color-primary-dark);
  font-size: 20px;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 18px;
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

.section-title {
  margin: 0 0 16px;
  color: var(--color-primary-dark);
  font-size: 18px;
}

.score-row {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) 58px;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  font-size: 13px;
}

.score-row strong {
  text-align: right;
}

.score-version {
  margin: 16px 0 0;
  color: var(--color-muted);
  font-size: 12px;
}

.param-panel,
.source-panel {
  margin-top: 18px;
}

.param-grid,
.source-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.param-item {
  min-height: 78px;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f9fafb;
}

.param-item span,
.param-item strong {
  display: block;
}

.param-item span {
  color: var(--color-muted);
  font-size: 12px;
}

.param-item strong {
  margin-top: 8px;
  color: var(--color-primary-dark);
  font-size: 15px;
}

.feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.feature-list span {
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  background: rgba(22, 163, 74, 0.1);
  color: var(--color-success);
  font-size: 12px;
}

.feature-list span.muted {
  background: #f3f4f6;
  color: var(--color-muted);
}

.source-grid p {
  margin: 0;
  padding: 14px;
  color: var(--color-muted);
  line-height: 1.7;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f9fafb;
}

@media (max-width: 980px) {
  .detail-hero,
  .detail-grid,
  .param-grid,
  .source-grid {
    grid-template-columns: 1fr;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
