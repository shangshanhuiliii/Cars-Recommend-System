import http from './http'

export function fetchAdminUsers(params = {}) {
  return http.get('/admin/users', { params })
}

export function fetchAdminUserDetail(userId) {
  return http.get(`/admin/users/${userId}`)
}

export function fetchAdminUserRecommendRecords(userId, params = {}) {
  return http.get(`/admin/users/${userId}/recommend-records`, { params })
}

export function fetchAdminUserFavorites(userId, params = {}) {
  return http.get(`/admin/users/${userId}/favorites`, { params })
}

export function fetchAdminUserFeedbacks(userId, params = {}) {
  return http.get(`/admin/users/${userId}/feedbacks`, { params })
}

export function updateAdminUserStatus(userId, status) {
  return http.put(`/admin/users/${userId}/status`, { status })
}
