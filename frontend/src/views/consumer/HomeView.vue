<template>
  <div class="trace-page">
    <!-- 顶部导航条 -->
    <header class="t-nav">
      <div class="t-brand"><img class="brand-ico" src="../../assets/logo-icon.svg" alt="安溯·知源" />安溯·知源</div>
      <div class="t-links">
        <router-link to="/assistant">我的助手</router-link>
        <router-link to="/invite/new" v-if="consumer.profile">邀请朋友</router-link>
        <a v-if="consumer.profile" href="javascript:;" @click="switchAccount">切换账号</a>
        <router-link to="/login">管理入口</router-link>
      </div>
    </header>

    <div class="t-body" v-loading="loading">
      <!-- 助手 Tip 横条：未注册时点击弹出注册表单，已注册进入助手 -->
      <el-card v-if="home?.tip" shadow="never" class="t-card home-tip"
               @click="onTipClick">
        <span class="tip-ico">💡</span>
        <span class="tip-text">{{ home.tip }}</span>
        <span class="tip-arrow">→</span>
      </el-card>

      <!-- 发现好农货：注册后没有关注时的浏览入口 -->
      <el-card v-if="discover.length" shadow="never" class="t-card">
        <template #header>
          <div class="disc-head">
            <span>🌱 发现好农货</span>
            <span class="disc-sub">点进去可关注农场、加入常购、问 AI</span>
          </div>
        </template>
        <div class="disc-list">
          <div v-for="(d, i) in discover" :key="i" class="disc-item" @click="openTraceCode(d.traceCode)">
            <div class="disc-crop">{{ d.cropName }}<span class="disc-variety">{{ d.variety }}</span></div>
            <div class="disc-farmer">{{ d.farmerName || '本地农户' }}</div>
            <div class="disc-tags">
              <el-tag v-if="d.promiseSummary" size="small" type="success" effect="plain">{{ d.promiseSummary }}</el-tag>
              <el-tag v-if="d.subscribeOpen" size="small" effect="plain">可订阅</el-tag>
              <el-tag v-if="d.onSale" size="small" effect="plain" type="warning">在售</el-tag>
            </div>
            <div class="disc-bottom">
              <span class="disc-status">{{ d.statusText }}<template v-if="d.expectDate"> · 预计 {{ d.expectDate }}</template></span>
              <span class="disc-go">去看看 →</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 我的信任清单 -->
      <el-card shadow="never" class="t-card">
        <template #header>🌾 我的信任清单</template>
        <div v-if="home?.follows?.length" class="follow-list">
          <div v-for="(f, i) in home.follows" :key="i" class="follow-item"
               :class="{ 'follow-link': f.traceCode }" @click="openTraceCode(f.traceCode)">
            <div class="follow-main">
              <span class="follow-target">你关注 {{ f.targetName }}</span>
              <span class="follow-meta">· {{ f.days }} 天 · 买过 {{ f.buyCount || 0 }} 次</span>
            </div>
            <el-tag v-if="f.notifyEnabled" size="small" effect="dark" type="success" style="border:none">🔔 提醒中</el-tag>
            <span v-if="f.traceCode" class="follow-go">溯源 →</span>
          </div>
        </div>
        <el-empty v-else description="还没关注任何农场" :image-size="60" />
      </el-card>

      <!-- 正在长的 -->
      <el-card shadow="never" class="t-card">
        <template #header>🌱 正在长的</template>
        <div v-if="home?.growing?.length" class="growing-list">
          <div v-for="(g, i) in home.growing" :key="i" class="growing-item">
            <div class="growing-row">
              <div class="growing-main">
                <span class="growing-crop">{{ g.cropName }}</span>
                <span class="growing-meta">{{ g.farmerName || '' }} · {{ g.statusText || growingStatusText(g.status) }}</span>
              </div>
              <div class="growing-right">
                <span v-if="g.expectDate" class="growing-date">预计 {{ g.expectDate }}</span>
                <el-switch v-model="g.notifyEnabled" size="small" active-text="🔔 上市提醒我"
                           @change="toggleNotify(g)" />
              </div>
            </div>
            <!-- 农户承诺兑现状态：由系统按真实流转时间自动判定 -->
            <div v-if="g.promises?.length" class="promise-row">
              <el-tooltip v-for="(p, pi) in g.promises" :key="pi" :content="p.remark || '暂无判定依据'" placement="top">
                <el-tag size="small" effect="plain" class="promise-tag" :type="promiseTagType(p.status)">
                  {{ promiseIcon(p.status) }} {{ p.typeText || '承诺' }}<template v-if="p.value"> · {{ p.value }}</template>
                  · {{ p.statusText || '待验证' }}
                </el-tag>
              </el-tooltip>
              <span v-if="g.traceCode" class="promise-more" @click="openTrace(g)">查看完整承诺与判定依据 →</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无正在生长的批次" :image-size="60" />
      </el-card>

      <!-- 待办 -->
      <el-card shadow="never" class="t-card">
        <template #header>📋 待办</template>
        <div v-if="home?.todos?.length" class="todo-list">
          <div v-for="(t, i) in home.todos" :key="i" class="todo-item" :class="{ overdue: t.type === 'complaint' && isComplaintOverdue(t) }">
            <div class="todo-main">
              <el-tag size="small" effect="plain" :type="t.type === 'subscribe' ? 'success' : 'warning'">
                {{ t.type === 'subscribe' ? '🛒 该买了' : '📢 售后' }}
              </el-tag>
              <span class="todo-text">{{ t.text }}</span>
            </div>
            <div class="todo-right">
              <span v-if="t.type === 'subscribe'" class="todo-date">{{ t.nextDate }}</span>
              <span v-else class="todo-sla" :class="{ 'sla-over': t.type === 'complaint' && isComplaintOverdue(t) }">
                剩余 {{ slaRemainHours(t) }} 小时
              </span>
              <el-button v-if="t.type === 'subscribe'" size="small" type="primary" text @click="markBought(t, i)">
                买过了
              </el-button>
              <el-button v-else size="small" type="primary" text @click="goComplaint(t)">
                去看
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="太棒了，暂无待办" :image-size="60" />
      </el-card>

      <!-- 助手推荐：一键进入 -->
      <el-card shadow="never" class="t-card home-assistant-cta" @click="$router.push('/assistant')">
        <div class="ha-icon">🤖</div>
        <div class="ha-body">
          <div class="ha-title">有问题？问我的消费助手</div>
          <div class="ha-desc">怎么存 · 什么时候再买 · 上次那个菜怎么样</div>
        </div>
        <span class="tip-arrow">→</span>
      </el-card>

      <!-- 未登录态：手机号登录 -->
      <el-card v-if="!consumer.profile" shadow="never" class="t-card home-login">
        <template #header>🔑 用手机号登录查看更多</template>
        <el-form label-width="80px">
          <el-form-item label="手机号" required>
            <el-input v-model="loginForm.phone" placeholder="11 位手机号" clearable maxlength="11" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" style="width: 100%" :loading="loginSubmitting" @click="doLogin">
              登录
            </el-button>
          </el-form-item>
          <div class="login-tip">
            还没账号？<a class="link-btn" @click="openRegister">手机号直接注册 →</a>
            或 <router-link to="/invite/admin">用邀请码注册</router-link>
          </div>
        </el-form>
      </el-card>

      <div class="t-foot">从田间到餐桌 · 每一步都可信 —— 安溯·知源 VeriTrace 平台提供</div>
    </div>

    <!-- 注册成为信任者 -->
    <el-dialog v-model="registerVisible" title="注册成为信任者" width="420px" append-to-body>
      <el-form label-width="72px">
        <el-form-item label="手机号" required>
          <el-input v-model="regForm.phone" placeholder="11 位手机号" clearable maxlength="11" />
        </el-form-item>
        <el-form-item label="怎么称呼" required>
          <el-input v-model="regForm.nickname" placeholder="如「王姐」" clearable maxlength="20" />
        </el-form-item>
      </el-form>
      <div class="reg-tip">注册后可关注农场、设置上市提醒、使用专属消费助手。</div>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="registerSubmitting" @click="doRegister">成为信任者</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http, { type ApiError } from '../../api/http'
