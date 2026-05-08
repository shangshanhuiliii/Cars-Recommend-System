import { defineStore } from 'pinia'

import { fetchCurrentPrincipal, loginAdmin, loginUser, logoutAuth, registerUser } from '@/api/auth'

const STORAGE_KEY = 'cars-recommend-auth'

function readStoredState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function writeStoredState(state) {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      token: state.token,
      principal: state.principal,
      permissions: state.permissions,
      menus: state.menus,
    }),
  )
}

const stored = readStoredState()

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: stored.token || '',
    principal: stored.principal || null,
    permissions: Array.isArray(stored.permissions) ? stored.permissions : [],
    menus: Array.isArray(stored.menus) ? stored.menus : [],
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token && state.principal),
    principalType: (state) => state.principal?.principalType || '',
    displayName: (state) => state.principal?.displayName || state.principal?.username || '',
  },
  actions: {
    async login(loginType, credentials) {
      const response = loginType === 'ADMIN'
        ? await loginAdmin(credentials)
        : await loginUser(credentials)
      this.setSession(response.data)
      return response.data
    },
    async register(payload) {
      const response = await registerUser(payload)
      this.setSession(response.data)
      return response.data
    },
    async refreshMe() {
      if (!this.token) {
        return null
      }
      const response = await fetchCurrentPrincipal()
      this.principal = response.data
      this.permissions = response.data?.permissions || []
      this.menus = response.data?.menus || []
      writeStoredState(this)
      return this.principal
    },
    async logout() {
      try {
        if (this.token) {
          await logoutAuth()
        }
      } finally {
        this.clear()
      }
    },
    setSession(data) {
      this.token = data?.token || ''
      this.principal = data?.principal || null
      this.permissions = data?.principal?.permissions || []
      this.menus = data?.principal?.menus || []
      writeStoredState(this)
    },
    clear() {
      this.token = ''
      this.principal = null
      this.permissions = []
      this.menus = []
      localStorage.removeItem(STORAGE_KEY)
    },
    hasRole(role) {
      return !role || this.principalType === role
    },
    hasPermission(permission) {
      return !permission || this.permissions.includes(permission)
    },
  },
})
