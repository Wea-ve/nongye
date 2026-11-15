import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { auth, fetchMe, roleMap, type Role } from '../store/auth'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    role?: Role
  }
}

// 注意：createWebHistory('/app/') 已含 base，路由路径不再带 /app 前缀
const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  // 消费者公开页
  { path: '/trace/:code', component: () => import('../views/consumer/TraceDetailView.vue'), meta: { public: true } },
  { path: '/invite/:code', component: () => import('../views/consumer/InviteView.vue'), meta: { public: true } },
  { path: '/consumer/home', component: () => import('../views/consumer/HomeView.vue'), meta: { public: true } },
  { path: '/assistant', component: () => import('../views/consumer/AssistantView.vue'), meta: { public: true } },
  { path: '/assistant/:batchId', component: () => import('../views/consumer/AssistantView.vue'), meta: { public: true } },
  { path: '/complaint/new', component: () => import('../views/consumer/ComplaintNewView.vue'), meta: { public: true } },
  { path: '/complaint/query', component: () => import('../views/consumer/ComplaintQueryView.vue'), meta: { public: true } },
  // 管理员可视化大屏（全屏独立页，不套控制台布局）
  { path: '/admin/bigscreen', component: () => import('../views/admin/BigScreenView.vue'), meta: { role: 'admin' } },
  // 控制台（三端共用布局）
  {
    path: '/',
    component: () => import('../components/ConsoleLayout.vue'),
    children: [
      { path: '', redirect: '/login' },
      // 农户端
      { path: 'farmer/dashboard', component: () => import('../views/farmer/DashboardView.vue'), meta: { role: 'farmer' } },
      { path: 'farmer/batch', component: () => import('../views/farmer/BatchListView.vue'), meta: { role: 'farmer' } },
      { path: 'farmer/batch/:id/logs', component: () => import('../views/farmer/BatchLogsView.vue'), meta: { role: 'farmer' } },
      { path: 'farmer/logs', component: () => import('../views/farmer/LogsView.vue'), meta: { role: 'farmer' } },
      { path: 'farmer/trace-codes', component: () => import('../views/farmer/TraceCodesView.vue'), meta: { role: 'farmer' } },
      { path: 'farmer/reports', component: () => import('../views/farmer/ReportsView.vue'), meta: { role: 'farmer' } },
      { path: 'farmer/sales', component: () => import('../views/farmer/SalesView.vue'), meta: { role: 'farmer' } },
      { path: 'farmer/promises', component: () => import('../views/farmer/PromiseView.vue'), meta: { role: 'farmer' } },
      { path: 'farmer/complaints', component: () => import('../views/farmer/ComplaintsView.vue'), meta: { role: 'farmer' } },
      // 经销商端
      { path: 'dealer/dashboard', component: () => import('../views/dealer/DashboardView.vue'), meta: { role: 'dealer' } },
      { path: 'dealer/inbound', component: () => import('../views/dealer/InboundView.vue'), meta: { role: 'dealer' } },
      { path: 'dealer/outbound', component: () => import('../views/dealer/OutboundView.vue'), meta: { role: 'dealer' } },
      { path: 'dealer/records', component: () => import('../views/dealer/RecordsView.vue'), meta: { role: 'dealer' } },
      { path: 'dealer/inbound-records', component: () => import('../views/dealer/InboundRecordsView.vue'), meta: { role: 'dealer' } },
      { path: 'dealer/outbound-records', component: () => import('../views/dealer/OutboundRecordsView.vue'), meta: { role: 'dealer' } },
      { path: 'dealer/trace', component: () => import('../views/dealer/TraceView.vue'), meta: { role: 'dealer' } },
      // 管理员端
      { path: 'admin/dashboard', component: () => import('../views/admin/DashboardView.vue'), meta: { role: 'admin' } },
      { path: 'admin/users', component: () => import('../views/admin/UsersView.vue'), meta: { role: 'admin' } },
      { path: 'admin/complaints', component: () => import('../views/admin/ComplaintsView.vue'), meta: { role: 'admin' } },
      { path: 'admin/batches', component: () => import('../views/admin/BatchesView.vue'), meta: { role: 'admin' } },
      { path: 'admin/farming-logs', component: () => import('../views/admin/FarmingLogsView.vue'), meta: { role: 'admin' } },
      { path: 'admin/reports', component: () => import('../views/admin/ReportsView.vue'), meta: { role: 'admin' } },
      { path: 'admin/trace-records', component: () => import('../views/admin/TraceRecordsView.vue'), meta: { role: 'admin' } },
      { path: 'admin/trace', component: () => import('../views/admin/TraceView.vue'), meta: { role: 'admin' } },
      { path: 'admin/scan-stats', component: () => import('../views/admin/ScanStatsView.vue'), meta: { role: 'admin' } }
    ]
  },
  { path: '/:pathMatch(.*)', component: () => import('../views/NotFoundView.vue'), meta: { public: true } }
]

