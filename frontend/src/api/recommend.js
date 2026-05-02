import http from './http'

export function createUserDemand(data) {
  return http.post('/user/demand', data)
}

export function parseDemandText(data) {
  return http.post('/user/demand/parse-text', data)
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
