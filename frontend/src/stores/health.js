import { defineStore } from 'pinia'

import { getHealth } from '@/api/health'

export const useHealthStore = defineStore('health', {
  state: () => ({
    loading: false,
    loaded: false,
    error: '',
    status: {
      backend: 'unknown',
      database: 'unknown',
    },
  }),
  actions: {
    async load() {
      this.loading = true
      this.error = ''
      try {
        const response = await getHealth()
        this.status = response.data || this.status
        this.loaded = true
      } catch (error) {
        this.error = error?.message || 'health request failed'
      } finally {
        this.loading = false
      }
    },
  },
})
