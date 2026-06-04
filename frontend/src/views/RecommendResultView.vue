<template>
  <section class="result-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">推荐结果</h1>
        <p class="page-subtitle">以下车型根据你的购车需求生成，可进入详情查看更多参数。</p>
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
                <p class="eyebrow">需求摘要</p>
                <h2>{{ detail.profileText || '已根据你的选择生成推荐' }}</h2>
              </div>
              <el-tag :type="statusType(detail.recommendStatus)" size="large">
                {{ statusLabel(detail.recommendStatus) }}
              </el-tag>
            </div>
            <div class="record-meta">
              <span>{{ formatDate(detail.createTime) }}</span>
              <span>{{ detail.items?.length || 0 }} 款车型</span>
            </div>
          </div>
        </div>

        <div class="panel focus-panel">
          <div class="panel__body">
            <p class="eyebrow">关注重点</p>
            <div v-for="row in topWeightRows" :key="row.key" class="weight-row">
              <span>{{ row.label }}</span>
              <el-progress :percentage="toPercent(row.value)" :stroke-width="8" :show-text="false" />
              <strong>{{ formatWeight(row.value) }}</strong>
            </div>
          </div>
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

      <div v-if="detail.recommendStatus !== 'EMPTY' && detail.items?.length" class="panel price-demo-panel">
        <div class="panel__body price-demo-entry">
          <div>
            <p class="eyebrow">当前推荐车型</p>
            <h2>价格从低到高演示</h2>
            <p class="inline-state">仅调整本页展示顺序，推荐排名仍以卡片上的 # 排名为准。</p>
          </div>
          <el-button type="primary" plain @click="priceSortDemoActive = !priceSortDemoActive">
            {{ priceSortDemoActive ? '恢复推荐排序' : '价格从低到高演示' }}
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
        <section v-for="group in displayItemGroups" :key="group.key" class="result-group">
          <div class="group-head">
            <h2>{{ group.title }}</h2>
            <span>{{ group.items.length }} 款车型</span>
          </div>

          <article v-for="item in group.items" :key="`${detail.recordId}-${item.rankNo}`" class="recommend-card">
            <div class="card-top">
              <RouterLink
                class="car-visual-link"
                :to="`/car/${item.carId}?recordId=${detail.recordId}`"
                :aria-label="`${item.brand} ${item.modelName} 详情`"
              >
                <img :src="carImageSrc(item.imageUrl)" :alt="item.modelName" @error="fallbackCarImage" />
              </RouterLink>

              <div class="car-overview">
                <div class="car-title-row">
                  <div>
                    <span class="rank-badge">#{{ item.rankNo }}</span>
                    <h2>
                      <RouterLink :to="`/car/${item.carId}?recordId=${detail.recordId}`">
                        {{ item.brand }} {{ item.modelName }}
                      </RouterLink>
                    </h2>
                    <p>{{ item.series }} · {{ item.bodyType }} · {{ item.energyType }} · {{ item.seats }} 座</p>
                  </div>
                  <div class="score-box" :class="scoreClass(item.totalScore)">
                    <strong>{{ formatScore(item.totalScore) }}</strong>
                    <span>推荐分</span>
                  </div>
                </div>

                <div class="recommend-meta">
                  <strong class="price-text">{{ formatWan(item.guidePrice) }}</strong>
                  <div class="tag-line">
                    <el-tag v-for="tag in displayTags(item.tags)" :key="tag" effect="light">{{ tag }}</el-tag>
                  </div>
                </div>

                <div class="card-actions">
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
            </div>

            <div class="explain-grid">
              <div>
                <h3>推荐理由</h3>
                <p>{{ displayExplanationText(item.reasonText) }}</p>
              </div>
              <div>
                <h3>不足提醒</h3>
                <p>{{ displayExplanationText(item.weaknessText) }}</p>
              </div>
            </div>

            <div class="score-grid">
              <div v-for="row in scoreRows(item)" :key="row.key" class="score-row">
                <span>{{ row.label }}</span>
                <el-progress
                  :percentage="scorePercent(row.value)"
                  :status="scoreStatus(row.value)"
                  :stroke-width="7"
                  :show-text="false"
                />
                <strong>{{ formatScore(row.value) }}</strong>
              </div>
            </div>
          </article>
        </section>
      </div>

      <div v-if="detail.recommendStatus !== 'EMPTY'" class="panel feedback-panel">
        <div class="panel__body">
          <div class="feedback-head">
            <div>
              <h2>推荐反馈</h2>
              <p>你的反馈会帮助我们了解推荐体验。</p>
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
              placeholder="可以补充推荐是否符合预算、车型、空间、动力，或推荐理由是否清楚"
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
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { addFavorite, fetchFavoriteStatus, removeFavorite } from '@/api/favorites'
import { fetchRecommendationDetail, fetchRecommendationFeedback, submitRecommendationFeedback } from '@/api/recommend'
import { addUserCompare, fetchUserCompare } from '@/api/userCompare'
import { carImageSrc, fallbackCarImage } from '@/utils/carImage'
import {
  clearCompareReturn,
  readCompareScrollFor,
  saveCompareReturn,
} from '@/utils/compareSelection'
import { displayExplanationText, displayTags, rankOrderedItems } from '@/utils/recommendPresentation'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const detail = ref(null)
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
const priceSortDemoActive = ref(false)

