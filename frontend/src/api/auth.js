import http from './http'

export function loginUnified(data) {
  return http.post('/auth/login', data)
}

export function loginUser(data) {
  return http.post('/auth/user/login', data)
}

export function loginAdmin(data) {
  return http.post('/auth/admin/login', data)
}

export function registerUser(data) {
  return http.post('/auth/user/register', data)
}

export function fetchCurrentPrincipal() {
  return http.get('/auth/me')
}

export function fetchUserProfile() {
  return http.get('/auth/profile')
}

export function updateUserProfile(data) {
  return http.put('/auth/profile', data)
}

export function logoutAuth() {
  return http.post('/auth/logout')
}
