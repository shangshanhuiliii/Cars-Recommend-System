<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">
          查看普通用户账号、账号状态以及其需求、推荐历史、收藏和反馈。管理端只读追溯用户数据，不触发推荐生成。
        </p>
      </div>
      <el-button type="primary" plain :loading="loadingList" @click="reloadUsers">刷新用户</el-button>
    </div>

    <el-alert
      v-if="listError"
      class="state-alert"
      type="error"
      :closable="false"
      :title="listError"
      show-icon
    />

    <el-alert
      v-if="actionMessage"
      class="state-alert"
      type="success"
      :closable="false"
      :title="actionMessage"
      show-icon
    />

    <div class="admin-user-layout">
      <div class="panel">
        <div class="panel__body">
          <div class="admin-user-toolbar">
            <el-input
              v-model.trim="query.keyword"
              clearable
              placeholder="搜索用户名、昵称或手机号"
              @keyup.enter="reloadFirstPage"
              @clear="reloadFirstPage"
            />
            <el-select v-model="query.status" clearable placeholder="全部状态" @change="reloadFirstPage">
              <el-option label="ACTIVE" value="ACTIVE" />
              <el-option label="DISABLED" value="DISABLED" />
            </el-select>
            <el-button type="primary" :loading="loadingList" @click="reloadFirstPage">查询</el-button>
          </div>

          <el-table
            v-loading="loadingList"
            :data="users"
            class="admin-user-table"
            row-key="id"
            @row-click="selectUser"
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="用户">
              <template #default="{ row }">
                <strong>{{ row.nickname || row.username }}</strong>
                <p class="muted-line">{{ row.username }} · {{ row.phone || '未填写手机号' }}</p>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'warning'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="recommendRecordCount" label="推荐" width="90" />
            <el-table-column prop="favoriteCount" label="收藏" width="90" />
            <el-table-column prop="feedbackCount" label="反馈" width="90" />
            <el-table-column label="创建时间" width="170">
              <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="selectUser(row)">详情</el-button>
                <el-button
                  link
                  :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
                  :loading="updatingUserId === row.id"
                  @click.stop="toggleStatus(row)"
                >
                  {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="history-footer">
            <el-pagination
              v-model:current-page="query.page"
              v-model:page-size="query.size"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @current-change="loadUsers"
              @size-change="reloadFirstPage"
            />
          </div>
        </div>
      </div>

      <aside class="panel admin-user-detail">
        <div class="panel__body">
          <el-skeleton v-if="loadingDetail" :rows="12" animated />

          <el-alert
            v-else-if="detailError"
            type="error"
            :closable="false"
            :title="detailError"
            show-icon
          />

          <el-empty v-else-if="!detail" description="请选择左侧用户" />

          <template v-else>
            <div class="detail-head">
              <div>
                <p class="eyebrow">用户 #{{ detail.user.id }}</p>
                <h2>{{ detail.user.nickname || detail.user.username }}</h2>
                <p class="muted-line">{{ detail.user.username }} · {{ detail.user.phone || '未填写手机号' }}</p>
              </div>
              <el-tag :type="detail.user.status === 'ACTIVE' ? 'success' : 'warning'" size="large">
                {{ detail.user.status }}
              </el-tag>
            </div>

            <div class="admin-user-stats">
              <div>
                <span>需求</span>
                <strong>{{ detail.summary.demandCount }}</strong>
              </div>
              <div>
                <span>推荐</span>
                <strong>{{ detail.summary.recommendRecordCount }}</strong>
              </div>
              <div>
                <span>收藏</span>
                <strong>{{ detail.summary.favoriteCount }}</strong>
              </div>
              <div>
                <span>反馈</span>
                <strong>{{ detail.summary.feedbackCount }}</strong>
              </div>
            </div>

            <section class="detail-section">
              <h3>最近需求</h3>
              <div v-if="detail.latestDemand" class="demand-card">
                <strong>#{{ detail.latestDemand.id }}</strong>
                <p>{{ detail.latestDemand.profileText || detail.latestDemand.rawText || '暂无画像文本' }}</p>
                <small>{{ formatDate(detail.latestDemand.createTime) }}</small>
              </div>
              <el-empty v-else description="暂无需求" />
            </section>

            <section class="detail-section">
              <h3>最近推荐记录</h3>
              <div v-if="detail.recentRecommendRecords?.length" class="compact-list">
                <button
                  v-for="record in detail.recentRecommendRecords"
                  :key="record.recordId"
                  type="button"
                  @click="$router.push(`/admin/recommend-records?userId=${detail.user.id}`)"
                >
                  <span>#{{ record.recordId }} · {{ record.recommendStatus }}</span>
                  <small>{{ formatDate(record.createTime) }}</small>
                </button>
              </div>
              <el-empty v-else description="暂无推荐记录" />
            </section>

            <section class="detail-section">
              <h3>收藏车型</h3>
              <div v-if="detail.favorites?.length" class="compact-list">
                <button v-for="favorite in detail.favorites" :key="favorite.favoriteId" type="button">
                  <span>{{ favorite.brand }} {{ favorite.series }} {{ favorite.modelName }}</span>
                  <small>{{ formatDate(favorite.favoriteTime) }}</small>
                </button>
              </div>
              <el-empty v-else description="暂无收藏" />
            </section>

            <section class="detail-section">
              <h3>反馈记录</h3>
              <div v-if="detail.feedbacks?.length" class="compact-list">
                <button v-for="feedback in detail.feedbacks" :key="feedback.id" type="button">
                  <span>记录 #{{ feedback.recordId }} · {{ feedback.satisfactionScore }} 分</span>
                  <small>{{ feedback.comment || feedback.satisfactionLevel }}</small>
                </button>
              </div>
              <el-empty v-else description="暂无反馈" />
            </section>
          </template>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'

import {
  fetchAdminUserDetail,
  fetchAdminUsers,
  updateAdminUserStatus,
} from '@/api/adminUsers'

const loadingList = ref(false)
const loadingDetail = ref(false)
const listError = ref('')
const detailError = ref('')
const actionMessage = ref('')
const users = ref([])
const total = ref(0)
const detail = ref(null)
const updatingUserId = ref(null)

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: '',
})

