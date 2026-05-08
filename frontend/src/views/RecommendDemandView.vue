<template>
  <section class="demand-page">
    <div class="demand-hero">
      <div>
        <p class="eyebrow">购车推荐</p>
        <h1>填写你的购车需求</h1>
        <p>选择预算、品牌、车型、动力、座位和使用场景，我们会基于你的偏好生成车型推荐。</p>
      </div>
      <el-button type="primary" size="large" :icon="ArrowRight" :loading="submitting" @click="submitDemand">
        生成推荐
      </el-button>
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
          <el-step title="保存需求" description="整理你的购车偏好" />
          <el-step title="匹配车型" description="筛选合适候选车型" />
          <el-step title="生成结果" description="准备推荐理由和车型列表" />
        </el-steps>
      </div>
    </div>

    <el-form ref="formRef" :model="form" label-position="top">
      <div class="demand-form">
        <section class="demand-card demand-card--wide">
          <div class="section-head">
            <div>
              <span>01</span>
              <h2>预算范围</h2>
            </div>
            <p>未选择则不限预算。</p>
          </div>
          <div class="pill-group">
            <button
              v-for="item in budgetOptions"
              :key="item.value"
              class="choice-pill"
              :class="{ 'choice-pill--active': budgetMode === item.value }"
              type="button"
              @click="chooseBudget(item.value)"
            >
              {{ item.label }}
            </button>
          </div>
          <p class="selection-line">{{ selectedBudgetLabel }}</p>
        </section>

        <section class="demand-card">
          <div class="section-head">
            <div>
              <span>02</span>
              <h2>品牌</h2>
            </div>
            <p>不选表示全部品牌。</p>
          </div>
          <el-select
            v-model="form.brands"
            class="brand-select"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            :loading="brandLoading"
            placeholder="选择品牌"
          >
            <el-option v-for="brand in brandOptions" :key="brand" :label="brand" :value="brand" />
          </el-select>
        </section>

        <section class="demand-card">
          <div class="section-head">
            <div>
              <span>03</span>
              <h2>车型级别</h2>
            </div>
            <p>可多选，留空表示全部。</p>
          </div>
          <div class="pill-group">
            <button
              v-for="item in bodyTypes"
              :key="item"
              class="choice-pill"
              :class="{ 'choice-pill--active': form.bodyTypes.includes(item) }"
              type="button"
              @click="toggleArrayValue(form.bodyTypes, item)"
            >
              {{ item }}
            </button>
          </div>
        </section>

        <section class="demand-card">
          <div class="section-head">
            <div>
              <span>04</span>
              <h2>动力类型</h2>
            </div>
            <p>不选表示全部动力。</p>
          </div>
          <div class="pill-group">
            <button
              v-for="item in demandEnergyTypes"
              :key="item"
              class="choice-pill"
              :class="{ 'choice-pill--active': form.energyTypes.includes(item) }"
              type="button"
              @click="toggleArrayValue(form.energyTypes, item)"
            >
              {{ item }}
            </button>
          </div>
        </section>

        <section class="demand-card">
          <div class="section-head">
            <div>
              <span>05</span>
              <h2>座位数</h2>
            </div>
            <p>不选表示全部座位。</p>
          </div>
          <div class="pill-group">
            <button class="choice-pill" type="button" @click="form.seatOptions = []">全部</button>
            <button
              v-for="item in seatOptions"
              :key="item.value"
              class="choice-pill"
              :class="{ 'choice-pill--active': form.seatOptions.includes(item.value) }"
              type="button"
              @click="toggleArrayValue(form.seatOptions, item.value)"
            >
              {{ item.label }}
            </button>
          </div>
        </section>

        <section class="demand-card demand-card--wide">
          <div class="section-head">
            <div>
              <span>06</span>
              <h2>使用场景</h2>
            </div>
            <p>可多选，留空时按综合需求处理。</p>
          </div>
          <div class="scene-grid">
            <button
              v-for="scene in sceneOptions"
              :key="scene.value"
              class="scene-card"
              :class="{ 'scene-card--active': form.scenes.includes(scene.value) }"
              type="button"
              @click="toggleArrayValue(form.scenes, scene.value)"
            >
              <strong>{{ scene.value }}</strong>
              <span>{{ scene.description }}</span>
            </button>
          </div>
        </section>

        <section class="demand-card demand-card--wide">
          <div class="section-head">
            <div>
              <span>07</span>
              <h2>偏好重点</h2>
            </div>
            <p>默认全为 0；越高表示越关注。</p>
          </div>
          <div class="factor-grid">
            <article v-for="item in factorOptions" :key="item.key" class="factor-card">
              <div class="factor-card__head">
                <strong>{{ item.label }}</strong>
                <span>{{ form.factorWeights[item.key] }}</span>
              </div>
              <el-slider v-model="form.factorWeights[item.key]" :min="0" :max="10" :step="1" />
            </article>
          </div>
        </section>
      </div>

      <div class="form-actions">
        <el-button :icon="Refresh" @click="resetForm">重置</el-button>
        <el-button type="primary" :icon="ArrowRight" :loading="submitting" @click="submitDemand">
          生成推荐
        </el-button>
      </div>
    </el-form>

    <el-dialog v-model="customBudgetDialogVisible" title="自定义预算" width="520px" class="budget-dialog">
      <div class="custom-budget">
        <p>{{ customBudgetLabel(customBudgetRange) }}</p>
        <el-slider
          v-model="customBudgetRange"
          range
          :min="0"
          :max="105"
          :step="5"
          show-stops
          :marks="budgetMarks"
          :format-tooltip="budgetTooltip"
          @input="normalizeCustomBudgetRange"
        />
      </div>
      <template #footer>
        <el-button @click="customBudgetDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCustomBudget">确定</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Refresh } from '@element-plus/icons-vue'

