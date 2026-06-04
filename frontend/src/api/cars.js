import http from './http'

export function fetchCars(params = {}) {
  return http.get('/car', { params })
}

export function fetchCarDetail(id) {
  return http.get(`/car/${id}`)
}

export function fetchCarBrands() {
  return http.get('/car/brands')
}

export function fetchCarOptions(params = {}) {
  return http.get('/car/options', { params })
}

export function fetchHomeCarouselCars(limit = 6) {
  return http.get('/car/home-carousel', { params: { limit } })
}

export function fetchCarCompare(carIds = []) {
  return http.get('/car/compare', { params: { carIds: carIds.join(',') } })
}
