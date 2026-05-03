<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">购车需求</h1>
        <p class="page-subtitle">填写结构化偏好后，系统会生成画像权重，并基于真实车型评分生成推荐结果。</p>
      </div>
    </div>

    <div class="panel parse-panel">
      <div class="panel__body">
        <div class="parse-head">
          <div>
            <p class="aside-label">自然语言辅助填写</p>
            <h2>一句话描述购车需求</h2>
            <p>系统只会解析成表单草稿，不会保存需求，也不会直接生成推荐。</p>
          </div>
          <el-button type="primary" :icon="Search" :loading="parseLoading" @click="parseNaturalLanguage">
            解析需求并填入表单
          </el-button>
        </div>
        <el-input
          v-model="form.rawText"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="例如：我想买10到15万的SUV，家用带娃，最好插混或增程，空间大、安全、省油。"
        />
        <el-alert
          v-if="parseError"
          class="parse-alert"
          type="error"
          :closable="false"
          :title="parseError"
          show-icon
        />
        <el-alert
          v-if="parseSuccess"
          class="parse-alert"
          type="success"
          :closable="false"
          title="解析结果已填入表单，请确认后再生成推荐"
          show-icon
        />
        <div v-if="parseResult" class="parse-result">
          <div class="parse-result__summary">
            <span v-for="item in parsedSummaryItems" :key="item.label">
              <strong>{{ item.label }}</strong>{{ item.value }}
            </span>
          </div>
          <p>{{ parseResult.profileText }}</p>
          <div class="parse-result__meta">
            <el-tag type="success" effect="plain">解析置信度 {{ parseResult.confidenceScore }}</el-tag>
            <el-tag v-if="parseResult.unsupportedTerms?.length" type="warning" effect="plain">
              需人工确认：{{ parseResult.unsupportedTerms.join('、') }}
            </el-tag>
            <el-tag v-if="parseResult.ambiguousTerms?.length" type="warning" effect="plain">
              表达较模糊：{{ parseResult.ambiguousTerms.join('、') }}
            </el-tag>
            <el-tag effect="plain">已填入下方结构化表单</el-tag>
          </div>
        </div>
      </div>
    </div>

    <el-alert
      v-if="submitError"
      class="state-alert"
      type="error"
      :closable="false"
      :title="submitError"
      show-icon
    />

    <el-alert
      v-if="optionLoadError"
      class="state-alert"
      type="warning"
      :closable="false"
      :title="optionLoadError"
      show-icon
    />

    <div v-if="submitting" class="panel process-panel">
      <div class="panel__body">
        <el-steps :active="submitStep" finish-status="success" process-status="process" align-center>
          <el-step title="保存需求" description="生成画像文本与权重" />
          <el-step title="计算匹配" description="执行过滤、加权评分与排序" />
          <el-step title="生成结果" description="保存推荐记录并跳转结果页" />
        </el-steps>
      </div>
    </div>

    <div class="demand-layout">
      <div class="panel">
        <div class="panel__body">
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <div class="form-grid">
              <el-form-item label="预算下限（万元）" prop="budgetMinWan">
                <el-input-number v-model="form.budgetMinWan" :min="0" :step="1" controls-position="right" />
              </el-form-item>
              <el-form-item label="预算上限（万元）" prop="budgetMaxWan">
                <el-input-number v-model="form.budgetMaxWan" :min="0" :step="1" controls-position="right" />
              </el-form-item>
              <el-form-item label="最低座位数" prop="minSeats">
                <el-input-number v-model="form.minSeats" :min="2" :max="9" controls-position="right" />
              </el-form-item>
              <el-form-item label="排除品牌">
                <el-select
                  v-model="form.excludedBrands"
                  multiple
                  filterable
                  clearable
                  :loading="brandLoading"
                  placeholder="搜索并选择已有品牌"
                >
                  <el-option v-for="brand in brandOptions" :key="brand" :label="brand" :value="brand" />
                </el-select>
              </el-form-item>
            </div>

            <section class="choice-section">
              <div class="section-head">
                <h2>可接受车型类型</h2>
                <span>可多选，留空表示不限车型类型</span>
              </div>
              <el-checkbox-group v-model="form.bodyTypes" class="button-grid">
                <el-checkbox-button v-for="item in bodyTypes" :key="item" :label="item">
                  {{ item }}
                </el-checkbox-button>
              </el-checkbox-group>
            </section>

            <section class="choice-section">
              <div class="section-head">
                <h2>可接受动力类型</h2>
                <span>“新能源”会在后端展开为纯电、插混和增程</span>
              </div>
              <el-checkbox-group v-model="form.energyTypes" class="button-grid">
                <el-checkbox-button v-for="item in demandEnergyTypes" :key="item" :label="item">
                  {{ item }}
                </el-checkbox-button>
              </el-checkbox-group>
            </section>

            <section class="choice-section">
              <div class="section-head">
                <h2>使用场景</h2>
                <span>可多选；全部滑块为 0 时，系统按场景模板生成默认权重</span>
              </div>
              <div class="scene-grid">
                <button
                  v-for="scene in sceneOptions"
                  :key="scene.value"
                  class="scene-card"
                  :class="{ 'scene-card--active': form.scenes.includes(scene.value) }"
                  type="button"
                  @click="toggleScene(scene.value)"
                >
                  <span>{{ scene.value }}</span>
                  <small>{{ scene.description }}</small>
                </button>
              </div>
            </section>

            <section class="choice-section">
              <div class="section-head">
                <h2>偏好权重</h2>
                <span>滑块越高表示越重视该因素，最终权重以后端归一化结果为准</span>
              </div>
              <div class="factor-grid">
                <div v-for="item in factorOptions" :key="item.key" class="factor-row">
                  <span>{{ item.label }}</span>
                  <el-slider v-model="form.factorWeights[item.key]" :min="0" :max="10" :step="1" />
                  <strong>{{ form.factorWeights[item.key] }}</strong>
                </div>
              </div>
            </section>

            <section class="choice-section">
              <div class="section-head">
                <h2>排除车型</h2>
                <span>仅能从数据库已有车型中搜索选择</span>
              </div>
              <el-select
                v-model="form.excludedCarIds"
                class="car-option-select"
                multiple
                filterable
                remote
                clearable
                reserve-keyword
                :remote-method="searchCarOptions"
                :loading="carOptionLoading"
                placeholder="输入品牌或车型名称搜索"
              >
                <el-option
                  v-for="car in carOptions"
                  :key="car.id"
                  :label="car.displayName"
                  :value="car.id"
                />
              </el-select>
            </section>

            <div class="form-actions">
              <el-button :icon="Refresh" @click="resetForm">重置</el-button>
              <el-button type="primary" :icon="ArrowRight" :loading="submitting" @click="submitDemand">
                生成推荐
              </el-button>
            </div>
          </el-form>
        </div>
      </div>

      <aside class="panel">
        <div class="panel__body demand-aside">
          <p class="aside-label">当前画像方向</p>
          <h2>{{ sceneSummary }}</h2>
          <p>{{ bodyEnergySummary }}</p>
          <div class="focus-summary">
            <span v-for="item in highWeightFactors" :key="item.key">{{ item.label }} {{ item.value }}</span>
          </div>
          <el-divider />
          <p class="aside-label">推荐规则提示</p>
          <ul class="rule-list">
            <li>预算上限、车型类型、动力类型和最低座位数会影响候选范围。</li>
            <li>预算下限只影响价格匹配分，不会过滤车型。</li>
            <li>偏好滑块只影响权重和排序，不会变成硬性过滤条件。</li>
          </ul>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Refresh, Search } from '@element-plus/icons-vue'

