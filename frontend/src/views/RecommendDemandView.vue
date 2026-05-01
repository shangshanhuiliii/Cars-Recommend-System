<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">购车需求</h1>
        <p class="page-subtitle">填写结构化需求后，系统会生成用户画像与权重，并立即基于真实车型评分生成推荐结果。</p>
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
              <el-form-item label="车型类型" prop="bodyType">
                <el-segmented v-model="form.bodyType" :options="bodyTypes" />
              </el-form-item>
              <el-form-item label="动力类型" prop="energyType">
                <el-segmented v-model="form.energyType" :options="demandEnergyTypes" />
              </el-form-item>
              <el-form-item label="最低座位数" prop="seats">
                <el-input-number v-model="form.seats" :min="2" :max="9" controls-position="right" />
              </el-form-item>
              <el-form-item label="推荐数量" prop="topK">
                <el-input-number v-model="form.topK" :min="1" :max="20" controls-position="right" />
              </el-form-item>
              <el-form-item class="grid-span-2" label="自然语言原文（可选）">
                <el-input
                  v-model="form.rawText"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                  placeholder="例如：家庭用车，预算 10-15 万，想要安全、空间大、能耗低。"
                />
              </el-form-item>
              <el-form-item class="grid-span-2" label="排除品牌（可选，逗号分隔）">
                <el-input v-model="form.excludedBrandsText" placeholder="例如：品牌A, 品牌B" />
              </el-form-item>
              <el-form-item class="grid-span-2" label="排除车型 ID（可选，逗号分隔）">
                <el-input v-model="form.excludedCarIdsText" placeholder="例如：3, 8, 12" />
              </el-form-item>
            </div>

            <el-form-item label="使用场景" prop="scene">
              <div class="scene-grid">
                <button
                  v-for="scene in sceneOptions"
                  :key="scene.value"
                  class="scene-card"
                  :class="{ 'scene-card--active': form.scene === scene.value }"
                  type="button"
                  @click="form.scene = scene.value"
                >
                  <span>{{ scene.value }}</span>
                  <small>{{ scene.description }}</small>
                </button>
              </div>
            </el-form-item>

            <el-form-item label="关注因素" prop="focusFactors">
              <el-checkbox-group v-model="form.focusFactors" class="focus-grid">
                <el-checkbox-button v-for="item in focusOptions" :key="item.value" :label="item.value">
                  {{ item.value }}
                </el-checkbox-button>
              </el-checkbox-group>
            </el-form-item>

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
          <h2>{{ selectedScene?.value }}</h2>
          <p>{{ selectedScene?.description }}</p>
          <div class="focus-summary">
            <span v-for="item in form.focusFactors" :key="item">{{ item }}</span>
          </div>
          <el-divider />
          <p class="aside-label">推荐规则提示</p>
          <ul class="rule-list">
            <li>预算上限、车型、动力和最低座位数会影响候选范围。</li>
            <li>预算下限只影响价格匹配分，不会过滤车型。</li>
            <li>关注因素只调整权重，不会变成硬性过滤条件。</li>
          </ul>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import { createUserDemand, generateRecommendation } from '@/api/recommend'
import { bodyTypes, demandEnergyTypes } from '@/constants/enums'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const submitStep = ref(0)
const submitError = ref('')

const sceneOptions = [
  { value: '城市通勤', description: '更关注价格、能耗和日常便利性' },
  { value: '家庭出行', description: '更关注空间、安全和舒适性' },
  { value: '长途自驾', description: '更关注舒适、安全和续航/能耗' },
  { value: '新手代步', description: '更关注价格、安全和辅助驾驶' },
  { value: '商务接待', description: '更关注舒适、口碑和体面感' },
  { value: '综合需求', description: '各维度较均衡' },
]

const focusOptions = ['价格', '空间', '安全', '能耗', '智能', '舒适', '动力', '口碑', '热度'].map((value) => ({
  value,
}))

const form = reactive(defaultForm())

const selectedScene = computed(() => sceneOptions.find((item) => item.value === form.scene))

const rules = {
  budgetMinWan: [{ validator: validateBudget, trigger: 'change' }],
  budgetMaxWan: [{ validator: validateBudget, trigger: 'change' }],
  bodyType: [{ required: true, message: '请选择车型类型', trigger: 'change' }],
  energyType: [{ required: true, message: '请选择动力类型', trigger: 'change' }],
  seats: [{ required: true, message: '请输入最低座位数', trigger: 'change' }],
  scene: [{ required: true, message: '请选择使用场景', trigger: 'change' }],
  focusFactors: [{ type: 'array', min: 1, message: '请至少选择一个关注因素', trigger: 'change' }],
}

function defaultForm() {
  return {
    rawText: '',
    budgetMinWan: 10,
    budgetMaxWan: 15,
    bodyType: 'SUV',
    energyType: '插混',
    seats: 5,
    scene: '家庭出行',
    focusFactors: ['空间', '安全'],
    excludedBrandsText: '',
    excludedCarIdsText: '',
    topK: 10,
  }
}

function validateBudget(rule, value, callback) {
  if (form.budgetMinWan != null && form.budgetMaxWan != null && form.budgetMinWan > form.budgetMaxWan) {
    callback(new Error('预算下限不能高于预算上限'))
    return
  }
  callback()
}

async function submitDemand() {
  submitError.value = ''
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请先修正表单校验错误')
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
      topK: form.topK,
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
    bodyType: form.bodyType,
    energyType: form.energyType,
    seats: form.seats,
    scene: form.scene,
    focusFactors: [...form.focusFactors],
    excludedBrands: splitText(form.excludedBrandsText),
    excludedCarIds: splitIds(form.excludedCarIdsText),
  }
}

function toYuan(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }
  return Number(value) * 10000
}

function splitText(value) {
  if (!value) return []
  return value
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function splitIds(value) {
  return splitText(value)
    .map((item) => Number(item))
    .filter((item) => Number.isInteger(item) && item > 0)
}

function resetForm() {
  Object.assign(form, defaultForm())
  submitError.value = ''
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.state-alert,
.process-panel {
  margin-bottom: 18px;
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
.form-grid :deep(.el-segmented) {
  width: 100%;
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

.focus-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
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
  .scene-grid {
    grid-template-columns: 1fr;
  }

  .grid-span-2 {
    grid-column: span 1;
  }
}
</style>