import { fetchCarBrands } from '@/api/cars'
import { createUserDemand, generateRecommendation } from '@/api/recommend'
import { bodyTypes, demandEnergyTypes } from '@/constants/enums'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const submitStep = ref(0)
const submitError = ref('')
const brandLoading = ref(false)
const brandLoadError = ref('')
const brandOptions = ref([])
const budgetMode = ref('')
const customBudgetDialogVisible = ref(false)
const customBudgetRange = ref([0, 10])
const appliedCustomBudgetRange = ref([0, 10])

const budgetOptions = [
  { value: 'under10', label: '10万以下', min: 0, max: 100000 },
  { value: '10to15', label: '10-15万', min: 100000, max: 150000 },
  { value: '15to20', label: '15-20万', min: 150000, max: 200000 },
  { value: '20to25', label: '20-25万', min: 200000, max: 250000 },
  { value: 'custom', label: '自定义' },
]

const budgetMarks = {
  0: '0',
  20: '20',
  40: '40',
  60: '60',
  80: '80',
  100: '100',
  105: '不限',
}

const seatOptions = [
  { value: '2', label: '2座' },
  { value: '4', label: '4座' },
  { value: '5', label: '5座' },
  { value: '6', label: '6座' },
  { value: '7', label: '7座' },
  { value: '7_PLUS', label: '7座以上' },
]

const sceneOptions = [
  { value: '城市通勤', description: '日常上下班，关注能耗和便利性' },
  { value: '家庭出行', description: '一家人乘坐，关注空间和安全' },
  { value: '长途自驾', description: '高速和远途，关注舒适和续航' },
  { value: '新手代步', description: '好开易用，关注安全辅助' },
  { value: '商务接待', description: '稳重体面，关注舒适和口碑' },
  { value: '接送孩子', description: '短途高频，关注安全和空间' },
  { value: '露营旅行', description: '装载和通过性更重要' },
  { value: '年轻运动', description: '关注动力、操控和个性' },
  { value: '豪华舒适', description: '关注乘坐质感和配置' },
  { value: '低成本通勤', description: '关注购车成本和能耗' },
  { value: '科技智能', description: '关注车机和辅助驾驶' },
  { value: '综合需求', description: '各方面表现更均衡' },
]

const factorOptions = [
  ['price', '价格敏感'],
  ['space', '空间宽敞'],
  ['safety', '安全配置'],
  ['energy', '能耗经济'],
  ['intelligence', '智能科技'],
  ['comfort', '舒适体验'],
  ['power', '动力表现'],
  ['reputation', '口碑质量'],
  ['popularity', '热度关注'],
].map(([key, label]) => ({ key, label }))

const form = reactive(defaultForm())

const optionLoadError = computed(() => brandLoadError.value)
const selectedBudgetLabel = computed(() => {
  if (!budgetMode.value) return '当前未限定预算'
  if (budgetMode.value === 'custom') return `当前预算：${customBudgetLabel(appliedCustomBudgetRange.value)}`
  return `当前预算：${budgetOptions.find((item) => item.value === budgetMode.value)?.label || '不限'}`
})

onMounted(loadBrands)

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

function defaultForm() {
  return {
    brands: [],
    bodyTypes: [],
    energyTypes: [],
    seatOptions: [],
    scenes: [],
    factorWeights: Object.fromEntries(factorOptions.map((item) => [item.key, 0])),
  }
}

function chooseBudget(value) {
  if (value === 'custom') {
    customBudgetRange.value = [...appliedCustomBudgetRange.value]
    customBudgetDialogVisible.value = true
    return
  }
  budgetMode.value = budgetMode.value === value ? '' : value
}

function confirmCustomBudget() {
  normalizeCustomBudgetRange()
  appliedCustomBudgetRange.value = [...customBudgetRange.value]
  budgetMode.value = 'custom'
  customBudgetDialogVisible.value = false
}

