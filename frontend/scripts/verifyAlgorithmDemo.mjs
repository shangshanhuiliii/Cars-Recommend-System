import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const router = readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8')
const view = readFileSync(new URL('../src/views/AlgorithmDemoView.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../src/api/algorithmVisualization.js', import.meta.url), 'utf8')

assert.match(router, /\/algorithm-demo/)
assert.match(router, /AlgorithmDemoView/)
assert.match(api, /algorithm-visualization/)
assert.match(view, /rankNo/)
assert.match(view, /fetchAlgorithmVisualization/)
assert.match(view, /displayTags\(item\.tags\)/)
assert.doesNotMatch(view, /recommend\/generate/)
assert.doesNotMatch(view, /generateRecommendation/)

console.log('algorithm demo checks passed')
