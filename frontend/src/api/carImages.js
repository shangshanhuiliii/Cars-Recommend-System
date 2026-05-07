import http from './http'

export function uploadCarImage({ carId, file }) {
  const formData = new FormData()
  formData.append('carId', carId)
  formData.append('file', file)
  return http.post('/admin/car-images', formData)
}

export function fetchCarImages(params) {
  return http.get('/admin/car-images', { params })
}

export function auditCarImage(id, data) {
  return http.put(`/admin/car-images/${id}/audit`, data)
}

export function deleteCarImage(id) {
  return http.delete(`/admin/car-images/${id}`)
}
