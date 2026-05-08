<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">推荐记录</h1>
        <p class="page-subtitle">按历史快照追溯推荐依据，展示需求、画像、权重、降级提示、分数、标签、理由和不足。</p>
      </div>
      <el-button type="primary" plain @click="reloadRecords">刷新记录</el-button>
    </div>

    <div class="record-layout">
      <div class="panel">
        <div class="panel__body">
          <el-skeleton v-if="loadingList" :rows="7" animated />

          <el-alert
            v-else-if="listError"
            type="error"
            :closable="false"
            :title="listError"
            show-icon
          />

          <el-empty v-else-if="!records.length" description="暂无推荐记录">
            <el-button type="primary" @click="$router.push('/recommend')">生成一条推荐</el-button>
          </el-empty>

          <template v-else>
            <div class="record-list">
              <button
                v-for="record in records"
                :key="record.recordId"
                class="record-list__item"
                :class="{ 'record-list__item--active': record.recordId === selectedRecordId }"
                type="button"
                @click="selectRecord(record.recordId)"
              >
                <div>
                  <div class="record-line">
                    <strong>#{{ record.recordId }}</strong>
                    <el-tag :type="statusType(record.recommendStatus)" size="small">
                      {{ statusLabel(record.recommendStatus) }}
                    </el-tag>
                  </div>
                  <p>{{ record.profileText || '暂无画像文本' }}</p>
                  <small>{{ formatDate(record.createTime) }}</small>
                  <div class="top-car-line">
                    <span v-for="name in record.topCarNames" :key="name">{{ name }}</span>
                    <span v-if="!record.topCarNames?.length">无推荐明细</span>
                  </div>
                </div>
                <b>{{ record.itemCount }}</b>
              </button>
            </div>

            <div class="history-footer">
              <el-pagination
                v-model:current-page="query.page"
                v-model:page-size="query.size"
                :total="total"
                :page-sizes="[5, 10, 20]"
                layout="total, sizes, prev, pager, next"
                @current-change="loadRecords"
                @size-change="reloadFirstPage"
              />
            </div>
          </template>
        </div>
      </div>

      <div class="detail-stack">
        <div class="panel">
          <div class="panel__body">
            <el-skeleton v-if="loadingDetail" :rows="10" animated />

            <el-alert
              v-else-if="detailError"
              type="error"
              :closable="false"
              :title="detailError"
              show-icon
            />

            <el-empty v-else-if="!detail" description="请选择左侧推荐记录" />

            <template v-else>
              <div class="detail-head">
                <div>
                  <p class="eyebrow">推荐记录 #{{ detail.recordId }}</p>
                  <h2>{{ detail.profileText }}</h2>
                  <p class="muted-line">{{ formatDate(detail.createTime) }} · 需求 #{{ detail.demandId }} · 用户 #{{ detail.userId }}</p>
                </div>
                <el-tag :type="statusType(detail.recommendStatus)" size="large">
                  {{ statusLabel(detail.recommendStatus) }}
                </el-tag>
              </div>

              <el-alert
                v-if="detail.fallbackMessage"
                class="fallback-alert"
                type="warning"
                :closable="false"
                :title="detail.fallbackMessage"
                show-icon
              />

              <div class="trace-grid">
                <div v-for="row in algorithmRows" :key="row.label">
                  <span>{{ row.label }}</span>
                  <strong>{{ row.value }}</strong>
                </div>
              </div>

              <div class="snapshot-grid">
                <div>
                  <p class="section-title">用户需求</p>
                  <div class="demand-tags">
                    <span>{{ formatWan(detail.demand?.budgetMin) }} - {{ formatWan(detail.demand?.budgetMax) }}</span>
                    <span>{{ arrayText(detail.demand?.bodyTypes, '未指定车型') }}</span>
                    <span>{{ arrayText(detail.demand?.energyTypes, '未指定动力') }}</span>
                    <span>{{ arrayText(detail.demand?.scenes, '未指定场景') }}</span>
                    <span>{{ detail.demand?.minSeats || '未指定' }} 座以上</span>
                  </div>
                  <p class="raw-text">{{ detail.demand?.rawText || '无自然语言原文' }}</p>
                  <div class="factor-line">
                    <el-tag v-for="factor in demandFactorRows" :key="factor.key" effect="light">
                      {{ factor.label }} {{ factor.value }}
                    </el-tag>
                  </div>
                </div>

                <div>
                  <p class="section-title">最终权重快照</p>
                  <div v-for="row in weightRows" :key="row.key" class="weight-row">
                    <span>{{ row.label }}</span>
                    <el-progress :percentage="toPercent(row.value)" :stroke-width="8" :show-text="false" />
                    <strong>{{ formatWeight(row.value) }}</strong>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>

        <div v-if="detail" class="panel">
          <div class="panel__body">
            <div class="section-header">
              <div>
                <p class="eyebrow">推荐明细快照</p>
                <h3>按 rankNo 升序展示，不重新计算历史结果</h3>
              </div>
              <span>{{ detail.items?.length || 0 }} 条</span>
            </div>

            <el-empty v-if="!detail.items?.length" description="该推荐记录没有明细" />

            <div v-else class="item-list">
              <article v-for="item in detail.items" :key="item.rankNo" class="item-card">
                <div class="item-card__head">
                  <div>
                    <span class="rank">TOP {{ item.rankNo }}</span>
                    <h3>{{ item.brand }} {{ item.modelName }}</h3>
                    <p>{{ item.series }} · {{ formatWan(item.guidePrice) }} · {{ item.bodyType }} · {{ item.energyType }}</p>
                  </div>
                  <div class="score-pill">
                    <strong>{{ formatScore(item.totalScore) }}</strong>
                    <span>综合分</span>
                  </div>
                </div>

                <div class="tag-row">
                  <el-tag v-for="tag in item.tags" :key="tag" effect="light">{{ tag }}</el-tag>
                  <el-tag :type="matchTagType(item.matchLevel)" effect="dark">{{ matchLabel(item.matchLevel) }}</el-tag>
                </div>

                <div class="explain-grid">
                  <div>
                    <p>推荐理由</p>
                    <span>{{ item.reasonText }}</span>
                  </div>
                  <div>
                    <p>不足提醒</p>
                    <span>{{ item.weaknessText }}</span>
                  </div>
                </div>

                <div class="score-grid">
                  <div v-for="row in scoreRows(item)" :key="row.key" class="score-row">
                    <span>{{ row.label }}</span>
                    <strong>{{ formatScore(row.value) }}</strong>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import { fetchAdminRecommendationDetail, fetchAdminRecommendationHistory } from '@/api/adminRecommendRecords'

