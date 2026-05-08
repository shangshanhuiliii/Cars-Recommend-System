<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">收藏车型</h1>
        <p class="page-subtitle">按车型聚合用户收藏数据，只读查看收藏排行和收藏用户。</p>
      </div>
      <el-button type="primary" plain :loading="loadingCars" @click="reloadCars">刷新排行</el-button>
    </div>

    <el-alert
      v-if="routeUserId"
      class="state-alert"
      type="info"
      :closable="false"
      :title="`当前筛选用户 #${routeUserId} 收藏过的车型，收藏数仍展示全站总量。`"
      show-icon
    />

    <el-alert v-if="error" class="state-alert" type="error" :closable="false" :title="error" show-icon />

    <div class="favorite-admin-layout">
      <div class="panel">
        <div class="panel__body">
          <div class="favorite-toolbar">
            <el-input
              v-model.trim="query.keyword"
              clearable
              placeholder="搜索品牌、车系或车型"
              @keyup.enter="reloadFirstPage"
              @clear="reloadFirstPage"
            />
            <el-button type="primary" :loading="loadingCars" @click="reloadFirstPage">查询</el-button>
          </div>

          <el-table
            v-loading="loadingCars"
            :data="cars"
            row-key="carId"
            highlight-current-row
            empty-text="暂无收藏车型"
            @row-click="selectCar"
          >
            <el-table-column label="车型" min-width="260">
              <template #default="{ row }">
                <div class="car-cell">
                  <img :src="carImageSrc(row.imageUrl)" :alt="row.modelName" @error="fallbackCarImage" />
                  <div>
                    <strong>{{ row.brand }} {{ row.modelName }}</strong>
                    <p>{{ row.series }} · {{ row.bodyType }} · {{ row.energyType }}</p>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="指导价" width="110">
              <template #default="{ row }">{{ formatWan(row.guidePrice) }}</template>
            </el-table-column>
            <el-table-column prop="favoriteCount" label="收藏数" width="100" />
            <el-table-column label="最近收藏" width="160">
              <template #default="{ row }">{{ formatDate(row.latestFavoriteTime) }}</template>
            </el-table-column>
          </el-table>

          <div class="history-footer">
            <el-pagination
              v-model:current-page="query.page"
              v-model:page-size="query.size"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @current-change="loadCars"
              @size-change="reloadFirstPage"
            />
          </div>
        </div>
      </div>

      <aside class="panel favorite-users-panel">
        <div class="panel__body">
          <template v-if="selectedCar">
            <div class="detail-head">
              <div>
                <p class="eyebrow">车型 #{{ selectedCar.carId }}</p>
                <h2>{{ selectedCar.brand }} {{ selectedCar.modelName }}</h2>
                <p class="muted-line">全站收藏 {{ selectedCar.favoriteCount }} 次</p>
              </div>
              <el-tag type="success" effect="light">只读</el-tag>
            </div>

            <el-skeleton v-if="loadingUsers" :rows="7" animated />
            <el-empty v-else-if="!favoriteUsers.length" description="暂无收藏用户" />
            <div v-else class="user-list">
              <button v-for="user in favoriteUsers" :key="user.userId" type="button">
                <span>{{ user.nickname || user.username }}</span>
                <small>{{ user.username }} · {{ user.phone || '未填写手机号' }}</small>
                <em>{{ statusLabel(user.status) }} · {{ formatDate(user.favoriteTime) }}</em>
              </button>
            </div>

            <div class="history-footer">
              <el-pagination
                v-model:current-page="userQuery.page"
                v-model:page-size="userQuery.size"
                :total="userTotal"
                :page-sizes="[10, 20, 50]"
                layout="total, prev, pager, next"
                @current-change="loadFavoriteUsers"
                @size-change="reloadFavoriteUsers"
              />
            </div>
          </template>
          <el-empty v-else description="点击左侧车型查看收藏用户" />
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { fetchAdminFavoriteCars, fetchAdminFavoriteUsers } from '@/api/adminFavorites'
import { carImageSrc, fallbackCarImage } from '@/utils/carImage'

