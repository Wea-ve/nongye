<template>
  <div class="trace-page">
    <!-- 顶部导航条 -->
    <header class="t-nav">
      <div class="t-brand"><img class="brand-ico" src="../../assets/logo-icon.svg" alt="安溯·知源" />安溯·知源</div>
      <div class="t-links">
        <router-link to="/consumer/home">进入首页</router-link>
        <router-link to="/login">我要登录管理端</router-link>
      </div>
    </header>

    <div class="t-body" v-loading="loading">
      <!-- 错误态 -->
      <el-card v-if="error" shadow="never" class="t-card">
        <el-result icon="warning" title="该邀请码不存在，可能已失效" :sub-title="error">
          <template #extra>
            <el-button type="primary" @click="$router.push('/consumer/home')">回到首页</el-button>
          </template>
        </el-result>
      </el-card>

      <template v-else-if="data">
        <!-- Hero 卡片 -->
        <div class="invite-hero">
          <div class="invite-hero-left">
            <span class="invite-shield">🛡️</span>
            <div>
              <div class="invite-hero-title">{{ data.inviter.name }} 邀请你成为信任者</div>
              <div class="invite-hero-sub">来自 {{ data.inviter.role === 'FARMER' ? '农户' : '管理员' }} · 邀请码 {{ code }}</div>
            </div>
          </div>
        </div>

        <!-- 1. 这是谁？ -->
        <el-card shadow="never" class="t-card">
          <template #header>👤 这是谁？</template>
          <div class="invite-profile">
            <div class="invite-profile-row">
              <span class="invite-label">姓名</span><span>{{ data.inviter.name }}</span>
            </div>
            <div class="invite-profile-row">
              <span class="invite-label">身份</span>
              <el-tag size="small" effect="plain"
                      :type="data.inviter.role === 'FARMER' ? 'success' : 'info'">
                {{ data.inviter.role === 'FARMER' ? '农户' : '管理员' }}
              </el-tag>
            </div>
            <div class="invite-profile-row" v-if="data.inviter.batchCount != null">
              <span class="invite-label">批次数</span><span>{{ data.inviter.batchCount }} 批次</span>
            </div>
            <div class="invite-profile-row" v-if="data.inviter.location">
              <span class="invite-label">产地</span><span>{{ data.inviter.location }}</span>
            </div>
          </div>
        </el-card>

        <!-- 2. 种得怎么样？ -->
        <el-card shadow="never" class="t-card">
          <template #header>🌱 种得怎么样？</template>
          <div class="trust-score-wrap">
            <div class="trust-score-big">{{ data.trustStats.avgTrustScore.toFixed(1) }}</div>
            <div class="trust-score-unit">平均可信指数 / 100</div>
          </div>
          <div class="trust-levels">
            <div class="trust-level-row">
              <span class="tl-dot dot-GREEN"></span>
              <span class="tl-label">可信</span>
              <div class="tl-bar"><span class="tl-bar-in bar-GREEN" :style="{ width: trustPercent('GREEN') + '%' }"></span></div>
              <span class="tl-count">{{ data.trustStats.trustLevelCounts?.GREEN || 0 }}</span>
            </div>
            <div class="trust-level-row">
              <span class="tl-dot dot-YELLOW"></span>
              <span class="tl-label">基本可信</span>
              <div class="tl-bar"><span class="tl-bar-in bar-YELLOW" :style="{ width: trustPercent('YELLOW') + '%' }"></span></div>
              <span class="tl-count">{{ data.trustStats.trustLevelCounts?.YELLOW || 0 }}</span>
            </div>
            <div class="trust-level-row">
              <span class="tl-dot dot-RED"></span>
              <span class="tl-label">需谨慎</span>
              <div class="tl-bar"><span class="tl-bar-in bar-RED" :style="{ width: trustPercent('RED') + '%' }"></span></div>
              <span class="tl-count">{{ data.trustStats.trustLevelCounts?.RED || 0 }}</span>
            </div>
          </div>
        </el-card>

        <!-- 3. 敢承诺什么？ -->
        <el-card shadow="never" class="t-card">
          <template #header>📜 敢承诺什么？</template>
          <div v-if="promises.length" class="promise-list">
            <div v-for="(p, i) in promises" :key="i" class="promise-item">
              <el-tag size="small" effect="dark" type="success">✓</el-tag>
              <span class="promise-text">{{ p.content }}</span>
            </div>
          </div>
          <el-empty v-else description="该农户还没设置承诺" :image-size="60" />
        </el-card>

        <!-- 4. 已有多少人跟着买？ -->
        <el-card shadow="never" class="t-card">
          <template #header>👥 已有多少人跟着买？</template>
          <div class="follow-count-wrap">
            <span class="follow-num">{{ data.inviter.followerCount ?? 0 }}</span>
            <span class="follow-label">位信任者已加入</span>
          </div>
        </el-card>

        <!-- 底部注册表单 -->
        <el-card shadow="never" class="t-card invite-register-card">
          <template #header>✨ 成为信任者，4 步完成</template>
          <el-form label-width="80px">
            <el-form-item label="手机号" required>
              <el-input v-model="form.phone" placeholder="请输入手机号" clearable maxlength="11" />
            </el-form-item>
            <el-form-item label="怎么称呼" required>
              <el-input v-model="form.nickname" placeholder="给自己取个称呼，如「王姐」" clearable maxlength="20" />
            </el-form-item>
            <el-form-item label="验证码">
              <el-input v-model="form.code" placeholder="选填，后续扩展" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" style="width: 100%"
                         :loading="submitting" @click="submit">
                🤝 成为信任者
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <div class="t-foot">从田间到餐桌 · 每一步都可信 —— 安溯·知源 VeriTrace 平台提供</div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http, { type ApiError } from '../../api/http'
import { consumerRegister } from '../../store/auth'

