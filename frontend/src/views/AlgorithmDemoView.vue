<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">推荐算法可视化演示</h1>
        <p class="page-subtitle">答辩展示页从推荐快照读取数据，展示当前 Pareto-TOPSIS 推荐算法的完整过程。</p>
      </div>
      <el-button @click="$router.push('/')">返回首页</el-button>
    </div>

    <div class="panel control-panel">
      <div class="panel__body control-row">
        <div>
          <p class="eyebrow">推荐记录</p>
          <h2>{{ detail ? `记录 #${detail.recordId}` : '选择一条历史推荐' }}</h2>
          <p class="muted-line">本页只读取 recommend_record 与 recommend_item 快照，不会重新生成推荐。</p>
        </div>
        <div class="record-picker">
          <el-input
            v-model="recordInput"
            class="record-input"
            clearable
            placeholder="输入 recordId"
            @keyup.enter="loadVisualization"
          />
          <el-button type="primary" :loading="loading" @click="loadVisualization">加载算法过程</el-button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="panel state-panel">
      <div class="panel__body">
        <el-skeleton :rows="9" animated />
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

    <div v-else-if="empty" class="panel state-panel">
      <div class="panel__body">
        <el-empty description="暂无推荐记录">
          <el-button type="primary" @click="$router.push('/recommend')">先完成一次推荐</el-button>
        </el-empty>
      </div>
    </div>

    <template v-else-if="detail">
      <el-alert
        v-if="detail.compatibilityNote"
        class="state-alert"
        type="warning"
        :closable="false"
        :title="detail.compatibilityNote"
        show-icon
      />

      <div class="overview-grid">
        <div class="panel hero-panel">
          <div class="panel__body">
            <p class="eyebrow">算法版本 algorithmVersion</p>
            <h2>{{ detail.algorithmVersion }}</h2>
            <div class="hero-metrics">
              <div>
                <span>组合系数 alpha</span>
                <strong>{{ formatAlpha(detail.alpha) }}</strong>
              </div>
              <div>
                <span>推荐状态</span>
                <strong>{{ statusLabel(detail.recommendStatus) }}</strong>
              </div>
              <div>
                <span>推荐明细</span>
                <strong>{{ detail.items?.length || 0 }} 条</strong>
              </div>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel__body">
            <p class="eyebrow">推荐快照追溯</p>
            <p class="snapshot-note">{{ detail.snapshotNote }}</p>
            <div class="trace-line">
              <span>recordId #{{ detail.recordId }}</span>
              <span>demandId #{{ detail.demandId }}</span>
              <span>{{ formatDate(detail.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>

      <section class="panel section-panel">
        <div class="panel__body">
          <div class="section-head">
            <div>
              <p class="eyebrow">算法流程详解</p>
              <h2>15 步算法流程详解</h2>
            </div>
          </div>
          <div class="pipeline-detail-list">
            <div v-for="step in detail.pipeline" :key="step.step" class="pipeline-step pipeline-step--detail">
              <div class="pipeline-step__head">
                <span>{{ step.step }}</span>
                <strong>{{ step.title }}</strong>
              </div>
              <p class="pipeline-description">{{ step.description }}</p>
              <div class="pipeline-facts">
                <div>
                  <b>输入数据</b>
                  <p>{{ step.inputSummary }}</p>
                </div>
                <div>
                  <b>输出数据</b>
                  <p>{{ step.outputSummary }}</p>
                </div>
                <div>
                  <b>本次记录结果摘要</b>
                  <p>{{ step.recordResult }}</p>
                </div>
                <div>
                  <b>对应代码模块</b>
                  <p>{{ step.codeModule }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="two-column">
        <div class="panel section-panel">
          <div class="panel__body">
            <div class="section-head">
              <div>
                <p class="eyebrow">用户需求与约束</p>
                <h2>{{ detail.profileText || '暂无画像文本' }}</h2>
              </div>
            </div>
            <div class="demand-grid">
              <div v-for="row in demandRows" :key="row.label">
                <span>{{ row.label }}</span>
                <strong>{{ row.value }}</strong>
              </div>
            </div>
            <div class="constraint-list">
              <div v-for="constraint in detail.constraints" :key="constraint.name" class="constraint-row">
                <el-tag :type="constraint.type === 'hard' ? 'danger' : 'warning'" effect="plain">
                  {{ constraint.type === 'hard' ? '硬性约束' : '软偏好' }}
                </el-tag>
                <div>
                  <strong>{{ constraint.name }}：{{ constraint.value }}</strong>
                  <p>{{ constraint.description }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="panel section-panel">
          <div class="panel__body">
            <div class="section-head">
              <div>
                <p class="eyebrow">车型特征评分说明</p>
                <h2>九维指标来源</h2>
              </div>
            </div>
            <div class="rule-list">
              <div v-for="rule in detail.featureScoreRules" :key="rule.dimension" class="rule-row">
                <span>{{ rule.label }}</span>
                <p>{{ rule.summary }}</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="featureExample" class="panel section-panel">
        <div class="panel__body">
          <div class="section-head">
            <div>
              <p class="eyebrow">车型特征评分示例</p>
              <h2>rankNo = 1 车型评分拆解</h2>
            </div>
            <span class="section-meta">示例只读展示，不覆盖推荐快照</span>
          </div>

          <div class="feature-example">
            <div class="feature-summary">
              <div>
                <p class="eyebrow">示例车型</p>
                <h3>{{ featureExample.brand }} {{ featureExample.modelName }}</h3>
                <div class="feature-tags">
                  <el-tag effect="plain">{{ featureExample.bodyType || '车型未知' }}</el-tag>
                  <el-tag effect="plain">{{ featureExample.energyType || '动力未知' }}</el-tag>
                  <el-tag effect="plain">{{ featureExample.seats || '未知' }} 座</el-tag>
                </div>
              </div>
              <div class="feature-price">
                <span>指导价</span>
                <strong>{{ formatWan(featureExample.guidePrice) }}</strong>
              </div>
            </div>

            <div class="feature-param-grid">
              <div v-for="row in featureParamRows" :key="row.key">
                <span>{{ row.label }}</span>
                <strong>{{ row.value }}</strong>
              </div>
            </div>

            <div class="feature-score-grid">
              <div v-for="score in featureScoreRows" :key="score.key" class="feature-score-card">
                <span>{{ score.label }}</span>
                <strong>{{ formatScore(score.value) }}</strong>
                <div class="score-track">
                  <i :style="{ width: `${Math.min(100, Number(score.value || 0))}%` }" />
                </div>
              </div>
            </div>

            <div class="breakdown-list">
              <article v-for="breakdown in featureExample.scoreBreakdown" :key="breakdown.dimension" class="breakdown-card">
                <div class="breakdown-card__head">
                  <div>
                    <span>{{ breakdown.label }}</span>
                    <h3>{{ formatScore(breakdown.finalScore) }}</h3>
                  </div>
                  <small>{{ breakdown.dimension }}</small>
                </div>
                <p class="formula-text">{{ breakdown.formulaText }}</p>
                <div class="matched-rule-list">
                  <div v-for="rule in breakdown.matchedRules" :key="`${breakdown.dimension}-${rule.ruleName}`">
                    <strong>{{ rule.ruleName }}</strong>
                    <b>+{{ formatScore(rule.delta) }}</b>
                    <p>{{ rule.reason }}</p>
                  </div>
                </div>
                <p class="breakdown-explain">{{ breakdown.explanation }}</p>
              </article>
            </div>
          </div>
        </div>
      </section>

      <section class="panel section-panel">
        <div class="panel__body">
          <div class="section-head">
            <div>
              <p class="eyebrow">权重可视化</p>
              <h2>用户主观权重 subjectiveWeight / 熵权法客观权重 objectiveWeight / 主客观组合权重 finalWeight</h2>
            </div>
            <span class="section-meta">组合系数 alpha {{ formatAlpha(detail.alpha) }}</span>
          </div>
          <div class="weight-table">
            <div class="weight-table__head">
              <span>维度</span>
              <span>用户主观权重 subjectiveWeight</span>
              <span>熵权法客观权重 objectiveWeight</span>
              <span>主客观组合权重 finalWeight</span>
            </div>
            <div v-for="row in weightRows" :key="row.key" class="weight-compare-row">
              <strong>{{ row.label }}</strong>
              <WeightBar :value="row.subjective" accent="#2563eb" />
              <WeightBar :value="row.objective" accent="#0891b2" />
              <WeightBar :value="row.finalWeight" accent="#f59e0b" />
            </div>
          </div>
        </div>
      </section>

      <section class="panel section-panel">
        <div class="panel__body">
          <div class="section-head">
            <div>
              <p class="eyebrow">候选阶段统计</p>
              <h2>STRICT 与降级候选分布</h2>
            </div>
          </div>
          <div class="stage-grid">
            <div v-for="stage in detail.stageStats" :key="stage.matchLevel" class="stage-card">
              <span>{{ stage.label }}</span>
              <strong>{{ stage.count }}</strong>
              <div class="stage-track">
                <i :style="{ width: `${stagePercent(stage.count)}%` }" />
              </div>
              <small>{{ stage.matchLevel }}</small>
            </div>
          </div>
        </div>
      </section>

      <section class="panel section-panel">
        <div class="panel__body">
          <div class="section-head">
            <div>
              <p class="eyebrow">九维评分矩阵</p>
              <h2>按 rankNo 展示推荐快照</h2>
            </div>
            <div class="section-actions">
              <el-segmented v-model="matrixFilter" :options="matrixFilterOptions" />
              <el-button plain @click="showAllMatrix = !showAllMatrix">
                {{ showAllMatrix ? '收起' : '查看全部' }}
              </el-button>
            </div>
          </div>
          <div class="matrix-table">
            <div class="matrix-row matrix-row--head">
              <span>排名</span>
              <span>车型</span>
              <span v-for="dimension in dimensions" :key="dimension.key">{{ dimension.label }}</span>
            </div>
            <div v-for="item in visibleMatrixItems" :key="`matrix-${item.rankNo}`" class="matrix-row">
              <strong>#{{ item.rankNo }}</strong>
              <span>{{ item.brand }} {{ item.modelName }}</span>
              <b
                v-for="dimension in dimensions"
                :key="dimension.key"
                class="heat-cell"
                :style="heatStyle(item.scores?.[dimension.key])"
              >
                {{ formatScore(item.scores?.[dimension.key]) }}
              </b>
            </div>
          </div>
          <p v-if="filteredMatrixItems.length > visibleMatrixItems.length" class="muted-line matrix-tip">
            当前仅展示前 {{ visibleMatrixItems.length }} 条，共 {{ filteredMatrixItems.length }} 条。
          </p>
        </div>
      </section>

      <section class="panel section-panel">
        <div class="panel__body">
          <div class="section-head">
            <div>
              <p class="eyebrow">Pareto-TOPSIS 排序</p>
              <h2>非支配标记与相对接近度</h2>
            </div>
          </div>
          <p class="explain-copy">
            Pareto 非支配车型在用户高权重维度上更不容易被其他车型全面压制。TOPSIS 推荐分表示车型接近理想车型、远离最差车型的程度。
          </p>
          <div class="ranking-table">
            <div class="ranking-row ranking-row--head">
              <span>排序 rankNo</span>
              <span>车型</span>
              <span>匹配阶段 matchLevel</span>
              <span>Pareto 标记</span>
              <span>综合推荐分 totalScore</span>
              <span>接近度 closeness</span>
              <span>正理想距离 D+</span>
              <span>负理想距离 D-</span>
            </div>
            <div v-for="item in rankingItems" :key="`rank-${item.rankNo}`" class="ranking-row">
              <strong>#{{ item.rankNo }}</strong>
              <span>{{ item.brand }} {{ item.modelName }}</span>
              <el-tag :type="matchTagType(item.matchLevel)" effect="plain">{{ item.matchLevelLabel }}</el-tag>
              <span>{{ item.paretoDominated ? '被支配' : '非支配' }}</span>
              <strong>{{ formatScore(item.totalScore) }}</strong>
              <span>{{ formatDistance(item.topsis?.closeness) }}</span>
              <span>{{ formatDistance(item.topsis?.positiveDistance) }}</span>
              <span>{{ formatDistance(item.topsis?.negativeDistance) }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="panel section-panel">
        <div class="panel__body">
          <div class="section-head">
            <div>
              <p class="eyebrow">推荐解释区</p>
              <h2>推荐标签 tags / 推荐理由 reasonText / 不足提醒 weaknessText</h2>
            </div>
            <span class="section-meta">展示前 {{ explanationItems.length }} 条</span>
          </div>
          <div class="explanation-grid">
            <article v-for="item in explanationItems" :key="`explain-${item.rankNo}`" class="explanation-card">
              <div class="explanation-card__head">
                <div>
                  <span class="rank-badge">#{{ item.rankNo }}</span>
                  <h3>{{ item.brand }} {{ item.modelName }}</h3>
                </div>
                <strong>{{ formatScore(item.totalScore) }}</strong>
              </div>
              <div class="tag-line">
                <el-tag v-for="tag in displayTags(item.tags)" :key="tag" effect="light">{{ tag }}</el-tag>
              </div>
              <div class="reason-grid">
                <div>
                  <span>推荐理由</span>
                  <p>{{ item.reasonText }}</p>
                </div>
                <div>
                  <span>不足提醒</span>
                  <p>{{ item.weaknessText }}</p>
                </div>
              </div>
              <div class="contribution-line">
                <span>贡献度最高</span>
                <b v-for="entry in topEntries(item.contribution)" :key="entry.key">
                  {{ dimensionLabel(entry.key) }} {{ formatDistance(entry.value) }}
                </b>
              </div>
              <div class="contribution-line">
                <span>差距最大</span>
                <b v-for="entry in topEntries(item.gap)" :key="entry.key">
                  {{ dimensionLabel(entry.key) }} {{ formatDistance(entry.value) }}
                </b>
              </div>
              <el-alert
                v-if="item.featureScoreSnapshotMismatch || !item.featureScoreSourceAvailable"
                class="source-alert"
                type="warning"
                :closable="false"
                :title="item.featureScoreSourceNote"
              />
            </article>
          </div>
        </div>
      </section>
    </template>
  </section>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref } from 'vue'

import { fetchAlgorithmVisualization } from '@/api/algorithmVisualization'
import { fetchRecommendationHistory } from '@/api/recommend'
import { displayTags, matchTagType } from '@/utils/recommendPresentation'

const WeightBar = defineComponent({
  props: {
    value: {
      type: [Number, String],
      default: 0,
    },
    accent: {
      type: String,
      default: '#2563eb',
    },
  },
  setup(props) {
    return () => {
      const value = Number(props.value || 0)
      const percent = Math.max(0, Math.min(100, value * 100))
      return h('div', { class: 'mini-weight' }, [
        h('div', { class: 'mini-weight__track' }, [
          h('i', {
            style: {
              width: `${percent}%`,
              background: props.accent,
            },
          }),
        ]),
        h('strong', `${percent.toFixed(1)}%`),
      ])
    }
  },
})

const loading = ref(false)
const error = ref('')
const empty = ref(false)
const detail = ref(null)
const recordInput = ref('')
const matrixFilter = ref('all')
const showAllMatrix = ref(false)

const matrixFilterOptions = [
  { label: '全部', value: 'all' },
  { label: '完全匹配', value: 'strict' },
  { label: '推荐', value: 'recommend' },
]

const fallbackDimensions = [
  { key: 'price', label: '价格' },
  { key: 'space', label: '空间' },
  { key: 'safety', label: '安全' },
  { key: 'energy', label: '能耗' },
  { key: 'intelligence', label: '智能' },
  { key: 'comfort', label: '舒适' },
  { key: 'power', label: '动力' },
  { key: 'reputation', label: '口碑' },
  { key: 'popularity', label: '热度' },
]

const dimensions = computed(() => detail.value?.dimensions?.length ? detail.value.dimensions : fallbackDimensions)

const demandRows = computed(() => {
  const demand = detail.value?.demand || {}
  return [
    { label: '预算', value: `${formatWan(demand.budgetMin)} - ${formatWan(demand.budgetMax)}` },
    { label: '车型类型', value: arrayText(demand.bodyTypes, '不限') },
    { label: '动力类型', value: arrayText(demand.energyTypes, '不限') },
    { label: '使用场景', value: arrayText(demand.scenes, '综合需求') },
    { label: '最低座位', value: demand.minSeats ? `${demand.minSeats} 座以上` : '未设置' },
    { label: '排除品牌', value: arrayText(demand.excludedBrands, '无') },
    { label: '排除车型', value: demand.excludedCarIds?.length ? `${demand.excludedCarIds.length} 款` : '无' },
  ]
})

const weightRows = computed(() =>
  dimensions.value.map((dimension) => ({
    key: dimension.key,
    label: dimension.label,
    subjective: Number(detail.value?.weights?.subjectiveWeight?.[dimension.key] || 0),
    objective: Number(detail.value?.weights?.objectiveWeight?.[dimension.key] || 0),
    finalWeight: Number(detail.value?.weights?.finalWeight?.[dimension.key] || 0),
  })),
)

const maxStageCount = computed(() =>
  Math.max(1, ...(detail.value?.stageStats || []).map((stage) => Number(stage.count || 0))),
)

const items = computed(() => detail.value?.items || [])
const featureExample = computed(() => detail.value?.featureScoreExample || null)

const featureParamRows = computed(() => {
  const params = featureExample.value?.params || {}
  return [
    { key: 'wheelbaseMm', label: '轴距', value: unitText(params.wheelbaseMm, 'mm') },
    { key: 'lengthMm', label: '车长', value: unitText(params.lengthMm, 'mm') },
    { key: 'airbagCount', label: '气囊数量', value: unitText(params.airbagCount, '个') },
    { key: 'totalRangeKm', label: '综合续航', value: unitText(params.totalRangeKm, 'km') },
    { key: 'electricRangeKm', label: '纯电续航', value: unitText(params.electricRangeKm, 'km') },
    { key: 'fuelConsumption', label: '燃油油耗', value: unitText(params.fuelConsumption, 'L/100km') },
    { key: 'acceleration100', label: '百公里加速', value: unitText(params.acceleration100, 's') },
    { key: 'assistDriveLevel', label: '辅助驾驶', value: params.assistDriveLevel || '未记录' },
    { key: 'screenSize', label: '屏幕尺寸', value: unitText(params.screenSize, '英寸') },
    { key: 'userRating', label: '用户评分', value: unitText(params.userRating, '分') },
    { key: 'salesVolume', label: '销量', value: unitText(params.salesVolume, '辆') },
    { key: 'maxSalesVolume', label: '库内最大销量', value: unitText(params.maxSalesVolume, '辆') },
  ]
})

const featureScoreRows = computed(() => {
  const scores = featureExample.value?.scores || {}
  return fallbackDimensions
    .filter((dimension) => dimension.key !== 'price')
    .map((dimension) => ({
      key: dimension.key,
      label: `${dimension.label} ${dimension.key}Score`,
      value: scores[dimension.key],
    }))
})

const filteredMatrixItems = computed(() => {
  if (matrixFilter.value === 'strict') {
    return items.value.filter((item) => item.matchLevel === 'STRICT')
  }
  if (matrixFilter.value === 'recommend') {
    return items.value.filter((item) => item.matchLevel !== 'STRICT')
  }
  return items.value
})

const visibleMatrixItems = computed(() =>
  showAllMatrix.value ? filteredMatrixItems.value : filteredMatrixItems.value.slice(0, 15),
)

const rankingItems = computed(() => items.value.slice(0, 15))
const explanationItems = computed(() => items.value.slice(0, 6))

onMounted(loadLatestRecord)

async function loadLatestRecord() {
  loading.value = true
  error.value = ''
  empty.value = false
  try {
    const response = await fetchRecommendationHistory({ page: 1, size: 1 })
    const records = response.data?.records || []
    if (!records.length) {
      detail.value = null
      empty.value = true
      return
    }
    recordInput.value = String(records[0].recordId)
    await loadVisualization()
  } catch (requestError) {
    detail.value = null
    error.value = requestError?.response?.data?.message || requestError?.message || '推荐历史加载失败。'
  } finally {
    loading.value = false
  }
}

async function loadVisualization() {
  const recordId = Number(recordInput.value)
  if (!Number.isInteger(recordId) || recordId <= 0) {
    error.value = '请输入有效的推荐记录 ID。'
    detail.value = null
    empty.value = false
    return
  }
  loading.value = true
  error.value = ''
  empty.value = false
  try {
    const response = await fetchAlgorithmVisualization(recordId)
    detail.value = response.data
    showAllMatrix.value = false
    matrixFilter.value = 'all'
  } catch (requestError) {
    detail.value = null
    if (requestError?.response?.status === 404) {
      error.value = '推荐记录不存在，或当前演示用户无权查看该记录。'
    } else {
      error.value = requestError?.response?.data?.message || requestError?.message || '算法过程加载失败。'
    }
  } finally {
    loading.value = false
  }
}

function statusLabel(value) {
  if (value === 'SUCCESS') return '完全匹配'
  if (value === 'FALLBACK') return '含补充推荐'
  if (value === 'EMPTY') return '暂无结果'
  return value || '未知'
}

function stagePercent(count) {
  return Math.max(0, Math.min(100, (Number(count || 0) / maxStageCount.value) * 100))
}

function heatStyle(value) {
  const score = Math.max(0, Math.min(100, Number(value || 0)))
  let color = '37, 99, 235'
  if (score >= 85) color = '22, 163, 74'
  if (score < 60) color = '245, 158, 11'
  return {
    background: `rgba(${color}, ${0.12 + score / 180})`,
    color: score >= 72 ? '#0f172a' : '#374151',
  }
}

function topEntries(values, limit = 2) {
  return Object.entries(values || {})
    .map(([key, value]) => ({ key, value: Number(value || 0) }))
    .sort((left, right) => right.value - left.value)
    .slice(0, limit)
}

function dimensionLabel(key) {
  return dimensions.value.find((dimension) => dimension.key === key)?.label || key
}

function formatScore(value) {
  return Number(value || 0).toFixed(2)
}

function formatDistance(value) {
  return Number(value || 0).toFixed(4)
}

function formatAlpha(value) {
  if (value === null || value === undefined || value === '') return '未记录'
  return Number(value).toFixed(2)
}

function formatWan(value) {
  if (value === null || value === undefined || value === '') return '未填'
  return `${(Number(value) / 10000).toFixed(1).replace(/\.0$/, '')} 万`
}

function arrayText(values, fallback) {
  return Array.isArray(values) && values.length ? values.join(' / ') : fallback
}

function unitText(value, unit) {
  if (value === null || value === undefined || value === '') return '未记录'
  return `${value} ${unit}`
}

function formatDate(value) {
  if (!value) return '时间未知'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.state-alert,
.section-panel,
.two-column,
.overview-grid {
  margin-top: 18px;
}

.state-panel {
  margin-top: 18px;
}

.control-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.control-row h2,
.section-head h2,
.hero-panel h2 {
  margin: 6px 0 0;
  color: var(--color-primary-dark);
  font-size: 22px;
  line-height: 1.35;
}

.eyebrow {
  margin: 0;
  color: var(--color-muted);
  font-size: 12px;
}

.muted-line {
  margin: 8px 0 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.7;
}

.record-picker {
  display: flex;
  gap: 10px;
}

.record-input {
  width: 180px;
}

.overview-grid,
.two-column {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.72fr);
  gap: 18px;
}

.hero-panel {
  color: #fff;
  background: linear-gradient(135deg, #0f172a, #155e75 58%, #f59e0b);
}

.hero-panel h2,
.hero-panel .eyebrow {
  color: #fff;
}

.hero-metrics,
.stage-grid,
.demand-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.hero-metrics div {
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.1);
}

.hero-metrics span,
.hero-metrics strong,
.demand-grid span,
.demand-grid strong {
  display: block;
}

.hero-metrics span {
  color: rgba(255, 255, 255, 0.75);
  font-size: 12px;
}

.hero-metrics strong {
  margin-top: 6px;
  font-size: 22px;
}

.snapshot-note,
.explain-copy {
  margin: 8px 0 0;
  color: var(--color-muted);
  line-height: 1.8;
}

.trace-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.trace-line span,
.demand-grid div {
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f9fafb;
}

.trace-line span,
.demand-grid span {
  color: var(--color-muted);
  font-size: 12px;
}

.demand-grid strong {
  margin-top: 5px;
  color: var(--color-primary-dark);
  font-size: 13px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.section-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.section-meta {
  color: var(--color-muted);
  font-size: 13px;
}

.pipeline-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.pipeline-detail-list {
  display: grid;
  gap: 12px;
}

.pipeline-step {
  min-height: 132px;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f9fafb;
}

.pipeline-step--detail {
  min-height: 0;
  background: #fff;
}

.pipeline-step__head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pipeline-step span {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: var(--color-primary-dark);
  font-size: 12px;
  font-weight: 700;
}

.pipeline-step strong {
  display: block;
  margin-top: 10px;
  color: var(--color-primary-dark);
}

.pipeline-step__head strong {
  margin-top: 0;
}

.pipeline-step p,
.constraint-row p,
.rule-row p {
  margin: 7px 0 0;
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.6;
}

.pipeline-description {
  font-size: 13px;
}

.pipeline-facts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.pipeline-facts div {
  padding: 10px;
  border-radius: var(--radius-sm);
  background: #f8fafc;
}

.pipeline-facts b {
  display: block;
  color: var(--color-primary-dark);
  font-size: 12px;
}

.constraint-list,
.rule-list {
  display: grid;
  gap: 10px;
}

.constraint-row {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
}

.constraint-row strong,
.rule-row span {
  color: var(--color-primary-dark);
}

.rule-row {
  padding: 11px 12px;
  border-left: 3px solid var(--color-accent);
  border-radius: var(--radius-sm);
  background: #f9fafb;
}

.feature-example {
  display: grid;
  gap: 16px;
}

.feature-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, #f8fafc, #ecfeff);
}

.feature-summary h3 {
  margin: 6px 0 0;
  color: var(--color-primary-dark);
  font-size: 22px;
}

.feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.feature-price span,
.feature-price strong {
  display: block;
  text-align: right;
}

.feature-price span {
  color: var(--color-muted);
  font-size: 12px;
}

.feature-price strong {
  margin-top: 6px;
  color: var(--color-primary);
  font-size: 26px;
}

.feature-param-grid,
.feature-score-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.feature-param-grid div,
.feature-score-card {
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
}

.feature-param-grid span,
.feature-score-card span {
  display: block;
  color: var(--color-muted);
  font-size: 12px;
}

.feature-param-grid strong,
.feature-score-card strong {
  display: block;
  margin-top: 6px;
  color: var(--color-primary-dark);
}

.score-track {
  height: 7px;
  overflow: hidden;
  margin-top: 10px;
  border-radius: 999px;
  background: #e5e7eb;
}

.score-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--color-accent), var(--color-warning));
}

.breakdown-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.breakdown-card {
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
}

.breakdown-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.breakdown-card__head span,
.breakdown-card__head small {
  color: var(--color-muted);
  font-size: 12px;
}

.breakdown-card__head h3 {
  margin: 5px 0 0;
  color: var(--color-primary);
  font-size: 24px;
}

.formula-text,
.breakdown-explain {
  margin: 10px 0 0;
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.7;
}

.matched-rule-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.matched-rule-list div {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 54px;
  gap: 6px 10px;
  padding: 10px;
  border-radius: var(--radius-sm);
  background: #f8fafc;
}

.matched-rule-list strong {
  color: var(--color-primary-dark);
  font-size: 12px;
}

.matched-rule-list b {
  color: var(--color-accent);
  font-size: 12px;
  text-align: right;
}

.matched-rule-list p {
  grid-column: 1 / -1;
  margin: 0;
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.6;
}

.weight-table {
  display: grid;
  gap: 10px;
}

.weight-table__head,
.weight-compare-row {
  display: grid;
  grid-template-columns: 86px repeat(3, minmax(0, 1fr));
  align-items: center;
  gap: 14px;
}

.weight-table__head {
  color: var(--color-muted);
  font-size: 12px;
}

.weight-compare-row {
  padding: 10px 0;
  border-top: 1px solid var(--color-border);
}

.mini-weight {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 56px;
  align-items: center;
  gap: 8px;
}

.mini-weight__track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #e5e7eb;
}

.mini-weight__track i {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.mini-weight strong {
  color: var(--color-primary-dark);
  font-size: 12px;
  text-align: right;
}

.stage-card {
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
}

.stage-card span,
.stage-card small {
  color: var(--color-muted);
  font-size: 12px;
}

.stage-card strong {
  display: block;
  margin: 8px 0;
  color: var(--color-primary-dark);
  font-size: 28px;
}

.stage-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #e5e7eb;
}

.stage-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--color-accent), var(--color-warning));
}

