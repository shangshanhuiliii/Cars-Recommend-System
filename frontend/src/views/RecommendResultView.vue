<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">推荐结果</h1>
        <p class="page-subtitle">推荐详情来自已保存的历史快照，页面不会重新计算标签、分数、理由、不足或匹配状态。</p>
      </div>
      <el-button @click="$router.push('/recommend')">重新填写需求</el-button>
    </div>

    <div v-if="loading" class="panel">
      <div class="panel__body">
        <el-skeleton :rows="8" animated />
      </div>
    </div>

    <el-alert
      v-else-if="error"
      class="state-alert"
      type="error"
      :closable="false"
      :title="error"
      show-icon
    >
      <template #default>
        <el-button class="state-action" size="small" @click="$router.push('/history')">返回推荐历史</el-button>
      </template>
    </el-alert>

    <template v-else-if="detail">
      <div class="result-summary">
        <div class="panel profile-panel">
          <div class="panel__body">
            <div class="summary-head">
              <div>
                <p class="eyebrow">用户画像</p>
                <h2>{{ detail.profileText || '暂无画像文本' }}</h2>
              </div>
              <el-tag :type="statusType(detail.recommendStatus)" size="large">
                {{ statusLabel(detail.recommendStatus) }}
              </el-tag>
            </div>
            <div class="record-meta">
              <span>记录 #{{ detail.recordId }}</span>
              <span>需求 #{{ detail.demandId }}</span>
              <span>{{ formatDate(detail.createTime) }}</span>
            </div>
          </div>
        </div>

        <div class="panel weight-panel">
          <div class="panel__body">
            <p class="eyebrow">权重摘要</p>
            <div v-for="row in weightRows" :key="row.key" class="weight-row">
              <span>{{ row.label }}</span>
              <el-progress :percentage="toPercent(row.value)" :stroke-width="8" :show-text="false" />
              <strong>{{ formatWeight(row.value) }}</strong>
            </div>
          </div>
        </div>
      </div>

      <div v-if="detail.recommendStatus !== 'EMPTY' && detail.items?.length" class="panel ranking-note">
        <div class="panel__body">
          <p>完全匹配表示满足预算、车型、动力、座位等硬性条件。</p>
          <p>综合推荐分表示基于价格、空间、安全、能耗、智能、舒适、动力、口碑、热度计算的多维推荐分。</p>
          <p>不同分组之间优先看条件匹配状态，同组内部按综合推荐分排序。</p>
        </div>
      </div>

      <div v-if="detail.recommendStatus !== 'EMPTY' && detail.items?.length" class="panel compare-entry-panel">
        <div class="panel__body compare-entry">
          <div>
            <p class="eyebrow">车型对比</p>
            <h2>已选择 {{ compareIds.length }}/3 款</h2>
            <p class="inline-state" :class="{ 'inline-state--error': compareError }">
              {{ compareEntryText }}
            </p>
          </div>
          <el-button type="primary" plain :disabled="!compareIds.length" @click="goToCompare">
            查看对比（{{ compareIds.length }}/3）
          </el-button>
        </div>
      </div>

      <el-alert
        v-if="favoriteLoadError"
        class="state-alert"
        type="warning"
        :closable="false"
        :title="favoriteLoadError"
        show-icon
      />

      <div v-if="detail.recommendStatus === 'EMPTY' || !detail.items?.length" class="panel empty-panel">
        <div class="panel__body">
          <el-empty description="暂未找到合适车型">
            <el-button type="primary" @click="$router.push('/recommend')">调整需求重新推荐</el-button>
          </el-empty>
        </div>
      </div>

      <div v-else class="result-list">
        <section v-for="group in itemGroups" :key="group.key" class="result-group">
          <div class="group-head">
            <h2>{{ group.title }}</h2>
            <span>{{ group.items.length }} 款车型</span>
          </div>

          <article v-for="item in group.items" :key="`${detail.recordId}-${item.rankNo}`" class="recommend-card">
            <div class="car-visual">
              <img :src="carImageSrc(item.imageUrl)" :alt="item.modelName" @error="fallbackCarImage" />
            </div>

            <div class="recommend-main">
              <div class="car-title-row">
                <div>
                  <span class="rank-badge">#{{ item.rankNo }}</span>
                  <h2>{{ item.brand }} {{ item.modelName }}</h2>
                  <p>{{ item.series }} · {{ item.bodyType }} · {{ item.energyType }} · {{ item.seats }} 座</p>
                </div>
                <div class="score-box" :class="scoreClass(item.totalScore)">
                  <strong>{{ formatScore(item.totalScore) }}</strong>
                  <span>综合推荐分</span>
                </div>
              </div>

              <div class="recommend-meta">
                <div class="tag-line">
                  <span class="meta-label">推荐标签</span>
                  <el-tag v-for="tag in displayTags(item.tags)" :key="tag" effect="light">{{ tag }}</el-tag>
                </div>
                <span class="price-text">{{ formatWan(item.guidePrice) }}</span>
              </div>

              <div class="explain-grid">
                <div>
                  <p class="explain-label">推荐理由</p>
                  <p>{{ item.reasonText }}</p>
                </div>
                <div>
                  <p class="explain-label">不足提醒</p>
                  <p>{{ item.weaknessText }}</p>
                </div>
              </div>

              <div class="score-grid">
                <div v-for="row in scoreRows(item)" :key="row.key" class="score-row">
                  <span>{{ row.label }}</span>
                  <el-progress
                    :percentage="scorePercent(row.value)"
                    :status="scoreStatus(row.value)"
                    :stroke-width="8"
                    :show-text="false"
                  />
                  <strong>{{ formatScore(row.value) }}</strong>
                </div>
              </div>

              <div class="card-actions">
                <el-button type="primary" plain @click="openCarDetail(item.carId)">查看车型详情</el-button>
                <el-button
                  :type="isInCompare(item.carId) ? 'success' : 'default'"
                  :loading="compareOperatingId === item.carId"
                  @click="addToCompare(item.carId)"
                >
                  {{ compareButtonText(item.carId) }}
                </el-button>
                <el-button
                  :type="isFavorited(item.carId) ? 'warning' : 'default'"
                  :loading="favoriteOperatingId === item.carId"
                  @click="toggleFavorite(item.carId)"
                >
                  {{ isFavorited(item.carId) ? '已收藏' : '收藏' }}
                </el-button>
              </div>
              <p
                v-if="actionMessage(item.carId)"
                class="inline-action-message"
                :class="`inline-action-message--${actionMessage(item.carId).type}`"
              >
                {{ actionMessage(item.carId).text }}
              </p>
            </div>
          </article>
        </section>
      </div>

      <div v-if="detail.recommendStatus !== 'EMPTY'" class="panel feedback-panel">
        <div class="panel__body">
          <div class="feedback-head">
            <div>
              <h2>推荐反馈</h2>
              <p>反馈只进入统计分析，当前版本不会自动调整推荐权重或排序。</p>
            </div>
            <el-tag v-if="feedbackSaved" type="success" effect="light">反馈已记录</el-tag>
          </div>

          <el-skeleton v-if="feedbackLoading" :rows="4" animated />
          <el-alert
            v-else-if="feedbackError"
            type="warning"
            :closable="false"
            :title="feedbackError"
            show-icon
          />
          <div v-else class="feedback-form">
            <div class="feedback-score">
              <span>满意度</span>
              <el-rate v-model="feedbackForm.satisfactionScore" :max="5" />
            </div>
            <el-checkbox-group v-model="feedbackForm.reasonTags" class="reason-tags">
              <el-checkbox-button v-for="tag in feedbackReasonOptions" :key="tag" :label="tag" />
            </el-checkbox-group>
            <el-input
              v-model="feedbackForm.comment"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="可以补充推荐是否符合预算、车型、空间、动力或解释是否清楚"
            />
            <div class="feedback-actions">
              <el-button
                type="primary"
                :loading="feedbackSubmitting"
                :disabled="!feedbackForm.satisfactionScore"
                @click="submitFeedback"
              >
                提交反馈
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <el-drawer v-model="carDrawerVisible" size="560px" destroy-on-close title="车型详情">
      <div v-if="carDetailLoading" class="drawer-state">
        <el-skeleton :rows="8" animated />
      </div>
      <el-alert
        v-else-if="carDetailError"
        type="error"
        :closable="false"
        :title="carDetailError"
        show-icon
      />
      <div v-else-if="selectedCarDetail" class="drawer-detail">
        <div class="drawer-visual">
          <img :src="carImageSrc(selectedCar.imageUrl)" :alt="selectedCar.modelName" @error="fallbackCarImage" />
        </div>
        <h2>{{ selectedCar.brand }} {{ selectedCar.modelName }}</h2>
        <p class="drawer-subtitle">
          {{ selectedCar.series }} · {{ selectedCar.bodyType }} · {{ selectedCar.energyType }} · {{ selectedCar.seats }} 座
        </p>
        <div class="drawer-metrics">
          <div>
            <span>指导价</span>
            <strong>{{ formatWan(selectedCar.guidePrice) }}</strong>
          </div>
          <div>
            <span>上市年份</span>
            <strong>{{ selectedCar.launchYear || '未知' }}</strong>
          </div>
          <div>
            <span>口碑评分</span>
            <strong>{{ selectedCar.userRating || '暂无' }}</strong>
          </div>
        </div>

        <div class="drawer-actions">
          <el-button
            :type="isInCompare(selectedCar.id) ? 'success' : 'default'"
            :loading="compareOperatingId === selectedCar.id"
            @click="addToCompare(selectedCar.id)"
          >
            {{ compareButtonText(selectedCar.id) }}
          </el-button>
          <el-button
            :type="isFavorited(selectedCar.id) ? 'warning' : 'default'"
            :loading="favoriteOperatingId === selectedCar.id"
            @click="toggleFavorite(selectedCar.id)"
          >
            {{ isFavorited(selectedCar.id) ? '已收藏' : '收藏' }}
          </el-button>
          <el-button type="primary" plain @click="$router.push(`/car/${selectedCar.id}?recordId=${detail.recordId}`)">
            打开详情页
          </el-button>
        </div>
        <p
          v-if="actionMessage(selectedCar.id)"
          class="inline-action-message drawer-action-message"
          :class="`inline-action-message--${actionMessage(selectedCar.id).type}`"
        >
          {{ actionMessage(selectedCar.id).text }}
        </p>

        <h3>特征评分</h3>
        <template v-if="selectedScore">
          <div v-for="row in detailScoreRows" :key="row.key" class="score-row drawer-score-row">
            <span>{{ row.label }}</span>
            <el-progress
              :percentage="scorePercent(row.value)"
              :status="scoreStatus(row.value)"
              :stroke-width="8"
              :show-text="false"
            />
            <strong>{{ formatScore(row.value) }}</strong>
          </div>
        </template>
        <el-empty v-else description="该车型暂无特征评分" />

        <h3>车型参数</h3>
        <template v-if="selectedParam">
          <div class="drawer-param-grid">
            <div v-for="row in detailParamRows" :key="row.label">
              <span>{{ row.label }}</span>
              <strong>{{ row.value }}</strong>
            </div>
          </div>
        </template>
        <el-empty v-else description="该车型暂无参数信息" />
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchCarDetail } from '@/api/cars'
import { addFavorite, fetchFavoriteStatus, removeFavorite } from '@/api/favorites'
import { fetchRecommendationDetail, fetchRecommendationFeedback, submitRecommendationFeedback } from '@/api/recommend'
import { addUserCompare, fetchUserCompare } from '@/api/userCompare'
import { carImageSrc, fallbackCarImage } from '@/utils/carImage'
import {
  clearCompareReturn,
  readCompareScrollFor,
  saveCompareReturn,
} from '@/utils/compareSelection'
import { displayTags, rankOrderedItems } from '@/utils/recommendPresentation'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const detail = ref(null)
const carDrawerVisible = ref(false)
const carDetailLoading = ref(false)
const carDetailError = ref('')
const selectedCarDetail = ref(null)
const favoriteStatus = ref({})
const favoriteOperatingId = ref(null)
const compareOperatingId = ref(null)
const favoriteLoadError = ref('')
const feedbackLoading = ref(false)
const feedbackSubmitting = ref(false)
const feedbackError = ref('')
const feedbackSaved = ref(false)
const feedbackForm = ref({
  satisfactionScore: 0,
  reasonTags: [],
  comment: '',
})
const compareIds = ref([])
const compareNotice = ref('')
const compareError = ref('')
const actionMessages = ref({})

