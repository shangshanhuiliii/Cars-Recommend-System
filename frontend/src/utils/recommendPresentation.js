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
  return tags.filter((tag) => tag !== '完全匹配')
}
