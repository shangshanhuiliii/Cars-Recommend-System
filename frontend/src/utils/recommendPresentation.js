export const MATCH_LEVEL_LABELS = {
  STRICT: '完全匹配',
  RELAX_BUDGET: '放宽预算',
  RELAX_BODY_TYPE: '放宽车型',
  RELAX_ENERGY_TYPE: '放宽动力',
  SIMILAR_RECOMMEND: '相似推荐',
}

export function matchLabel(value) {
  return MATCH_LEVEL_LABELS[value] || value || '未知'
}

export function matchTagType(value) {
  if (value === 'STRICT') return 'success'
  if (value === 'RELAX_BUDGET') return 'warning'
  if (value === 'RELAX_ENERGY_TYPE') return 'primary'
  return 'info'
}

export function displayTags(tags) {
  if (!Array.isArray(tags)) {
    return []
  }
  const technicalTags = new Set([
    '完全匹配',
    '降级推荐',
    '放宽预算',
    '放宽车型',
    '放宽动力',
    '相似推荐',
    'STRICT',
    'RELAX_BUDGET',
    'RELAX_BODY_TYPE',
    'RELAX_ENERGY_TYPE',
    'SIMILAR_RECOMMEND',
    'TOPSIS',
    'Pareto',
    '熵权',
  ])
  return tags.filter((tag) => !technicalTags.has(tag))
}

export function rankOrderedItems(items) {
  if (!Array.isArray(items)) {
    return []
  }
  return items
    .map((item, index) => ({ item, index }))
    .sort((left, right) => {
      const leftRank = Number(left.item?.rankNo ?? left.index + 1)
      const rightRank = Number(right.item?.rankNo ?? right.index + 1)
      if (leftRank !== rightRank) return leftRank - rightRank
      return left.index - right.index
    })
    .map(({ item }) => item)
}
