<template>
  <section class="car-list-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">车型库</h1>
        <p class="page-subtitle">按品牌、级别、动力和价格浏览车型信息。</p>
      </div>
    </div>

    <VehicleCarousel />

    <section class="filter-panel">
      <el-input v-model="filters.keyword" class="keyword-input" clearable placeholder="搜索品牌、车系或车型" @keyup.enter="searchCars" />
      <el-select v-model="filters.brand" clearable placeholder="品牌">
        <el-option v-for="brand in brands" :key="brand" :label="brand" :value="brand" />
      </el-select>
      <el-select v-model="filters.bodyType" clearable placeholder="级别">
        <el-option v-for="item in bodyTypes" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="filters.energyType" clearable placeholder="动力">
        <el-option v-for="item in energyTypes" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="priceRange" clearable placeholder="价格">
        <el-option v-for="item in priceRanges" :key="item.label" :label="item.label" :value="item.label" />
      </el-select>
      <el-button type="primary" @click="searchCars">筛选</el-button>
    </section>

    <p v-if="inlineMessage" class="inline-action-message" :class="`inline-action-message--${inlineMessageType}`">
      {{ inlineMessage }}
    </p>

    <div v-if="loading" class="car-grid">
      <div v-for="index in 6" :key="index" class="car-card car-card--loading">
        <el-skeleton :rows="5" animated />
      </div>
    </div>

    <el-empty v-else-if="!cars.length" description="暂无符合条件的车型" />

    <div v-else class="car-grid">
      <article v-for="item in cars" :key="item.carId" class="car-card">
        <RouterLink class="car-card__image" :to="`/car/${item.carId}`">
          <img :src="carImageSrc(item.imageUrl)" :alt="item.modelName" @error="fallbackCarImage" />
        </RouterLink>
        <div class="car-card__body">
          <span class="brand-line">{{ item.brand }} / {{ item.series }}</span>
          <h2>{{ item.modelName }}</h2>
          <strong>{{ formatWan(item.priceMin) }}</strong>
          <div class="summary-tags">
            <span>{{ item.bodyType || '暂无级别' }}</span>
            <span>{{ item.energyType || '暂无动力' }}</span>
            <span>{{ item.seatCount ? `${item.seatCount}座` : '暂无座位' }}</span>
          </div>
        </div>
        <div class="car-card__actions">
          <el-button @click="$router.push(`/car/${item.carId}`)">查看详情</el-button>
          <el-button :loading="operatingCompareId === item.carId" @click="addCompare(item.carId)">加入对比</el-button>
          <el-button :loading="operatingFavoriteId === item.carId" @click="favoriteCar(item.carId)">收藏</el-button>
        </div>
      </article>
    </div>

    <div class="pagination-row">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        @current-change="changePage"
      />
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchCarBrands, fetchCars } from '@/api/cars'
import { addFavorite } from '@/api/favorites'
import { addUserCompare } from '@/api/userCompare'
import VehicleCarousel from '@/components/VehicleCarousel.vue'
import { useAuthStore } from '@/stores/auth'
import { carImageSrc, fallbackCarImage } from '@/utils/carImage'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const filters = reactive({
  keyword: '',
  brand: '',
  bodyType: '',
  energyType: '',
})
const priceRange = ref('')
const priceRanges = [
  { label: '10万以内', min: 0, max: 100000 },
  { label: '10-15万', min: 100000, max: 150000 },
  { label: '15-20万', min: 150000, max: 200000 },
  { label: '20-30万', min: 200000, max: 300000 },
  { label: '30万以上', min: 300000, max: null },
]
const bodyTypes = ['轿车', 'SUV', 'MPV', '跑车', '卡车']
const energyTypes = ['燃油', '纯电', '插混', '增程']

const brands = ref([])
const cars = ref([])
const loading = ref(false)
const pagination = reactive({ page: 1, size: 12, total: 0 })
const operatingCompareId = ref(null)
const operatingFavoriteId = ref(null)
const inlineMessage = ref('')
const inlineMessageType = ref('info')

