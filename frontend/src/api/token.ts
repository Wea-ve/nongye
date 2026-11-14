/**
 * 登录令牌存储。
 * sessionStorage：按标签页隔离，同一浏览器多个标签页可登录不同角色互不影响；
 * localStorage：兜底，新标签页无需重新登录。
 */
const KEY = 'vt_auth_token'

export function getAuthToken(): string {
  return sessionStorage.getItem(KEY) || localStorage.getItem(KEY) || ''
}

export function setAuthToken(token: string): void {
  sessionStorage.setItem(KEY, token)
  localStorage.setItem(KEY, token)
}

export function clearAuthToken(): void {
  sessionStorage.removeItem(KEY)
  localStorage.removeItem(KEY)
}
