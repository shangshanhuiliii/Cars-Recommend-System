import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const authDialog = read('../src/components/AuthDialog.vue')
const app = read('../src/App.vue')
const demandView = read('../src/views/RecommendDemandView.vue')
const resultView = read('../src/views/RecommendResultView.vue')
const carDetailView = read('../src/views/CarDetailView.vue')
const router = read('../src/router/index.js')
const authApi = read('../src/api/auth.js')

assert.doesNotMatch(authDialog, /测试账号|管理员切换|el-radio-button|loginType/)
assert.match(authDialog, /authStore\.loginUnified/)
assert.match(authDialog, /authStore\.register/)
assert.match(authApi, /loginUnified/)
assert.match(authApi, /\/auth\/login/)
assert.match(app, /<AuthDialog/)

assert.match(router, /to\.name === 'home' && authStore\.isAuthenticated && authStore\.principalType === 'ADMIN'/)
assert.match(router, /buildAuthQuery\('login', to\.fullPath\)/)
assert.doesNotMatch(router, /LoginView|AdminLoginView|RegisterView/)

assert.doesNotMatch(demandView, /parseDemandText|parse-text|自然语言|排除品牌|排除车型|excludedBrands|excludedCarIds/)
assert.match(demandView, /budgetOptions/)
assert.match(demandView, /customBudgetRange/)
assert.match(demandView, /brands/)
assert.match(demandView, /seatOptions/)
assert.match(demandView, /sceneOptions/)
assert.match(demandView, /factorWeights/)

assert.match(resultView, /RouterLink/)
assert.match(resultView, /\/car\/\$\{item\.carId\}\?recordId=\$\{detail\.recordId\}/)
assert.doesNotMatch(resultView, /查看车型详情|GET \/api|快照|不会重新计算|TOPSIS|Pareto|熵权/)

assert.match(carDetailView, /返回推荐结果/)
assert.match(carDetailView, /addUserCompare/)
assert.match(carDetailView, /toggleFavorite/)
assert.doesNotMatch(carDetailView, /GET \/api|测试|演示/)

console.log('product UX checks passed')

function read(relativePath) {
  return readFileSync(new URL(relativePath, import.meta.url), 'utf8')
}
