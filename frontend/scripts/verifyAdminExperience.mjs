import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const app = read('../src/App.vue')
const router = read('../src/router/index.js')
const loginView = read('../src/views/LoginView.vue')
const adminLoginView = read('../src/views/AdminLoginView.vue')
const adminUsersView = read('../src/views/AdminUsersView.vue')
const adminFavoritesView = read('../src/views/AdminFavoritesView.vue')
const adminFeedbacksView = read('../src/views/AdminFeedbacksView.vue')
const adminFavoritesApi = read('../src/api/adminFavorites.js')
const adminFeedbacksApi = read('../src/api/adminFeedbacks.js')

assert.doesNotMatch(loginView, /demo_user|demo_admin|测试账号|本地用户|本地管理员|el-radio-button|loginType/)
assert.match(loginView, /authStore\.login\('USER'/)
assert.match(loginView, /\/register/)
assert.match(loginView, /\/admin\/login/)

assert.doesNotMatch(adminLoginView, /demo_user|demo_admin|测试账号|本地用户|本地管理员|el-radio-button|\/register|RouterLink/)
assert.match(adminLoginView, /authStore\.login\('ADMIN'/)
assert.match(adminLoginView, /\/admin\/cars/)

assert.match(app, /const brandTarget = computed\(\(\) => \(isAdmin\.value \? '\/admin\/cars' : '\/'\)\)/)
assert.match(app, /const showHomeMenu = computed\(\(\) => !authStore\.isAuthenticated \|\| !isAdmin\.value\)/)
assert.match(app, /item\.code !== 'home'/)
assert.match(app, /isAdmin\.value \? '管理员' : authStore\.displayName/)
assert.doesNotMatch(app, /<el-menu-item[^>]*\/login|index="\/login"/)

assert.match(router, /path: '\/admin\/login'/)
assert.match(router, /path: '\/admin\/favorites'/)
assert.match(router, /path: '\/admin\/feedbacks'/)
assert.match(router, /to\.name === 'home' && authStore\.isAuthenticated && authStore\.principalType === 'ADMIN'/)
assert.match(router, /return '\/admin\/cars'/)
assert.match(router, /path: to\.path\.startsWith\('\/admin'\).*'\/admin\/login'/)

assert.doesNotMatch(adminUsersView, />详情<|详情<\/el-button>|showDetail/)
assert.match(adminUsersView, /\/admin\/recommend-records/)
assert.match(adminUsersView, /\/admin\/favorites/)
assert.match(adminUsersView, /\/admin\/feedbacks/)
assert.match(adminUsersView, /SUCCESS.*完全匹配|完全匹配.*SUCCESS/)
assert.match(adminUsersView, /FALLBACK.*含补充推荐|含补充推荐.*FALLBACK/)
assert.match(adminUsersView, /EMPTY.*暂无结果|暂无结果.*EMPTY/)

assert.match(adminFavoritesView, /fetchAdminFavoriteCars/)
assert.match(adminFavoritesView, /fetchAdminFavoriteUsers/)
assert.doesNotMatch(adminFavoritesView, /delete|remove|取消收藏/)
assert.match(adminFavoritesApi, /\/admin\/favorites\/cars/)
assert.doesNotMatch(adminFavoritesApi, /delete|remove/)

assert.match(adminFeedbacksView, /fetchAdminFeedbacks/)
assert.match(adminFeedbacksView, /\/admin\/recommend-records/)
assert.doesNotMatch(adminFeedbacksView, /delete|remove|删除/)
assert.match(adminFeedbacksApi, /\/admin\/feedbacks/)
assert.doesNotMatch(adminFeedbacksApi, /delete|remove/)

console.log('admin experience checks passed')

function read(relativePath) {
  return readFileSync(new URL(relativePath, import.meta.url), 'utf8')
}
