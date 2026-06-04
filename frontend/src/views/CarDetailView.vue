<template>
  <section class="car-detail-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">车型详情</h1>
        <p class="page-subtitle">查看车型基础信息、参数配置和评分表现。</p>
      </div>
      <div class="header-actions">
        <el-button v-if="recordId" @click="$router.push(`/recommend/result/${recordId}`)">返回推荐结果</el-button>
        <el-button v-else @click="$router.push('/cars')">返回车型库</el-button>
      </div>
    </div>

    <div v-if="loading" class="panel">
      <div class="panel__body">
        <el-skeleton :rows="8" animated />
      </div>
    </div>

    <el-alert v-else-if="error" type="error" :closable="false" :title="error" show-icon />

    <template v-else-if="detail">
      <section class="detail-hero">
        <div class="detail-visual">
          <img :src="carImageSrc(car.imageUrl)" :alt="car.modelName" @error="fallbackCarImage" />
        </div>
        <aside class="detail-summary">
          <p v-if="recordId" class="context-line">来自本次推荐</p>
          <p class="brand-line">{{ text(car.brand) }} / {{ text(car.series) }}</p>
          <h2>{{ text(car.modelName) }}</h2>
          <strong class="price-line">{{ formatWan(car.guidePrice) }}</strong>
          <div class="summary-tags">
            <span>{{ text(car.bodyType) }}</span>
            <span>{{ text(car.energyType) }}</span>
            <span>{{ car.seats ? `${car.seats}座` : '暂无座位' }}</span>
          </div>
          <div class="detail-actions">
            <el-button :loading="compareOperating" @click="addToCompare(car.id)">
              {{ compareButtonText(car.id) }}
            </el-button>
            <el-button :type="favorited ? 'warning' : 'default'" :loading="favoriteOperating" @click="toggleFavorite">
              {{ favorited ? '已收藏' : '收藏' }}
            </el-button>
            <el-button type="primary" plain :disabled="!compareIds.length" @click="goToCompare">
              查看对比（{{ compareIds.length }}/3）
            </el-button>
          </div>
          <p v-if="inlineMessage" class="inline-action-message" :class="`inline-action-message--${inlineMessageType}`">
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
            <h2>参数配置</h2>
          </div>
          <template v-if="param">
            <el-tabs v-model="activeParamTab">
              <el-tab-pane label="动力" name="power">
                <div class="param-grid">
                  <article v-for="item in powerRows" :key="item.label" class="param-item">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </article>
                </div>
              </el-tab-pane>
              <el-tab-pane label="空间" name="space">
                <div class="param-grid">
                  <article v-for="item in spaceRows" :key="item.label" class="param-item">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </article>
                </div>
              </el-tab-pane>
              <el-tab-pane label="安全配置" name="safety">
                <div class="feature-list">
                  <span v-for="item in safetyFeatures" :key="item.label" :class="{ muted: !item.enabled }">
                    {{ item.label }}
                  </span>
                </div>
              </el-tab-pane>
            </el-tabs>
          </template>
          <el-empty v-else description="该车型暂无参数配置信息" />
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
import { useAuthStore } from '@/stores/auth'
import { carImageSrc, fallbackCarImage } from '@/utils/carImage'
import { saveCompareReturn } from '@/utils/compareSelection'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const error = ref('')
const detail = ref(null)
const favorited = ref(false)
const favoriteOperating = ref(false)
const compareIds = ref([])
const compareOperating = ref(false)
const inlineMessage = ref('')
const inlineMessageType = ref('info')
const activeParamTab = ref('power')

const carId = computed(() => route.params.id)
const recordId = computed(() => route.query.recordId)
const car = computed(() => detail.value?.carModel || {})
const param = computed(() => detail.value?.carParam || null)
const overviewRows = computed(() => [
  { label: '品牌', value: text(car.value.brand) },
  { label: '车系', value: text(car.value.series) },
  { label: '指导价', value: formatWan(car.value.guidePrice) },
  { label: '车型级别', value: text(car.value.bodyType) },
  { label: '动力类型', value: text(car.value.energyType) },
  { label: '座位数', value: car.value.seats ? `${car.value.seats}座` : '暂无' },
  { label: '上市年份', value: car.value.launchYear || '暂无' },
  { label: '用户评分', value: car.value.userRating ? `${car.value.userRating} / 5` : '暂无' },
])

const powerRows = computed(() => {
  const value = param.value
  if (!value) return []
  return [
    ['动力形式', car.value.energyType || '暂无'],
    ['百公里加速', value.acceleration100 ? `${value.acceleration100}s` : '暂无'],
    ['燃油油耗', value.fuelConsumption ? `${value.fuelConsumption}L/100km` : '不适用'],
    ['电耗', value.electricConsumption ? `${value.electricConsumption}kWh/100km` : '不适用'],
    ['纯电续航', value.electricRangeKm ? `${value.electricRangeKm}km` : '不适用'],
    ['综合续航', value.totalRangeKm ? `${value.totalRangeKm}km` : '暂无'],
  ].map(([label, rowValue]) => ({ label, value: rowValue }))
})

const spaceRows = computed(() => {
  const value = param.value
  if (!value) return []
  return [
    ['长宽高', `${value.lengthMm || '-'} / ${value.widthMm || '-'} / ${value.heightMm || '-'}mm`],
    ['轴距', value.wheelbaseMm ? `${value.wheelbaseMm}mm` : '暂无'],
    ['座位布局', car.value.seats ? `${car.value.seats}座` : '暂无'],
    ['后备厢容积', '暂无'],
  ].map(([label, rowValue]) => ({ label, value: rowValue }))
})

