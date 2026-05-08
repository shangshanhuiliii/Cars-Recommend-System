import http from './http'

export function fetchAdminFavoriteCars(params = {}) {
  return http.get('/admin/favorites/cars', { params })
}

export function fetchAdminFavoriteUsers(carId, params = {}) {
  return http.get(`/admin/favorites/cars/${carId}/users`, { params })
}
