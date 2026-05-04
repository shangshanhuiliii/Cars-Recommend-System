<template>
  <section class="home-page">
    <el-carousel
      class="hero-carousel"
      height="420px"
      indicator-position="outside"
      arrow="always"
      :interval="5200"
    >
      <el-carousel-item v-for="slide in heroSlides" :key="slide.title">
        <article class="hero-slide" :class="slide.className">
          <div class="hero-copy">
            <p class="eyebrow">{{ slide.eyebrow }}</p>
            <h1>{{ slide.title }}</h1>
            <p>{{ slide.description }}</p>
            <div class="hero-action-row">
              <el-button type="primary" size="large" @click="router.push(slide.to)">
                {{ slide.cta }}
              </el-button>
              <span>{{ slide.actionHint }}</span>
            </div>
          </div>
          <div class="hero-visual" aria-hidden="true">
            <div class="visual-panel">
              <div class="visual-panel__head">
                <span>{{ slide.visualTitle }}</span>
                <strong>{{ slide.visualValue }}</strong>
              </div>
              <img src="/images/cars/default-car.svg" alt="" />
              <div class="visual-specs">
                <span v-for="spec in slide.specs" :key="spec">{{ spec }}</span>
              </div>
            </div>
          </div>
        </article>
      </el-carousel-item>
    </el-carousel>

    <section class="recommend-entry" aria-labelledby="recommend-entry-title">
      <div class="section-heading">
        <p class="section-label">购车推荐入口</p>
        <h2 id="recommend-entry-title">从需求到结果，按你的偏好推进</h2>
      </div>
      <div class="entry-grid">
        <button v-for="entry in quickEntries" :key="entry.title" class="entry-card" type="button" @click="router.push(entry.to)">
          <span>{{ entry.kicker }}</span>
          <h3>{{ entry.title }}</h3>
          <p>{{ entry.description }}</p>
        </button>
      </div>
    </section>

    <section class="capability-section" aria-labelledby="capability-title">
      <div class="section-heading">
        <p class="section-label">核心能力</p>
        <h2 id="capability-title">推荐结果清晰、可比较、可回看</h2>
      </div>
      <div class="capability-grid">
        <article v-for="item in capabilities" :key="item.title" class="capability-card">
          <span>{{ item.index }}</span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
        </article>
      </div>
    </section>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const heroSlides = [
  {
    eyebrow: '个性化购车推荐',
    title: '找到更适合你的车型',
    description: '围绕预算、用车场景、座位需求和偏好重点生成推荐结果，帮助你快速缩小选择范围。',
    cta: '开始推荐',
    actionHint: '约 1 分钟填写核心需求',
    to: '/recommend',
    className: 'hero-slide--primary',
    visualTitle: '推荐决策',
    visualValue: '按偏好匹配',
    specs: ['预算匹配', '场景适配', '偏好优先'],
  },
  {
    eyebrow: '推荐记录回看',
    title: '按预算、场景和偏好生成推荐',
    description: '每次推荐都会保留当时的需求和结果，方便你之后继续比较、复盘和查看车型详情。',
    cta: '查看历史',
    actionHint: '继续查看已保存的结果',
    to: '/history',
    className: 'hero-slide--green',
    visualTitle: '推荐记录',
    visualValue: '快照回看',
    specs: ['历史快照', '结果回看', '详情入口'],
  },
  {
    eyebrow: '收藏与对比',
    title: '查看推荐理由、不足提醒和车型对比',
    description: '把感兴趣的车型加入收藏或对比，从空间、安全、能耗、舒适等维度辅助最终判断。',
    cta: '车型对比',
    actionHint: '最多选择 3 款车型',
    to: '/compare',
    className: 'hero-slide--amber',
    visualTitle: '车型决策',
    visualValue: '横向比较',
    specs: ['理由清晰', '不足提醒', '横向对比'],
  },
]

const quickEntries = [
  {
    kicker: '推荐',
    title: '开始购车推荐',
    description: '填写预算、车型、动力、座位和场景偏好，生成适合当前需求的车型推荐。',
    to: '/recommend',
  },
  {
    kicker: '历史',
    title: '查看推荐历史',
    description: '回看已生成的推荐记录，继续查看推荐详情和车型信息。',
    to: '/history',
  },
  {
    kicker: '收藏',
    title: '我的收藏',
    description: '管理关注车型，后续可从收藏中进入详情或加入对比。',
    to: '/favorites',
  },
  {
    kicker: '对比',
    title: '车型对比',
    description: '对 1-3 款车型做横向比较，查看参数和维度评分差异。',
    to: '/compare',
  },
]

const capabilities = [
  {
    index: '01',
    title: '多维车型评分',
    description: '围绕空间、安全、能耗、智能、舒适、动力、口碑和热度展示车型表现。',
  },
  {
    index: '02',
    title: '个性化偏好权重',
    description: '根据用户主动填写的关注因素和使用场景，让推荐结果贴近真实购车偏好。',
  },
  {
    index: '03',
    title: '推荐理由',
    description: '推荐结果说明车型为什么适合当前需求，减少只看参数带来的判断成本。',
  },
  {
    index: '04',
    title: '不足提醒',
    description: '在推荐车型中同步提示可能不满足预期的地方，帮助用户做平衡取舍。',
  },
  {
    index: '05',
    title: '收藏与对比',
    description: '支持关注车型和横向对比，让后续筛选、复看和决策更连贯。',
  },
]
</script>

