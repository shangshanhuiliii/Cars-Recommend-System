import http from './http'

export function fetchAdminStatOverview() {
  return http.get('/admin/stat/overview')
}
