import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { clearAuthToken, getAuthToken } from './token'

/** 后端统一响应结构 */
export interface ApiEnvelope<T = unknown> {
  code: number
  msg?: string
  data?: T
}

/** 请求失败时 reject 的对象 */
export interface ApiError {
  status?: number
  msg?: string
}

/** 业务请求客户端：拦截器已解包为 data，因此返回 Promise<T> */
export interface RequestClient {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  request<T = unknown>(config: AxiosRequestConfig): Promise<T>
}

/**
 * 统一 axios 实例：后端返回 { code, msg, data }
 * 成功直接返回 data；失败弹提示并 reject { status, msg }
 */
const instance: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 30000,
  withCredentials: true
})

/**
 * 请求头携带登录令牌：不同标签页各自保存 token，实现多角色同时在线
 */
instance.interceptors.request.use((config) => {
  const token = getAuthToken()
  if (token) {
    config.headers.set('X-Auth-Token', token)
  }
  return config
})

/**
 * 403：当前会话身份与页面所需角色不一致（常见于多标签页切换了账号）。
 * 给出明确提示并回到登录页重新以正确角色登录。
 */
function handleForbidden(msg?: string): void {
  const tip = msg && msg !== '无权限'
    ? msg
    : '当前登录身份无权访问该功能（可能已在其他标签页切换过账号），请重新登录'
  ElMessage.error(tip)
  setTimeout(() => {
    if (!location.pathname.startsWith('/app/login')) location.href = '/app/login'
  }, 1200)
}

instance.interceptors.response.use(
  // 拦截器把响应解包成业务 data，因此返回类型与 Axios 默认签名不同
  (res: AxiosResponse): any => {
    const body = res.data as ApiEnvelope | unknown
    // 文件流（Excel 导出/二维码等）直接透传
    if (res.config.responseType === 'blob') return res
    if (body && typeof body === 'object' && 'code' in body) {
      const envelope = body as ApiEnvelope
      if (envelope.code === 0) return envelope.data
      if (envelope.code === 403) {
        handleForbidden(envelope.msg)
        return Promise.reject({ status: 403, msg: envelope.msg } satisfies ApiError)
      }
      ElMessage.error(envelope.msg || '操作失败')
      return Promise.reject({ status: envelope.code, msg: envelope.msg } satisfies ApiError)
    }
    return body
  },
  (err): any => {
    const status: number | undefined = err.response?.status
    const msg: string = err.response?.data?.msg || err.message || '网络异常'
    if (status === 401) {
      // 会话/令牌失效：清理本地令牌并回到登录页（消费者公开页不经过此拦截）
      clearAuthToken()
      if (!location.pathname.startsWith('/app/login')) {
        location.href = '/app/login'
      }
      return Promise.reject({ status, msg } satisfies ApiError)
    }
    if (status === 403) {
      handleForbidden(msg)
      return Promise.reject({ status, msg } satisfies ApiError)
    }
    ElMessage.error(msg)
    return Promise.reject({ status, msg } satisfies ApiError)
  }
)

const http = instance as unknown as RequestClient

export default http
