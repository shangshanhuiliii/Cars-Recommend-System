<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">推荐结果</h1>
        <p class="page-subtitle">推荐记录 {{ recordId }} 的结果页路由已占位，业务数据会在推荐算法和追溯阶段接入。</p>
      </div>
    </div>
    <div class="panel">
      <div class="panel__body">
        <div class="placeholder-list">
          <div class="placeholder-item">
            <h3>综合匹配度</h3>
            <p>推荐卡片将展示总分、价格分、维度分数和匹配状态。</p>
          </div>
          <div class="placeholder-item">
            <h3>解释与不足</h3>
            <p>每条推荐会展示标签、推荐理由、不足提醒和降级提示。</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const recordId = computed(() => route.params.recordId)
</script>
