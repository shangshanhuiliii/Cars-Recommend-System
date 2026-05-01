<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">车型管理</h1>
        <p class="page-subtitle">维护车型基础信息、参数和特征评分，确保推荐算法读取到可信的数据基础。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" :loading="recalculatingAll" @click="recalculateAllScores">全部评分重算</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增车型</el-button>
      </div>
    </div>

    <div class="panel admin-car-panel">
      <div class="panel__body">
        <el-form class="car-filter" :inline="true" :model="query">
          <el-form-item label="关键词">
            <el-input v-model="query.keyword" clearable placeholder="品牌 / 车系 / 车型" />
          </el-form-item>
          <el-form-item label="车型">
            <el-select v-model="query.bodyType" clearable placeholder="全部" style="width: 120px">
              <el-option v-for="item in bodyTypes" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="动力">
            <el-select v-model="query.energyType" clearable placeholder="全部" style="width: 120px">
              <el-option v-for="item in carEnergyTypes" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="searchCars">查询</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="cars" class="car-table" row-key="id">
          <el-table-column prop="brand" label="品牌" width="110" />
          <el-table-column prop="series" label="车系" width="130" />
          <el-table-column prop="modelName" label="车型名称" min-width="220" show-overflow-tooltip />
          <el-table-column prop="guidePrice" label="指导价" width="120">
            <template #default="{ row }">{{ formatPrice(row.guidePrice) }}</template>
          </el-table-column>
          <el-table-column prop="bodyType" label="车型" width="90" />
          <el-table-column prop="energyType" label="动力" width="90" />
          <el-table-column prop="seats" label="座位" width="80" />
          <el-table-column prop="auditStatus" label="审核" width="120">
            <template #default="{ row }">
              <el-tag :type="auditTagType(row.auditStatus)" effect="light">{{ row.auditStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="评分状态" width="150">
            <template #default="{ row }">
              <div class="score-cell">
                <el-tag v-if="scoreMap[row.id]" type="success" effect="light">已评分</el-tag>
                <el-tag v-else type="warning" effect="light">待评分</el-tag>
                <span v-if="scoreMap[row.id]">空间 {{ formatScore(scoreMap[row.id].spaceScore) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="维护" width="330" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="Edit" @click="openEditDialog(row)">基础信息</el-button>
              <el-button size="small" :icon="Setting" @click="openParamDialog(row)">参数</el-button>
              <el-button size="small" @click="openScoreDialog(row)">评分</el-button>
              <el-button size="small" :loading="recalculatingCarId === row.id" @click="recalculateCarScore(row)">重算</el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="confirmDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadCars"
            @size-change="searchCars"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="carDialogVisible" :title="carDialogTitle" width="680px">
      <el-form ref="carFormRef" :model="carForm" :rules="carRules" label-width="92px">
        <div class="form-grid">
          <el-form-item label="品牌" prop="brand">
            <el-input v-model="carForm.brand" />
          </el-form-item>
          <el-form-item label="车系" prop="series">
            <el-input v-model="carForm.series" />
          </el-form-item>
          <el-form-item label="车型名称" prop="modelName" class="grid-span-2">
            <el-input v-model="carForm.modelName" />
          </el-form-item>
          <el-form-item label="指导价" prop="guidePrice">
            <el-input-number v-model="carForm.guidePrice" :min="1" :step="1000" controls-position="right" />
          </el-form-item>
          <el-form-item label="车型" prop="bodyType">
            <el-select v-model="carForm.bodyType">
              <el-option v-for="item in bodyTypes" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="动力" prop="energyType">
            <el-select v-model="carForm.energyType">
              <el-option v-for="item in carEnergyTypes" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="座位数" prop="seats">
            <el-input-number v-model="carForm.seats" :min="2" :max="9" controls-position="right" />
          </el-form-item>
          <el-form-item label="上市年份" prop="launchYear">
            <el-input-number v-model="carForm.launchYear" :min="1990" :max="2100" controls-position="right" />
          </el-form-item>
          <el-form-item label="销量" prop="salesVolume">
            <el-input-number v-model="carForm.salesVolume" :min="0" :step="100" controls-position="right" />
          </el-form-item>
          <el-form-item label="口碑" prop="userRating">
            <el-input-number v-model="carForm.userRating" :min="0" :max="5" :step="0.1" controls-position="right" />
          </el-form-item>
          <el-form-item label="审核" prop="auditStatus">
            <el-select v-model="carForm.auditStatus">
              <el-option v-for="item in auditStatuses" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="图片地址" prop="imageUrl" class="grid-span-2">
            <el-input v-model="carForm.imageUrl" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="carDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingCar" @click="submitCar">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="paramDialogVisible" :title="paramDialogTitle" width="760px">
      <el-form ref="paramFormRef" :model="paramForm" :rules="paramRules" label-width="118px">
        <div class="form-grid param-grid">
          <el-form-item label="车长 mm" prop="lengthMm">
            <el-input-number v-model="paramForm.lengthMm" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="车宽 mm" prop="widthMm">
            <el-input-number v-model="paramForm.widthMm" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="车高 mm" prop="heightMm">
            <el-input-number v-model="paramForm.heightMm" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="轴距 mm" prop="wheelbaseMm">
            <el-input-number v-model="paramForm.wheelbaseMm" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="油耗" prop="fuelConsumption">
            <el-input-number v-model="paramForm.fuelConsumption" :min="0" :step="0.1" controls-position="right" />
          </el-form-item>
          <el-form-item label="电耗" prop="electricConsumption">
            <el-input-number v-model="paramForm.electricConsumption" :min="0" :step="0.1" controls-position="right" />
          </el-form-item>
          <el-form-item label="纯电续航" prop="electricRangeKm">
            <el-input-number v-model="paramForm.electricRangeKm" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="综合续航" prop="totalRangeKm">
            <el-input-number v-model="paramForm.totalRangeKm" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="百公里加速" prop="acceleration100">
            <el-input-number v-model="paramForm.acceleration100" :min="0" :step="0.1" controls-position="right" />
          </el-form-item>
          <el-form-item label="气囊数量" prop="airbagCount">
            <el-input-number v-model="paramForm.airbagCount" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="屏幕尺寸" prop="screenSize">
            <el-input-number v-model="paramForm.screenSize" :min="0" :step="0.1" controls-position="right" />
          </el-form-item>
          <el-form-item label="辅助驾驶" prop="assistDriveLevel">
            <el-select v-model="paramForm.assistDriveLevel" clearable>
              <el-option label="NONE" value="NONE" />
              <el-option label="L1" value="L1" />
              <el-option label="L2" value="L2" />
            </el-select>
          </el-form-item>
        </div>
        <div class="feature-switches">
          <el-checkbox v-model="paramForm.hasAbs">ABS</el-checkbox>
          <el-checkbox v-model="paramForm.hasEsp">ESP</el-checkbox>
          <el-checkbox v-model="paramForm.hasActiveBrake">主动刹车</el-checkbox>
          <el-checkbox v-model="paramForm.hasLaneKeep">车道保持</el-checkbox>
          <el-checkbox v-model="paramForm.hasAdaptiveCruise">自适应巡航</el-checkbox>
          <el-checkbox v-model="paramForm.hasBlindSpot">并线辅助</el-checkbox>
          <el-checkbox v-model="paramForm.hasReverseCamera">倒车影像</el-checkbox>
          <el-checkbox v-model="paramForm.has360Camera">360 全景</el-checkbox>
          <el-checkbox v-model="paramForm.hasOta">OTA</el-checkbox>
          <el-checkbox v-model="paramForm.hasVoiceControl">语音交互</el-checkbox>
          <el-checkbox v-model="paramForm.hasAutoParking">自动泊车</el-checkbox>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="paramDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingParam" @click="submitParam">保存参数</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="scoreDialogVisible" :title="scoreDialogTitle" width="720px">
      <div v-loading="scoreLoading">
        <template v-if="currentScore">
          <div class="score-overview">
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
          </div>
          <p class="score-meta">
            评分版本：{{ currentScore.scoreVersion }} · 计算时间：{{ formatDate(currentScore.calculatedTime) }}
          </p>
        </template>
        <el-empty v-else description="该车型暂无评分">
          <el-button
            v-if="currentScoreCar"
            type="primary"
            :loading="recalculatingCarId === currentScoreCar.id"
            @click="recalculateCarScore(currentScoreCar)"
          >
            立即重算评分
          </el-button>
        </el-empty>
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Plus, Refresh, Search, Setting } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  createAdminCar,
  deleteAdminCar,
  fetchAdminCarParam,
  fetchAdminCars,
  fetchAdminCarScore,
  recalculateAdminCarScore,
  recalculateAllAdminCarScores,
  saveAdminCarParam,
  updateAdminCar,
} from '@/api/adminCars'
import { bodyTypes, carEnergyTypes } from '@/constants/enums'

const auditStatuses = ['APPROVED', 'PENDING', 'REJECTED']

const loading = ref(false)
const cars = ref([])
const total = ref(0)
const scoreMap = ref({})
const recalculatingAll = ref(false)
const recalculatingCarId = ref(null)
const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  bodyType: '',
  energyType: '',
})