<style scoped>
.home-page {
  display: grid;
  gap: 28px;
}

.hero-carousel {
  border-radius: var(--radius-lg);
}

.hero-carousel :deep(.el-carousel__container) {
  border-radius: var(--radius-lg);
}

.hero-slide {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 36px;
  height: 100%;
  overflow: hidden;
  padding: 48px;
  border-radius: var(--radius-lg);
  color: #fff;
  box-shadow: var(--shadow-card);
}

.hero-slide::before {
  position: absolute;
  inset: 0;
  content: "";
  background:
    linear-gradient(90deg, rgba(15, 23, 42, 0.96), rgba(15, 23, 42, 0.72) 55%, rgba(15, 23, 42, 0.18)),
    repeating-linear-gradient(112deg, transparent 0 38px, rgba(255, 255, 255, 0.08) 38px 40px);
}

.hero-slide::after {
  position: absolute;
  right: -120px;
  bottom: -90px;
  width: 520px;
  height: 240px;
  content: "";
  border: 1px solid rgba(255, 255, 255, 0.18);
  transform: rotate(-12deg);
}

.hero-slide--primary {
  background: linear-gradient(135deg, #0f172a, #2563eb 58%, #0891b2);
}

.hero-slide--green {
  background: linear-gradient(135deg, #0f172a, #0f766e 58%, #16a34a);
}

.hero-slide--amber {
  background: linear-gradient(135deg, #111827, #b45309 58%, #f59e0b);
}

.hero-copy,
.hero-visual {
  position: relative;
  z-index: 1;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 14px;
  padding: 7px 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.84);
  font-size: 13px;
  font-weight: 700;
}

.hero-copy h1 {
  max-width: 680px;
  margin: 0;
  font-size: 48px;
  line-height: 1.12;
}

.hero-copy p:not(.eyebrow) {
  max-width: 680px;
  margin: 20px 0 30px;
  color: rgba(255, 255, 255, 0.84);
  font-size: 16px;
  line-height: 1.8;
}

.hero-action-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
}

.hero-action-row span {
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}

.hero-visual {
  display: grid;
  align-content: center;
}

.visual-panel {
  position: relative;
  display: grid;
  gap: 18px;
  min-height: 290px;
  padding: 22px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: var(--radius-md);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.08));
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.28);
  backdrop-filter: blur(10px);
}

.visual-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.visual-panel__head span,
.visual-panel__head strong {
  display: block;
}

.visual-panel__head span {
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

.visual-panel__head strong {
  color: #fff;
  font-size: 22px;
  line-height: 1.2;
  text-align: right;
}

.visual-panel img {
  width: 100%;
  max-height: 168px;
  object-fit: contain;
  filter: drop-shadow(0 22px 24px rgba(15, 23, 42, 0.38));
}

.visual-specs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.visual-specs span {
  min-height: 42px;
  display: grid;
  place-items: center;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.88);
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.recommend-entry,
.capability-section {
  display: grid;
  gap: 18px;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.section-label {
  margin: 0 0 6px;
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 700;
}

.section-heading h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: 24px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.entry-card,
.capability-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--shadow-card);
}

.entry-card {
  min-height: 176px;
  padding: 22px;
  text-align: left;
  cursor: pointer;
}

.entry-card span,
.capability-card span {
  color: var(--color-accent);
  font-size: 13px;
  font-weight: 700;
}

.entry-card h3,
.capability-card h3 {
  margin: 16px 0 10px;
  color: var(--color-primary-dark);
  font-size: 19px;
}

.entry-card p,
.capability-card p {
  margin: 0;
  color: var(--color-muted);
  line-height: 1.7;
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.capability-card {
  min-height: 166px;
  padding: 20px;
}

@media (max-width: 1080px) {
  .hero-slide {
    grid-template-columns: minmax(0, 1fr) 320px;
    gap: 24px;
  }

  .entry-grid,
  .capability-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero-carousel {
    border-radius: var(--radius-md);
  }

  .hero-carousel :deep(.el-carousel__container) {
    height: 390px !important;
  }

  .hero-slide {
    grid-template-columns: 1fr;
    gap: 16px;
    padding: 26px;
  }

  .hero-copy h1 {
    font-size: 29px;
    line-height: 1.16;
  }

  .hero-copy p:not(.eyebrow) {
    margin: 16px 0 22px;
    font-size: 15px;
    line-height: 1.7;
  }

  .hero-action-row {
    align-items: flex-start;
    gap: 10px;
  }

  .hero-action-row span {
    flex-basis: 100%;
  }

  .hero-visual {
    display: none;
  }

  .section-heading {
    display: block;
  }

  .entry-grid,
  .capability-grid {
    grid-template-columns: 1fr;
  }
}
</style>
