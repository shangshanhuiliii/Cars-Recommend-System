import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const app = read('../src/App.vue')
const router = read('../src/router/index.js')
const authApi = read('../src/api/auth.js')
const authStore = read('../src/stores/auth.js')
const authDialog = read('../src/components/AuthDialog.vue')
const http = read('../src/api/http.js')

assert.match(app, /<AuthDialog/)
assert.match(app, /account-pill/)
assert.match(app, /el-dropdown/)
assert.match(app, /handleAccountCommand/)
assert.doesNotMatch(app, /<el-button[^>]*@click="logout"|"\/login">登录|<RouterLink[^>]*to="\/login"/)

assert.match(router, /path: '\/login'[\s\S]*redirect:/)
assert.match(router, /path: '\/admin\/login'[\s\S]*redirect:/)
assert.match(router, /buildAuthQuery\('login', to\.fullPath\)/)
assert.doesNotMatch(router, /LoginView|AdminLoginView|RegisterView/)

assert.match(authApi, /loginUnified/)
assert.match(authApi, /\/auth\/login/)
assert.match(authApi, /\/auth\/user\/login/)
assert.match(authApi, /\/auth\/admin\/login/)
assert.match(authStore, /loginUnified/)
assert.match(authDialog, /authStore\.loginUnified/)
assert.match(authDialog, /authStore\.register/)
assert.match(http, /auth: 'login'/)
assert.match(http, /redirect: currentPath/)

console.log('unified auth experience checks passed')

function read(relativePath) {
  return readFileSync(new URL(relativePath, import.meta.url), 'utf8')
}
