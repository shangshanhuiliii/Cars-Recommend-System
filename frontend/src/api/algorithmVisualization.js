import http from './http'

export function fetchAlgorithmVisualization(recordId, params = {}) {
  return http.get(`/recommend/${recordId}/algorithm-visualization`, { params })
}
