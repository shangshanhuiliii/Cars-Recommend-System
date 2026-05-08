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
    if (error?.response?.status === 401) {
      localStorage.removeItem(STORAGE_KEY)
      const { useAuthStore } = await import('@/stores/auth')
      useAuthStore().clear()
      const currentPath = `${window.location.pathname}${window.location.search}${window.location.hash}`
      if (!currentPath.startsWith('/login') && !currentPath.startsWith('/admin/login')) {
        const { default: router } = await import('@/router')
        await router.push({
          path: currentPath.startsWith('/admin') ? '/admin/login' : '/login',
          query: { redirect: currentPath },
        })
      }
    }
    return Promise.reject(error)
  },
)

export default http
