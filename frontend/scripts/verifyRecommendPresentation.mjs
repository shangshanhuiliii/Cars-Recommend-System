import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

import { displayTags, matchLabel, rankOrderedItems } from '../src/utils/recommendPresentation.js'

const recommendApi = readFileSync(new URL('../src/api/recommend.js', import.meta.url), 'utf8')
const demandView = readFileSync(new URL('../src/views/RecommendDemandView.vue', import.meta.url), 'utf8')
const resultView = readFileSync(new URL('../src/views/RecommendResultView.vue', import.meta.url), 'utf8')
const parseStart = demandView.indexOf('async function parseNaturalLanguage')
const applyStart = demandView.indexOf('function applyParsedDemand')
const parseBlock = demandView.slice(parseStart, applyStart)

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
assert.deepEqual(displayTags(['TOPSIS', '多维表现均衡', 'Pareto', 'RELAX_BUDGET']), ['多维表现均衡'])
assert.deepEqual(
  rankOrderedItems([
    { rankNo: 3, totalScore: 99, modelName: 'C' },
    { rankNo: 1, totalScore: 60, modelName: 'A' },
    { rankNo: 2, totalScore: 88, modelName: 'B' },
  ]).map((item) => item.modelName),
  ['A', 'B', 'C'],
)

assert.match(resultView, /综合推荐分/)
assert.doesNotMatch(resultView, /综合匹配度/)
assert.match(resultView, /完全匹配表示满足预算、车型、动力、座位等硬性条件/)
assert.match(resultView, /综合推荐分表示基于价格、空间、安全、能耗、智能、舒适、动力、口碑、热度计算的多维推荐分/)
assert.match(resultView, /不同分组之间优先看条件匹配状态，同组内部按综合推荐分排序/)

assert.match(recommendApi, /\/user\/demand\/parse-text/)
assert.doesNotMatch(recommendApi, /['"`]\/demand\/parse-text['"`]/)
assert.match(demandView, /自然语言辅助填写/)
assert.match(demandView, /解析需求并填入表单/)
assert.match(demandView, /解析结果已填入表单，请确认后再生成推荐/)
assert.match(demandView, /parseDemandText/)
assert.match(demandView, /bodyTypes/)
assert.match(demandView, /energyTypes/)
assert.match(demandView, /scenes/)
assert.match(demandView, /factorWeights/)
assert.doesNotMatch(parseBlock, /generateRecommendation/)
assert.doesNotMatch(parseBlock, /recommend\/generate/)

console.log('recommend presentation checks passed')