const recordId = computed(() => route.params.recordId)

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

const feedbackReasonOptions = [
  '推荐有帮助',
  '解释清楚',
  '价格偏高',
  '车型不合适',
  '动力不符合',
  '空间不足',
  '还想看更多选择',
]

const weightRows = computed(() =>
  weightConfig.map(([key, label]) => ({
    key,
    label,
    value: Number(detail.value?.weights?.[key] || 0),
  })),
)

const topWeightRows = computed(() =>
  [...weightRows.value]
    .sort((left, right) => right.value - left.value)
    .slice(0, 5),
)

const strictItems = computed(() => rankOrderedItems((detail.value?.items || []).filter((item) => item.matchLevel === 'STRICT')))
const recommendationItems = computed(() =>
  rankOrderedItems((detail.value?.items || []).filter((item) => item.matchLevel !== 'STRICT')),
)

const itemGroups = computed(() => {
  const groups = []
  if (strictItems.value.length) {
    groups.push({ key: 'strict', title: '更符合需求', items: strictItems.value })
  }
  if (recommendationItems.value.length) {
    groups.push({ key: 'recommendation', title: '可参考车型', items: recommendationItems.value })
  }
  return groups
})

const priceSortedItems = computed(() =>
  [...rankOrderedItems(detail.value?.items || [])].sort((left, right) => {
    const leftPrice = Number(left.guidePrice || Number.MAX_SAFE_INTEGER)
    const rightPrice = Number(right.guidePrice || Number.MAX_SAFE_INTEGER)
    if (leftPrice !== rightPrice) return leftPrice - rightPrice
    return Number(left.rankNo || 0) - Number(right.rankNo || 0)
  }),
)

const displayItemGroups = computed(() => {
  if (!priceSortDemoActive.value) return itemGroups.value
  return [{ key: 'price-demo', title: '价格从低到高演示', items: priceSortedItems.value }]
})

const compareEntryText = computed(() => {
  if (compareError.value) return compareError.value
  if (compareNotice.value) return compareNotice.value
  if (compareIds.value.length === 0) return '在推荐卡片中加入车型后，可进入对比页查看。'
  if (compareIds.value.length === 1) return '已加入 1 款车型，可继续加入更多车型。'
  return '可进入对比页查看基础信息、参数和评分差异。'
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
    priceSortDemoActive.value = false
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
  if (value === 'SUCCESS') return '全部符合需求'
  if (value === 'FALLBACK') return '含参考车型'
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
  return Number(value || 0).toFixed(1)
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
  max-width: 780px;
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
  grid-template-columns: 48px minmax(0, 1fr) 48px;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  font-size: 12px;
}

.weight-row strong,
.score-row strong {
  text-align: right;
}

.empty-panel,
.compare-entry-panel,
.price-demo-panel {
  margin-bottom: 20px;
}

.compare-entry,
.price-demo-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.compare-entry h2,
.price-demo-entry h2 {
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
  gap: 18px;
  padding: 18px;
  border: 1px solid rgba(226, 232, 240, 0.96);
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.07);
}

.card-top {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
}

.car-visual-link {
  display: block;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  border-radius: 16px;
  background: #eef2f7;
}

.car-visual-link img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.car-overview {
  display: flex;
  flex-direction: column;
  min-width: 0;
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
  font-size: 24px;
  line-height: 1.2;
}

.car-title-row h2 a:hover {
  color: var(--color-primary);
}

.car-title-row p {
  margin: 8px 0 0;
  color: var(--color-muted);
  font-size: 13px;
}

.score-box {
  width: 110px;
  min-width: 110px;
  padding: 12px;
  text-align: center;
  border-radius: 14px;
  background: #f8fafc;
}

.score-box strong,
.score-box span {
  display: block;
}

.score-box strong {
  font-size: 30px;
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
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.tag-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.price-text {
  color: var(--color-primary-dark);
  font-size: 18px;
}

.explain-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.explain-grid > div {
  min-height: 116px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  background: #fbfdff;
}

.explain-grid h3 {
  margin: 0 0 8px;
  color: var(--color-primary-dark);
  font-size: 15px;
}

.explain-grid p {
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.8;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 16px;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  margin-top: auto;
  padding-top: 14px;
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

@media (max-width: 980px) {
  .result-summary,
  .compare-entry,
  .price-demo-entry,
  .card-top,
  .explain-grid,
  .score-grid {
    grid-template-columns: 1fr;
  }

  .car-title-row {
    display: grid;
  }
}
</style>