const route = useRoute()
const loadingCars = ref(false)
const loadingUsers = ref(false)
const error = ref('')
const cars = ref([])
const total = ref(0)
const selectedCar = ref(null)
const favoriteUsers = ref([])
const userTotal = ref(0)
const routeUserId = computed(() => route.query.userId || '')

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
})

const userQuery = reactive({
  page: 1,
  size: 10,
})

onMounted(loadCars)

watch(
  () => route.query.userId,
  () => reloadFirstPage(),
)

async function loadCars() {
  loadingCars.value = true
  error.value = ''
  try {
    const response = await fetchAdminFavoriteCars({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      userId: routeUserId.value || undefined,
    })
    cars.value = response.data.records || []
    total.value = response.data.total || 0
    if (cars.value.length) {
      await selectCar(cars.value[0])
    } else {
      selectedCar.value = null
      favoriteUsers.value = []
      userTotal.value = 0
    }
  } catch (requestError) {
    cars.value = []
    total.value = 0
    selectedCar.value = null
    favoriteUsers.value = []
    error.value = requestError?.response?.data?.message || requestError?.message || '收藏车型加载失败。'
  } finally {
    loadingCars.value = false
  }
}

function reloadCars() {
  return loadCars()
}

function reloadFirstPage() {
  query.page = 1
  return loadCars()
}

async function selectCar(row) {
  selectedCar.value = row
  userQuery.page = 1
  await loadFavoriteUsers()
}

async function loadFavoriteUsers() {
  if (!selectedCar.value?.carId) return
  loadingUsers.value = true
  try {
    const response = await fetchAdminFavoriteUsers(selectedCar.value.carId, {
      page: userQuery.page,
      size: userQuery.size,
    })
    favoriteUsers.value = response.data.records || []
    userTotal.value = response.data.total || 0
  } catch (requestError) {
    favoriteUsers.value = []
    userTotal.value = 0
    error.value = requestError?.response?.data?.message || requestError?.message || '收藏用户加载失败。'
  } finally {
    loadingUsers.value = false
  }
}

function reloadFavoriteUsers() {
  userQuery.page = 1
  return loadFavoriteUsers()
}

function statusLabel(value) {
  if (value === 'ACTIVE') return '启用'
  if (value === 'DISABLED') return '禁用'
  return value || '未知'
}

function formatWan(value) {
  return `${(Number(value || 0) / 10000).toFixed(1).replace(/\.0$/, '')} 万`
}

function formatDate(value) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.state-alert {
  margin-bottom: 16px;
}

.favorite-admin-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
  align-items: start;
}

.favorite-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto;
  gap: 10px;
  margin-bottom: 16px;
}

.car-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}

.car-cell img {
  width: 72px;
  height: 48px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: #eef2f7;
}

.car-cell strong,
.detail-head h2 {
  color: var(--color-primary-dark);
}

.car-cell p,
.muted-line {
  margin: 4px 0 0;
  color: var(--color-muted);
  font-size: 12px;
}

.favorite-users-panel {
  position: sticky;
  top: 96px;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-head h2 {
  margin: 4px 0 0;
  font-size: 18px;
}

.eyebrow {
  margin: 0;
  color: var(--color-muted);
  font-size: 12px;
  letter-spacing: 0.16em;
}

.user-list {
  display: grid;
  gap: 8px;
}

.user-list button {
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: #fff;
  text-align: left;
}

.user-list span,
.user-list small,
.user-list em {
  display: block;
}

.user-list span {
  color: var(--color-primary-dark);
  font-weight: 700;
}

.user-list small,
.user-list em {
  margin-top: 4px;
  color: var(--color-muted);
  font-size: 12px;
  font-style: normal;
}

.history-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 980px) {
  .favorite-admin-layout,
  .favorite-toolbar {
    grid-template-columns: 1fr;
  }

  .favorite-users-panel {
    position: static;
  }
}
</style>