const recordId = computed(() => route.params.recordId)
const selectedCar = computed(() => selectedCarDetail.value?.carModel || {})
const selectedParam = computed(() => selectedCarDetail.value?.carParam || null)
const selectedScore = computed(() => selectedCarDetail.value?.carFeatureScore || null)

const weightConfig = [
  ['price', '价格'],
  ['space', '空间'],
  ['safety', '安全'],
  ['energy', '能耗'],
  ['intelligence', '智能'],
  ['comfort', '舒适'],
  ['power', '动力'],
  ['reputation', '口碑'],
  ['popularity', '热度'],
]

const scoreConfig = [
  ['priceScore', '价格'],
  ['spaceScore', '空间'],
  ['safetyScore', '安全'],
  ['energyScore', '能耗'],
  ['intelligenceScore', '智能'],
  ['comfortScore', '舒适'],
  ['powerScore', '动力'],
  ['reputationScore', '口碑'],
  ['popularityScore', '热度'],
]

const featureScoreConfig = [
  ['spaceScore', '空间'],
  ['safetyScore', '安全'],
  ['energyScore', '能耗'],
  ['intelligenceScore', '智能'],
  ['comfortScore', '舒适'],
  ['powerScore', '动力'],
  ['reputationScore', '口碑'],
  ['popularityScore', '热度'],
]

