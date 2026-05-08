import http from './http'

export function fetchUserCompare() {
  return http.get('/user/compare')
}

export function addUserCompare(carId) {
  return http.post(`/user/compare/${carId}`)
}

export function removeUserCompare(carId) {
  return http.delete(`/user/compare/${carId}`)
}

export function clearUserCompare() {
  return http.delete('/user/compare')
}
