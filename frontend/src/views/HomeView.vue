<template>
  <section class="home-page">
    <section class="home-hero" aria-labelledby="home-hero-title">
      <div class="hero-heading">
        <p class="eyebrow">产品化购车决策</p>
        <h1 id="home-hero-title">先看真实车型，再开始一段清晰的购车推荐。</h1>
        <p>
          轮播展示已审核车型图片；点击任意车辆可直接进入详情页。推荐入口在下方独立呈现，让浏览和决策各自清晰。
        </p>
      </div>

      <div class="carousel-shell">
        <div v-if="carouselLoading" class="carousel-state">
          <span class="state-orb" />
          <strong>正在加载车辆图片</strong>
          <p>从公开车型接口读取已审核车型。</p>
        </div>

        <div v-else-if="carouselError" class="carousel-state carousel-state--error">
          <strong>车辆轮播暂时不可用</strong>
          <p>{{ carouselError }}</p>
          <el-button type="primary" plain @click="loadCarousel">重新加载</el-button>
        </div>

        <div v-else-if="!carouselCars.length" class="carousel-state">
          <img src="/images/cars/default-car.svg" alt="" />
          <strong>暂时没有可展示车型</strong>
          <p>有审核通过车型后，首页会自动展示车辆图片。</p>
        </div>

        <el-carousel
          v-else
          class="vehicle-carousel"
          height="520px"
          indicator-position="outside"
          arrow="always"
          trigger="click"
          :interval="5600"
        >
          <el-carousel-item v-for="car in carouselCars" :key="car.id">
            <button
              class="carousel-slide"
              type="button"
              :aria-label="`查看${car.brand}${car.modelName}详情`"
              @click="goCar(car.id)"
            >
              <img class="carousel-slide__image" :src="carImageSrc(car.imageUrl)" :alt="`${car.brand} ${car.modelName}`" @error="fallbackCarImage" />
              <span class="carousel-slide__veil" aria-hidden="true" />
              <span class="carousel-slide__content">
                <span class="slide-kicker">{{ car.brand }} · {{ car.series || '精选车型' }}</span>
                <strong>{{ car.modelName }}</strong>
                <span class="slide-price">{{ formatPrice(car.guidePrice) }}</span>
                <span class="slide-specs">
                  <span>{{ car.energyType || '动力待补充' }}</span>
                  <span>{{ car.bodyType || '车型待补充' }}</span>
                  <span>{{ formatSeats(car.seats) }}</span>
                </span>
              </span>
            </button>
          </el-carousel-item>
        </el-carousel>
      </div>
    </section>

    <section class="recommend-core" aria-labelledby="recommend-core-title">
      <div class="recommend-core__copy">
        <p class="eyebrow">核心入口</p>
        <h2 id="recommend-core-title">开始购车推荐</h2>
        <p>用预算、场景、偏好找到适合车型。系统会保存当次需求和推荐结果，后续可继续回看和比较。</p>
        <div class="recommend-core__actions">
          <el-button class="primary-cta" type="primary" size="large" @click="router.push('/recommend')">
            开始购车推荐
          </el-button>
          <span>约 1 分钟完成核心需求</span>
        </div>
      </div>
      <div class="recommend-core__panel" aria-hidden="true">
        <span class="panel-chip">预算</span>
        <span class="panel-chip">场景</span>
        <span class="panel-chip">偏好</span>
        <div class="panel-line panel-line--wide" />
        <div class="panel-line" />
        <div class="panel-score">推荐分</div>
      </div>
    </section>

    <section class="assist-section" aria-labelledby="assist-title">
      <div class="section-heading">
        <p class="section-label">辅助入口</p>
        <h2 id="assist-title">把关注车型留给后续判断</h2>
      </div>
      <div class="assist-grid">
        <button v-for="entry in assistEntries" :key="entry.title" class="assist-card" type="button" @click="router.push(entry.to)">
          <span>{{ entry.kicker }}</span>
          <strong>{{ entry.title }}</strong>
          <p>{{ entry.description }}</p>
        </button>
      </div>
    </section>

    <section class="feature-entry" aria-labelledby="feature-entry-title">
      <div class="section-heading">
        <p class="section-label">特色介绍</p>
        <h2 id="feature-entry-title">了解每个环节如何辅助购车判断</h2>
      </div>
      <div class="feature-grid">
        <button v-for="feature in featureCards" :key="feature.section" class="feature-card" type="button" @click="goFeature(feature.section)">
          <span>{{ feature.index }}</span>
          <strong>{{ feature.title }}</strong>
          <p>{{ feature.description }}</p>
        </button>
      </div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchHomeCarouselCars } from '@/api/cars'
import { carImageSrc, fallbackCarImage } from '@/utils/carImage'

const router = useRouter()