const feedbackReasonOptions = [
  '推荐有帮助',
  '解释清楚',
  '推荐太贵',
  '车型不合适',
  '动力不符合',
  '空间不足',
  '解释不清楚',
]

const weightRows = computed(() =>
  weightConfig.map(([key, label]) => ({
    key,
    label,
    value: Number(detail.value?.weights?.[key] || 0),
  })),
)

const strictItems = computed(() => rankOrderedItems((detail.value?.items || []).filter((item) => item.matchLevel === 'STRICT')))
const recommendationItems = computed(() =>
  rankOrderedItems((detail.value?.items || []).filter((item) => item.matchLevel !== 'STRICT')),
)

const itemGroups = computed(() => {
  const groups = []
  if (strictItems.value.length) {
    groups.push({ key: 'strict', title: '完全匹配车型', items: strictItems.value })
  }
  if (recommendationItems.value.length) {
    groups.push({ key: 'recommendation', title: '推荐', items: recommendationItems.value })
  }
  return groups
})

const compareEntryText = computed(() => {
  if (compareError.value) return compareError.value
  if (compareNotice.value) return compareNotice.value
  if (compareIds.value.length === 0) return '在推荐卡片中加入车型后，可主动进入对比页查看。'
  if (compareIds.value.length === 1) return '已加入 1 款车型，可继续从推荐结果中加入更多车型。'
  return '可进入对比页查看基础信息、参数和八维评分差异。'
})