function normalizeCustomBudgetRange() {
  const [lower, upper] = customBudgetRange.value
  if (lower > upper) {
    customBudgetRange.value = [upper, lower]
  }
}

function budgetTooltip(value) {
  return value === 105 ? '不限' : `${value}万`
}

function customBudgetLabel(range) {
  const [lower, upper] = range
  if (lower === 0 && upper === 10) return '10万以下'
  if (upper === 105) return `${lower}万以上`
  return `${lower}-${upper}万`
}

function toggleArrayValue(values, value) {
  const index = values.indexOf(value)
  if (index >= 0) {
    values.splice(index, 1)
    return
  }
  values.push(value)
}

async function submitDemand() {
  submitError.value = ''
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
  const budget = selectedBudgetRange()
  return {
    rawText: null,
    budgetMin: budget.min,
    budgetMax: budget.max,
    brands: [...form.brands],
    bodyTypes: [...form.bodyTypes],
    energyTypes: [...form.energyTypes],
    seatOptions: [...form.seatOptions],
    scenes: [...form.scenes],
    factorWeights: { ...form.factorWeights },
  }
}

function selectedBudgetRange() {
  if (!budgetMode.value) return { min: null, max: null }
  if (budgetMode.value === 'custom') {
    const [lower, upper] = appliedCustomBudgetRange.value
    return {
      min: lower * 10000,
      max: upper === 105 ? null : upper * 10000,
    }
  }
  const selected = budgetOptions.find((item) => item.value === budgetMode.value)
  return {
    min: selected?.min ?? null,
    max: selected?.max ?? null,
  }
}

function resetForm() {
  Object.assign(form, defaultForm())
  budgetMode.value = ''
  customBudgetRange.value = [0, 10]
  appliedCustomBudgetRange.value = [0, 10]
  submitError.value = ''
  formRef.value?.clearValidate?.()
}
</script>

<style scoped>
.demand-page {
  display: grid;
  gap: 24px;
}

.demand-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 42px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 28px;
  background:
    radial-gradient(circle at 10% 0%, rgba(37, 99, 235, 0.14), transparent 34%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(240, 249, 255, 0.88));
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.08);
}

.eyebrow {
  margin: 0 0 12px;
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 700;
}

.demand-hero h1 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: clamp(34px, 5vw, 56px);
  line-height: 1.08;
  letter-spacing: 0;
}

.demand-hero p:not(.eyebrow) {
  max-width: 700px;
  margin: 18px 0 0;
  color: var(--color-muted);
  font-size: 16px;
  line-height: 1.8;
}

.state-alert,
.process-panel {
  margin-bottom: 0;
}

.demand-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.demand-card {
  min-height: 220px;
  padding: 24px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.06);
}

.demand-card--wide {
  grid-column: 1 / -1;
  min-height: auto;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.section-head div {
  display: grid;
  gap: 6px;
}

.section-head span {
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 700;
}

.section-head h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 21px;
}

.section-head p {
  max-width: 260px;
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.7;
  text-align: right;
}

.pill-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.choice-pill {
  min-height: 40px;
  padding: 0 16px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: #fff;
  color: var(--color-primary-dark);
  font-weight: 700;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, color 0.18s ease;
}

.choice-pill--active {
  border-color: rgba(37, 99, 235, 0.52);
  color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
}

.selection-line {
  margin: 16px 0 0;
  color: var(--color-muted);
  font-size: 13px;
}

.brand-select {
  width: 100%;
}

.scene-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.scene-card {
  min-height: 112px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.scene-card:hover {
  transform: translateY(-1px);
}

.scene-card strong,
.scene-card span {
  display: block;
}

.scene-card strong {
  color: var(--color-primary-dark);
  font-size: 15px;
}

.scene-card span {
  margin-top: 8px;
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.6;
}

.scene-card--active {
  border-color: rgba(37, 99, 235, 0.5);
  box-shadow: 0 12px 30px rgba(37, 99, 235, 0.12);
}

.factor-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.factor-card {
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  background: #fbfdff;
}

.factor-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.factor-card__head strong {
  color: var(--color-primary-dark);
  font-size: 14px;
}

.factor-card__head span {
  color: var(--color-accent);
  font-weight: 700;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.custom-budget {
  padding: 4px 8px 28px;
}

.custom-budget p {
  margin: 0 0 22px;
  color: var(--color-primary-dark);
  font-size: 18px;
  font-weight: 700;
}

@media (max-width: 980px) {
  .demand-hero {
    display: grid;
    padding: 30px;
  }

  .demand-form,
  .scene-grid,
  .factor-grid {
    grid-template-columns: 1fr;
  }

  .section-head {
    display: grid;
  }

  .section-head p {
    max-width: none;
    text-align: left;
  }
}
</style>