const route = useRoute()
const router = useRouter()
const code = computed<string>(() => (route.params.code as string) || '')
const loading = ref<boolean>(false)
const error = ref<string>('')
const submitting = ref<boolean>(false)

interface InviterInfo {
  id: number
  name: string
  role: string
  batchCount?: number
  location?: string | null
  followerCount?: number
}

interface TrustStats {
  trustLevelCounts?: Record<string, number>
  avgTrustScore: number
}

interface InviteData {
  inviter: InviterInfo
  trustStats: TrustStats
}

interface PromiseItem {
  content: string
}

const data = ref<InviteData | null>(null)
const promises = ref<PromiseItem[]>([])
const form = reactive({ phone: '', nickname: '', code: '' })

/** 信任等级总数（用于百分比计算） */
const trustTotal = computed<number>(() => {
  const c = data.value?.trustStats.trustLevelCounts || {}
  return (c.GREEN || 0) + (c.YELLOW || 0) + (c.RED || 0)
})

function trustPercent(level: string): number {
  if (!trustTotal.value) return 0
  const n = data.value?.trustStats.trustLevelCounts?.[level] || 0
  return Math.round(n / trustTotal.value * 100)
}

async function load(): Promise<void> {
  if (!code.value) {
    error.value = '邀请码不能为空'
    return
  }
  loading.value = true
  try {
    data.value = await http.get<InviteData>(`/api/invite/${code.value}`)
  } catch (e) {
    error.value = (e as ApiError)?.msg || '该邀请码不存在，可能已失效'
  } finally {
    loading.value = false
  }
}

async function submit(): Promise<void> {
  if (!form.phone.trim()) { ElMessage.warning('请填写手机号'); return }
  if (!form.nickname.trim()) { ElMessage.warning('请填写称呼'); return }
  submitting.value = true
  try {
    await consumerRegister(form.phone.trim(), form.nickname.trim(), code.value)
    ElMessage.success('恭喜你成为信任者！')
    router.push('/consumer/home')
  } catch { /* 已提示 */ } finally { submitting.value = false }
}

onMounted(load)
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

/* 邀请专属 */
.invite-hero {
  background: linear-gradient(135deg, #43a047, #1b5e20);
  border-radius: 14px; padding: 28px 24px;
  color: #fff; margin-bottom: 16px;
  box-shadow: 0 4px 18px rgba(27, 94, 32, .25);
}
.invite-hero-left { display: flex; align-items: center; gap: 16px; }
.invite-shield { font-size: 46px; }
.invite-hero-title { font-size: 22px; font-weight: 700; letter-spacing: 1px; }
.invite-hero-sub { font-size: 13px; opacity: .85; margin-top: 6px; }

/* 信息行 */
.invite-profile { display: flex; flex-direction: column; gap: 10px; }
.invite-profile-row { display: flex; align-items: center; gap: 14px; font-size: 14px; }
.invite-label { color: #8a9a8e; width: 70px; flex: none; }

/* 可信指数 */
.trust-score-wrap { text-align: center; padding: 6px 0 14px; }
.trust-score-big {
  font-size: 44px; font-weight: 800; color: #1b5e20;
  font-variant-numeric: tabular-nums; letter-spacing: -1px;
}
.trust-score-unit { font-size: 12px; color: #8a9a8e; margin-top: 4px; }

.trust-levels { display: flex; flex-direction: column; gap: 8px; }
.trust-level-row {
  display: flex; align-items: center; gap: 10px; font-size: 13px;
}
.tl-dot { width: 9px; height: 9px; border-radius: 50%; flex: none; }
.dot-GREEN { background: #22b573; }
.dot-YELLOW { background: #f5a623; }
.dot-RED { background: #e74c3c; }
.tl-label { color: #23422a; width: 72px; flex: none; }
.tl-bar { flex: 1; height: 7px; background: #eef4ef; border-radius: 4px; overflow: hidden; }
.tl-bar-in { display: block; height: 100%; border-radius: 4px; transition: width .6s ease; }
.bar-GREEN { background: linear-gradient(90deg, #3dd68c, #1fa45b); }
.bar-YELLOW { background: linear-gradient(90deg, #ffd54f, #f5a623); }
.bar-RED { background: linear-gradient(90deg, #ff8a65, #e74c3c); }
.tl-count { width: 28px; text-align: right; color: #6d7f70; font-variant-numeric: tabular-nums; flex: none; }

/* 承诺列表 */
.promise-list { display: flex; flex-direction: column; gap: 8px; }
.promise-item { display: flex; align-items: center; gap: 8px; }
.promise-text { color: #33493a; font-size: 14px; }

/* 关注人数 */
.follow-count-wrap { text-align: center; padding: 10px 0; }
.follow-num {
  font-size: 40px; font-weight: 800; color: #1b5e20;
  font-variant-numeric: tabular-nums; margin-right: 6px;
}
.follow-label { font-size: 14px; color: #6d7f70; }

/* 注册卡 */
.invite-register-card .el-card__header {
  background: linear-gradient(135deg, #e8f5e9, #c8e6c9);
  color: #1b5e20; font-weight: 600;
}

@media (max-width: 768px) {
  .invite-hero-title { font-size: 18px; }
  .t-links { gap: 12px; }
}
</style>
