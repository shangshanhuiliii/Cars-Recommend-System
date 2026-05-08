import http from './http'

export function loginUser(data) {
  return http.post('/auth/user/login', data)
}

export function loginAdmin(data) {
  return http.post('/auth/admin/login', data)
}

export function fetchCurrentPrincipal() {
  return http.get('/auth/me')
}

export function logoutAuth() {
  return http.post('/auth/logout')
}
