import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

import { displayTags, matchLabel, rankOrderedItems } from '../src/utils/recommendPresentation.js'

const recommendApi = read('../src/api/recommend.js')
const userCompareApi = read('../src/api/userCompare.js')
const demandView = read('../src/views/RecommendDemandView.vue')
const resultView = read('../src/views/RecommendResultView.vue')
const carDetailView = read('../src/views/CarDetailView.vue')
const compareSelection = read('../src/utils/compareSelection.js')

assert.notEqual(matchLabel('STRICT'), 'STRICT')
assert.notEqual(matchLabel('RELAX_BUDGET'), 'RELAX_BUDGET')
assert.deepEqual(displayTags(['usable', 'TOPSIS', 'Pareto', 'RELAX_BUDGET', 'STRICT']), ['usable'])
assert.deepEqual(
  rankOrderedItems([
    { rankNo: 3, totalScore: 99, modelName: 'C' },
    { rankNo: 1, totalScore: 60, modelName: 'A' },
    { rankNo: 2, totalScore: 88, modelName: 'B' },
  ]).map((item) => item.modelName),
  ['A', 'B', 'C'],
)

assert.match(recommendApi, /\/user\/demand\/parse-text/)
assert.match(userCompareApi, /\/user\/compare/)
assert.doesNotMatch(compareSelection, /localStorage|cars-recommend-compare-ids|addCompareId|writeCompareIds|readCompareIds/)

assert.doesNotMatch(demandView, /parseDemandText|parse-text|自然语言|排除品牌|排除车型|excludedBrands|excludedCarIds/)
assert.match(demandView, /budgetOptions/)
assert.match(demandView, /customBudgetRange/)
assert.match(demandView, /brands/)
assert.match(demandView, /seatOptions/)
assert.match(demandView, /factorWeights/)
assert.match(demandView, /budgetMax/)

assert.match(resultView, /rankOrderedItems/)
assert.match(resultView, /RouterLink/)
assert.match(resultView, /\/car\/\$\{item\.carId\}\?recordId=\$\{detail\.recordId\}/)
assert.match(resultView, /reasonText/)
assert.match(resultView, /weaknessText/)
assert.match(resultView, /scoreRows/)
assert.match(resultView, /addUserCompare/)
assert.doesNotMatch(resultView, /查看车型详情|fetchCarDetail|ElDrawer|drawer|GET \/api|快照|不会重新计算/)
assert.doesNotMatch(resultView, /TOPSIS|Pareto|熵权/)
assert.doesNotMatch(resultView, /localStorage|cars-recommend-compare-ids|addCompareId|writeCompareIds|readCompareIds/)

assert.match(carDetailView, /recordId/)
assert.match(carDetailView, /addUserCompare/)
assert.match(carDetailView, /toggleFavorite/)
assert.doesNotMatch(carDetailView, /GET \/api|测试|演示/)

console.log('recommend presentation checks passed')

function read(relativePath) {
  return readFileSync(new URL(relativePath, import.meta.url), 'utf8')
}