const detailScoreRows = computed(() =>
  featureScoreConfig.map(([key, label]) => ({
    key,
    label,
    value: Number(selectedScore.value?.[key] || 0),
  })),
)

const detailParamRows = computed(() => {
  const value = selectedParam.value
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
  ].map(([label, rowValue]) => ({ label, value: rowValue }))
})

onMounted(loadDetail)
watch(recordId, loadDetail)

async function loadDetail() {
  if (!recordId.value) return
  loading.value = true
  error.value = ''
  try {
    const response = await fetchRecommendationDetail(recordId.value)
    detail.value = response.data
    loadCompareList()
    loadFavoriteStatuses()
    loadFeedback()
  } catch (requestError) {
    detail.value = null
    if (requestError?.response?.status === 404) {
      error.value = '推荐记录不存在，或当前用户无权查看该记录。'
    } else {
      error.value = requestError?.response?.data?.message || requestError?.message || '推荐结果加载失败。'
    }
  } finally {
    loading.value = false
    await restoreRecommendScroll()
  }
}

async function loadCompareList() {
  compareError.value = ''
  try {
    const response = await fetchUserCompare()
    compareIds.value = response.data?.carIds || []
  } catch (requestError) {
    compareIds.value = []
    compareError.value = requestError?.response?.data?.message || requestError?.message || '对比列表加载失败。'
  }
}

