<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">反馈记录</h1>
        <p class="page-subtitle">查看用户对推荐记录的反馈，只读展示满意度、原因标签和评论，反馈只进入统计分析。</p>
      </div>
      <el-button type="primary" plain :loading="loading" @click="reloadFeedbacks">刷新反馈</el-button>
    </div>

    <el-alert
      v-if="routeUserId"
      class="state-alert"
      type="info"
      :closable="false"
      :title="`当前仅查看用户 #${routeUserId} 的反馈记录。`"
      show-icon
    />
    <el-alert v-if="error" class="state-alert" type="error" :closable="false" :title="error" show-icon />

    <div class="panel">
      <div class="panel__body">
        <div class="feedback-toolbar">
          <el-input
            v-model.trim="query.keyword"
            clearable
            placeholder="搜索用户名、昵称或评论"
            @keyup.enter="reloadFirstPage"
            @clear="reloadFirstPage"
          />
          <el-select v-model="query.satisfactionScore" clearable placeholder="全部评分" @change="reloadFirstPage">
            <el-option v-for="score in [5, 4, 3, 2, 1]" :key="score" :label="`${score} 分`" :value="score" />
          </el-select>
          <el-button type="primary" :loading="loading" @click="reloadFirstPage">查询</el-button>
        </div>

        <el-table v-loading="loading" :data="records" row-key="feedbackId" empty-text="暂无反馈记录">
          <el-table-column label="用户" min-width="170">
            <template #default="{ row }">
              <strong>{{ row.nickname || row.username }}</strong>
              <p class="muted-line">{{ row.username }} · 用户 #{{ row.userId }}</p>
            </template>
          </el-table-column>
          <el-table-column label="推荐记录" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="$router.push(`/admin/recommend-records?recordId=${row.recordId}`)">
                #{{ row.recordId }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="满意度" width="120">
            <template #default="{ row }">
              <el-tag :type="satisfactionTagType(row.satisfactionScore)" effect="light">
                {{ row.satisfactionScore }} 分
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="评价" min-width="260">
            <template #default="{ row }">
              <div class="feedback-content">
                <div class="reason-tags">
                  <el-tag v-for="tag in row.reasonTags" :key="tag" effect="light">{{ tag }}</el-tag>
                  <span v-if="!row.reasonTags?.length" class="muted-line">未选择原因标签</span>
                </div>
                <p>{{ row.comment || satisfactionLabel(row.satisfactionLevel) }}</p>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
          </el-table-column>
        </el-table>

        <div class="history-footer">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadFeedbacks"
            @size-change="reloadFirstPage"
          />
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { fetchAdminFeedbacks } from '@/api/adminFeedbacks'

const route = useRoute()
const loading = ref(false)
const error = ref('')
const records = ref([])
const total = ref(0)
const routeUserId = computed(() => route.query.userId || '')

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  satisfactionScore: '',
})

onMounted(loadFeedbacks)

watch(
  () => route.query.userId,
  () => reloadFirstPage(),
)

async function loadFeedbacks() {
  loading.value = true
  error.value = ''
  try {
    const response = await fetchAdminFeedbacks({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      userId: routeUserId.value || undefined,
      satisfactionScore: query.satisfactionScore || undefined,
    })
    records.value = response.data.records || []
    total.value = response.data.total || 0
  } catch (requestError) {
    records.value = []
    total.value = 0
    error.value = requestError?.response?.data?.message || requestError?.message || '反馈记录加载失败。'
  } finally {
    loading.value = false
  }
}

function reloadFeedbacks() {
  return loadFeedbacks()
}

function reloadFirstPage() {
  query.page = 1
  return loadFeedbacks()
}

function satisfactionTagType(score) {
  const value = Number(score || 0)
  if (value >= 4) return 'success'
  if (value === 3) return 'warning'
  return 'danger'
}

function satisfactionLabel(value) {
  if (value === 'SATISFIED') return '满意'
  if (value === 'NEUTRAL') return '一般'
  if (value === 'DISSATISFIED') return '不满意'
  return value || '未填写评论'
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

.feedback-toolbar {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 140px auto;
  gap: 10px;
  margin-bottom: 16px;
}

.muted-line {
  margin: 4px 0 0;
  color: var(--color-muted);
  font-size: 12px;
}

.feedback-content {
  display: grid;
  gap: 8px;
}

.feedback-content p {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 13px;
  line-height: 1.7;
}

.reason-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.history-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 980px) {
  .feedback-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
