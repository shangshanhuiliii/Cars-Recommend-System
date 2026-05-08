import http from './http'

export function fetchAlgorithmVisualization(recordId) {
  return http.get(`/admin/recommend-records/${recordId}/algorithm-visualization`)
}
