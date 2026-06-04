<template>
  <section class="home-page">
    <section class="recommend-orbit" aria-labelledby="recommend-orbit-title">
      <div class="orbit-copy">
        <p class="eyebrow">智能购车决策</p>
        <p>用预算、场景和偏好为您生成可解释推荐</p>
      </div>

      <div class="orbit-stage" aria-label="购车推荐入口与产品能力">
        <button class="orbit-core" type="button" @click="router.push('/recommend')">
          <strong>开始推荐</strong>
        </button>

        <button
          v-for="feature in featureCards"
          :key="feature.section"
          :class="['orbit-node', `orbit-node--${feature.position}`]"
          type="button"
          @click="goFeature(feature.section)"
        >
          <strong>{{ feature.title }}</strong>
        </button>
      </div>
    </section>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const featureCards = [
  {
    section: 'demand',
    position: 'top',
    title: '结构化需求',
    description: '预算、车型、动力、座位和场景按步骤整理',
  },
  {
    section: 'result',
    position: 'right',
    title: '推荐理由',
    description: '说明车型适合当前需求的关键原因',
  },
  {
    section: 'weakness',
    position: 'bottom-right',
    title: '不足提醒',
    description: '提前看到需要取舍的地方',
  },
  {
    section: 'decision',
    position: 'bottom-left',
    title: '候选留存',
    description: '收藏与对比留给后续横向判断',
  },
  {
    section: 'history',
    position: 'left',
    title: '历史快照',
    description: '按当时保存的信息回看结果',
  },
]

function goFeature(section) {
  router.push({ path: '/features', query: { section } })
}
</script>

<style scoped>
.home-page {
  position: relative;
  display: grid;
  min-height: calc(100vh - 76px);
  overflow: hidden;
  animation: page-in 520ms ease both;
  isolation: isolate;
}

.home-page::before,
.home-page::after {
  position: absolute;
  content: "";
  pointer-events: none;
}

