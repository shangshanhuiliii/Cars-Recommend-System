<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">系统健康检查</h1>
        <p class="page-subtitle">查看后端服务和数据库连接状态，用于管理端运行排查。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadHealth">重新检查</el-button>
    </div>

    <el-alert
      v-if="error"
      class="state-alert"
      type="error"
      :closable="false"
      :title="error"
      show-icon
    />

    <div class="health-grid">
      <article class="health-card">
        <span>后端服务状态</span>
        <strong>{{ backendStatusText }}</strong>
        <el-tag :type="backendTagType" effect="light">{{ backendLabel }}</el-tag>
      </article>
      <article class="health-card">
        <span>数据库状态</span>
        <strong>{{ databaseStatusText }}</strong>
        <el-tag :type="databaseTagType" effect="light">{{ databaseLabel }}</el-tag>
      </article>
      <article class="health-card">
        <span>检查时间</span>
        <strong>{{ checkedAtText }}</strong>
        <el-tag type="info" effect="light">本次页面检查</el-tag>
      </article>
    </div>

    <section class="panel raw-panel">
      <div class="panel__body">
        <el-collapse>
          <el-collapse-item title="原始返回信息" name="raw-response">
            <pre>{{ rawResponseText }}</pre>
          </el-collapse-item>
        </el-collapse>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'

import { getHealth } from '@/api/health'

const loading = ref(false)
const error = ref('')
const status = ref({})
const rawResponse = ref(null)
const checkedAt = ref('')

const backendStatus = computed(() => status.value.backend || 'unknown')
const databaseStatus = computed(() => status.value.database || 'unknown')
const backendLabel = computed(() => (backendStatus.value === 'running' ? '正常' : '异常'))
const backendStatusText = computed(() => (backendStatus.value === 'running' ? '运行中' : '未知'))
const backendTagType = computed(() => (backendStatus.value === 'running' ? 'success' : 'danger'))
const databaseLabel = computed(() => {
  if (databaseStatus.value === 'connected') return '已连接'
  if (databaseStatus.value === 'not_configured') return '未配置'
  return '不可用'
})
const databaseStatusText = computed(() => {
  if (databaseStatus.value === 'connected') return '已连接'
  if (databaseStatus.value === 'not_configured') return '未配置'
  return '未知'
})
const databaseTagType = computed(() => {
  if (databaseStatus.value === 'connected') return 'success'
  if (databaseStatus.value === 'not_configured') return 'warning'
  return 'danger'
})
const checkedAtText = computed(() => checkedAt.value || '尚未检查')
const rawResponseText = computed(() => JSON.stringify(rawResponse.value || {}, null, 2))

onMounted(loadHealth)

async function loadHealth() {
  loading.value = true
  error.value = ''
  try {
    const response = await getHealth()
    rawResponse.value = response.data || {}
    status.value = response.data || {}
    checkedAt.value = formatTime(new Date())
  } catch (requestError) {
    status.value = {}
    rawResponse.value = requestError?.response?.data || {
      message: requestError?.message || '健康检查请求失败',
    }
    checkedAt.value = formatTime(new Date())
    error.value = requestError?.response?.data?.message || requestError?.message || '系统健康检查失败。'
  } finally {
    loading.value = false
  }
}

function formatTime(value) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(value)
}
</script>

<style scoped>
.state-alert {
  margin-bottom: 18px;
}

.health-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.health-card {
  min-height: 160px;
  padding: 22px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--shadow-card);
}

.health-card span {
  display: block;
  color: var(--color-muted);
  font-size: 13px;
}

.health-card strong {
  display: block;
  margin: 12px 0 18px;
  color: var(--color-primary-dark);
  font-size: 28px;
}

.raw-panel {
  margin-top: 18px;
}

pre {
  overflow: auto;
  margin: 0;
  padding: 16px;
  border-radius: var(--radius-sm);
  background: #0f172a;
  color: #e5e7eb;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .health-grid {
    grid-template-columns: 1fr;
  }
}
</style>
