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
      <el-alert
        v-if="detail.recommendStatus === 'FALLBACK'"
        class="fallback-alert"
        type="warning"
        :closable="false"
        :title="detail.fallbackMessage || '未找到足够的完全匹配车型，系统已放宽部分条件。'"
        show-icon
      />

      <div class="result-summary">
        <div class="panel profile-panel">
          <div class="panel__body">
            <div class="summary-head">
              <div>
                <p class="eyebrow">用户画像</p>
                <h2>{{ detail.profileText || '暂无画像文本' }}</h2>
              </div>
              <el-tag :type="statusType(detail.recommendStatus)" size="large">{{ statusLabel(detail.recommendStatus) }}</el-tag>
            </div>
            <p v-if="detail.fallbackMessage" class="fallback-text">{{ detail.fallbackMessage }}</p>
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

      <div v-if="detail.recommendStatus === 'EMPTY' || !detail.items?.length" class="panel empty-panel">
        <div class="panel__body">
          <el-empty description="暂未找到合适车型">
            <el-button type="primary" @click="$router.push('/recommend')">调整需求重新推荐</el-button>
          </el-empty>
        </div>
      </div>

      <div v-else class="result-list">
        <article v-for="item in detail.items" :key="`${detail.recordId}-${item.rankNo}`" class="recommend-card">
          <div class="car-visual">
            <img :src="carImageSrc(item.imageUrl)" :alt="item.modelName" @error="fallbackCarImage" />
          </div>

          <div class="recommend-main">
            <div class="car-title-row">
              <div>
                <span class="rank-badge">TOP {{ item.rankNo }}</span>
                <h2>{{ item.brand }} {{ item.modelName }}</h2>
                <p>{{ item.series }} · {{ item.bodyType }} · {{ item.energyType }} · {{ item.seats }} 座</p>
              </div>
              <div class="score-box" :class="scoreClass(item.totalScore)">
                <strong>{{ formatScore(item.totalScore) }}</strong>
                <span>综合匹配度</span>
              </div>
            </div>

            <div class="recommend-meta">
              <div class="tag-line">
                <span class="meta-label">推荐标签</span>
                <el-tag v-for="tag in displayTags(item.tags)" :key="tag" effect="light">{{ tag }}</el-tag>
              </div>
              <div class="match-line">
                <span class="meta-label">匹配状态</span>
                <el-tag :type="matchTagType(item.matchLevel)" effect="dark">{{ matchLabel(item.matchLevel) }}</el-tag>
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
              <el-button type="primary" plain @click="goCarDetail(item.carId)">查看车型详情</el-button>
            </div>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchRecommendationDetail } from '@/api/recommend'
import { carImageSrc, fallbackCarImage } from '@/utils/carImage'
import { displayTags, matchLabel, matchTagType } from '@/utils/recommendPresentation'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const detail = ref(null)

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

const weightRows = computed(() =>
  weightConfig.map(([key, label]) => ({
    key,
    label,
    value: Number(detail.value?.weights?.[key] || 0),
  })),
)

onMounted(loadDetail)
watch(recordId, loadDetail)

async function loadDetail() {
  if (!recordId.value) return
  loading.value = true
  error.value = ''
  try {
    const response = await fetchRecommendationDetail(recordId.value)
    detail.value = response.data
  } catch (requestError) {
    detail.value = null
    if (requestError?.response?.status === 404) {
      error.value = '推荐记录不存在，或当前演示用户无权查看该记录。'
    } else {
      error.value = requestError?.response?.data?.message || requestError?.message || '推荐结果加载失败。'
    }
  } finally {
    loading.value = false
  }
}

function scoreRows(item) {
  return scoreConfig.map(([key, label]) => ({
    key,
    label,
    value: Number(item?.[key] || 0),
  }))
}

function goCarDetail(carId) {
  router.push({ path: `/car/${carId}`, query: { recordId: detail.value.recordId } })
}

function statusLabel(value) {
  if (value === 'SUCCESS') return '完全匹配'
  if (value === 'FALLBACK') return '降级推荐'
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
.state-alert,
.fallback-alert {
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

.fallback-text {
  margin: 16px 0 0;
  color: var(--color-muted);
  line-height: 1.7;
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

.result-list {
  display: grid;
  gap: 18px;
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

.car-visual img {
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

.tag-line,
.match-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.match-line {
  padding-left: 10px;
  border-left: 1px solid var(--color-border);
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
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 980px) {
  .result-summary,
  .recommend-card,
  .explain-grid,
  .score-grid {
    grid-template-columns: 1fr;
  }

  .price-text {
    margin-left: 0;
  }

  .match-line {
    padding-left: 0;
    border-left: 0;
  }
}
</style>