const carouselCars = ref([])
const carouselLoading = ref(false)
const carouselError = ref('')

const assistEntries = [
  {
    kicker: '历史',
    title: '历史回看',
    description: '读取每次推荐发生时保存的结果，适合继续比较和复盘。',
    to: '/history',
  },
  {
    kicker: '收藏',
    title: '我的收藏',
    description: '把感兴趣的车型集中起来，之后从收藏进入详情。',
    to: '/favorites',
  },
  {
    kicker: '对比',
    title: '车型对比',
    description: '选择 1-3 款车型横向查看参数和维度表现。',
    to: '/compare',
  },
]

const featureCards = [
  {
    index: '01',
    section: 'demand',
    title: '结构化购车需求',
    description: '预算、车型、动力、座位、场景和偏好按步骤组织。',
  },
  {
    index: '02',
    section: 'result',
    title: '推荐理由',
    description: '结果说明车型为什么适合当前需求，并展示重点标签。',
  },
  {
    index: '03',
    section: 'weakness',
    title: '不足提醒',
    description: '同步提示可能需要取舍的地方，避免只看亮点。',
  },
  {
    index: '04',
    section: 'decision',
    title: '收藏与对比',
    description: '把候选车留给后续查看，不改变推荐结果本身。',
  },
  {
    index: '05',
    section: 'history',
    title: '历史回看',
    description: '按当时保存的快照读取结果，保留决策上下文。',
  },
]

onMounted(loadCarousel)

async function loadCarousel() {
  carouselLoading.value = true
  carouselError.value = ''
  try {
    const response = await fetchHomeCarouselCars(6)
    carouselCars.value = Array.isArray(response?.data) ? response.data : []
  } catch (error) {
    carouselError.value = error?.response?.data?.message || '请稍后再试，页面其他入口仍可使用。'
    carouselCars.value = []
  } finally {
    carouselLoading.value = false
  }
}

function goCar(id) {
  if (id) {
    router.push(`/car/${id}`)
  }
}

function goFeature(section) {
  router.push({ path: '/features', query: { section } })
}

function formatPrice(value) {
  const amount = Number(value)
  if (!Number.isFinite(amount) || amount <= 0) {
    return '指导价待补充'
  }
  const price = amount / 10000
  return `${Number.isInteger(price) ? price.toFixed(0) : price.toFixed(1)}万`
}

function formatSeats(seats) {
  const value = Number(seats)
  return Number.isFinite(value) && value > 0 ? `${value}座` : '座位待补充'
}
</script>

<style scoped>
.home-page {
  display: grid;
  gap: 34px;
  animation: page-in 520ms ease both;
}

.home-hero {
  display: grid;
  gap: 22px;
}

.hero-heading {
  max-width: 860px;
}

.eyebrow,
.section-label {
  margin: 0 0 10px;
  color: #0b65c2;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.hero-heading h1 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: clamp(40px, 5vw, 72px);
  line-height: 1.03;
  letter-spacing: -0.055em;
}

.hero-heading p:not(.eyebrow) {
  max-width: 760px;
  margin: 20px 0 0;
  color: var(--color-muted);
  font-size: 17px;
  line-height: 1.85;
}

.carousel-shell {
  min-height: 560px;
}

.vehicle-carousel :deep(.el-carousel__container),
.carousel-slide {
  border-radius: var(--radius-xl);
}

.vehicle-carousel :deep(.el-carousel__arrow) {
  border: 1px solid rgba(255, 255, 255, 0.6);
  background: rgba(17, 24, 39, 0.4);
  backdrop-filter: blur(16px);
}

.carousel-slide {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  overflow: hidden;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: #111827;
  box-shadow: 0 28px 70px rgba(17, 24, 39, 0.16);
  color: #fff;
  cursor: pointer;
  text-align: left;
}

.carousel-slide__image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scale(1.01);
  transition: transform 700ms ease;
}

.carousel-slide:hover .carousel-slide__image {
  transform: scale(1.045);
}

.carousel-slide__veil {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(8, 13, 23, 0.84), rgba(8, 13, 23, 0.45) 48%, rgba(8, 13, 23, 0.08)),
    linear-gradient(0deg, rgba(8, 13, 23, 0.32), transparent 48%);
}

.carousel-slide__content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  width: min(560px, 70%);
  height: 100%;
  padding: 52px;
}

.slide-kicker {
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.carousel-slide__content strong {
  margin-top: 14px;
  font-size: clamp(34px, 5vw, 62px);
  line-height: 1.02;
  letter-spacing: -0.055em;
}

.slide-price {
  margin-top: 18px;
  font-size: 22px;
  font-weight: 800;
}

.slide-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.slide-specs span {
  padding: 9px 13px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  font-weight: 700;
  backdrop-filter: blur(12px);
}

.carousel-state {
  display: grid;
  min-height: 520px;
  place-items: center;
  align-content: center;
  gap: 12px;
  padding: 36px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.68);
  box-shadow: var(--shadow-card);
  text-align: center;
  backdrop-filter: blur(18px);
}

