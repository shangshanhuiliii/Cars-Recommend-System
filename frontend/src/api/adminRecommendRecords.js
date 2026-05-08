import http from './http'

export function fetchAdminRecommendationHistory(params = {}) {
  return http.get('/admin/recommend-records', { params })
}

export function fetchAdminRecommendationDetail(recordId) {
  return http.get(`/admin/recommend-records/${recordId}`)
}

export function fetchAdminAlgorithmVisualization(recordId) {
  return http.get(`/admin/recommend-records/${recordId}/algorithm-visualization`)
}