async function loadFavoriteStatuses() {
  favoriteLoadError.value = ''
  const ids = (detail.value?.items || []).map((item) => item.carId)
  if (!ids.length) {
    favoriteStatus.value = {}
    return
  }
  try {
    const response = await fetchFavoriteStatus(ids)
    favoriteStatus.value = Object.fromEntries((response.data || []).map((item) => [item.carId, item.favorited]))
  } catch {
    favoriteStatus.value = {}
    favoriteLoadError.value = '收藏状态加载失败，不影响推荐结果展示。'
  }
}

async function loadFeedback() {
  if (!recordId.value) return
  feedbackLoading.value = true
  feedbackError.value = ''
  feedbackSaved.value = false
  try {
    const response = await fetchRecommendationFeedback(recordId.value)
    if (response.data) {
      feedbackForm.value = {
        satisfactionScore: Number(response.data.satisfactionScore || 0),
        reasonTags: response.data.reasonTags || [],
        comment: response.data.comment || '',
      }
      feedbackSaved.value = true
    } else {
      feedbackForm.value = {
        satisfactionScore: 0,
        reasonTags: [],
        comment: '',
      }
    }
  } catch (requestError) {
    feedbackError.value = requestError?.response?.data?.message || requestError?.message || '反馈状态加载失败。'
  } finally {
    feedbackLoading.value = false
  }
}

async function openCarDetail(carId) {
  carDrawerVisible.value = true
  carDetailLoading.value = true
  carDetailError.value = ''
  selectedCarDetail.value = null
  try {
    const response = await fetchCarDetail(carId)
    selectedCarDetail.value = response.data
  } catch (requestError) {
    carDetailError.value = requestError?.response?.data?.message || requestError?.message || '车型详情加载失败。'
  } finally {
    carDetailLoading.value = false
  }
}

async function toggleFavorite(carId) {
  favoriteOperatingId.value = carId
  clearActionMessage(carId)
  try {
    if (isFavorited(carId)) {
      await removeFavorite(carId)
      favoriteStatus.value = { ...favoriteStatus.value, [carId]: false }
    } else {
      await addFavorite(carId)
      favoriteStatus.value = { ...favoriteStatus.value, [carId]: true }
    }
  } catch (requestError) {
    setActionMessage(carId, requestError?.response?.data?.message || requestError?.message || '收藏操作失败，请稍后重试。', 'error')
  } finally {
    favoriteOperatingId.value = null
  }
}

function isFavorited(carId) {
  return Boolean(favoriteStatus.value?.[carId])
}

async function addToCompare(carId) {
  compareOperatingId.value = carId
  clearActionMessage(carId)
  try {
    const response = await addUserCompare(carId)
    compareIds.value = response.data?.carIds || []
    compareError.value = ''
    compareNotice.value = isInCompare(carId) ? '该车型已加入对比。' : '已更新对比列表。'
  } catch (requestError) {
    const message = requestError?.response?.data?.message || requestError?.message || '加入对比失败，请稍后重试。'
    compareError.value = message
    compareNotice.value = ''
    setActionMessage(carId, message, 'error')
  } finally {
    compareOperatingId.value = null
  }
}

function goToCompare() {
  saveCompareReturn(route.fullPath, window.scrollY)
  router.push('/compare')
}

function isInCompare(carId) {
  return compareIds.value.includes(Number(carId))
}

function compareButtonText(carId) {
  if (isInCompare(carId)) return '已加入对比'
  if (compareIds.value.length >= 3) return '对比已满'
  return '加入对比'
}

function setActionMessage(carId, text, type = 'info') {
  actionMessages.value = { ...actionMessages.value, [carId]: { text, type } }
}