// 页面标题映射：多标签页场景下能一眼分清在哪个页面
const TITLE_MAP: Record<string, string> = {
  '/login': '登录',
  '/consumer/home': '信任者首页',
  '/consumer/assistant': '消费助手',
  '/assistant': '消费助手',
  '/complaint/new': '提交投诉',
  '/complaint/query': '投诉查询',
  '/admin/bigscreen': '数据大屏',
  '/farmer/dashboard': '农户工作台',
  '/farmer/batch': '批次管理',
  '/farmer/logs': '农事记录',
  '/farmer/trace-codes': '溯源码管理',
  '/farmer/reports': '检测报告',
  '/farmer/sales': '销售记录',
  '/farmer/promises': '信任承诺',
  '/farmer/complaints': '售后工单',
  '/dealer/dashboard': '经销商工作台',
  '/dealer/inbound': '入库登记',
  '/dealer/outbound': '出库登记',
  '/dealer/records': '流转记录',
  '/dealer/inbound-records': '入库记录',
  '/dealer/outbound-records': '出库记录',
  '/dealer/trace': '溯源查询',
  '/admin/dashboard': '管理工作台',
  '/admin/users': '用户管理',
  '/admin/complaints': '工单调度',
  '/admin/batches': '批次总览',
  '/admin/farming-logs': '农事留痕',
  '/admin/reports': '检测报告管理',
  '/admin/trace-records': '追溯记录',
  '/admin/trace': '追溯查询',
  '/admin/scan-stats': '扫码统计'
}

const router = createRouter({
  history: createWebHistory('/app/'),
  routes
})

router.afterEach((to) => {
  let name = TITLE_MAP[to.path]
  if (!name && to.path.startsWith('/trace/')) name = '溯源详情'
  if (!name && to.path.startsWith('/invite/')) name = '农户邀请'
  if (!name && to.path.startsWith('/assistant/')) name = '消费助手'
  if (!name && to.path.startsWith('/farmer/batch/')) name = '批次详情'
  document.title = name ? `${name} · 安溯·知源` : '安溯·知源 VeriTrace'
})

router.beforeEach(async (to) => {
  if (to.meta.public) {
    // 已登录访问登录页 → 直接进对应工作台
    if (to.path === '/login' && auth.loaded && auth.user) {
      return roleMap[auth.user.role]?.home || '/login'
    }
    return true
  }
  if (!auth.loaded) await fetchMe()
  else await fetchMe(true) // 每次进入受保护页面同步一次会话身份，避免多标签页切换账号后身份不一致
  if (!auth.user) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.role && to.meta.role !== auth.user.role) {
    // 同一浏览器只能有一个登录身份：其他标签页登录了别的角色会把会话顶掉，
    // 这里明确提示，避免用户误以为页面自己乱跳
    ElMessage.warning(`当前登录身份为「${roleMap[auth.user.role]?.name || auth.user.role}」，已切换到对应工作台；如需使用其他角色功能请退出后重新登录`)
    return roleMap[auth.user.role]?.home || '/login'
  }
  return true
})

export default router