onMounted(loadUsers)

async function loadUsers() {
  loadingList.value = true
  listError.value = ''
  actionMessage.value = ''
  try {
    const response = await fetchAdminUsers({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
    })
    users.value = response.data.records || []
    total.value = response.data.total || 0
  } catch (error) {
    listError.value = error?.response?.data?.message || error?.message || '加载用户列表失败。'
  } finally {
    loadingList.value = false
  }
}

function reloadUsers() {
  return loadUsers()
}

function reloadFirstPage() {
  query.page = 1
  return loadUsers()
}

async function selectUser(row) {
  if (!row?.id) {
    return
  }
  loadingDetail.value = true
  detailError.value = ''
  try {
    const response = await fetchAdminUserDetail(row.id)
    detail.value = response.data
  } catch (error) {
    detailError.value = error?.response?.data?.message || error?.message || '加载用户详情失败。'
  } finally {
    loadingDetail.value = false
  }
}

async function toggleStatus(row) {
  updatingUserId.value = row.id
  actionMessage.value = ''
  listError.value = ''
  try {
    const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
    const response = await updateAdminUserStatus(row.id, nextStatus)
    row.status = response.data.status
    actionMessage.value = `用户 ${row.username} 已更新为 ${response.data.status}`
    if (detail.value?.user?.id === row.id) {
      detail.value.user.status = response.data.status
    }
  } catch (error) {
    listError.value = error?.response?.data?.message || error?.message || '更新用户状态失败。'
  } finally {
    updatingUserId.value = null
  }
}

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}
</script>

<style scoped>
.admin-user-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 24px;
  align-items: start;
}

.admin-user-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 160px auto;
  gap: 12px;
  margin-bottom: 18px;
}

.admin-user-table {
  width: 100%;
}

.admin-user-detail {
  position: sticky;
  top: 96px;
}

.muted-line {
  margin: 4px 0 0;
  color: var(--color-muted);
  font-size: 12px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-head h2 {
  margin: 4px 0 0;
  color: var(--color-primary-dark);
}

.eyebrow {
  margin: 0;
  color: var(--color-muted);
  font-size: 12px;
  letter-spacing: 0.18em;
}

.admin-user-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin: 18px 0;
}

.admin-user-stats div {
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: #f8fafc;
}

.admin-user-stats span,
.compact-list small,
.demand-card small {
  display: block;
  color: var(--color-muted);
  font-size: 12px;
}

.admin-user-stats strong {
  display: block;
  margin-top: 4px;
  color: var(--color-primary-dark);
  font-size: 20px;
}

.detail-section {
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.detail-section + .detail-section {
  margin-top: 16px;
}

.detail-section h3 {
  margin: 0 0 10px;
  color: var(--color-primary-dark);
  font-size: 15px;
}

.demand-card {
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: #ffffff;
}

.demand-card p {
  margin: 6px 0;
  color: var(--color-text);
  font-size: 13px;
  line-height: 1.7;
}

.compact-list {
  display: grid;
  gap: 8px;
}

.compact-list button {
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: #fff;
  color: var(--color-text);
  cursor: pointer;
  text-align: left;
}

.compact-list button:hover {
  border-color: var(--color-primary);
}

@media (max-width: 980px) {
  .admin-user-layout {
    grid-template-columns: 1fr;
  }

  .admin-user-detail {
    position: static;
  }

  .admin-user-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
