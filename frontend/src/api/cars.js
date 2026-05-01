import http from './http'

export function fetchCarDetail(id) {
  return http.get(`/car/${id}`)
}

export function fetchCarBrands() {
  return http.get('/car/brands')
}

export function fetchCarOptions(params = {}) {
  return http.get('/car/options', { params })
}