const carDialogVisible = ref(false)
const carFormRef = ref()
const savingCar = ref(false)
const editingCarId = ref(null)
const carForm = reactive(defaultCarForm())

const paramDialogVisible = ref(false)
const paramFormRef = ref()
const savingParam = ref(false)
const currentParamCar = ref(null)
const paramForm = reactive(defaultParamForm())

const scoreDialogVisible = ref(false)
const scoreLoading = ref(false)
const currentScoreCar = ref(null)
const currentScore = ref(null)

const carDialogTitle = computed(() => (editingCarId.value ? '编辑车型基础信息' : '新增车型'))
const paramDialogTitle = computed(() => (currentParamCar.value ? `${currentParamCar.value.modelName} 参数维护` : '参数维护'))
const scoreDialogTitle = computed(() => (currentScoreCar.value ? `${currentScoreCar.value.modelName} 特征评分` : '特征评分'))
const scoreRows = computed(() => {
  const score = currentScore.value || {}
  return [
    ['spaceScore', '空间'],
    ['safetyScore', '安全'],
    ['energyScore', '能耗'],
    ['intelligenceScore', '智能'],
    ['comfortScore', '舒适'],
    ['powerScore', '动力'],
    ['reputationScore', '口碑'],
    ['popularityScore', '热度'],
  ].map(([key, label]) => ({ key, label, value: Number(score[key] || 0) }))
})

