<template>
  <section class="feature-page">
    <header class="feature-hero">
      <p class="eyebrow">特色介绍</p>
      <h1>把复杂购车判断拆成可理解的几个步骤。</h1>
      <p>
        从需求填写到推荐结果、车型详情、收藏对比和历史回看，页面围绕普通购车用户能直接理解的信息组织。
      </p>
    </header>

    <nav class="feature-nav" aria-label="特色介绍导航">
      <button v-for="item in sections" :key="item.id" type="button" @click="scrollToSection(item.id)">
        {{ item.shortTitle }}
      </button>
    </nav>

    <section
      v-for="item in sections"
      :id="item.id"
      :key="item.id"
      class="feature-block"
      :aria-labelledby="`${item.id}-title`"
    >
      <div class="feature-block__copy">
        <span>{{ item.index }}</span>
        <h2 :id="`${item.id}-title`">{{ item.title }}</h2>
        <p>{{ item.description }}</p>
      </div>
      <div class="feature-block__panel">
        <div v-for="point in item.points" :key="point.title" class="feature-point">
          <strong>{{ point.title }}</strong>
          <p>{{ point.text }}</p>
        </div>
      </div>
    </section>
  </section>
</template>

<script setup>
import { nextTick, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const sections = [
  {
    id: 'demand',
    index: '01',
    shortTitle: '结构化需求',
    title: '结构化购车需求',
    description: '用清晰表单把真实购车约束变成可保存、可回看的需求，而不是让用户在大段文字里反复补充。',
    points: [
      { title: '预算范围', text: '支持预算下限和上限，上限为空时表示不设上限。' },
      { title: '车型偏好', text: '品牌、车型类型、动力类型和座位数都可以多选，空选表示不限。' },
      { title: '使用场景', text: '通勤、家庭、自驾、商务等场景帮助用户表达真实用车环境。' },
      { title: '偏好重点', text: '价格、空间、安全、能耗、智能、舒适、动力、口碑和热度按关注程度填写。' },
    ],
  },
  {
    id: 'result',
    index: '02',
    shortTitle: '推荐结果',
    title: '推荐结果清楚说明为什么适合',
    description: '推荐结果页重点展示用户能直接理解的分数、标签、推荐理由、不足提醒和维度评分。',
    points: [
      { title: '综合推荐分', text: '用于理解整体匹配程度，展示顺序以后端返回排名为准。' },
      { title: '推荐标签', text: '用简洁标签概括优势，避免只靠参数表判断。' },
      { title: '推荐理由', text: '说明车型与预算、场景、座位和偏好之间的匹配关系。' },
      { title: '维度评分', text: '用空间、安全、能耗、智能、舒适等维度帮助横向理解。' },
    ],
  },
  {
    id: 'weakness',
    index: '03',
    shortTitle: '不足提醒',
    title: '不足提醒帮助提前做取舍',
    description: '每个推荐车型不仅展示亮点，也同步提示可能不满足预期的地方，让选择更稳妥。',
    points: [
      { title: '预算提醒', text: '当价格接近或超出预期时，页面用用户能理解的方式提示。' },
      { title: '场景提醒', text: '空间、动力、能耗或座位数不够理想时，提醒用户进一步确认。' },
      { title: '取舍视角', text: '把优势和不足放在一起，帮助用户做平衡判断。' },
    ],
  },
  {
    id: 'detail',
    index: '04',
    shortTitle: '车型详情',
    title: '车型详情承接图片、参数和评分来源',
    description: '从首页轮播或推荐结果进入车型详情后，可以查看车型图片、基础信息、参数和评分来源。',
    points: [
      { title: '车辆图片', text: '优先展示数据库中的当前生效图片，缺失时使用本地兜底图。' },
      { title: '基础参数', text: '指导价、车型类型、动力类型、座位数和关键配置集中展示。' },
      { title: '评分来源', text: '展示车型在多个维度上的表现，帮助理解结果依据。' },
    ],
  },
  {
    id: 'decision',
    index: '05',
    shortTitle: '收藏对比',
    title: '收藏与对比服务于后续判断',
    description: '收藏和对比用于记录关注车型、横向比较参数，不改变已经生成的推荐结果。',
    points: [
      { title: '收藏车型', text: '把感兴趣的车型放入列表，方便之后继续查看。' },
      { title: '车型对比', text: '选择 1-3 款车型横向查看关键参数和维度表现。' },
      { title: '边界清晰', text: '这些操作只辅助用户判断，不参与推荐结果重新排序。' },
    ],
  },
  {
    id: 'history',
    index: '06',
    shortTitle: '历史回看',
    title: '历史回看读取当时快照',
    description: '每次推荐都会保存当时需求和结果，历史页面读取快照，不用当前数据覆盖过去的判断。',
    points: [
      { title: '需求快照', text: '保留当时填写的预算、偏好和场景。' },
      { title: '结果快照', text: '保留当时推荐车型、分数、标签、理由和不足提醒。' },
      { title: '继续查看', text: '可以从历史记录继续进入推荐详情和车型详情。' },
    ],
  },
]

onMounted(() => {
  scrollToQuerySection()
})

watch(
  () => route.query.section,
  () => scrollToQuerySection(),
)

function scrollToQuerySection() {
  const section = typeof route.query.section === 'string' ? route.query.section : ''
  if (section) {
    scrollToSection(section)
  }
}

async function scrollToSection(section) {
  await nextTick()
  document.getElementById(section)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<style scoped>
.feature-page {
  display: grid;
  gap: 26px;
  animation: feature-in 520ms ease both;
}

.feature-hero {
  padding: clamp(36px, 7vw, 78px);
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: var(--radius-xl);
  background:
    radial-gradient(circle at 82% 18%, rgba(90, 200, 250, 0.24), transparent 32%),
    rgba(255, 255, 255, 0.74);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(20px);
}

.eyebrow {
  margin: 0 0 12px;
  color: #0b65c2;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.14em;
}

.feature-hero h1 {
  max-width: 860px;
  margin: 0;
  color: var(--color-primary-dark);
  font-size: clamp(40px, 6vw, 72px);
  line-height: 1.04;
  letter-spacing: -0.055em;
}

.feature-hero p:not(.eyebrow) {
  max-width: 760px;
  margin: 22px 0 0;
  color: var(--color-muted);
  font-size: 17px;
  line-height: 1.85;
}

.feature-nav {
  position: sticky;
  top: 92px;
  z-index: 5;
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(18px);
}

.feature-nav button {
  flex: 0 0 auto;
  padding: 9px 14px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #344054;
  cursor: pointer;
  font-weight: 800;
}

.feature-nav button:hover {
  background: rgba(10, 132, 255, 0.08);
  color: var(--color-primary);
}

.feature-block {
  scroll-margin-top: 112px;
  display: grid;
  grid-template-columns: minmax(0, 0.82fr) minmax(0, 1.18fr);
  gap: 22px;
  padding: clamp(26px, 4vw, 44px);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.68);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(18px);
}

.feature-block__copy span {
  color: #0b65c2;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.16em;
}

.feature-block__copy h2 {
  margin: 14px 0 0;
  color: var(--color-primary-dark);
  font-size: clamp(28px, 4vw, 46px);
  line-height: 1.08;
  letter-spacing: -0.045em;
}

.feature-block__copy p {
  margin: 18px 0 0;
  color: var(--color-muted);
  font-size: 16px;
  line-height: 1.85;
}

.feature-block__panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.feature-point {
  min-height: 142px;
  padding: 20px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.74);
}

.feature-point strong {
  color: var(--color-primary-dark);
  font-size: 18px;
}

.feature-point p {
  margin: 10px 0 0;
  color: var(--color-muted);
  line-height: 1.72;
}

@keyframes feature-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 900px) {
  .feature-nav {
    top: 124px;
    border-radius: var(--radius-md);
  }

  .feature-block {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .feature-hero,
  .feature-block {
    padding: 24px;
  }

  .feature-block__panel {
    grid-template-columns: 1fr;
  }
}
</style>