import { fetchCarBrands, fetchCarOptions } from '@/api/cars'
import { createUserDemand, generateRecommendation, parseDemandText } from '@/api/recommend'
import { bodyTypes, demandEnergyTypes } from '@/constants/enums'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const submitStep = ref(0)
const submitError = ref('')
const brandLoading = ref(false)
const carOptionLoading = ref(false)
const brandOptions = ref([])
const carOptions = ref([])
const parseLoading = ref(false)
const parseError = ref('')
const parseSuccess = ref(false)
const parseResult = ref(null)
const brandLoadError = ref('')
const carOptionLoadError = ref('')

const sceneOptions = [
  { value: '城市通勤', description: '更关注价格、能耗和日常便利性' },
  { value: '家庭出行', description: '更关注空间、安全和舒适性' },
  { value: '长途自驾', description: '更关注舒适、安全和续航/能耗' },
  { value: '新手代步', description: '更关注价格、安全和辅助驾驶' },
  { value: '商务接待', description: '更关注舒适、口碑和体面感' },
  { value: '综合需求', description: '各维度较均衡' },
]

const factorOptions = [
  ['price', '价格'],
  ['space', '空间'],
  ['safety', '安全'],
  ['energy', '能耗'],
  ['intelligence', '智能'],
  ['comfort', '舒适'],
  ['power', '动力'],
  ['reputation', '口碑'],
  ['popularity', '热度'],
].map(([key, label]) => ({ key, label }))

