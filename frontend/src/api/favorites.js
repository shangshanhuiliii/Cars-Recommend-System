import http from './http'

export function addFavorite(carId) {
  return http.post(`/user/favorites/${carId}`)
}

export function removeFavorite(carId) {
  return http.delete(`/user/favorites/${carId}`)
}

export function fetchFavorites(params = {}) {
  return http.get('/user/favorites', { params })
}

export function fetchFavoriteStatus(carIds = []) {
  return http.get('/user/favorites/status', { params: { carIds: carIds.join(',') } })
}
