<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">我的收藏</h1>
        <p class="page-subtitle">收藏只记录演示用户关注的车型，不参与推荐排序，也不会改变推荐算法权重。</p>
      </div>
      <el-button type="primary" @click="$router.push('/recommend')">继续购车推荐</el-button>
    </div>

    <div class="panel">
      <div class="panel__body">
        <el-skeleton v-if="loading" :rows="8" animated />

        <el-alert
          v-else-if="error"
          type="error"
          :closable="false"
          :title="error"
          show-icon
        />

        <el-empty v-else-if="!records.length" description="暂无收藏车型">
          <el-button type="primary" @click="$router.push('/recommend')">去推荐结果中收藏车型</el-button>
        </el-empty>

        <template v-else>
          <div class="favorite-grid">
            <article v-for="car in records" :key="car.favoriteId" class="favorite-card">
              <div class="favorite-visual">
                <img :src="carImageSrc(car.imageUrl)" :alt="car.modelName" @error="fallbackCarImage" />
              </div>
              <div class="favorite-body">
                <h2>{{ car.brand }} {{ car.modelName }}</h2>
                <p>{{ car.series }} · {{ car.bodyType }} · {{ car.energyType }} · {{ car.seats }} 座</p>
                <div class="favorite-meta">
                  <strong>{{ formatWan(car.guidePrice) }}</strong>
                  <span>{{ formatDate(car.favoriteTime) }}</span>
                </div>

                <div v-if="car.scoreSummary" class="score-strip">
                  <span v-for="score in scoreHighlights(car)" :key="score.key">
                    {{ score.label }} {{ formatScore(score.value) }}
                  </span>
                </div>
                <p v-else class="missing-score">暂无评分，请在管理端重算评分。</p>

                <div class="favorite-actions">
                  <el-button type="primary" plain @click="$router.push(`/car/${car.carId}`)">查看详情</el-button>
                  <el-button @click="goCompare(car.carId)">加入对比</el-button>
                  <el-button
                    type="danger"
                    plain
                    :loading="operatingId === car.carId"
                    @click="cancelFavorite(car.carId)"
                  >
                    取消收藏
                  </el-button>
                </div>
              </div>
            </article>
          </div>

          <div class="favorite-footer">
            <el-pagination
              v-model:current-page="query.page"
              v-model:page-size="query.size"
              :total="total"
              :page-sizes="[6, 10, 20]"
              layout="total, sizes, prev, pager, next"
              @current-change="loadFavorites"
              @size-change="reloadFirstPage"
            />
          </div>
        </template>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchFavorites, removeFavorite } from '@/api/favorites'
import { carImageSrc, fallbackCarImage } from '@/utils/carImage'
import { addCompareId, compareQuery } from '@/utils/compareSelection'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const records = ref([])
const total = ref(0)
const operatingId = ref(null)
const query = reactive({
  page: 1,
  size: 6,
})

const scoreLabels = [
  ['space', '空间'],
  ['safety', '安全'],
  ['energy', '能耗'],
  ['comfort', '舒适'],
]

onMounted(loadFavorites)

async function loadFavorites() {
  loading.value = true
  error.value = ''
  try {
    const response = await fetchFavorites({ page: query.page, size: query.size })
    records.value = response.data.records || []
    total.value = response.data.total || 0
  } catch (requestError) {
    records.value = []
    total.value = 0
    error.value = requestError?.response?.data?.message || requestError?.message || '收藏列表加载失败。'
  } finally {
    loading.value = false
  }
}

function reloadFirstPage() {
  query.page = 1
  loadFavorites()
}

async function cancelFavorite(carId) {
  operatingId.value = carId
  try {
    await removeFavorite(carId)
    ElMessage.success('已取消收藏')
    loadFavorites()
  } catch (requestError) {
    ElMessage.error(requestError?.response?.data?.message || requestError?.message || '取消收藏失败')
  } finally {
    operatingId.value = null
  }
}

function goCompare(carId) {
  const result = addCompareId(carId)
  if (!result.ok) {
    ElMessage.warning(result.reason)
    return
  }
  ElMessage.success(result.reason)
  router.push({ path: '/compare', query: compareQuery(result.ids) })
}

function scoreHighlights(car) {
  return scoreLabels.map(([key, label]) => ({
    key,
    label,
    value: car.scoreSummary?.[key],
  }))
}

function formatScore(value) {
  return Number(value || 0).toFixed(0)
}

function formatWan(value) {
  return `${(Number(value || 0) / 10000).toFixed(1).replace(/\.0$/, '')} 万`
}

function formatDate(value) {
  if (!value) return '收藏时间未知'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.favorite-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.favorite-card {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
}

.favorite-visual {
  min-height: 150px;
  overflow: hidden;
  border-radius: var(--radius-sm);
  background: #eef2f7;
}

.favorite-visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.favorite-body h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 18px;
}

.favorite-body p {
  margin: 8px 0 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.7;
}

.favorite-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

.favorite-meta strong {
  color: var(--color-primary);
  font-size: 18px;
}

.favorite-meta span {
  color: var(--color-muted);
  font-size: 12px;
}

.score-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 12px;
}

.score-strip span {
  padding: 5px 8px;
  border-radius: var(--radius-sm);
  background: rgba(8, 145, 178, 0.08);
  color: var(--color-accent);
  font-size: 12px;
}

.missing-score {
  color: var(--color-warning) !important;
}

.favorite-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.favorite-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 980px) {
  .favorite-grid,
  .favorite-card {
    grid-template-columns: 1fr;
  }
}
</style>
