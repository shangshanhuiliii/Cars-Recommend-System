const STORAGE_KEY = 'cars-recommend-compare-ids'
const RETURN_ROUTE_KEY = 'cars-recommend-compare-return-route'
const RETURN_SCROLL_KEY = 'cars-recommend-compare-return-scroll-y'
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
    return { ok: true, reason: '该车型已加入对比。', ids }
  }
  if (ids.length >= MAX_COMPARE_COUNT) {
    return { ok: false, reason: '最多选择 3 款车型进行对比。', ids }
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

export function isCompareSelected(carId) {
  const id = Number(carId)
  return Number.isFinite(id) && readCompareIds().includes(id)
}

export function saveCompareReturn(routePath, scrollY = 0) {
  const path = String(routePath || '').trim()
  if (!path) return
  sessionStorage.setItem(RETURN_ROUTE_KEY, path)
  sessionStorage.setItem(RETURN_SCROLL_KEY, String(Math.max(0, Number(scrollY) || 0)))
}

export function readCompareReturn() {
  const path = sessionStorage.getItem(RETURN_ROUTE_KEY)
  if (!path) return null
  return {
    path,
    scrollY: Math.max(0, Number(sessionStorage.getItem(RETURN_SCROLL_KEY) || 0)),
  }
}

export function readCompareScrollFor(routePath) {
  const saved = readCompareReturn()
  return saved?.path === routePath ? saved.scrollY : null
}

export function clearCompareReturn() {
  sessionStorage.removeItem(RETURN_ROUTE_KEY)
  sessionStorage.removeItem(RETURN_SCROLL_KEY)
}

function normalizeIds(carIds) {
  return [...new Set((carIds || []).map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0))]
}