const form = reactive(defaultForm())

const sceneSummary = computed(() => (form.scenes.length ? form.scenes.join(' / ') : '综合需求'))

const bodyEnergySummary = computed(() => {
  const body = form.bodyTypes.length ? form.bodyTypes.join(' / ') : '车型不限'
  const energy = form.energyTypes.length ? form.energyTypes.join(' / ') : '动力不限'
  return `${body} · ${energy} · 最低 ${form.minSeats || '-'} 座`
})

const highWeightFactors = computed(() =>
  factorOptions
    .map((item) => ({ ...item, value: Number(form.factorWeights[item.key] || 0) }))
    .filter((item) => item.value > 0)
    .sort((a, b) => b.value - a.value)
    .slice(0, 5),
)

const parsedSummaryItems = computed(() => {
  if (!parseResult.value) {
    return []
  }
  return [
    { label: '预算', value: parsedBudgetText(parseResult.value.budgetMin, parseResult.value.budgetMax) },
    { label: '车型', value: arrayText(parseResult.value.bodyTypes, '不限') },
    { label: '动力', value: arrayText(parseResult.value.energyTypes, '不限') },
    { label: '场景', value: arrayText(parseResult.value.scenes, '综合需求') },
    { label: '座位', value: parseResult.value.minSeats ? `最低 ${parseResult.value.minSeats} 座` : '未识别' },
  ]
})

const optionLoadError = computed(() => brandLoadError.value || carOptionLoadError.value)

const rules = {
  budgetMinWan: [{ validator: validateBudget, trigger: 'change' }],
  budgetMaxWan: [{ validator: validateBudget, trigger: 'change' }],
  minSeats: [{ required: true, message: '请输入最低座位数', trigger: 'change' }],
}

onMounted(() => {
  loadBrands()
  searchCarOptions('')
})

function defaultForm() {
  return {
    rawText: '',
    budgetMinWan: 10,
    budgetMaxWan: 15,
    bodyTypes: ['SUV'],
    energyTypes: ['插混', '新能源'],
    minSeats: 5,
    scenes: ['家庭出行'],
    factorWeights: {
      price: 0,
      space: 8,
      safety: 8,
      energy: 0,
      intelligence: 0,
      comfort: 6,
      power: 0,
      reputation: 0,
      popularity: 0,
    },
    excludedBrands: [],
    excludedCarIds: [],
  }
}

async function loadBrands() {
  brandLoading.value = true
  brandLoadError.value = ''
  try {
    const response = await fetchCarBrands()
    brandOptions.value = response.data || []
  } catch (error) {
    brandLoadError.value = error?.response?.data?.message || error?.message || '品牌列表加载失败。'
  } finally {
    brandLoading.value = false
  }
}

