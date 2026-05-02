const STORAGE_KEY = 'cars-recommend-compare-ids'
const MAX_COMPARE_COUNT = 3

export function readCompareIds() {
  try {
    const value = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    if (!Array.isArray(value)) return []
    return normalizeIds(value)
  } catch {
    return []
  }
}

export function addCompareId(carId) {
  const id = Number(carId)
  if (!Number.isFinite(id) || id <= 0) {
    return { ok: false, reason: '车型信息无效。', ids: readCompareIds() }
  }
  const ids = readCompareIds()
  if (ids.includes(id)) {
    return { ok: true, reason: '该车型已在对比列表中。', ids }
  }
  if (ids.length >= MAX_COMPARE_COUNT) {
    return { ok: false, reason: '最多只能选择 3 款车型进行对比。', ids }
  }
  const nextIds = [...ids, id]
  writeCompareIds(nextIds)
  return { ok: true, reason: '已加入对比。', ids: nextIds }
}

export function removeCompareId(carId) {
  const id = Number(carId)
  const nextIds = readCompareIds().filter((value) => value !== id)
  writeCompareIds(nextIds)
  return nextIds
}

export function writeCompareIds(carIds) {
  const ids = normalizeIds(carIds).slice(0, MAX_COMPARE_COUNT)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(ids))
  return ids
}

export function compareQuery(ids) {
  return ids.length ? { carIds: ids.join(',') } : {}
}

function normalizeIds(carIds) {
  return [...new Set((carIds || []).map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0))]
}
