import assert from 'node:assert/strict'

import { displayTags, matchLabel } from '../src/utils/recommendPresentation.js'

assert.equal(matchLabel('STRICT'), '完全匹配')
assert.equal(matchLabel('RELAX_BUDGET'), '放宽预算')
assert.equal(matchLabel('RELAX_BODY_TYPE'), '放宽车型')
assert.equal(matchLabel('RELAX_ENERGY_TYPE'), '放宽动力')
assert.equal(matchLabel('SIMILAR_RECOMMEND'), '相似推荐')
assert.notEqual(matchLabel('RELAX_BUDGET'), '完全匹配')
assert.notEqual(matchLabel('RELAX_BODY_TYPE'), '完全匹配')
assert.notEqual(matchLabel('RELAX_ENERGY_TYPE'), '完全匹配')
assert.notEqual(matchLabel('SIMILAR_RECOMMEND'), '完全匹配')
assert.deepEqual(displayTags(['空间优秀', '完全匹配', '价格匹配度高']), ['空间优秀', '价格匹配度高'])

console.log('recommend presentation checks passed')