async function searchCarOptions(keyword) {
  carOptionLoading.value = true
  carOptionLoadError.value = ''
  try {
    const response = await fetchCarOptions({ keyword, limit: 50 })
    carOptions.value = response.data || []
  } catch (error) {
    carOptionLoadError.value = error?.response?.data?.message || error?.message || '车型选项加载失败。'
  } finally {
    carOptionLoading.value = false
  }
}

function toggleScene(value) {
  const index = form.scenes.indexOf(value)
  if (index >= 0) {
    form.scenes.splice(index, 1)
    return
  }
  form.scenes.push(value)
}

function validateBudget(rule, value, callback) {
  if (form.budgetMinWan != null && form.budgetMaxWan != null && form.budgetMinWan > form.budgetMaxWan) {
    callback(new Error('预算下限不能高于预算上限'))
    return
  }
  callback()
}

async function parseNaturalLanguage() {
  parseError.value = ''
  parseSuccess.value = false
  const text = form.rawText?.trim()
  if (!text) {
    parseError.value = '请先输入一句购车需求'
    return
  }

  parseLoading.value = true
  try {
    const response = await parseDemandText({ text })
    parseResult.value = response.data
    applyParsedDemand(response.data)
    parseSuccess.value = true
  } catch (error) {
    parseError.value = error?.response?.data?.message || error?.message || '自然语言需求解析失败'
  } finally {
    parseLoading.value = false
  }
}

function applyParsedDemand(data) {
  if (!data) {
    return
  }
  form.rawText = data.rawText || form.rawText
  form.budgetMinWan = toWan(data.budgetMin)
  form.budgetMaxWan = toWan(data.budgetMax)
  form.bodyTypes = Array.isArray(data.bodyTypes) ? [...data.bodyTypes] : []
  form.energyTypes = Array.isArray(data.energyTypes) ? [...data.energyTypes] : []
  form.minSeats = data.minSeats ?? null
  form.scenes = Array.isArray(data.scenes) && data.scenes.length ? [...data.scenes] : ['综合需求']
  form.factorWeights = normalizeFactorWeights(data.factorWeights)
  form.excludedBrands = Array.isArray(data.excludedBrands) ? [...data.excludedBrands] : []
  form.excludedCarIds = Array.isArray(data.excludedCarIds) ? [...data.excludedCarIds] : []
  formRef.value?.clearValidate()
}

async function submitDemand() {
  submitError.value = ''
  try {
    await formRef.value.validate()
  } catch {
    submitError.value = '请先修正表单校验错误'
    return
  }

  submitting.value = true
  try {
    submitStep.value = 1
    const demandResponse = await createUserDemand(toDemandPayload())
    const demand = demandResponse.data

    submitStep.value = 2
    const recommendResponse = await generateRecommendation({
      demandId: demand.id,
    })

    submitStep.value = 3
    await router.push(`/recommend/result/${recommendResponse.data.recordId}`)
  } catch (error) {
    submitError.value = error?.response?.data?.message || error?.message || '推荐生成失败，请检查需求信息后重试。'
  } finally {
    submitting.value = false
  }
}

function toDemandPayload() {
  return {
    rawText: form.rawText?.trim() || null,
    budgetMin: toYuan(form.budgetMinWan),
    budgetMax: toYuan(form.budgetMaxWan),
    bodyTypes: [...form.bodyTypes],
    energyTypes: [...form.energyTypes],
    minSeats: form.minSeats,
    scenes: [...form.scenes],
    factorWeights: { ...form.factorWeights },
    excludedBrands: [...form.excludedBrands],
    excludedCarIds: [...form.excludedCarIds],
  }
}

function toYuan(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }
  return Number(value) * 10000
}

function toWan(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }
  return Number(value) / 10000
}