onMounted(async () => {
  await Promise.all([loadBrands(), loadCars()])
})

watch(() => route.query.auth, (value) => {
  if (!value && inlineMessage.value === '请先登录后继续操作。') {
    inlineMessage.value = ''
  }
})

async function loadBrands() {
  try {
    const response = await fetchCarBrands()
    brands.value = response.data || []
  } catch {
    brands.value = []
  }
}

async function loadCars() {
  loading.value = true
  const range = priceRanges.find((item) => item.label === priceRange.value)
  try {
    const response = await fetchCars({
      page: pagination.page,
      pageSize: pagination.size,
      keyword: [filters.keyword, filters.brand].filter(Boolean).join(' '),
      bodyType: filters.bodyType || undefined,
      energyType: filters.energyType || undefined,
      priceMin: range?.min,
      priceMax: range?.max,
    })
    cars.value = response.data?.records || []
    pagination.total = Number(response.data?.total || 0)
  } catch (error) {
    cars.value = []
    pagination.total = 0
    setInlineMessage(error?.response?.data?.message || '车型列表加载失败，请稍后重试。', 'error')
  } finally {
    loading.value = false
  }
}

function searchCars() {
  pagination.page = 1
  loadCars()
}

function changePage(page) {
  pagination.page = page
  loadCars()
}

async function addCompare(carId) {
  if (!ensureUserAuth()) return
  operatingCompareId.value = carId
  try {
    await addUserCompare(carId)
    setInlineMessage('已加入对比，可前往车型对比查看。')
  } catch (error) {
    setInlineMessage(error?.response?.data?.message || '加入对比失败，请稍后重试。', 'error')
  } finally {
    operatingCompareId.value = null
  }
}

async function favoriteCar(carId) {
  if (!ensureUserAuth()) return
  operatingFavoriteId.value = carId
  try {
    await addFavorite(carId)
    setInlineMessage('已收藏该车型。')
  } catch (error) {
    setInlineMessage(error?.response?.data?.message || '收藏失败，请稍后重试。', 'error')
  } finally {
    operatingFavoriteId.value = null
  }
}

function ensureUserAuth() {
  if (authStore.isAuthenticated && authStore.principalType === 'USER') {
    return true
  }
  setInlineMessage('请先登录后继续操作。')
  router.push({
    path: route.path,
    query: { ...route.query, auth: 'login', redirect: route.fullPath },
  })
  return false
}

function setInlineMessage(text, type = 'info') {
  inlineMessage.value = text
  inlineMessageType.value = type
}

function formatWan(value) {
  const number = Number(value || 0)
  if (!number) return '暂无报价'
  return `${(number / 10000).toFixed(1).replace(/\.0$/, '')}万`
}

</script>

<style scoped>
.car-list-page {
  display: grid;
  gap: 20px;
}

.filter-panel {
  display: grid;
  grid-template-columns: minmax(220px, 1.4fr) repeat(4, minmax(132px, 1fr)) auto;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  background: #fff;
}

.inline-action-message {
  margin: 0;
  color: var(--color-muted);
  font-size: 14px;
}

.inline-action-message--error {
  color: var(--color-danger);
}

.car-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.car-card {
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.car-card__image {
  display: block;
  aspect-ratio: 16 / 9;
  background: #eef2f7;
}

.car-card__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.car-card__body {
  display: grid;
  gap: 10px;
  padding: 16px;
}

.brand-line {
  color: var(--color-muted);
  font-size: 12px;
}

.car-card h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 18px;
  line-height: 1.35;
}

.car-card strong {
  color: var(--color-primary);
  font-size: 18px;
}

.summary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-tags span {
  padding: 5px 9px;
  border-radius: 999px;
  background: #f1f5f9;
  color: var(--color-primary-dark);
  font-size: 12px;
}

.car-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 16px 16px;
}

.pagination-row {
  display: flex;
  justify-content: center;
}

@media (max-width: 1280px) {
  .filter-panel {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .car-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .filter-panel,
  .car-grid {
    grid-template-columns: 1fr;
  }
}
</style>
