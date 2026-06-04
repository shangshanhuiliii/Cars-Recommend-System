import http from './http'

export function createUserDemand(data) {
  return http.post('/user/demand', data)
}

export function generateRecommendation(data) {
  return http.post('/recommend/generate', data)
}

export function fetchRecommendationDetail(recordId, params = {}) {
  return http.get(`/recommend/${recordId}`, { params })
}

export function fetchRecommendationHistory(params = {}) {
  return http.get('/recommend/history', { params })
}

export function submitRecommendationFeedback(recordId, data) {
  return http.post(`/recommend/${recordId}/feedback`, data)
}

export function fetchRecommendationFeedback(recordId, params = {}) {
  return http.get(`/recommend/${recordId}/feedback`, { params })
}
