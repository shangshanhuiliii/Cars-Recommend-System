<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">推荐历史</h1>
        <p class="page-subtitle">历史列表展示已保存的推荐记录摘要，进入详情后可查看当次推荐的标签、理由、不足和分数快照。</p>
      </div>
      <el-button type="primary" @click="$router.push('/recommend')">新建购车需求</el-button>
    </div>

    <div class="panel">
      <div class="panel__body">
        <el-skeleton v-if="loading" :rows="6" animated />

        <el-alert
          v-else-if="error"
          type="error"
          :closable="false"
          :title="error"
          show-icon
        />

        <el-empty v-else-if="!records.length" description="暂无推荐记录">
          <el-button type="primary" @click="$router.push('/recommend')">先填写购车需求</el-button>
        </el-empty>

        <template v-else>
          <div class="history-list">
            <button
              v-for="record in records"
              :key="record.recordId"
              class="history-item"
              type="button"
              @click="goDetail(record.recordId)"
            >
              <div class="history-item__main">
                <div class="history-title">
                  <span>推荐记录 #{{ record.recordId }}</span>
                  <el-tag :type="statusType(record.recommendStatus)" effect="light">
                    {{ statusLabel(record.recommendStatus) }}
                  </el-tag>
                </div>
                <p>{{ record.profileText || '暂无画像文本' }}</p>
                <div class="top-cars">
                  <span v-for="name in record.topCarNames" :key="name">{{ name }}</span>
                  <span v-if="!record.topCarNames?.length">暂无推荐车型</span>
                </div>
              </div>
              <div class="history-item__side">
                <strong>{{ record.itemCount }}</strong>
                <span>推荐车型</span>
                <small>{{ formatDate(record.createTime) }}</small>
              </div>
            </button>
          </div>

          <div class="history-footer">
            <el-pagination
              v-model:current-page="query.page"
              v-model:page-size="query.size"
              :total="total"
              :page-sizes="[5, 10, 20]"
              layout="total, sizes, prev, pager, next"
              @current-change="loadHistory"
              @size-change="reloadFirstPage"
            />
          </div>
        </template>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchRecommendationHistory } from '@/api/recommend'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const records = ref([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
})

onMounted(loadHistory)

async function loadHistory() {
  loading.value = true
  error.value = ''
  try {
    const response = await fetchRecommendationHistory({
      page: query.page,
      size: query.size,
    })
    records.value = response.data.records || []
    total.value = response.data.total || 0
  } catch (requestError) {
    records.value = []
    total.value = 0
    error.value = requestError?.response?.data?.message || requestError?.message || '推荐历史加载失败。'
  } finally {
    loading.value = false
  }
}

function reloadFirstPage() {
  query.page = 1
  loadHistory()
}

function goDetail(recordId) {
  router.push(`/recommend/result/${recordId}`)
}

function statusLabel(value) {
  if (value === 'SUCCESS') return '完全匹配'
  if (value === 'FALLBACK') return '含补充推荐'
  if (value === 'EMPTY') return '暂无结果'
  return value || '未知'
}

function statusType(value) {
  if (value === 'SUCCESS') return 'success'
  if (value === 'FALLBACK') return 'warning'
  if (value === 'EMPTY') return 'danger'
  return 'info'
}

function formatDate(value) {
  if (!value) return '时间未知'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.history-list {
  display: grid;
  gap: 14px;
}

.history-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 130px;
  gap: 18px;
  width: 100%;
  padding: 18px;
  text-align: left;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.history-item:hover {
  border-color: rgba(37, 99, 235, 0.45);
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.08);
}

.history-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.history-title span {
  color: var(--color-primary-dark);
  font-size: 16px;
  font-weight: 700;
}

.history-item p {
  margin: 10px 0 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.7;
}

.top-cars {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.top-cars span {
  padding: 5px 9px;
  border-radius: var(--radius-sm);
  background: rgba(37, 99, 235, 0.08);
  color: var(--color-primary);
  font-size: 12px;
}

.history-item__side {
  display: grid;
  place-items: center;
  align-content: center;
  border-left: 1px solid var(--color-border);
}

.history-item__side strong {
  color: var(--color-primary-dark);
  font-size: 30px;
}

.history-item__side span,
.history-item__side small {
  color: var(--color-muted);
  font-size: 12px;
}

.history-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 760px) {
  .history-item {
    grid-template-columns: 1fr;
  }

  .history-item__side {
    justify-items: start;
    border-left: 0;
    border-top: 1px solid var(--color-border);
    padding-top: 12px;
  }
}
</style>
