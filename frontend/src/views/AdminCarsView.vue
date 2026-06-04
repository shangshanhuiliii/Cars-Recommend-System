<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">车型管理</h1>
        <p class="page-subtitle">维护车型基础信息、参数和特征评分，确保推荐算法读取到可信的数据基础。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Upload" @click="openImportDialog">导入数据源</el-button>
        <el-button :icon="Refresh" :loading="recalculatingAll" @click="recalculateAllScores">全部评分重算</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增车型</el-button>
      </div>
    </div>

    <div class="panel admin-car-panel">
      <div class="panel__body">
        <p v-if="operationMessage" class="inline-state" :class="{ 'inline-state--error': operationMessageType === 'error' }">
          {{ operationMessage }}
        </p>

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
          <el-table-column label="图片" width="96">
            <template #default="{ row }">
              <img class="car-thumb" :src="carImageSrc(row.imageUrl)" :alt="row.modelName" @error="fallbackCarImage" />
            </template>
          </el-table-column>
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
              <el-tag :type="auditTagType(row.auditStatus)" effect="light">{{ auditLabel(row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="评分状态" width="150">
            <template #default="{ row }">
              <div class="score-cell">
                <el-tag v-if="scoreMap[row.id]" type="success" effect="light">已评分</el-tag>
                <el-tag v-else type="warning" effect="light">待评分</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="维护" width="132" fixed="right">
            <template #default="{ row }">
              <div class="car-row-actions">
                <el-button size="small" type="primary" plain :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
                <el-dropdown
                  trigger="click"
                  popper-class="admin-car-row-dropdown"
                  @command="(command) => handleRowCommand(command, row)"
                >
                  <el-button size="small" :icon="MoreFilled">更多</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="param" :icon="Setting">参数</el-dropdown-item>
                      <el-dropdown-item command="score">评分</el-dropdown-item>
                      <el-dropdown-item command="recalculate" :disabled="recalculatingCarId === row.id">
                        {{ recalculatingCarId === row.id ? '重算中' : '重算评分' }}
                      </el-dropdown-item>
                      <el-dropdown-item command="delete" :icon="Delete" class="admin-car-row-dropdown__danger" divided>删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
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

    <el-dialog v-model="carDialogVisible" :title="carDialogTitle" width="860px" @closed="resetImageManager">
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
              <el-option v-for="item in auditStatuses" :key="item" :label="auditLabel(item)" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="图片地址" prop="imageUrl" class="grid-span-2">
            <el-input v-model="carForm.imageUrl" />
          </el-form-item>
        </div>
      </el-form>

      <div v-if="editingCarId" class="image-manager">
        <div class="image-manager__header">
          <div>
            <h3>图片资源</h3>
            <p>JPG / PNG，5MB 内。</p>
          </div>
          <el-tag :type="carForm.imageUrl ? 'success' : 'info'" effect="light">
            {{ carForm.imageUrl ? '已有生效图片' : '暂无生效图片' }}
          </el-tag>
        </div>

        <p
          v-if="imageMessage"
          class="inline-state"
          :class="{ 'inline-state--error': imageMessageType === 'error' }"
        >
          {{ imageMessage }}
        </p>

        <div class="image-upload-row">
          <div class="image-preview-box">
            <img :src="selectedPreviewSrc" :alt="carForm.modelName || '车型图片预览'" @error="fallbackCarImage" />
          </div>
          <div class="image-upload-controls">
            <input type="file" accept="image/jpeg,image/png" @change="handleImageFileChange" />
            <div class="upload-actions">
              <el-button
                type="primary"
                :icon="Upload"
                :disabled="!selectedImageFile"
                :loading="uploadingImage"
                @click="uploadSelectedImage"
              >
                上传图片
              </el-button>
              <span v-if="selectedImageFile">{{ selectedImageFile.name }}</span>
            </div>
          </div>
        </div>

        <el-table
          v-loading="imageAssetsLoading"
          :data="imageAssets"
          size="small"
          class="image-asset-table"
          empty-text="暂无图片资源"
        >
          <el-table-column label="预览" width="82">
            <template #default="{ row }">
              <img class="asset-thumb" :src="carImageSrc(row.publicUrl)" :alt="row.originalFilename" @error="fallbackCarImage" />
            </template>
          </el-table-column>
          <el-table-column prop="originalFilename" label="文件" min-width="150" show-overflow-tooltip />
          <el-table-column label="尺寸" width="120">
            <template #default="{ row }">{{ formatImageSize(row) }}</template>
          </el-table-column>
          <el-table-column label="大小" width="96">
            <template #default="{ row }">{{ formatFileSize(row.sizeBytes) }}</template>
          </el-table-column>
          <el-table-column prop="auditStatus" label="状态" width="96">
            <template #default="{ row }">
              <el-tag :type="auditTagType(row.auditStatus)" effect="light">{{ imageAuditLabel(row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审核操作" width="320">
            <template #default="{ row }">
              <div v-if="row.auditStatus === 'PENDING'" class="asset-actions">
                <el-input
                  v-model="rejectReasonMap[row.id]"
                  size="small"
                  maxlength="500"
                  placeholder="拒绝原因"
                />
                <el-button size="small" type="success" :loading="auditingImageId === row.id" @click="approveImage(row)">
                  通过
                </el-button>
                <el-button size="small" type="warning" :loading="auditingImageId === row.id" @click="rejectImage(row)">
                  拒绝
                </el-button>
              </div>
              <span v-else class="asset-review-text">{{ row.rejectReason || formatDate(row.reviewTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="资源" width="92">
            <template #default="{ row }">
              <el-button size="small" type="danger" :loading="deletingImageId === row.id" @click="removeImage(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <p v-else class="image-manager-note">保存车型后可维护图片资源。</p>

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

    <el-dialog v-model="importDialogVisible" title="导入车型数据源" width="760px">
      <div class="data-source-import">
        <p class="import-note">
          上传 JSON 数据文件，系统按品牌、车系、车型名称和年款匹配车型；新车型会新增，已存在车型会更新基础信息和参数。
        </p>
        <el-upload
          class="data-source-upload"
          drag
          action="#"
          :auto-upload="false"
          :limit="1"
          accept=".json,application/json"
          :on-change="handleDataSourceFileChange"
          :on-remove="clearDataSourceFile"
        >
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">将 JSON 文件拖到这里，或点击选择</div>
        </el-upload>

        <p v-if="importMessage" class="inline-state" :class="{ 'inline-state--error': importMessageType === 'error' }">
          {{ importMessage }}
        </p>

        <div v-if="importResult" class="import-result">
          <div class="import-summary">
            <span>总数 {{ importResult.totalCount }}</span>
            <span>成功 {{ importResult.successCount }}</span>
            <span>新增 {{ importResult.createdCount }}</span>
            <span>更新 {{ importResult.updatedCount }}</span>
            <span>跳过 {{ importResult.skippedCount }}</span>
            <span>失败 {{ importResult.failedCount }}</span>
          </div>
          <p class="import-note">匹配规则：{{ importResult.matchingRule }}</p>
          <p class="import-note">下一步：{{ importResult.nextStep }}</p>
          <el-table v-if="importResult.issues?.length" :data="importResult.issues" class="import-issue-table" size="small">
            <el-table-column prop="rowNo" label="行号" width="80" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ importIssueTypeLabel(row.type) }}</template>
            </el-table-column>
            <el-table-column prop="uniqueKey" label="匹配键" min-width="180" show-overflow-tooltip />
            <el-table-column prop="message" label="原因" min-width="220" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importingDataSource" @click="submitDataSourceImport">开始导入</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, MoreFilled, Plus, Refresh, Search, Setting, Upload } from '@element-plus/icons-vue'
import { ElMessageBox as ConfirmBox } from 'element-plus'

import {
  createAdminCar,
  deleteAdminCar,
  fetchAdminCarParam,
  fetchAdminCars,
  fetchAdminCarScore,
  importAdminCarDataSource,
  recalculateAdminCarScore,
  recalculateAllAdminCarScores,
  saveAdminCarParam,
  updateAdminCar,
} from '@/api/adminCars'
import { auditCarImage, deleteCarImage, fetchCarImages, uploadCarImage } from '@/api/carImages'
import { bodyTypes, carEnergyTypes } from '@/constants/enums'
import { carImageSrc, fallbackCarImage } from '@/utils/carImage'

const auditStatuses = ['APPROVED', 'PENDING', 'REJECTED']

const loading = ref(false)
const cars = ref([])
const total = ref(0)
const scoreMap = ref({})
const recalculatingAll = ref(false)
const recalculatingCarId = ref(null)
const operationMessage = ref('')
const operationMessageType = ref('info')
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
const imageAssets = ref([])
const imageAssetsLoading = ref(false)
const selectedImageFile = ref(null)
const selectedImagePreview = ref('')
const uploadingImage = ref(false)
const imageMessage = ref('')
const imageMessageType = ref('info')
const auditingImageId = ref(null)
const deletingImageId = ref(null)
const rejectReasonMap = reactive({})

const paramDialogVisible = ref(false)
const paramFormRef = ref()
const savingParam = ref(false)
const currentParamCar = ref(null)
const paramForm = reactive(defaultParamForm())

const scoreDialogVisible = ref(false)
const scoreLoading = ref(false)
const currentScoreCar = ref(null)
const currentScore = ref(null)

const importDialogVisible = ref(false)
const selectedDataSourceFile = ref(null)
const importingDataSource = ref(false)
const importResult = ref(null)
const importMessage = ref('')
const importMessageType = ref('info')

const carDialogTitle = computed(() => (editingCarId.value ? '编辑车型基础信息' : '新增车型'))
const paramDialogTitle = computed(() => (currentParamCar.value ? `${currentParamCar.value.modelName} 参数维护` : '参数维护'))
const scoreDialogTitle = computed(() => (currentScoreCar.value ? `${currentScoreCar.value.modelName} 特征评分` : '特征评分'))
const selectedPreviewSrc = computed(() => selectedImagePreview.value || carImageSrc(carForm.imageUrl))
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
onBeforeUnmount(revokeSelectedImagePreview)

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

function handleRowCommand(command, row) {
  if (command === 'param') {
    openParamDialog(row)
    return
  }
  if (command === 'score') {
    openScoreDialog(row)
    return
  }
  if (command === 'recalculate') {
    recalculateCarScore(row)
    return
  }
  if (command === 'delete') {
    confirmDelete(row)
  }
}

function openCreateDialog() {
  editingCarId.value = null
  Object.assign(carForm, defaultCarForm())
  resetImageManager()
  carDialogVisible.value = true
}

function openEditDialog(row) {
  editingCarId.value = row.id
  resetImageManager()
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
  loadImageAssets(row.id)
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
    setOperationMessage('车型已保存')
    carDialogVisible.value = false
    loadCars()
  } catch (error) {
    setOperationMessage(error?.response?.data?.message || error?.message || '车型保存失败。', 'error')
  } finally {
    savingCar.value = false
  }
}

async function loadImageAssets(carId = editingCarId.value) {
  if (!carId) {
    imageAssets.value = []
    return
  }
  imageAssetsLoading.value = true
  try {
    const response = await fetchCarImages({ page: 1, size: 100, carId })
    imageAssets.value = response.data.records || []
    imageAssets.value.forEach((asset) => {
      if (asset.auditStatus === 'PENDING' && rejectReasonMap[asset.id] === undefined) {
        rejectReasonMap[asset.id] = ''
      }
    })
  } catch (error) {
    setImageMessage(error?.response?.data?.message || error?.message || '图片资源加载失败。', 'error')
  } finally {
    imageAssetsLoading.value = false
  }
}

function handleImageFileChange(event) {
  const file = event.target.files?.[0] || null
  selectedImageFile.value = file
  revokeSelectedImagePreview()
  if (file) {
    selectedImagePreview.value = URL.createObjectURL(file)
    setImageMessage('')
  }
  event.target.value = ''
}

async function uploadSelectedImage() {
  if (!editingCarId.value || !selectedImageFile.value) {
    return
  }
  uploadingImage.value = true
  try {
    const response = await uploadCarImage({ carId: editingCarId.value, file: selectedImageFile.value })
    setImageMessage(`图片已上传，当前状态：${imageAuditLabel(response.data.auditStatus)}`)
    selectedImageFile.value = null
    revokeSelectedImagePreview()
    await loadImageAssets()
  } catch (error) {
    setImageMessage(error?.response?.data?.message || error?.message || '图片上传失败。', 'error')
  } finally {
    uploadingImage.value = false
  }
}

async function approveImage(row) {
  auditingImageId.value = row.id
  try {
    const response = await auditCarImage(row.id, { auditStatus: 'APPROVED' })
    carForm.imageUrl = response.data.publicUrl
    setImageMessage('图片已通过审核并设为当前车型图片')
    await loadImageAssets()
    await loadCars()
  } catch (error) {
    setImageMessage(error?.response?.data?.message || error?.message || '图片审核失败。', 'error')
  } finally {
    auditingImageId.value = null
  }
}

async function rejectImage(row) {
  const rejectReason = (rejectReasonMap[row.id] || '').trim()
  if (!rejectReason) {
    setImageMessage('请填写拒绝原因。', 'error')
    return
  }
  auditingImageId.value = row.id
  try {
    await auditCarImage(row.id, { auditStatus: 'REJECTED', rejectReason })
    setImageMessage('图片已拒绝，车型当前图片保持不变')
    await loadImageAssets()
  } catch (error) {
    setImageMessage(error?.response?.data?.message || error?.message || '图片审核失败。', 'error')
  } finally {
    auditingImageId.value = null
  }
}

async function removeImage(row) {
  try {
    await ConfirmBox.confirm(`确认删除图片资源 ${row.originalFilename}？`, '删除图片资源', { type: 'warning' })
  } catch {
    return
  }
  deletingImageId.value = row.id
  try {
    await deleteCarImage(row.id)
    setImageMessage('图片资源已删除')
    await loadImageAssets()
  } catch (error) {
    setImageMessage(error?.response?.data?.message || error?.message || '图片资源删除失败。', 'error')
  } finally {
    deletingImageId.value = null
  }
}

async function confirmDelete(row) {
  try {
    await ConfirmBox.confirm(`确认删除 ${row.modelName}？`, '删除车型', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteAdminCar(row.id)
    setOperationMessage('车型已删除')
    loadCars()
  } catch (error) {
    setOperationMessage(error?.response?.data?.message || error?.message || '车型删除失败。', 'error')
  }
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
    setOperationMessage('车型参数已保存')
    paramDialogVisible.value = false
  } catch (error) {
    setOperationMessage(error?.response?.data?.message || error?.message || '车型参数保存失败。', 'error')
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
    setOperationMessage('单车评分已重算')
  } catch (error) {
    setOperationMessage(error?.response?.data?.message || error?.message || '单车评分重算失败。', 'error')
  } finally {
    recalculatingCarId.value = null
  }
}

async function recalculateAllScores() {
  recalculatingAll.value = true
  try {
    const response = await recalculateAllAdminCarScores()
    setOperationMessage(`已重算 ${response.data.recalculatedCount} 台车型评分`)
    await loadScoresForCurrentPage()
  } catch (error) {
    setOperationMessage(error?.response?.data?.message || error?.message || '全部评分重算失败。', 'error')
  } finally {
    recalculatingAll.value = false
  }
}

function openImportDialog() {
  selectedDataSourceFile.value = null
  importResult.value = null
  importMessage.value = ''
  importMessageType.value = 'info'
  importDialogVisible.value = true
}

function handleDataSourceFileChange(uploadFile) {
  selectedDataSourceFile.value = uploadFile.raw
  importResult.value = null
  importMessage.value = uploadFile.name ? `已选择 ${uploadFile.name}` : ''
  importMessageType.value = 'info'
}

function clearDataSourceFile() {
  selectedDataSourceFile.value = null
}

async function submitDataSourceImport() {
  if (!selectedDataSourceFile.value) {
    importMessage.value = '请先选择 JSON 数据文件。'
    importMessageType.value = 'error'
    return
  }
  importingDataSource.value = true
  try {
    const response = await importAdminCarDataSource(selectedDataSourceFile.value)
    importResult.value = response.data
    importMessage.value = `导入完成：成功 ${response.data.successCount} 条，失败 ${response.data.failedCount} 条。`
    importMessageType.value = response.data.failedCount > 0 ? 'error' : 'info'
    await loadCars()
  } catch (error) {
    importMessage.value = error?.response?.data?.message || error?.message || '车型数据源导入失败。'
    importMessageType.value = 'error'
  } finally {
    importingDataSource.value = false
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

function formatFileSize(value) {
  const bytes = Number(value || 0)
  if (bytes >= 1024 * 1024) {
    return `${(bytes / 1024 / 1024).toFixed(2)} MB`
  }
  if (bytes >= 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${bytes} B`
}

function formatImageSize(row) {
  return `${row.width || 0} x ${row.height || 0}`
}

function auditTagType(value) {
  if (value === 'APPROVED') return 'success'
  if (value === 'REJECTED') return 'danger'
  return 'warning'
}

function auditLabel(value) {
  if (value === 'APPROVED') return '已通过'
  if (value === 'PENDING') return '待审核'
  if (value === 'REJECTED') return '已拒绝'
  return value || '未知'
}

function imageAuditLabel(value) {
  return auditLabel(value)
}

function importIssueTypeLabel(value) {
  if (value === 'FAILED') return '失败'
  if (value === 'SKIPPED') return '跳过'
  return value || '未知'
}

function setOperationMessage(text, type = 'info') {
  operationMessage.value = text
  operationMessageType.value = type
}

function setImageMessage(text, type = 'info') {
  imageMessage.value = text
  imageMessageType.value = type
}

function resetImageManager() {
  imageAssets.value = []
  imageAssetsLoading.value = false
  selectedImageFile.value = null
  revokeSelectedImagePreview()
  uploadingImage.value = false
  imageMessage.value = ''
  imageMessageType.value = 'info'
  auditingImageId.value = null
  deletingImageId.value = null
  Object.keys(rejectReasonMap).forEach((key) => {
    delete rejectReasonMap[key]
  })
}

function revokeSelectedImagePreview() {
  if (selectedImagePreview.value) {
    URL.revokeObjectURL(selectedImagePreview.value)
    selectedImagePreview.value = ''
  }
}
</script>

<style scoped>
.admin-car-panel {
  overflow: hidden;
}

.inline-state {
  margin: 0 0 12px;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.6;
}

.inline-state--error {
  color: var(--color-danger);
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

.car-row-actions {
  display: flex;
  flex-wrap: nowrap;
  gap: 6px;
  width: 108px;
}

.car-row-actions :deep(.el-button) {
  margin-left: 0;
  padding: 5px 8px;
}

:global(.admin-car-row-dropdown .el-dropdown-menu) {
  padding: 6px;
}

:global(.admin-car-row-dropdown .el-dropdown-menu__item) {
  border-radius: 6px;
  margin: 2px 0;
}

:global(.admin-car-row-dropdown .el-dropdown-menu__item:not(.is-disabled):focus),
:global(.admin-car-row-dropdown .el-dropdown-menu__item:not(.is-disabled):hover) {
  background: #eff6ff;
  color: var(--color-primary);
}

:global(.admin-car-row-dropdown .admin-car-row-dropdown__danger) {
  color: #f56c6c;
}

:global(.admin-car-row-dropdown .admin-car-row-dropdown__danger .el-icon) {
  color: inherit;
}

:global(.admin-car-row-dropdown .admin-car-row-dropdown__danger:not(.is-disabled):focus),
:global(.admin-car-row-dropdown .admin-car-row-dropdown__danger:not(.is-disabled):hover) {
  background: #fef2f2;
  color: #f56c6c;
}

.car-thumb,
.asset-thumb {
  display: block;
  width: 64px;
  height: 44px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: #f3f4f6;
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

.image-manager {
  display: grid;
  gap: 14px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.image-manager__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.image-manager__header h3 {
  margin: 0 0 4px;
  font-size: 15px;
}

.image-manager__header p,
.image-manager-note {
  margin: 0;
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.6;
}

.image-manager-note {
  margin-top: 14px;
}

.image-upload-row {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
}

.image-preview-box {
  width: 180px;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f3f4f6;
}

.image-preview-box img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-upload-controls {
  display: grid;
  gap: 10px;
}

.upload-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.upload-actions span {
  color: var(--color-muted);
  font-size: 12px;
}

.image-asset-table {
  width: 100%;
}

.asset-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 8px;
  align-items: center;
}

.asset-review-text {
  color: var(--color-muted);
  font-size: 12px;
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

.data-source-import {
  display: grid;
  gap: 14px;
}

.import-note {
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.6;
}

.import-result {
  display: grid;
  gap: 10px;
}

.import-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.import-summary span {
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f9fafb;
  font-size: 13px;
}

.import-issue-table {
  width: 100%;
}

@media (max-width: 760px) {
  .form-grid,
  .feature-switches,
  .image-upload-row,
  .score-overview,
  .import-summary {
    grid-template-columns: 1fr;
  }

  .image-preview-box {
    width: 100%;
  }

  .asset-actions {
    grid-template-columns: 1fr;
  }

  .grid-span-2 {
    grid-column: span 1;
  }
}
</style>