.matrix-table,
.ranking-table {
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.matrix-row {
  display: grid;
  grid-template-columns: 62px 190px repeat(9, 74px);
  min-width: 918px;
  border-top: 1px solid var(--color-border);
}

.matrix-row:first-child,
.ranking-row:first-child {
  border-top: 0;
}

.matrix-row span,
.matrix-row strong,
.matrix-row b,
.ranking-row span,
.ranking-row strong {
  min-height: 42px;
  padding: 10px 9px;
  display: flex;
  align-items: center;
  font-size: 12px;
}

.matrix-row--head,
.ranking-row--head {
  color: var(--color-muted);
  background: #f8fafc;
  font-weight: 700;
}

.heat-cell {
  justify-content: center;
  border-left: 1px solid rgba(255, 255, 255, 0.6);
}

.matrix-tip {
  margin-top: 12px;
}

.ranking-row {
  display: grid;
  grid-template-columns: 68px minmax(180px, 1.3fr) 100px 90px 92px 90px 90px 90px;
  min-width: 800px;
  align-items: center;
  border-top: 1px solid var(--color-border);
}

.ranking-row .el-tag {
  width: max-content;
  margin-left: 8px;
}

.explanation-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.explanation-card {
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
}

.explanation-card__head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.rank-badge {
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 700;
}

.explanation-card h3 {
  margin: 6px 0 0;
  color: var(--color-primary-dark);
  font-size: 17px;
}

.explanation-card__head > strong {
  color: var(--color-primary);
  font-size: 24px;
}

.tag-line,
.contribution-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 12px;
}

.reason-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.reason-grid div {
  padding: 12px;
  border-radius: var(--radius-sm);
  background: #f9fafb;
}

.reason-grid span,
.contribution-line span {
  color: var(--color-primary-dark);
  font-size: 12px;
  font-weight: 700;
}

.reason-grid p {
  margin: 6px 0 0;
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.7;
}

.contribution-line b {
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  background: #f3f4f6;
  color: var(--color-muted);
  font-size: 12px;
}

.source-alert {
  margin-top: 12px;
}

@media (max-width: 1080px) {
  .overview-grid,
  .two-column,
  .pipeline-grid,
  .pipeline-facts,
  .feature-param-grid,
  .feature-score-grid,
  .breakdown-list,
  .stage-grid,
  .explanation-grid,
  .reason-grid {
    grid-template-columns: 1fr;
  }

  .control-row,
  .section-head,
  .feature-summary {
    display: block;
  }

  .feature-price span,
  .feature-price strong {
    margin-top: 10px;
    text-align: left;
  }

  .record-picker,
  .section-actions {
    margin-top: 14px;
    justify-content: flex-start;
  }

  .record-input {
    width: 100%;
  }

  .weight-table__head,
  .weight-compare-row {
    grid-template-columns: 1fr;
  }
}
</style>
