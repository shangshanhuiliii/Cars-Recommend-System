import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = dirname(fileURLToPath(import.meta.url))

const homeView = read('../src/views/HomeView.vue')
const featureView = read('../src/views/FeatureShowcaseView.vue')
const router = read('../src/router/index.js')
const carsApi = read('../src/api/cars.js')
const app = read('../src/App.vue')
const logo = read('../src/components/AppLogo.vue')
const brandConfig = read('../src/config/brand.js')

assert.match(homeView, /fetchHomeCarouselCars/)
assert.match(homeView, /carImageSrc\(car\.imageUrl\)/)
assert.match(homeView, /router\.push\(`\/car\/\$\{id\}`\)/)
assert.doesNotMatch(homeView, /TOPSIS|Pareto|熵权/)

const carouselBlock = extractBlock(homeView, '<el-carousel', '</el-carousel>')
assert.doesNotMatch(carouselBlock, /开始推荐|开始购车推荐|查看历史|车型对比|\/recommend|\/history|\/compare/)

assert.match(homeView, /\/features/)
assert.match(router, /FeatureShowcaseView/)
assert.match(router, /path:\s*'\/features'/)
assert.match(featureView, /结构化购车需求/)
assert.doesNotMatch(featureView, /TOPSIS|Pareto|熵权/)

assert.match(carsApi, /fetchHomeCarouselCars/)
assert.match(carsApi, /\/car\/home-carousel/)

assert.match(app, /<AppLogo/)
assert.doesNotMatch(app, /brand__mark|>CR<|汽车推荐系统|可解释购车决策/)
assert.match(logo, /brandConfig/)
assert.match(logo, /imageFailed/)
assert.match(brandConfig, /iconPath:\s*'\/brand\/logo-icon\.svg'/)
assert.ok(existsSync(join(root, '../public/brand/logo-icon.svg')))

console.log('home experience checks passed')

function read(relativePath) {
  return readFileSync(join(root, relativePath), 'utf8')
}

function extractBlock(source, startMarker, endMarker) {
  const start = source.indexOf(startMarker)
  const end = source.indexOf(endMarker, start)
  assert.ok(start >= 0 && end > start, `${startMarker} block not found`)
  return source.slice(start, end + endMarker.length)
}