const loadingList = ref(false)
const loadingDetail = ref(false)
const listError = ref('')
const detailError = ref('')
const records = ref([])
const total = ref(0)
const selectedRecordId = ref(null)
const detail = ref(null)
const query = reactive({
  page: 1,
  size: 10,
})

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

const demandFactorRows = computed(() =>
  weightConfig
    .map(([key, label]) => ({
      key,
      label,
      value: Number(detail.value?.demand?.factorWeights?.[key] || 0),
    }))
    .filter((item) => item.value > 0)
    .sort((a, b) => b.value - a.value),
)

const algorithmRows = computed(() => [
  {
    label: '算法版本',
    value: detail.value?.algorithmVersion || 'weighted-sum-v1',
  },
  {
    label: '组合系数 alpha',
    value: formatAlpha(detail.value?.alpha),
  },
  {
    label: '推荐状态',
    value: statusLabel(detail.value?.recommendStatus),
  },
])

onMounted(loadRecords)

async function loadRecords() {
  loadingList.value = true
  listError.value = ''
  try {
    const response = await fetchAdminRecommendationHistory({
      page: query.page,
      size: query.size,
    })
    records.value = response.data.records || []
    total.value = response.data.total || 0
    if (records.value.length && !selectedRecordId.value) {
      await selectRecord(records.value[0].recordId)
    }
    if (!records.value.length) {
      selectedRecordId.value = null
      detail.value = null
    }
  } catch (error) {
    records.value = []
    total.value = 0
    listError.value = error?.response?.data?.message || error?.message || '推荐记录加载失败。'
  } finally {
    loadingList.value = false
  }
}

function reloadRecords() {
  selectedRecordId.value = null
  detail.value = null
  loadRecords()
}

function reloadFirstPage() {
  query.page = 1
  reloadRecords()
}

