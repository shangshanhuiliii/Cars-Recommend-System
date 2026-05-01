import http from './http'

export function fetchCarDetail(id) {
  return http.get(`/car/${id}`)
}