.carousel-state img {
  width: min(320px, 80%);
  opacity: 0.72;
}

.carousel-state strong {
  color: var(--color-primary-dark);
  font-size: 24px;
}

.carousel-state p {
  margin: 0;
  color: var(--color-muted);
}

.carousel-state--error {
  border-color: rgba(194, 65, 12, 0.2);
}

.state-orb {
  width: 44px;
  height: 44px;
  border: 4px solid rgba(10, 132, 255, 0.14);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.recommend-core {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 28px;
  padding: clamp(28px, 5vw, 56px);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: var(--radius-xl);
  background:
    radial-gradient(circle at 90% 10%, rgba(90, 200, 250, 0.22), transparent 30%),
    rgba(255, 255, 255, 0.74);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(20px);
}

.recommend-core__copy h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: clamp(34px, 5vw, 64px);
  line-height: 1.04;
  letter-spacing: -0.055em;
}

.recommend-core__copy p:not(.eyebrow) {
  max-width: 660px;
  margin: 18px 0 0;
  color: var(--color-muted);
  font-size: 17px;
  line-height: 1.85;
}

.recommend-core__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  margin-top: 30px;
}

.recommend-core__actions span {
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 700;
}

.primary-cta {
  min-width: 178px;
  min-height: 48px;
  border-radius: 999px;
  box-shadow: 0 16px 30px rgba(10, 132, 255, 0.24);
  font-weight: 800;
}

.recommend-core__panel {
  position: relative;
  display: grid;
  align-content: start;
  gap: 14px;
  min-height: 250px;
  padding: 24px;
  border: 1px solid var(--color-border);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.62);
  box-shadow: var(--shadow-soft);
}

.panel-chip {
  width: fit-content;
  padding: 9px 14px;
  border-radius: 999px;
  background: #f2f4f7;
  color: #344054;
  font-size: 13px;
  font-weight: 800;
}

.panel-line {
  width: 72%;
  height: 10px;
  margin-top: 10px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(10, 132, 255, 0.28), rgba(10, 132, 255, 0.06));
}

.panel-line--wide {
  width: 100%;
}

.panel-score {
  position: absolute;
  right: 24px;
  bottom: 24px;
  display: grid;
  width: 88px;
  height: 88px;
  place-items: center;
  border-radius: 50%;
  background: #111827;
  color: #fff;
  font-size: 14px;
  font-weight: 900;
}

.assist-section,
.feature-entry {
  display: grid;
  gap: 18px;
}

.section-heading h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: clamp(24px, 3vw, 36px);
  letter-spacing: -0.035em;
}

.assist-grid,
.feature-grid {
  display: grid;
  gap: 16px;
}

.assist-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.feature-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.assist-card,
.feature-card {
  display: grid;
  gap: 12px;
  min-height: 168px;
  padding: 22px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: var(--shadow-soft);
  color: var(--color-text);
  cursor: pointer;
  text-align: left;
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease;
}

.assist-card:hover,
.feature-card:hover {
  transform: translateY(-4px);
  border-color: rgba(10, 132, 255, 0.22);
  box-shadow: 0 20px 48px rgba(17, 24, 39, 0.1);
}

.assist-card span,
.feature-card span {
  color: #0b65c2;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.assist-card strong,
.feature-card strong {
  color: var(--color-primary-dark);
  font-size: 20px;
  letter-spacing: -0.02em;
}

.assist-card p,
.feature-card p {
  margin: 0;
  color: var(--color-muted);
  line-height: 1.7;
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

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1080px) {
  .recommend-core {
    grid-template-columns: 1fr;
  }

  .recommend-core__panel {
    min-height: 220px;
  }

  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .hero-heading h1 {
    font-size: 42px;
  }

  .carousel-shell {
    min-height: 480px;
  }

  .vehicle-carousel :deep(.el-carousel__container),
  .carousel-state {
    height: 430px !important;
    min-height: 430px;
  }

  .carousel-slide__content {
    width: 100%;
    padding: 30px;
  }

  .assist-grid,
  .feature-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .hero-heading h1,
  .recommend-core__copy h2 {
    font-size: 34px;
  }

  .hero-heading p:not(.eyebrow),
  .recommend-core__copy p:not(.eyebrow) {
    font-size: 15px;
  }

  .carousel-slide__content strong {
    font-size: 32px;
  }

  .slide-price {
    font-size: 18px;
  }

  .slide-specs {
    gap: 8px;
  }

  .recommend-core {
    padding: 24px;
  }
}
</style>
