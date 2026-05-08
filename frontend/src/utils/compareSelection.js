const RETURN_ROUTE_KEY = 'cars-recommend-compare-return-route'
const RETURN_SCROLL_KEY = 'cars-recommend-compare-return-scroll-y'
export const MAX_COMPARE_COUNT = 3

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