function clearActionMessage(carId) {
  if (!actionMessages.value[carId]) return
  const next = { ...actionMessages.value }
  delete next[carId]
  actionMessages.value = next
}

function actionMessage(carId) {
  return actionMessages.value?.[carId] || null
}

async function restoreRecommendScroll() {
  const scrollY = readCompareScrollFor(route.fullPath)
  if (scrollY === null) return
  clearCompareReturn()
  await nextTick()
  window.scrollTo({ top: scrollY, behavior: 'auto' })
}

async function submitFeedback() {
  feedbackSubmitting.value = true
  feedbackError.value = ''
  try {
    const response = await submitRecommendationFeedback(recordId.value, feedbackForm.value)
    feedbackForm.value = {
      satisfactionScore: Number(response.data.satisfactionScore || 0),
      reasonTags: response.data.reasonTags || [],
      comment: response.data.comment || '',
    }
    feedbackSaved.value = true
  } catch (requestError) {
    feedbackError.value = requestError?.response?.data?.message || requestError?.message || '反馈提交失败。'
  } finally {
    feedbackSubmitting.value = false
  }
}

function scoreRows(item) {
  return scoreConfig.map(([key, label]) => ({
    key,
    label,
    value: Number(item?.[key] || 0),
  }))
}

function statusLabel(value) {
  if (value === 'SUCCESS') return '全部完全匹配'
  if (value === 'FALLBACK') return '含补充推荐'
  if (value === 'EMPTY') return '暂无结果'
  return value || '未知'
}

function statusType(value) {
  if (value === 'SUCCESS') return 'success'
  if (value === 'FALLBACK') return 'warning'
  if (value === 'EMPTY') return 'danger'
  return 'info'
}

function scoreStatus(value) {
  const score = Number(value || 0)
  if (score >= 85) return 'success'
  if (score < 60) return 'exception'
  return undefined
}

function scoreClass(value) {
  const score = Number(value || 0)
  if (score >= 85) return 'score-box--strong'
  if (score >= 70) return 'score-box--good'
  if (score >= 60) return 'score-box--warn'
  return 'score-box--weak'
}

function scorePercent(value) {
  return Math.max(0, Math.min(100, Number(value || 0)))
}

function toPercent(value) {
  return Math.max(0, Math.min(100, Number(value || 0) * 100))
}

function formatWeight(value) {
  return `${(Number(value || 0) * 100).toFixed(1)}%`
}

function formatScore(value) {
  return Number(value || 0).toFixed(2)
}

function formatWan(value) {
  return `${(Number(value || 0) / 10000).toFixed(1).replace(/\.0$/, '')} 万`
}

function formatDate(value) {
  if (!value) return '时间未知'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.state-alert {
  margin-bottom: 18px;
}

.state-action {
  margin-top: 10px;
}

.result-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
  margin-bottom: 20px;
}

.profile-panel h2 {
  max-width: 720px;
  margin: 6px 0 0;
  color: var(--color-primary-dark);
  font-size: 22px;
  line-height: 1.45;
}

.summary-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow {
  margin: 0;
  color: var(--color-muted);
  font-size: 12px;
}

.record-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.record-meta span {
  padding: 5px 9px;
  border-radius: var(--radius-sm);
  background: #f3f4f6;
  color: var(--color-muted);
  font-size: 12px;
}

.weight-row,
.score-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 56px;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  font-size: 12px;
}

.weight-row strong,
.score-row strong {
  text-align: right;
}

.empty-panel {
  margin-top: 20px;
}

.ranking-note {
  margin-bottom: 20px;
}

.compare-entry-panel {
  margin-bottom: 20px;
}

.compare-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.compare-entry h2 {
  margin: 4px 0 6px;
  color: var(--color-primary-dark);
  font-size: 18px;
}

.inline-state,
.inline-action-message {
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.6;
}

.inline-state--error,
.inline-action-message--error {
  color: var(--color-danger);
}

.inline-action-message {
  margin-top: 8px;
  text-align: right;
}