import { consumer, fetchConsumerMe, consumerLogin, consumerRegister, consumerLogout } from '../../store/auth'

const router = useRouter()
const loading = ref<boolean>(false)
const loginSubmitting = ref<boolean>(false)

interface FollowItem {
  targetName: string
  days: number
  buyCount?: number
  notifyEnabled?: boolean
  traceCode?: string
}

interface PromiseItem {
  type?: string
  typeText?: string
  value?: string
  status?: 'FULFILLED' | 'FAILED' | 'UNKNOWN' | string
  statusText?: string
  remark?: string
}

interface DiscoverItem {
  traceCode?: string
  cropName: string
  variety?: string
  farmerName?: string
  promiseSummary?: string
  price?: number
  onSale?: boolean
  subscribeOpen?: boolean
  statusText?: string
  expectDate?: string
}

interface GrowingItem {
  cropName: string
  farmerName?: string
  status?: string
  statusText?: string
  expectDate?: string
  notifyEnabled: boolean
  id?: number
  traceCode?: string
  promises?: PromiseItem[]
}

interface TodoItem {
  type: 'subscribe' | 'complaint'
  text: string
  nextDate?: string
  complaintId?: number
  slaHours?: number
  slaPassedHours?: number
  id?: number
}

interface HomeData {
  follows: FollowItem[]
  growing: GrowingItem[]
  todos: TodoItem[]
  tip?: string | null
}

