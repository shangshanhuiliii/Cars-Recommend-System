import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

const STORAGE_KEY = 'cars-recommend-auth'

function readToken() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw).token : ''
  } catch {
    return ''
  }
}

http.interceptors.request.use((config) => {
  const token = readToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const requestUrl = error?.config?.url || ''
    const isAuthRequest = /\/auth\/(login|user\/login|admin\/login|user\/register)$/.test(requestUrl)
    if (error?.response?.status === 401 && !isAuthRequest) {
      localStorage.removeItem(STORAGE_KEY)
      const { useAuthStore } = await import('@/stores/auth')
      useAuthStore().clear()
      const currentPath = `${window.location.pathname}${window.location.search}${window.location.hash}`
      if (!currentPath.startsWith('/login') && !currentPath.startsWith('/admin/login')) {
        const { default: router } = await import('@/router')
        const currentRoute = router.currentRoute.value
        const redirectQuery = { ...currentRoute.query }
        delete redirectQuery.auth
        delete redirectQuery.redirect
        const queryText = new URLSearchParams(redirectQuery).toString()
        const redirectPath = `${currentRoute.path || window.location.pathname || '/'}${queryText ? `?${queryText}` : ''}${currentRoute.hash || ''}`
        await router.replace({
          path: currentRoute.path || window.location.pathname || '/',
          query: {
            ...currentRoute.query,
            auth: currentRoute.path?.startsWith('/admin') || currentRoute.path === '/algorithm-demo' ? 'admin' : 'login',
            redirect: redirectPath,
          },
          hash: currentRoute.hash,
        })
      }
    }
    return Promise.reject(error)
  },
)

export default http
