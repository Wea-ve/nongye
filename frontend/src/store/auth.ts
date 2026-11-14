import { reactive } from 'vue'
import http from '../api/http'
import { clearAuthToken, getAuthToken, setAuthToken } from '../api/token'

export type Role = 'farmer' | 'dealer' | 'admin'

export interface AuthUser {
  id: number
  username: string
  realName: string
  role: Role
  phone?: string
  address?: string
  avatar?: string
}

interface AuthState {
  user: AuthUser | null
  loaded: boolean
}

interface RoleInfo {
  name: string
  icon: string
  home: string
}

/** 全局登录态（简单响应式单例，未引入 pinia） */
export const auth = reactive<AuthState>({
  user: null,
  loaded: false
})

export const roleMap: Record<Role, RoleInfo> = {
  farmer: { name: '农户', icon: '🧑‍🌾', home: '/farmer/dashboard' },
  dealer: { name: '经销商', icon: '🚚', home: '/dealer/dashboard' },
  admin: { name: '管理员', icon: '🛡️', home: '/admin/dashboard' }
}

export async function fetchMe(force = false): Promise<AuthUser | null> {
  if (auth.loaded && !force) return auth.user
  if (!getAuthToken()) {
    // 当前标签页没有任何登录凭证，无需请求后端
    auth.user = null
    auth.loaded = true
    return null
  }
  try {
    auth.user = await http.get<AuthUser>('/api/auth/me')
  } catch {
    auth.user = null
  }
  auth.loaded = true
  return auth.user
}

export async function login(username: string, password: string, role: Role): Promise<AuthUser> {
  const res = await http.post<AuthUser & { token: string }>('/api/auth/login', { username, password, role })
  const { token, ...user } = res
  setAuthToken(token)
  auth.user = user
  auth.loaded = true
  return user
}

export async function logout(): Promise<void> {
  try { await http.post('/api/auth/logout') } catch { /* 忽略 */ }
  clearAuthToken()
  auth.user = null
  auth.loaded = true
}

// ---------- consumer 单例 ----------

export interface ConsumerProfile {
  id: number
  nickname: string
  phone: string
  trustLevel?: 'FOLLOWER' | 'TRUSTER' | 'FAMILY'
  lastActiveTime?: string
}

interface ConsumerState {
  profile: ConsumerProfile | null
  loaded: boolean
}

export const consumer = reactive<ConsumerState>({ profile: null, loaded: false })

export async function fetchConsumerMe(): Promise<ConsumerProfile | null> {
  try { consumer.profile = await http.get<ConsumerProfile>('/api/consumer/me') }
  catch { consumer.profile = null }
  consumer.loaded = true
  return consumer.profile
}

export async function consumerRegister(phone: string, nickname: string, inviteCode?: string): Promise<ConsumerProfile> {
  const r: { account: ConsumerProfile } = await http.post('/api/consumer/register', { phone, nickname, inviteCode })
  consumer.profile = r.account
  consumer.loaded = true
  return consumer.profile
}

export async function consumerLogin(phone: string): Promise<ConsumerProfile> {
  const r: { account: ConsumerProfile } = await http.post('/api/consumer/login', { phone })
  consumer.profile = r.account
  consumer.loaded = true
  return consumer.profile
}

export async function consumerLogout(): Promise<void> {
  try { await http.post('/api/consumer/logout') } catch { /* 忽略 */ }
  consumer.profile = null
  consumer.loaded = true
}
