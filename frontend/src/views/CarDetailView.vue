<template>
  <section>
    <div class="page-header">
      <div>
        <h1 class="page-title">车型详情</h1>
        <p class="page-subtitle">车型 {{ carId }} 的详情页路由已占位，后续接入基础信息、参数和评分来源。</p>
      </div>
    </div>
    <div class="panel">
      <div class="panel__body">
        <div class="placeholder-list">
          <div class="placeholder-item">
            <h3>车型基础信息</h3>
            <p>品牌、车系、车型、指导价、车型类型、动力类型和座位数。</p>
          </div>
          <div class="placeholder-item">
            <h3>特征评分来源</h3>
            <p>空间、安全、能耗、智能、舒适、动力、口碑和热度评分。</p>
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
const carId = computed(() => route.params.id)
</script>
