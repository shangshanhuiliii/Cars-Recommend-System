<template>
  <section class="home-page">
    <section class="carousel-shell" aria-label="车辆图片轮播">
      <div v-if="carouselLoading" class="carousel-state">
        <span class="state-orb" />
        <strong>正在加载车辆图片</strong>
      </div>

      <div v-else-if="carouselError" class="carousel-state carousel-state--error">
        <strong>车辆轮播暂时不可用</strong>
        <p>{{ carouselError }}</p>
        <el-button type="primary" plain @click="loadCarousel">重新加载</el-button>
      </div>

      <div v-else-if="!carouselCars.length" class="carousel-state">
        <img src="/images/cars/default-car.svg" alt="" />
        <strong>暂无可展示车型</strong>
      </div>

      <el-carousel
        v-else
        class="vehicle-carousel"
        height="560px"
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
            <span class="vehicle-badge">
              <span class="vehicle-badge__brand">{{ car.brand }}</span>
              <strong>{{ car.modelName }}</strong>
              <span class="vehicle-badge__price">{{ formatPrice(car.guidePrice) }}</span>
              <span class="vehicle-badge__specs">
                <span>{{ car.bodyType || '车型待补充' }}</span>
                <span>{{ car.energyType || '动力待补充' }}</span>
                <span>{{ formatSeats(car.seats) }}</span>
              </span>
            </span>
          </button>
        </el-carousel-item>
      </el-carousel>
    </section>

    <section class="recommend-core" aria-labelledby="recommend-core-title">
      <p class="eyebrow">核心入口</p>
      <h1 id="recommend-core-title">开始购车推荐</h1>
      <p>用预算、场景和偏好找到适合车型。</p>
      <el-button class="primary-cta" type="primary" size="large" @click="router.push('/recommend')">
        开始购车推荐
      </el-button>
    </section>

    <section class="feature-entry" aria-labelledby="feature-entry-title">
      <div class="section-heading">
        <p class="section-label">产品特色</p>
        <h2 id="feature-entry-title">更少信息噪音，更清楚的购车判断</h2>
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

const featureCards = [
  {
    index: '01',
    section: 'demand',
    title: '结构化需求',
    description: '预算、车型、动力、座位、场景和偏好按步骤整理。',
  },
  {
    index: '02',
    section: 'result',
    title: '推荐理由',
    description: '说明车型适合当前需求的关键原因。',
  },
  {
    index: '03',
    section: 'weakness',
    title: '不足提醒',
    description: '提前看到需要取舍的地方。',
  },
  {
    index: '04',
    section: 'decision',
    title: '候选留存',
    description: '把关注车型留给后续查看和横向判断。',
  },
  {
    index: '05',
    section: 'history',
    title: '历史快照',
    description: '按当时保存的信息回看结果。',
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
    carouselError.value = error?.response?.data?.message || '请稍后再试。'
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

.carousel-shell {
  min-height: 600px;
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
  border: 1px solid rgba(255, 255, 255, 0.72);
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

.carousel-slide::after {
  position: absolute;
  inset: auto 0 0;
  height: 46%;
  content: "";
  background: linear-gradient(0deg, rgba(8, 13, 23, 0.58), transparent);
}

.carousel-slide:hover .carousel-slide__image {
  transform: scale(1.04);
}

.vehicle-badge {
  position: absolute;
  right: clamp(18px, 4vw, 44px);
  bottom: clamp(18px, 4vw, 44px);
  z-index: 1;
  display: grid;
  width: min(390px, calc(100% - 36px));
  gap: 8px;
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 24px;
  background: rgba(17, 24, 39, 0.42);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(18px) saturate(145%);
}

.vehicle-badge__brand {
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.vehicle-badge strong {
  color: #fff;
  font-size: clamp(24px, 3vw, 34px);
  line-height: 1.08;
  letter-spacing: -0.035em;
}

.vehicle-badge__price {
  color: rgba(255, 255, 255, 0.9);
  font-size: 18px;
  font-weight: 850;
}

.vehicle-badge__specs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.vehicle-badge__specs span {
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.13);
  color: rgba(255, 255, 255, 0.86);
  font-size: 12px;
  font-weight: 800;
}

.carousel-state {
  display: grid;
  min-height: 560px;
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
  justify-items: center;
  gap: 16px;
  padding: clamp(32px, 6vw, 68px);
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: var(--radius-xl);
  background:
    radial-gradient(circle at 50% 0%, rgba(90, 200, 250, 0.2), transparent 34%),
    rgba(255, 255, 255, 0.74);
  box-shadow: var(--shadow-card);
  text-align: center;
  backdrop-filter: blur(20px);
}

.eyebrow,
.section-label {
  margin: 0;
  color: #0b65c2;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.13em;
}

.recommend-core h1 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: clamp(38px, 6vw, 68px);
  line-height: 1.02;
  letter-spacing: -0.055em;
}

.recommend-core p:not(.eyebrow) {
  max-width: 560px;
  margin: 0;
  color: var(--color-muted);
  font-size: 17px;
  line-height: 1.75;
}

.primary-cta {
  min-width: 178px;
  min-height: 48px;
  margin-top: 8px;
  border-radius: 999px;
  box-shadow: 0 16px 30px rgba(10, 132, 255, 0.24);
  font-weight: 850;
}

.feature-entry {
  display: grid;
  gap: 18px;
}

.section-heading {
  display: grid;
  gap: 10px;
}

.section-heading h2 {
  margin: 0;
  color: var(--color-primary-dark);
  font-size: clamp(24px, 3vw, 36px);
  letter-spacing: -0.035em;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.feature-card {
  display: grid;
  gap: 12px;
  min-height: 164px;
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

.feature-card:hover {
  transform: translateY(-4px);
  border-color: rgba(10, 132, 255, 0.22);
  box-shadow: 0 20px 48px rgba(17, 24, 39, 0.1);
}

.feature-card span {
  color: #0b65c2;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.feature-card strong {
  color: var(--color-primary-dark);
  font-size: 20px;
  letter-spacing: -0.02em;
}

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
  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .carousel-shell {
    min-height: 500px;
  }

  .vehicle-carousel :deep(.el-carousel__container),
  .carousel-state {
    height: 460px !important;
    min-height: 460px;
  }

  .vehicle-badge {
    right: 18px;
    bottom: 18px;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .recommend-core h1 {
    font-size: 34px;
  }

  .recommend-core p:not(.eyebrow) {
    font-size: 15px;
  }

  .vehicle-badge strong {
    font-size: 24px;
  }

  .vehicle-badge__price {
    font-size: 16px;
  }
}
</style>