async function selectRecord(recordId) {
  selectedRecordId.value = recordId
  loadingDetail.value = true
  detailError.value = ''
  try {
    const response = await fetchAdminRecommendationDetail(recordId)
    detail.value = response.data
  } catch (error) {
    detail.value = null
    detailError.value = error?.response?.data?.message || error?.message || '推荐详情加载失败。'
  } finally {
    loadingDetail.value = false
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

function matchLabel(value) {
  const labels = {
    STRICT: '完全匹配',
    RELAX_BUDGET: '放宽预算',
    RELAX_BODY_TYPE: '放宽车型',
    RELAX_ENERGY_TYPE: '放宽动力',
    SIMILAR_RECOMMEND: '相似推荐',
  }
  return labels[value] || value
}

function matchTagType(value) {
  if (value === 'STRICT') return 'success'
  if (value === 'RELAX_BUDGET') return 'warning'
  if (value === 'RELAX_ENERGY_TYPE') return 'primary'
  return 'info'
}

function toPercent(value) {
  return Math.max(0, Math.min(100, Number(value || 0) * 100))
}

function formatWeight(value) {
  return `${(Number(value || 0) * 100).toFixed(1)}%`
}

function formatAlpha(value) {
  if (value === null || value === undefined || value === '') return '未记录'
  return Number(value).toFixed(2)
}

function formatScore(value) {
  return Number(value || 0).toFixed(2)
}

function formatWan(value) {
  if (value === null || value === undefined || value === '') return '未填'
  return `${(Number(value) / 10000).toFixed(1).replace(/\.0$/, '')} 万`
}

function arrayText(values, fallback) {
  return Array.isArray(values) && values.length ? values.join(' / ') : fallback
}

function formatDate(value) {
  if (!value) return '时间未知'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.record-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.record-list {
  display: grid;
  gap: 12px;
}

.record-list__item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 40px;
  gap: 12px;
  width: 100%;
  padding: 14px;
  text-align: left;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
  cursor: pointer;
}

.record-list__item--active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.record-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.record-line strong {
  color: var(--color-primary-dark);
}

.record-list__item p,
.raw-text,
.muted-line {
  margin: 8px 0 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.6;
}

.record-list__item small {
  display: block;
  margin-top: 8px;
  color: var(--color-muted);
}

.record-list__item b {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  color: var(--color-primary);
  background: rgba(37, 99, 235, 0.08);
}

.top-car-line,
.factor-line,
.tag-row,
.demand-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.top-car-line span,
.demand-tags span {
  padding: 5px 8px;
  border-radius: var(--radius-sm);
  background: #f3f4f6;
  color: var(--color-muted);
  font-size: 12px;
}

.history-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.detail-stack {
  display: grid;
  gap: 18px;
}

.detail-head,
.section-header,
.item-card__head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.detail-head h2 {
  margin: 6px 0 0;
  color: var(--color-primary-dark);
  font-size: 22px;
  line-height: 1.4;
}

.eyebrow {
  margin: 0;
  color: var(--color-muted);
  font-size: 12px;
}

.fallback-alert {
  margin-top: 16px;
}

.trace-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.trace-grid div {
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f9fafb;
}

.trace-grid span,
.trace-grid strong {
  display: block;
}

.trace-grid span {
  color: var(--color-muted);
  font-size: 12px;
}

.trace-grid strong {
  margin-top: 4px;
  color: var(--color-primary-dark);
  font-size: 13px;
}

.snapshot-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
  margin-top: 18px;
}

.section-title {
  margin: 0 0 10px;
  color: var(--color-primary-dark);
  font-weight: 700;
}

.weight-row {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) 58px;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  font-size: 12px;
}

.weight-row strong {
  text-align: right;
}

.section-header h3 {
  margin: 6px 0 0;
  color: var(--color-primary-dark);
  font-size: 18px;
}

.section-header span {
  color: var(--color-muted);
  font-size: 13px;
}

.item-list {
  display: grid;
  gap: 14px;
  margin-top: 16px;
}

.item-card {
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
}

.rank {
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 700;
}

.item-card h3 {
  margin: 6px 0;
  color: var(--color-primary-dark);
  font-size: 18px;
}

.item-card p {
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
}

.score-pill {
  min-width: 96px;
  padding: 10px;
  text-align: center;
  border-radius: var(--radius-sm);
  background: #f8fafc;
}

.score-pill strong,
.score-pill span {
  display: block;
}

.score-pill strong {
  color: var(--color-primary);
  font-size: 24px;
}

.score-pill span {
  color: var(--color-muted);
  font-size: 12px;
}

.explain-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.explain-grid > div {
  padding: 12px;
  border-radius: var(--radius-sm);
  background: #f9fafb;
}

.explain-grid p {
  margin: 0 0 6px;
  color: var(--color-primary-dark);
  font-weight: 700;
}

.explain-grid span {
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.7;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px 12px;
  margin-top: 14px;
}

.score-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  background: #f3f4f6;
  font-size: 12px;
}

.score-row span {
  color: var(--color-muted);
}

.score-row strong {
  color: var(--color-primary-dark);
}

@media (max-width: 1080px) {
  .record-layout,
  .trace-grid,
  .snapshot-grid,
  .explain-grid,
  .score-grid {
    grid-template-columns: 1fr;
  }
}
</style>