.home-page::before {
  inset: 0;
  z-index: -2;
  background:
    radial-gradient(circle at 18% 28%, rgba(125, 211, 252, 0.32), transparent 30%),
    radial-gradient(circle at 72% 18%, rgba(196, 181, 253, 0.34), transparent 28%),
    radial-gradient(circle at 68% 76%, rgba(59, 130, 246, 0.24), transparent 34%),
    radial-gradient(circle at 32% 82%, rgba(45, 212, 191, 0.22), transparent 30%),
    linear-gradient(135deg, #f8fbff 0%, #eef7ff 44%, #f8fbff 100%);
}

.home-page::after {
  inset: -18%;
  z-index: -1;
  background:
    repeating-linear-gradient(
      108deg,
      rgba(255, 255, 255, 0.68) 0%,
      rgba(255, 255, 255, 0.68) 8%,
      transparent 18%,
      transparent 24%,
      rgba(255, 255, 255, 0.6) 34%
    ),
    repeating-linear-gradient(
      108deg,
      rgba(59, 130, 246, 0.34) 8%,
      rgba(129, 140, 248, 0.34) 18%,
      rgba(56, 189, 248, 0.38) 30%,
      rgba(216, 180, 254, 0.34) 42%,
      rgba(45, 212, 191, 0.28) 54%,
      rgba(96, 165, 250, 0.34) 66%
    );
  background-position: 50% 50%, 50% 50%;
  background-size: 240% 240%, 180% 180%;
  filter: blur(18px) saturate(135%);
  opacity: 0.82;
  -webkit-mask-image: radial-gradient(ellipse at 62% 30%, black 14%, rgba(0, 0, 0, 0.72) 54%, transparent 88%);
  mask-image: radial-gradient(ellipse at 62% 30%, black 14%, rgba(0, 0, 0, 0.72) 54%, transparent 88%);
  animation: aurora-shift 60s linear infinite;
}

.recommend-orbit {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(0, 1.2fr);
  gap: clamp(24px, 5vw, 64px);
  align-items: center;
  width: min(1240px, calc(100% - 48px));
  min-height: calc(100vh - 76px);
  margin: 0 auto;
  padding: clamp(32px, 6vw, 72px) 0;
}

.orbit-copy {
  display: grid;
  gap: 16px;
}

.eyebrow {
  margin: 0;
  color: #0b65c2;
  font-size: 46px;
  font-weight: 760;
  line-height: 1;
}

.orbit-copy h1 {
  max-width: 420px;
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 52px;
  font-weight: 760;
  line-height: 1.12;
}

.orbit-copy p:not(.eyebrow) {
  max-width: 460px;
  margin: 0;
  color: var(--color-muted);
  font-size: 17px;
  line-height: 1.8;
}

.orbit-stage {
  position: relative;
  min-height: 590px;
}

.orbit-stage::before,
.orbit-stage::after {
  position: absolute;
  inset: 50% auto auto 50%;
  content: "";
  border: 1px solid rgba(10, 132, 255, 0.12);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.orbit-stage::before {
  width: min(500px, 88vw);
  height: min(500px, 88vw);
}

.orbit-stage::after {
  width: min(370px, 72vw);
  height: min(370px, 72vw);
  background: rgba(255, 255, 255, 0.28);
}

.orbit-core,
.orbit-node {
  position: absolute;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 22px 55px rgba(15, 23, 42, 0.1);
  color: var(--color-primary-dark);
  cursor: pointer;
  text-align: center;
  backdrop-filter: blur(18px);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease;
}

.orbit-core {
  top: 50%;
  left: 50%;
  z-index: 2;
  width: clamp(220px, 24vw, 280px);
  height: clamp(220px, 24vw, 280px);
  padding: 34px;
  background:
    radial-gradient(circle at 35% 22%, rgba(255, 255, 255, 0.96), transparent 30%),
    linear-gradient(145deg, #d9f0ff, #9dd8ff);
  color: #0b65c2;
  transform: translate(-50%, -50%);
}

.orbit-core:hover {
  transform: translate(-50%, -50%) scale(1.035);
  box-shadow: 0 28px 70px rgba(10, 132, 255, 0.16);
}

.orbit-core strong {
  font-size: 46px;
  font-weight: 760;
  line-height: 1;
}

.orbit-node {
  z-index: 1;
  width: 138px;
  height: 138px;
  padding: 18px;
}

.orbit-node:hover {
  border-color: rgba(10, 132, 255, 0.24);
  transform: translate(var(--x, 0), var(--y, 0)) scale(1.045);
  box-shadow: 0 26px 66px rgba(10, 132, 255, 0.16);
}

.orbit-node strong {
  font-size: 17px;
  font-weight: 760;
}

.orbit-node--top {
  top: -24px;
  left: 50%;
  --x: -50%;
  transform: translateX(-50%);
}

.orbit-node--right {
  top: 38%;
  right: 0;
}

.orbit-node--bottom-right {
  right: 12%;
  bottom: 0;
}

.orbit-node--bottom-left {
  bottom: 0;
  left: 12%;
}

.orbit-node--left {
  top: 38%;
  left: 0;
}

@keyframes page-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes aurora-shift {
  from {
    background-position: 50% 50%, 50% 50%;
  }

  to {
    background-position: 350% 50%, 350% 50%;
  }
}

@media (max-width: 1080px) {
  .recommend-orbit {
    grid-template-columns: 1fr;
    min-height: auto;
    width: min(100% - 32px, 1240px);
  }

  .orbit-copy {
    justify-items: center;
    text-align: center;
  }

  .orbit-stage {
    min-height: 620px;
  }

  .orbit-copy h1 {
    font-size: 46px;
  }
}

@media (max-width: 720px) {
  .recommend-orbit {
    padding: 28px 18px;
  }

  .orbit-stage {
    display: grid;
    min-height: 0;
    gap: 12px;
  }

  .orbit-stage::before,
  .orbit-stage::after {
    display: none;
  }

  .orbit-core,
  .orbit-node {
    position: static;
    width: 100%;
    height: auto;
    min-height: 104px;
    border-radius: 24px;
    transform: none;
  }

  .orbit-core:hover,
  .orbit-node:hover {
    transform: translateY(-2px);
  }

  .orbit-core strong {
    font-size: 30px;
  }

  .orbit-copy h1 {
    font-size: 34px;
  }
}
</style>