const home = ref<HomeData | null>(null)
const discover = ref<DiscoverItem[]>([])
const loginForm = reactive({ phone: '' })

/* ===== 注册成为信任者 ===== */
const registerVisible = ref<boolean>(false)
const registerSubmitting = ref<boolean>(false)
const regForm = reactive({ phone: '', nickname: '' })

function openRegister(): void {
  regForm.phone = loginForm.phone || ''
  regForm.nickname = ''
  registerVisible.value = true
}

/** Tip 横条：未注册先注册，已注册进入消费助手 */
function onTipClick(): void {
  if (consumer.profile) {
    router.push('/assistant')
  } else {
    openRegister()
  }
}

async function doRegister(): Promise<void> {
  if (!regForm.phone.trim() || regForm.phone.trim().length !== 11) {
    ElMessage.warning('请填写 11 位手机号')
    return
  }
  if (!regForm.nickname.trim()) { ElMessage.warning('请填写称呼'); return }
  registerSubmitting.value = true
  try {
    await consumerRegister(regForm.phone.trim(), regForm.nickname.trim(), '')
    ElMessage.success('恭喜你成为信任者！')
    registerVisible.value = false
    await loadHome()
  } catch { /* 已提示 */ } finally { registerSubmitting.value = false }
}

function growingStatusText(s?: string): string {
  if (!s) return '生长中'
  const m: Record<string, string> = {
    PLANTING: '种植中',
    GROWING: '生长中',
    HARVESTED: '已采收',
    HARVEST: '待采收'
  }
  return m[s] || s
}

