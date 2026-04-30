import http from './http'

export function fetchAdminCars(params) {
  return http.get('/admin/cars', { params })
}

export function fetchAdminCar(id) {
  return http.get(`/admin/cars/${id}`)
}

export function createAdminCar(data) {
  return http.post('/admin/cars', data)
}

export function updateAdminCar(id, data) {
  return http.put(`/admin/cars/${id}`, data)
}

export function deleteAdminCar(id) {
  return http.delete(`/admin/cars/${id}`)
}

export function fetchAdminCarParam(id) {
  return http.get(`/admin/cars/${id}/param`)
}

export function saveAdminCarParam(id, data) {
  return http.put(`/admin/cars/${id}/param`, data)
}