const carRules = {
  brand: [{ required: true, message: '请输入品牌', trigger: 'blur' }],
  series: [{ required: true, message: '请输入车系', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入车型名称', trigger: 'blur' }],
  guidePrice: [{ required: true, message: '请输入指导价', trigger: 'change' }],
  bodyType: [{ required: true, message: '请选择车型', trigger: 'change' }],
  energyType: [{ required: true, message: '请选择动力', trigger: 'change' }],
  seats: [{ required: true, message: '请输入座位数', trigger: 'change' }],
  salesVolume: [{ required: true, message: '请输入销量', trigger: 'change' }],
  userRating: [{ required: true, message: '请输入口碑评分', trigger: 'change' }],
  auditStatus: [{ required: true, message: '请选择审核状态', trigger: 'change' }],
}

const paramRules = {
  lengthMm: [{ required: true, message: '请输入车长', trigger: 'change' }],
  widthMm: [{ required: true, message: '请输入车宽', trigger: 'change' }],
  heightMm: [{ required: true, message: '请输入车高', trigger: 'change' }],
  wheelbaseMm: [{ required: true, message: '请输入轴距', trigger: 'change' }],
  airbagCount: [{ required: true, message: '请输入气囊数量', trigger: 'change' }],
}

onMounted(loadCars)

async function loadCars() {
  loading.value = true
  try {
    const response = await fetchAdminCars({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      bodyType: query.bodyType || undefined,
      energyType: query.energyType || undefined,
    })
    cars.value = response.data.records
    total.value = response.data.total
    await loadScoresForCurrentPage()
  } finally {
    loading.value = false
  }
}

async function loadScoresForCurrentPage() {
  const entries = await Promise.all(
    cars.value.map(async (car) => {
      try {
        const response = await fetchAdminCarScore(car.id)
        return [car.id, response.data]
      } catch (error) {
        if (error?.response?.status !== 404) {
          console.warn('load car score failed', car.id, error)
        }
        return [car.id, null]
      }
    }),
  )
  scoreMap.value = Object.fromEntries(entries)
}

function searchCars() {
  query.page = 1
  loadCars()
}

function resetQuery() {
  query.keyword = ''
  query.bodyType = ''
  query.energyType = ''
  searchCars()
}

function openCreateDialog() {
  editingCarId.value = null
  Object.assign(carForm, defaultCarForm())
  carDialogVisible.value = true
}

function openEditDialog(row) {
  editingCarId.value = row.id
  Object.assign(carForm, {
    brand: row.brand,
    series: row.series,
    modelName: row.modelName,
    guidePrice: Number(row.guidePrice),
    bodyType: row.bodyType,
    energyType: row.energyType,
    seats: row.seats,
    launchYear: row.launchYear,
    imageUrl: row.imageUrl || '',
    salesVolume: row.salesVolume,
    userRating: Number(row.userRating),
    auditStatus: row.auditStatus,
  })
  carDialogVisible.value = true
}

async function submitCar() {
  await carFormRef.value.validate()
  savingCar.value = true
  try {
    if (editingCarId.value) {
      await updateAdminCar(editingCarId.value, { ...carForm })
    } else {
      await createAdminCar({ ...carForm })
    }
    ElMessage.success('车型已保存')
    carDialogVisible.value = false
    loadCars()
  } finally {
    savingCar.value = false
  }
}

async function confirmDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除 ${row.modelName}？`, '删除车型', { type: 'warning' })
  } catch {
    return
  }
  await deleteAdminCar(row.id)
  ElMessage.success('车型已删除')
  loadCars()
}

async function openParamDialog(row) {
  currentParamCar.value = row
  Object.assign(paramForm, defaultParamForm(row.id))
  try {
    const response = await fetchAdminCarParam(row.id)
    Object.assign(paramForm, normalizeParam(response.data))
  } catch (error) {
    if (error?.response?.data?.code !== 404) {
      throw error
    }
  }
  paramDialogVisible.value = true
}

async function submitParam() {
  await paramFormRef.value.validate()
  savingParam.value = true
  try {
    await saveAdminCarParam(currentParamCar.value.id, { ...paramForm, carId: currentParamCar.value.id })
    ElMessage.success('车型参数已保存')
    paramDialogVisible.value = false
  } finally {
    savingParam.value = false
  }
}

async function openScoreDialog(row) {
  currentScoreCar.value = row
  scoreDialogVisible.value = true
  scoreLoading.value = true
  try {
    const response = await fetchAdminCarScore(row.id)
    currentScore.value = response.data
  } catch (error) {
    if (error?.response?.status !== 404) {
      throw error
    }
    currentScore.value = null
  } finally {
    scoreLoading.value = false
  }
}

async function recalculateCarScore(row) {
  recalculatingCarId.value = row.id
  try {
    const response = await recalculateAdminCarScore(row.id)
    scoreMap.value = { ...scoreMap.value, [row.id]: response.data }
    if (currentScoreCar.value?.id === row.id) {
      currentScore.value = response.data
    }
    ElMessage.success('单车评分已重算')
  } finally {
    recalculatingCarId.value = null
  }
}

async function recalculateAllScores() {
  recalculatingAll.value = true
  try {
    const response = await recalculateAllAdminCarScores()
    ElMessage.success(`已重算 ${response.data.recalculatedCount} 台车型评分`)
    await loadScoresForCurrentPage()
  } finally {
    recalculatingAll.value = false
  }
}

function defaultCarForm() {
  return {
    brand: '',
    series: '',
    modelName: '',
    guidePrice: 100000,
    bodyType: 'SUV',
    energyType: '燃油',
    seats: 5,
    launchYear: new Date().getFullYear(),
    imageUrl: '',
    salesVolume: 0,
    userRating: 0,
    auditStatus: 'PENDING',
  }
}

function defaultParamForm(carId = null) {
  return {
    carId,
    lengthMm: 4600,
    widthMm: 1850,
    heightMm: 1600,
    wheelbaseMm: 2700,
    fuelConsumption: null,
    electricConsumption: null,
    electricRangeKm: null,
    totalRangeKm: null,
    acceleration100: null,
    airbagCount: 0,
    hasAbs: false,
    hasEsp: false,
    hasActiveBrake: false,
    hasLaneKeep: false,
    hasAdaptiveCruise: false,
    hasBlindSpot: false,
    hasReverseCamera: false,
    has360Camera: false,
    hasOta: false,
    hasVoiceControl: false,
    hasAutoParking: false,
    screenSize: null,
    assistDriveLevel: '',
  }
}

function normalizeParam(param) {
  return {
    ...defaultParamForm(param.carId),
    ...param,
    fuelConsumption: toNullableNumber(param.fuelConsumption),
    electricConsumption: toNullableNumber(param.electricConsumption),
    acceleration100: toNullableNumber(param.acceleration100),
    screenSize: toNullableNumber(param.screenSize),
  }
}

function toNullableNumber(value) {
  return value === null || value === undefined ? null : Number(value)
}

function formatPrice(value) {
  return `${Number(value || 0).toLocaleString('zh-CN')} 元`
}

function formatScore(value) {
  return Number(value || 0).toFixed(2)
}

function scorePercent(value) {
  return Math.max(0, Math.min(100, Number(value || 0)))
}

function scoreStatus(value) {
  const score = Number(value || 0)
  if (score >= 85) return 'success'
  if (score < 60) return 'exception'
  return undefined
}

function formatDate(value) {
  if (!value) return '时间未知'
  return value.replace('T', ' ').slice(0, 16)
}

function auditTagType(value) {
  if (value === 'APPROVED') return 'success'
  if (value === 'REJECTED') return 'danger'
  return 'warning'
}
</script>

<style scoped>
.admin-car-panel {
  overflow: hidden;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.car-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
  margin-bottom: 16px;
}

.car-table {
  width: 100%;
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.score-cell {
  display: grid;
  gap: 5px;
}

.score-cell span {
  color: var(--color-muted);
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 16px;
}

.grid-span-2 {
  grid-column: span 2;
}

.param-grid :deep(.el-input-number) {
  width: 100%;
}

.form-grid :deep(.el-select),
.form-grid :deep(.el-input-number) {
  width: 100%;
}

.feature-switches {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 16px;
  margin-top: 8px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f9fafb;
}

.score-overview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
}

.score-row {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) 58px;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.score-row strong {
  text-align: right;
}

.score-meta {
  margin: 18px 0 0;
  color: var(--color-muted);
  font-size: 12px;
}

@media (max-width: 760px) {
  .form-grid,
  .feature-switches,
  .score-overview {
    grid-template-columns: 1fr;
  }

  .grid-span-2 {
    grid-column: span 1;
  }
}
</style>
