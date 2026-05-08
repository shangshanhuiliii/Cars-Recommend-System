import assert from 'node:assert/strict'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = dirname(fileURLToPath(import.meta.url))
const srcRoot = join(root, '../src')

const router = read('../src/router/index.js')
const resultView = read('../src/views/RecommendResultView.vue')
const compareView = read('../src/views/CarCompareView.vue')
const favoritesView = read('../src/views/FavoritesView.vue')
const compareSelection = read('../src/utils/compareSelection.js')
const userCompareApi = read('../src/api/userCompare.js')
const carsApi = read('../src/api/cars.js')
const favoritesApi = read('../src/api/favorites.js')
const recommendApi = read('../src/api/recommend.js')
const presentation = read('../src/utils/recommendPresentation.js')
const packageJson = read('../package.json')
const frontendSource = readTree(srcRoot)

assert.match(router, /\/compare/)
assert.match(router, /CarCompareView/)
assert.match(router, /\/favorites/)
assert.match(router, /FavoritesView/)

assert.match(carsApi, /\/car\/compare/)
assert.match(userCompareApi, /\/user\/compare/)
assert.match(favoritesApi, /\/user\/favorites/)
assert.match(favoritesApi, /\/user\/favorites\/status/)
assert.match(recommendApi, /\/recommend\/\$\{recordId\}\/feedback/)

assert.match(resultView, /addUserCompare/)
assert.match(resultView, /submitRecommendationFeedback/)
assert.match(resultView, /saveCompareReturn\(route\.fullPath,\s*window\.scrollY\)/)
assert.match(resultView, /readCompareScrollFor\(route\.fullPath\)/)
assert.doesNotMatch(resultView, /localStorage|cars-recommend-compare-ids|addCompareId|writeCompareIds|readCompareIds/)
assert.doesNotMatch(resultView, /不会自动调整推荐权重|推荐权重或排序/)

const addToCompareBlock = extractFunction(resultView, 'addToCompare')
assert.doesNotMatch(addToCompareBlock, /router\.push|\/compare/)
assert.match(addToCompareBlock, /addUserCompare/)
assert.doesNotMatch(addToCompareBlock, /localStorage|cars-recommend-compare-ids|addCompareId|writeCompareIds|readCompareIds/)

assert.match(compareView, /radar-svg/)
assert.match(compareView, /readCompareReturn/)
assert.match(compareView, /fetchUserCompare/)
assert.match(compareView, /addUserCompare/)
assert.match(compareView, /removeUserCompare/)
assert.match(compareView, /clearUserCompare/)
assert.doesNotMatch(compareView, /recommend\/generate/)
assert.doesNotMatch(compareView, /generateRecommendation/)
assert.doesNotMatch(compareView, /TopsisRanker|ParetoAnalyzer|RecommendationWeightService/)
assert.doesNotMatch(compareView, /不生成推荐|影响推荐排序/)

assert.match(favoritesView, /addUserCompare/)
assert.doesNotMatch(favoritesView, /recommend\/generate/)
assert.doesNotMatch(favoritesView, /generateRecommendation/)
assert.doesNotMatch(favoritesView, /不参与推荐排序|推荐算法权重/)

assert.match(compareSelection, /MAX_COMPARE_COUNT = 3/)
assert.match(compareSelection, /saveCompareReturn/)
assert.match(compareSelection, /readCompareScrollFor/)
assert.doesNotMatch(compareSelection, /localStorage|cars-recommend-compare-ids|addCompareId|writeCompareIds|readCompareIds/)

assert.match(presentation, /rankNo/)
assert.doesNotMatch(presentation, /favorite|feedback|收藏|反馈/)
assert.doesNotMatch(packageJson, /echarts/i)

assert.doesNotMatch(
  frontendSource,
  /ElMessage(?!Box)|ElNotification|\$message|message\.success|message\.error|toast|notify|notification/,
)

console.log('stage 11 feature checks passed')

function read(relativePath) {
  return readFileSync(join(root, relativePath), 'utf8')
}

function readTree(directory) {
  return readdirSync(directory)
    .map((name) => join(directory, name))
    .map((path) => {
      if (statSync(path).isDirectory()) {
        return readTree(path)
      }
      return path.endsWith('.vue') || path.endsWith('.js') ? readFileSync(path, 'utf8') : ''
    })
    .join('\n')
}

function extractFunction(source, name) {
  const start = source.indexOf(`function ${name}`)
  assert.notEqual(start, -1, `${name} function should exist`)
  const nextFunction = source.indexOf('\nfunction ', start + 1)
  return source.slice(start, nextFunction === -1 ? source.length : nextFunction)
}
