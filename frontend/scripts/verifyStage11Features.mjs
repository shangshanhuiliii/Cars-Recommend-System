import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const router = readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8')
const resultView = readFileSync(new URL('../src/views/RecommendResultView.vue', import.meta.url), 'utf8')
const compareView = readFileSync(new URL('../src/views/CarCompareView.vue', import.meta.url), 'utf8')
const favoritesView = readFileSync(new URL('../src/views/FavoritesView.vue', import.meta.url), 'utf8')
const carsApi = readFileSync(new URL('../src/api/cars.js', import.meta.url), 'utf8')
const favoritesApi = readFileSync(new URL('../src/api/favorites.js', import.meta.url), 'utf8')
const recommendApi = readFileSync(new URL('../src/api/recommend.js', import.meta.url), 'utf8')
const presentation = readFileSync(new URL('../src/utils/recommendPresentation.js', import.meta.url), 'utf8')
const packageJson = readFileSync(new URL('../package.json', import.meta.url), 'utf8')

assert.match(router, /\/compare/)
assert.match(router, /CarCompareView/)
assert.match(router, /\/favorites/)
assert.match(router, /FavoritesView/)

assert.match(carsApi, /\/car\/compare/)
assert.match(favoritesApi, /\/user\/favorites/)
assert.match(favoritesApi, /\/user\/favorites\/status/)
assert.match(recommendApi, /\/recommend\/\$\{recordId\}\/feedback/)

assert.match(resultView, /加入对比/)
assert.match(resultView, /收藏/)
assert.match(resultView, /推荐反馈/)
assert.match(resultView, /submitRecommendationFeedback/)
assert.match(resultView, /反馈只进入统计分析，当前版本不会自动调整推荐权重或排序/)

assert.match(compareView, /八维静态评分/)
assert.match(compareView, /radar-svg/)
assert.doesNotMatch(compareView, /recommend\/generate/)
assert.doesNotMatch(compareView, /generateRecommendation/)
assert.doesNotMatch(compareView, /TopsisRanker|ParetoAnalyzer|RecommendationWeightService/)

assert.match(favoritesView, /我的收藏/)
assert.match(favoritesView, /不参与推荐排序/)
assert.doesNotMatch(favoritesView, /recommend\/generate/)
assert.doesNotMatch(favoritesView, /generateRecommendation/)

assert.match(presentation, /rankNo/)
assert.doesNotMatch(presentation, /favorite|feedback|收藏|反馈/)
assert.doesNotMatch(packageJson, /echarts/i)

console.log('stage 11 feature checks passed')