const safetyFeatures = computed(() => {
  const value = param.value
  if (!value) return []
  return [
    ['安全气囊', `${value.airbagCount || 0}个`],
    ['ABS', value.hasAbs],
    ['ESP', value.hasEsp],
    ['主动刹车', value.hasActiveBrake],
    ['车道保持', value.hasLaneKeep],
    ['自适应巡航', value.hasAdaptiveCruise],
    ['盲区监测', value.hasBlindSpot],
    ['辅助驾驶', value.assistDriveLevel || '暂无'],
  ].map(([label, enabled]) => ({ label, enabled: enabled === true || typeof enabled === 'string' ? enabled : false }))
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
    if (authStore.isAuthenticated && authStore.principalType === 'USER') {
      await Promise.all([loadCompareList(), loadFavoriteStatus()])
    }
  } catch (requestError) {
    detail.value = null
    error.value = requestError?.response?.status === 404
      ? '车型不存在或已删除。'
      : requestError?.response?.data?.message || '车型详情加载失败。'
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
  }
}

async function loadFavoriteStatus() {
  favorited.value = false
  try {
    const response = await fetchFavoriteStatus([Number(carId.value)])
    favorited.value = Boolean(response.data?.[0]?.favorited)
  } catch {
    favorited.value = false
  }
}

async function toggleFavorite() {
  if (!ensureUserAuth()) return
  favoriteOperating.value = true
  setInlineMessage('')
  try {
    if (favorited.value) {
      await removeFavorite(carId.value)
      favorited.value = false
      setInlineMessage('已取消收藏。')
    } else {
      await addFavorite(carId.value)
      favorited.value = true
      setInlineMessage('已收藏该车型。')
    }
  } catch (requestError) {
    setInlineMessage(requestError?.response?.data?.message || '收藏操作失败，请稍后重试。', 'error')
  } finally {
    favoriteOperating.value = false
  }
}

async function addToCompare(id) {
  if (!ensureUserAuth()) return
  compareOperating.value = true
  setInlineMessage('')
  try {
    const response = await addUserCompare(id)
    compareIds.value = response.data?.carIds || []
    setInlineMessage('已加入对比，可主动进入车型对比查看。')
  } catch (requestError) {
    setInlineMessage(requestError?.response?.data?.message || '加入对比失败，请稍后重试。', 'error')
  } finally {
    compareOperating.value = false
  }
}

function ensureUserAuth() {
  if (authStore.isAuthenticated && authStore.principalType === 'USER') {
    return true
  }
  setInlineMessage('请先登录后继续操作。')
  router.push({
    path: route.path,
    query: { ...route.query, auth: 'login', redirect: route.fullPath },
    hash: route.hash,
  })
  return false
}

function goToCompare() {
  const returnPath = recordId.value ? `/recommend/result/${recordId.value}` : route.fullPath
  saveCompareReturn(returnPath, 0)
  router.push('/compare')
}

function compareButtonText(id) {
  if (compareIds.value.includes(Number(id))) return '已加入对比'
  if (compareIds.value.length >= 3) return '对比已满'
  return '加入对比'
}

function setInlineMessage(message, type = 'info') {
  inlineMessage.value = message
  inlineMessageType.value = type
}

function text(value) {
  return value || '暂无'
}

function formatWan(value) {
  const number = Number(value || 0)
  if (!number) return '暂无报价'
  return `${(number / 10000).toFixed(1).replace(/\.0$/, '')}万`
}

</script>

<style scoped>
.car-detail-page {
  display: grid;
  gap: 20px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.8fr);
  gap: 20px;
}

.detail-visual {
  overflow: hidden;
  aspect-ratio: 16 / 9;
  border-radius: 20px;
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
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.07);
}

.context-line,
.brand-line {
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
}

.context-line {
  color: var(--color-primary);
  font-weight: 700;
}

.detail-summary h2 {
  margin: 10px 0 14px;
  color: var(--color-primary-dark);
  font-size: 36px;
  line-height: 1.15;
}

.price-line {
  color: var(--color-primary);
  font-size: 24px;
}

.summary-tags,
.feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-tags {
  margin-top: 18px;
}

.summary-tags span,
.feature-list span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f1f5f9;
  color: var(--color-primary-dark);
  font-size: 12px;
  font-weight: 700;
}

.feature-list span {
  background: rgba(22, 163, 74, 0.1);
  color: var(--color-success);
}

.feature-list span.muted {
  background: #f3f4f6;
  color: var(--color-muted);
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

.info-grid,
.param-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.info-card,
.param-item {
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 14px;
  background: #fff;
}

.info-card span,
.param-item span {
  display: block;
  color: var(--color-muted);
  font-size: 12px;
}

.info-card strong,
.param-item strong {
  display: block;
  margin-top: 7px;
  color: var(--color-primary-dark);
  font-size: 16px;
}

.section-panel {
  margin-top: 0;
}

.section-title {
  margin-bottom: 18px;
}

.section-title h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 20px;
}

@media (max-width: 980px) {
  .detail-hero,
  .info-grid,
  .param-grid {
    grid-template-columns: 1fr;
  }

  .detail-summary h2 {
    font-size: 28px;
  }
}
</style>
