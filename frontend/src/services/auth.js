const KEY = 'tariff_auth'

export function setAuth(data) {
  localStorage.setItem(KEY, JSON.stringify(data))
}
export function getAuth() {
  try { return JSON.parse(localStorage.getItem(KEY) || 'null') } catch { return null }
}
export function getToken() {
  return getAuth()?.token || null
}
export function getUser() {
  const a = getAuth()
  if (!a) return null
  return { username: a.username, role: a.role }
}
export function isAuthed() {
  return !!getToken()
}
export function logout() {
  localStorage.removeItem(KEY)
}