.ranking-note .panel__body {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.ranking-note p {
  margin: 0;
  padding: 12px;
  border: 1px solid rgba(8, 145, 178, 0.18);
  border-radius: var(--radius-sm);
  background: rgba(8, 145, 178, 0.06);
  color: var(--color-primary-dark);
  font-size: 13px;
  line-height: 1.7;
}

.result-list,
.result-group {
  display: grid;
  gap: 18px;
}

.result-group + .result-group {
  margin-top: 10px;
}

.group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 4px;
}

.group-head h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 20px;
}

.group-head span {
  color: var(--color-muted);
  font-size: 13px;
}

.recommend-card {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  gap: 20px;
  padding: 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--shadow-card);
}

.car-visual {
  min-height: 180px;
  overflow: hidden;
  border-radius: var(--radius-sm);
  background: #eef2f7;
}

.car-visual img,
.drawer-visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.car-title-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.rank-badge {
  display: inline-block;
  margin-bottom: 8px;
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 700;
}

.car-title-row h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 20px;
}

.car-title-row p {
  margin: 8px 0 0;
  color: var(--color-muted);
  font-size: 13px;
}

.score-box {
  width: 118px;
  min-width: 118px;
  padding: 12px;
  text-align: center;
  border-radius: var(--radius-sm);
  background: #f8fafc;
}

.score-box strong,
.score-box span {
  display: block;
}

.score-box strong {
  font-size: 28px;
  line-height: 1;
}

.score-box span {
  margin-top: 8px;
  color: var(--color-muted);
  font-size: 12px;
}

.score-box--strong strong {
  color: var(--color-success);
}

.score-box--good strong {
  color: var(--color-primary);
}

.score-box--warn strong {
  color: var(--color-warning);
}

.score-box--weak strong {
  color: var(--color-muted);
}

.recommend-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
}

.tag-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.meta-label {
  color: var(--color-muted);
  font-size: 12px;
}

.price-text {
  margin-left: auto;
  color: var(--color-primary-dark);
  font-weight: 700;
}

.explain-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.explain-grid > div {
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f9fafb;
}

.explain-grid p {
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.7;
}

.explain-label {
  margin-bottom: 6px !important;
  color: var(--color-primary-dark) !important;
  font-weight: 700;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 16px;
  margin-top: 12px;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.feedback-panel {
  margin-top: 22px;
}

.feedback-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.feedback-head h2 {
  margin: 0 0 6px;
  color: var(--color-primary-dark);
  font-size: 18px;
}

.feedback-head p {
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
}

.feedback-form {
  display: grid;
  gap: 14px;
}

.feedback-score {
  display: flex;
  align-items: center;
  gap: 14px;
}

.feedback-score span {
  color: var(--color-primary-dark);
  font-weight: 700;
}

.reason-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.feedback-actions {
  display: flex;
  justify-content: flex-end;
}

.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.drawer-action-message {
  text-align: left;
}

.drawer-state {
  padding: 12px 0;
}

.drawer-detail h2 {
  margin: 16px 0 8px;
  color: var(--color-primary-dark);
  font-size: 22px;
}

.drawer-detail h3 {
  margin: 22px 0 12px;
  color: var(--color-primary-dark);
  font-size: 16px;
}

.drawer-subtitle {
  margin: 0;
  color: var(--color-muted);
}

.drawer-visual {
  height: 220px;
  overflow: hidden;
  border-radius: var(--radius-sm);
  background: #eef2f7;
}

.drawer-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.drawer-metrics div,
.drawer-param-grid div {
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f9fafb;
}

.drawer-metrics span,
.drawer-param-grid span {
  display: block;
  color: var(--color-muted);
  font-size: 12px;
}

.drawer-metrics strong,
.drawer-param-grid strong {
  display: block;
  margin-top: 4px;
  color: var(--color-primary-dark);
}

.drawer-param-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.drawer-score-row {
  grid-template-columns: 64px minmax(0, 1fr) 56px;
}

@media (max-width: 980px) {
  .result-summary,
  .compare-entry,
  .recommend-card,
  .explain-grid,
  .score-grid,
  .ranking-note .panel__body,
  .drawer-metrics,
  .drawer-param-grid {
    grid-template-columns: 1fr;
  }

  .price-text {
    margin-left: 0;
  }
}
</style>
