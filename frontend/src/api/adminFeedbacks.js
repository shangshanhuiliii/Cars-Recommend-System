import http from './http'

export function fetchAdminFeedbacks(params = {}) {
  return http.get('/admin/feedbacks', { params })
}
