<template>
  <section class="vehicle-carousel-shell" aria-label="车辆图片轮播">
    <div v-if="loading" class="carousel-state">
      <span class="state-spinner" />
      <strong>正在加载车辆图片</strong>
    </div>

    <div v-else-if="errorMessage" class="carousel-state carousel-state--error">
      <strong>车辆轮播暂时不可用</strong>
      <p>{{ errorMessage }}</p>
      <el-button type="primary" plain @click="loadCarousel">重新加载</el-button>
    </div>

    <div v-else-if="!carouselCars.length" class="carousel-state">
      <img src="/images/cars/default-car.svg" alt="" />
      <strong>暂无可展示车型</strong>
    </div>

    <el-carousel
      v-else
      class="vehicle-carousel"
      height="clamp(240px, 33vh, 340px)"
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
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchHomeCarouselCars } from '@/api/cars'
import { carImageSrc, fallbackCarImage } from '@/utils/carImage'

const router = useRouter()
const carouselCars = ref([])
const loading = ref(false)
const errorMessage = ref('')

onMounted(loadCarousel)

async function loadCarousel() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await fetchHomeCarouselCars(6)
    carouselCars.value = Array.isArray(response?.data) ? response.data : []
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || '请稍后再试。'
    carouselCars.value = []
  } finally {
    loading.value = false
  }
}

function goCar(id) {
  if (id) {
    router.push(`/car/${id}`)
  }
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
.vehicle-carousel-shell {
  --vehicle-carousel-height: clamp(240px, 33vh, 340px);

  position: relative;
  display: grid;
  min-height: calc(var(--vehicle-carousel-height) + 58px);
  place-items: center;
  padding: 22px clamp(10px, 4vw, 52px);
  isolation: isolate;
}

.vehicle-carousel-shell::before,
.vehicle-carousel-shell::after {
  position: absolute;
  z-index: -1;
  content: "";
  pointer-events: none;
}

.vehicle-carousel-shell::before {
  inset: 0;
  border-radius: 34px;
  background:
    radial-gradient(circle at 12% 42%, rgba(10, 132, 255, 0.16), transparent 26%),
    radial-gradient(circle at 88% 35%, rgba(90, 200, 250, 0.14), transparent 24%),
    linear-gradient(90deg, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0.56), rgba(255, 255, 255, 0.22));
}

.vehicle-carousel-shell::after {
  top: 50%;
  left: 50%;
  width: min(1120px, 92%);
  height: calc(var(--vehicle-carousel-height) + 18px);
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 34px;
  background:
    linear-gradient(90deg, rgba(148, 163, 184, 0.08) 1px, transparent 1px),
    rgba(255, 255, 255, 0.24);
  background-size: 120px 100%;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(18px);
  transform: translate(-50%, -50%);
}

.vehicle-carousel {
  z-index: 1;
  width: min(980px, 100%);
}

.vehicle-carousel :deep(.el-carousel__container),
.carousel-slide {
  border-radius: var(--radius-xl);
}

.vehicle-carousel :deep(.el-carousel__container) {
  height: var(--vehicle-carousel-height) !important;
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
  right: clamp(14px, 3vw, 28px);
  bottom: clamp(14px, 3vw, 28px);
  z-index: 1;
  display: grid;
  width: min(320px, calc(100% - 28px));
  gap: 6px;
  padding: 14px;
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
}

.vehicle-badge strong {
  color: #fff;
  font-size: 28px;
  line-height: 1.08;
}

.vehicle-badge__price {
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  font-weight: 850;
}

.vehicle-badge__specs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.vehicle-badge__specs span {
  padding: 6px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.13);
  color: rgba(255, 255, 255, 0.86);
  font-size: 12px;
  font-weight: 800;
}

.carousel-state {
  display: grid;
  width: min(980px, 100%);
  min-height: var(--vehicle-carousel-height);
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

.state-spinner {
  width: 44px;
  height: 44px;
  border: 4px solid rgba(10, 132, 255, 0.14);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 820px) {
  .vehicle-carousel-shell {
    --vehicle-carousel-height: clamp(240px, 33vh, 320px);

    min-height: calc(var(--vehicle-carousel-height) + 48px);
    padding-inline: 0;
  }

  .vehicle-carousel :deep(.el-carousel__container),
  .carousel-state {
    height: var(--vehicle-carousel-height) !important;
    min-height: var(--vehicle-carousel-height);
  }

  .vehicle-badge {
    right: 18px;
    bottom: 18px;
  }
}

@media (max-width: 560px) {
  .vehicle-badge strong {
    font-size: 24px;
  }

  .vehicle-badge__price {
    font-size: 16px;
  }
}
</style>