/** 承诺兑现状态 → 标签配色 */
function promiseTagType(status?: string): 'success' | 'danger' | 'info' {
  if (status === 'FULFILLED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

function promiseIcon(status?: string): string {
  if (status === 'FULFILLED') return '✅'
  if (status === 'FAILED') return '⚠️'
  return '⏳'
}

/** 跳转溯源页查看完整承诺与兑现判定依据 */
function openTrace(g: GrowingItem): void {
  if (g.traceCode) router.push('/trace/' + g.traceCode)
}

/** 信任清单条目 → 对应批次溯源页 */
function openTraceCode(traceCode?: string): void {
  if (traceCode) router.push('/trace/' + traceCode)
}

/** 退出当前消费者身份，便于演示时注册/切换成另一个信任者 */
async function switchAccount(): Promise<void> {
  await consumerLogout()
  consumer.profile = null
  consumer.loaded = false
  ElMessage.success('已退出当前消费者，可重新注册新的信任者')
  location.reload()
}

function slaRemainHours(t: TodoItem): number {
  const remain = (t.slaHours ?? 24) - (t.slaPassedHours ?? 0)
  return remain
}

function isComplaintOverdue(t: TodoItem): boolean {
  return slaRemainHours(t) < 0
}

async function loadHome(): Promise<void> {
  loading.value = true
  try {
    home.value = await http.get<HomeData>('/api/consumer/home')
  } catch (e) {
    const msg = (e as ApiError)?.msg
    // 401 或未登录时后端应返回空数据（消费者会话是可选的）
    if (msg && msg.includes('未登录')) {
      home.value = { follows: [], growing: [], todos: [], tip: null }
    } else {
      console.warn('loadHome failed:', msg)
    }
  } finally { loading.value = false }
}

async function toggleNotify(g: GrowingItem): Promise<void> {
  if (!consumer.profile) {
    // 未注册先引导注册，开关回退
    g.notifyEnabled = !g.notifyEnabled
    openRegister()
    return
  }
  if (!g.id) {
    ElMessage.info('暂无批次关联，设置已本地保存')
    return
  }
  try {
    await http.post('/api/consumer/notify', {
      batchId: g.id,
      notifyEnabled: g.notifyEnabled
    })
    ElMessage.success(g.notifyEnabled ? '已开启上市提醒' : '已关闭提醒')
  } catch {
    // 保留本地状态
    g.notifyEnabled = !g.notifyEnabled
  }
}

async function markBought(t: TodoItem, idx: number): Promise<void> {
  if (!t.id) {
    home.value?.todos?.splice(idx, 1)
    ElMessage.success('已标记')
    return
  }
  try {
    await http.post(`/api/consumer/subscription/${t.id}/consume`)
    home.value?.todos?.splice(idx, 1)
    ElMessage.success('已标记买过')
  } catch { /* 已提示 */ }
}

function goComplaint(t: TodoItem): void {
  router.push(`/complaint/query?cid=${t.complaintId || ''}`)
}

async function doLogin(): Promise<void> {
  if (!loginForm.phone.trim()) { ElMessage.warning('请填写手机号'); return }
  loginSubmitting.value = true
  try {
    await consumerLogin(loginForm.phone.trim())
    ElMessage.success('登录成功')
    await loadHome()
  } catch { /* 已提示 */ } finally { loginSubmitting.value = false }
}

onMounted(async () => {
  await fetchConsumerMe()
  await loadHome()
  try {
    discover.value = await http.get<DiscoverItem[]>('/api/consumer/discover')
  } catch { discover.value = [] }
})
</script>

<style scoped>
/* 复用 consumer 公共样式 */
.trace-page { min-height: 100vh; background: #f4f7f4; }
.t-nav {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; background: #fff;
  box-shadow: 0 1px 8px rgba(31, 66, 39, .08);
  position: sticky; top: 0; z-index: 100;
}
.t-brand { font-weight: 700; color: #1b5e20; font-size: 17px; letter-spacing: 1px; }
.brand-ico { width: 22px; height: 22px; vertical-align: -5px; margin-right: 6px; }
.t-links { display: flex; gap: 18px; }
.t-links a { color: #4a674e; text-decoration: none; font-size: 14px; }
.t-links a:hover { color: #2e7d32; }
.t-body { max-width: 720px; margin: 0 auto; padding: 20px 14px 60px; }
.t-card { margin-bottom: 16px; }
.t-foot { text-align: center; color: #9aab9d; font-size: 12px; letter-spacing: 2px; margin-top: 10px; }

/* Tip 横条 */
.home-tip {
  display: flex; align-items: center; gap: 10px; cursor: pointer;
  background: linear-gradient(135deg, #e8f5e9, #f1f8e9);
  transition: transform .15s;
}
.home-tip:hover { transform: translateY(-1px); }
.tip-ico { font-size: 20px; }
.tip-text { flex: 1; color: #2e7d32; font-size: 14px; }
.tip-arrow { color: #2e7d32; font-weight: 700; font-size: 16px; }

/* 发现好农货 */
.disc-head { display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.disc-sub { font-size: 12px; color: #8a9a8e; font-weight: 400; }
.disc-list { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.disc-item {
  border: 1px solid #e8f1e8; border-radius: 10px; padding: 12px;
  background: #f7faf7; cursor: pointer; transition: all .15s;
  display: flex; flex-direction: column; gap: 6px;
}
.disc-item:hover { background: #eef6ee; border-color: #c8e6c9; transform: translateY(-1px); }
.disc-crop { font-size: 15px; font-weight: 700; color: #23422a; }
.disc-variety { font-size: 12px; color: #8a9a8e; font-weight: 400; margin-left: 6px; }
.disc-farmer { font-size: 12.5px; color: #4b5f51; }
.disc-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.disc-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 2px; }
.disc-status { font-size: 12px; color: #8a9a8e; }
.disc-go { font-size: 12px; color: #2e7d32; }
.disc-item:hover .disc-go { text-decoration: underline; }

/* 清单行 */
.follow-list { display: flex; flex-direction: column; gap: 10px; }
.follow-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 12px; background: #f7faf7;
  border-radius: 8px; border: 1px solid #e8f1e8;
}
.follow-target { font-weight: 600; color: #23422a; }
.follow-meta { color: #8a9a8e; font-size: 12.5px; }
.follow-link { cursor: pointer; transition: all .15s; }
.follow-link:hover { background: #eef6ee; border-color: #c8e6c9; }
.follow-go { font-size: 12px; color: #2e7d32; margin-left: 8px; }
.follow-link:hover .follow-go { text-decoration: underline; }

/* Growing */
.growing-list { display: flex; flex-direction: column; gap: 10px; }
.growing-item {
  display: flex; flex-direction: column; align-items: stretch;
  padding: 10px 12px; background: #f7faf7;
  border-radius: 8px; border: 1px solid #e8f1e8; gap: 8px;
}
.growing-row {
  display: flex; justify-content: space-between; align-items: center;
  flex-wrap: wrap; gap: 8px;
}
.promise-row { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.promise-tag { cursor: help; }
.promise-more { margin-left: auto; color: #2e7d32; font-size: 12px; cursor: pointer; }
.promise-more:hover { text-decoration: underline; }
.growing-main { flex: 1; min-width: 160px; }
.growing-crop { font-weight: 600; color: #23422a; font-size: 14px; }
.growing-meta { color: #8a9a8e; font-size: 12.5px; margin-left: 8px; }
.growing-right { display: flex; align-items: center; gap: 10px; }
.growing-date { font-size: 12px; color: #4a674e; background: #e8f5e9; border-radius: 4px; padding: 1px 6px; }

/* Todo */
.todo-list { display: flex; flex-direction: column; gap: 10px; }
.todo-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 12px; background: #f7faf7;
  border-radius: 8px; border: 1px solid #e8f1e8; flex-wrap: wrap; gap: 8px;
}
.todo-item.overdue { background: #fef0f0; border-color: #fde2e2; }
.todo-main { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 160px; }
.todo-text { color: #23422a; font-size: 14px; }
.todo-right { display: flex; align-items: center; gap: 10px; }
.todo-date { font-size: 12px; color: #4a674e; background: #e8f5e9; border-radius: 4px; padding: 1px 6px; }
.todo-sla { font-size: 12px; color: #6d7f70; background: #f4f7f4; border-radius: 4px; padding: 1px 6px; }
.sla-over { color: #c0392b; background: #fde2e2; font-weight: 600; }

/* 助手 CTA */
.home-assistant-cta {
  display: flex; align-items: center; gap: 14px; cursor: pointer;
  background: linear-gradient(135deg, #43a047, #1b5e20);
  color: #fff; border: none;
  transition: transform .15s;
}
.home-assistant-cta:hover { transform: translateY(-1px); }
.ha-icon { font-size: 32px; }
.ha-body { flex: 1; }
.ha-title { font-size: 15px; font-weight: 600; }
.ha-desc { font-size: 12px; opacity: .85; margin-top: 2px; }

/* 未登录态 */
.home-login { opacity: .9; }
.login-tip { font-size: 12px; color: #8a9a8e; margin-top: 4px; }
.login-tip a { color: #2e7d32; }
.link-btn { color: #2e7d32; cursor: pointer; }
.link-btn:hover { text-decoration: underline; }
.reg-tip { font-size: 12px; color: #8a9a8e; line-height: 1.6; }

@media (max-width: 768px) {
  .t-links { gap: 12px; }
  .growing-right, .todo-right { width: 100%; justify-content: flex-start; }
  .disc-list { grid-template-columns: 1fr; }
}
</style>