function normalizeFactorWeights(weights = {}) {
  return Object.fromEntries(
    factorOptions.map((item) => [item.key, Math.max(0, Math.min(10, Number(weights?.[item.key] || 0)))]),
  )
}

function parsedBudgetText(min, max) {
  if (min != null && max != null) {
    return `${formatWan(min)}-${formatWan(max)} 万`
  }
  if (max != null) {
    return `${formatWan(max)} 万以内`
  }
  if (min != null) {
    return `${formatWan(min)} 万以上`
  }
  return '未识别'
}

function formatWan(value) {
  const wan = Number(value) / 10000
  return Number.isInteger(wan) ? String(wan) : wan.toFixed(1)
}

function arrayText(values, fallback) {
  return Array.isArray(values) && values.length ? values.join(' / ') : fallback
}

function resetForm() {
  Object.assign(form, defaultForm())
  submitError.value = ''
  parseError.value = ''
  parseSuccess.value = false
  parseResult.value = null
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.state-alert,
.process-panel {
  margin-bottom: 18px;
}

.parse-panel {
  margin-bottom: 18px;
}

.parse-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
}

.parse-head h2 {
  margin: 4px 0 6px;
  color: var(--color-primary-dark);
  font-size: 20px;
}

.parse-head p {
  margin: 0;
  color: var(--color-muted);
  line-height: 1.7;
}

.parse-alert {
  margin-top: 12px;
}

.parse-result {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid rgba(8, 145, 178, 0.24);
  border-radius: var(--radius-sm);
  background: rgba(8, 145, 178, 0.06);
}

.parse-result p {
  margin: 10px 0 0;
  color: var(--color-muted);
  line-height: 1.7;
}

.parse-result__summary,
.parse-result__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.parse-result__summary span {
  padding: 6px 9px;
  border-radius: var(--radius-sm);
  background: #ffffff;
  color: var(--color-muted);
  font-size: 12px;
}

.parse-result__summary strong {
  margin-right: 6px;
  color: var(--color-primary-dark);
}

.parse-result__meta {
  margin-top: 12px;
}

.demand-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 18px;
}

.grid-span-2 {
  grid-column: span 2;
}

.form-grid :deep(.el-input-number),
.form-grid :deep(.el-select),
.car-option-select {
  width: 100%;
}

.choice-section {
  margin-top: 20px;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-head h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 16px;
}

.section-head span {
  color: var(--color-muted);
  font-size: 12px;
}

.button-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.scene-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.scene-card {
  min-height: 96px;
  padding: 14px;
  text-align: left;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.scene-card span,
.scene-card small {
  display: block;
}

.scene-card span {
  margin-bottom: 8px;
  color: var(--color-primary-dark);
  font-size: 15px;
  font-weight: 700;
}

.scene-card small {
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.6;
}

.scene-card--active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.factor-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 18px;
}

.factor-row {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) 24px;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f9fafb;
}

.factor-row span {
  color: var(--color-primary-dark);
  font-size: 13px;
  font-weight: 700;
}

.factor-row strong {
  text-align: right;
  color: var(--color-accent);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.demand-aside h2 {
  margin: 6px 0 8px;
  color: var(--color-primary-dark);
  font-size: 22px;
}

.demand-aside p {
  margin: 0;
  color: var(--color-muted);
  line-height: 1.7;
}

.aside-label {
  color: var(--color-muted);
  font-size: 12px;
}

.focus-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.focus-summary span {
  padding: 5px 9px;
  border-radius: var(--radius-sm);
  background: rgba(8, 145, 178, 0.1);
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 600;
}

.rule-list {
  margin: 10px 0 0;
  padding-left: 18px;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.8;
}

@media (max-width: 980px) {
  .demand-layout,
  .form-grid,
  .scene-grid,
  .factor-grid {
    grid-template-columns: 1fr;
  }

  .grid-span-2 {
    grid-column: span 1;
  }

  .section-head {
    display: block;
  }
}
</style>
