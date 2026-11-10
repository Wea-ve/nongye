<template>
  <div class="trace-page">
    <!-- 顶部导航条 -->
    <header class="t-nav">
      <div class="t-brand"><img class="brand-ico" src="../../assets/logo-icon.svg" alt="安溯·知源" />安溯·知源</div>
      <div class="t-links">
        <router-link to="/assistant">🛡️ 消费助手</router-link>
        <router-link to="/complaint/new">我要投诉</router-link>
        <router-link to="/complaint/query">投诉查询</router-link>
        <router-link to="/login">管理入口</router-link>
      </div>
    </header>

    <div class="t-body" v-loading="loading">
      <!-- 加载失败 -->
      <el-card v-if="error" shadow="never" class="t-card">
        <el-result icon="warning" title="溯源码无效或不存在" :sub-title="error">
          <template #extra>
            <el-button type="primary" @click="reload">重新加载</el-button>
          </template>
        </el-result>
      </el-card>

      <template v-else-if="d.batch">
        <!-- 成功提交投诉提示 -->
        <el-alert v-if="justComplained" type="success" :closable="false" show-icon class="t-card"
                  :title="`投诉已提交！您的查询码为 ${$route.query.cid}，可用手机号或查询码在「投诉查询」页跟踪进度`" />

        <!-- Hero 卡片 -->
        <div class="trace-hero">
          <div class="hero-row">
            <div>
              <div class="hero-crop">🥬 {{ d.batch.cropName }}</div>
              <div class="hero-sub">
                <el-tag effect="dark" type="info" size="small" style="background: rgba(255,255,255,.2); border: none">
                  {{ d.batch.variety || '普通品种' }}
                </el-tag>
                <span class="hero-code">溯源码：{{ d.batch.traceCode }}</span>
              </div>
            </div>
            <div class="hero-badge" v-if="(d.nodeCount || 0) > 0">
              <div class="badge-num">{{ d.nodeCount }}</div>
              <div class="badge-text">可信流转节点</div>
            </div>
          </div>
        </div>

        <!-- 信任关系条：谁种的 + 关注状态 -->
        <div class="trust-rel" v-if="d.farmer">
          <div class="rel-left">
            <div class="rel-avatar">{{ (d.farmer.name || '农').slice(0, 1) }}</div>
            <div>
              <div class="rel-name">
                {{ d.farmer.name }}
                <el-tag size="small" effect="plain" type="success">实名农户</el-tag>
              </div>
              <div class="rel-meta">
                📍 {{ d.farmer.location || '产地未登记' }} · 联系电话 {{ d.farmer.phoneMasked || '未公开' }}
              </div>
            </div>
          </div>
          <div class="rel-right">
            <span class="rel-stat">本批 <b>{{ d.nodeCount || 0 }}</b> 个可信节点</span>
            <el-button size="small" round :type="d.following?.farmerFollowed ? 'info' : 'success'"
                       @click="followFarmer">
              {{ d.following?.farmerFollowed ? '已关注农场' : '+ 关注农场' }}
            </el-button>
          </div>
        </div>

        <!-- 决策翻译：该不该买 / 要注意什么 / 怎么存 -->
        <el-card shadow="never" class="t-card decision-card" v-if="d.decision">
          <div class="dc-head">
            <el-tag effect="dark" size="large"
                    :type="d.decision.level === 'GREEN' ? 'success' : d.decision.level === 'YELLOW' ? 'warning' : 'danger'">
              {{ d.decision.verdict }}
            </el-tag>
            <span class="dc-advice">{{ d.decision.advice }}</span>
          </div>
          <div class="dc-grid">
            <div class="dc-col">
              <div class="dc-title">✅ 可以放心的理由</div>
              <div v-for="(r, i) in d.decision.reasons" :key="'r' + i" class="dc-line">{{ r }}</div>
              <div v-if="!d.decision.reasons.length" class="dc-line muted">暂无可核验的正面依据</div>
            </div>
            <div class="dc-col">
              <div class="dc-title">⚠️ 需要留意</div>
              <div v-for="(c, i) in d.decision.cautions" :key="'c' + i" class="dc-line warn">{{ c }}</div>
              <div v-if="!d.decision.cautions.length" class="dc-line muted">无明显风险项</div>
            </div>
            <div class="dc-col">
              <div class="dc-title">🧊 买回家怎么存</div>
              <div class="dc-line">{{ d.decision.shelfTip }}</div>
            </div>
          </div>
          <div class="dc-actions">
            <el-button type="success" round @click="addToBasket">🛒 加入常购</el-button>
            <el-button round @click="followBatch">
              {{ d.following?.batchFollowed ? '已关注本批' : '🔔 关注本批上市' }}
            </el-button>
            <el-button type="warning" round plain @click="complaintVisible = true">📢 发起售后</el-button>
            <el-button round plain @click="openAssistant">🛡️ 问 AI 助手</el-button>
          </div>
        </el-card>

        <!-- 承诺兑现 -->
        <el-card shadow="never" class="t-card">
          <template #header>🤝 农户承诺兑现</template>
          <template v-if="d.promise && d.promise.judgments?.length">
            <div class="pm-list">
              <div v-for="j in d.promise.judgments" :key="j.promise.id" class="pm-item">
                <el-tag size="small" effect="dark"
                        :type="j.judgment === 'FULFILLED' ? 'success' : j.judgment === 'FAILED' ? 'danger' : 'info'">
                  {{ j.judgment === 'FULFILLED' ? '已兑现' : j.judgment === 'FAILED' ? '未兑现' : '待核验' }}
                </el-tag>
                <div class="pm-body">
                  <div class="pm-type">{{ promiseTypeText(j.promise.promiseType) }}：{{ j.promise.promiseValue }}</div>
                  <div class="pm-remark">{{ j.remark }}</div>
                </div>
              </div>
            </div>
            <div class="pm-note">承诺由农户事前公示，兑现结果由系统按真实流转时间自动判定，不可事后修改。</div>
          </template>
          <el-empty v-else description="该农户还没设置公开承诺" :image-size="70" />
        </el-card>

        <el-card shadow="never" class="t-card" v-if="d.trust">
          <template #header>🛡️ AI 可信指数</template>
          <div class="trust-wrap">
            <div class="gauge-box">
              <svg viewBox="0 0 120 120" width="132" height="132">
                <circle cx="60" cy="60" r="52" class="g-bg" />
                <circle cx="60" cy="60" r="52" class="g-val" :class="'g-' + d.trust.level"
                        :stroke-dasharray="RING_C" :stroke-dashoffset="gaugeOffset"
                        transform="rotate(-90 60 60)" />
              </svg>
              <div class="gauge-center">
                <div class="g-num">{{ d.trust.score }}</div>
                <div class="g-unit">可信指数 / 100</div>
              </div>
            </div>
            <div class="trust-main">
              <div class="trust-verdict">
                <el-tag effect="dark" size="small" :type="levelInfo.tag">{{ levelInfo.text }}</el-tag>
                <span>{{ d.trust.verdict }}</span>
              </div>
              <div class="trust-items">
                <div v-for="it in d.trust.items" :key="it.key" class="t-item">
                  <div class="t-item-head" @click="toggleItem(it.key)">
                    <span class="lv-dot" :class="'dot-' + it.level"></span>
                    <span class="t-label">{{ it.label }}</span>
                    <span class="t-bar"><span class="t-bar-in" :class="'bar-' + it.level"
                                             :style="{ width: (it.score / it.max * 100) + '%' }"></span></span>
                    <span class="t-score">{{ it.score }}/{{ it.max }}</span>
                    <span class="t-arrow" :class="{ open: openItem === it.key }">▾</span>
                  </div>
                  <div v-if="openItem === it.key" class="t-evidence">
                    <div class="ev-detail">{{ it.detail }}</div>
                    <div v-for="(ev, i) in it.evidence" :key="i" class="ev-line">· {{ ev }}</div>
                  </div>
                </div>
              </div>
              <div class="trust-note">评分由确定性规则引擎基于检测报告、农事记录与流转链路计算，每项可展开查看证据，可复核、可追溯，非 AI 主观打分。</div>
            </div>
          </div>
        </el-card>

        <!-- 批次信息 -->
        <el-card shadow="never" class="t-card">
          <template #header>📋 批次档案</template>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="批次号">{{ d.batch.batchNo }}</el-descriptions-item>
            <el-descriptions-item label="产地">{{ d.batch.landLocation || '未记录' }}</el-descriptions-item>
            <el-descriptions-item label="种植农户">{{ d.batch.farmerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="播种日期">{{ fmtDate(d.batch.plantDate) }}</el-descriptions-item>
            <el-descriptions-item label="采收日期">{{ fmtDate(d.batch.actualHarvestDate) }}</el-descriptions-item>
            <el-descriptions-item label="采收重量">
              {{ d.batch.harvestWeight ? d.batch.harvestWeight + ' 斤' : '未采收' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 流转链路 -->
        <el-card shadow="never" class="t-card" v-if="d.records?.length">
          <template #header>🔄 全链路流转（{{ d.records.length }} 个节点）</template>
          <el-timeline>
            <el-timeline-item v-for="r in d.records" :key="r.id"
                              :color="traceTypeMap[r.type]?.color" :hollow="false">
              <div class="flow-item">
                <el-tag size="small" effect="dark" :color="traceTypeMap[r.type]?.color" style="border: none">
                  {{ traceTypeMap[r.type]?.text || r.type }}
                </el-tag>
                <b>{{ r.fromUserName || '?' }} → {{ r.toUserName || '市场/消费者' }}</b>
                <span v-if="r.quantity" class="flow-meta">数量 {{ r.quantity }} 斤</span>
                <span v-if="r.location" class="flow-meta">📍 {{ r.location }}</span>
                <span class="flow-time">{{ fmtTime(r.operateTime) }}</span>
                <div v-if="r.remark" class="flow-remark">{{ r.remark }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 农事记录 -->
        <el-card shadow="never" class="t-card">
          <template #header>📓 农事记录（{{ d.logs?.length || 0 }} 条）</template>
          <el-timeline v-if="d.logs?.length">
            <el-timeline-item v-for="l in d.logs" :key="l.id" :timestamp="fmtDate(l.logDate)"
                              :color="logTypeMap[l.logType]?.color">
              <div class="flow-item">
                <el-tag size="small" effect="plain">{{ logTypeMap[l.logType]?.text || l.logType }}</el-tag>
                <span v-if="l.material" class="flow-meta">{{ l.material }}<template v-if="l.dosage">（{{ l.dosage }}）</template></span>
                <div v-if="l.content" class="flow-remark">{{ l.content }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无农事记录" :image-size="70" />
        </el-card>

        <!-- 检测报告 -->
        <el-card shadow="never" class="t-card">
          <template #header>🧪 第三方检测报告</template>
          <template v-if="d.reports?.length">
            <div v-for="r in d.reports" :key="r.id" class="report-row">
              <div>
                <div class="report-no">{{ r.reportNo || '检测报告' }}</div>
                <div class="report-meta">{{ r.institution || '检测机构' }} · {{ fmtDate(r.reportDate) }}</div>
              </div>
              <div style="display: flex; align-items: center; gap: 10px">
                <el-tag :type="r.result === '合格' ? 'success' : 'danger'" effect="dark" size="small">
                  {{ r.result === '合格' ? '✓ ' : '✗ ' }}{{ r.result }}
                </el-tag>
                <el-link v-if="r.reportUrl" type="primary" :href="r.reportUrl" target="_blank">查看报告</el-link>
              </div>
            </div>
          </template>
          <el-empty v-else description="该批次暂无检测报告" :image-size="70" />
        </el-card>

        <!-- AI 体检报告 -->
        <el-card shadow="never" class="t-card">
          <template #header>🩺 AI 批次体检报告</template>
          <div v-if="healthLoading" v-loading="true" element-loading-text="正在汇总检测报告与农事记录，生成体检报告…"
               element-loading-background="rgba(255,255,255,.85)">
            <div style="min-height: 96px"></div>
          </div>
          <template v-else-if="healthReport">
            <div class="hr-meta">🤖 AI 生成 · 事实基础为本批次登记数据 · {{ healthTime }}</div>
            <div class="hr-body">{{ healthReport }}</div>
          </template>
          <div v-else class="hr-empty">
            <div class="hr-desc">AI 将综合可信指数评分、检测报告、农事与流转数据，为该批次生成一份可核验的健康体检报告。</div>
            <el-button type="primary" round @click="genHealthReport">🤖 生成 AI 体检报告</el-button>
          </div>
        </el-card>

        <!-- 投诉入口 -->
        <el-card shadow="never" class="t-card" style="text-align: center">
          <div style="color: #8a9a8e; margin-bottom: 12px">对这批农产品有疑问或问题？</div>
          <el-button type="warning" size="large" round @click="complaintVisible = true">
            📢 对本批次发起投诉
          </el-button>
        </el-card>

        <div class="t-foot">从田间到餐桌 · 每一步都可信 —— 安溯·知源 VeriTrace 平台提供</div>
      </template>
    </div>

    <!-- 投诉弹窗 -->
    <el-dialog v-model="complaintVisible" title="投诉反馈" width="480px">
      <el-alert type="info" :closable="false" style="margin-bottom: 14px"
                :title="`投诉对象：${d.batch?.cropName}（${d.batch?.batchNo}）`" />
      <el-form label-width="80px">
        <el-form-item label="投诉人" required><el-input v-model="cForm.complainant" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="cForm.phone" placeholder="用于查询处理进度" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="cForm.type" style="width: 100%">
            <el-option v-for="(v, k) in complaintTypeMap" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="cForm.content" type="textarea" :rows="4"
                    placeholder="请描述您遇到的问题（变质、缺斤少两、信息不实等）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="complaintVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitComplaint">提交投诉</el-button>
      </template>
    </el-dialog>

    <!-- AI 浮窗 -->
    <AiChat v-if="d.batch" :batch-id="d.batch.id" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http, { type ApiError } from '../../api/http'
import AiChat from '../../components/AiChat.vue'
import { complaintTypeMap, fmtDate, fmtTime, logTypeMap, traceTypeMap } from '../../utils'
import type { ComplaintSubmitResult, HealthReportData, TraceDetailData } from '../../types'

const route = useRoute()
const router = useRouter()
const d = ref<Partial<TraceDetailData>>({})
const loading = ref<boolean>(false)
const error = ref<string>('')
const saving = ref<boolean>(false)
const complaintVisible = ref<boolean>(false)
const justComplained = computed<boolean>(() => route.query.complained === '1')
const cForm = reactive({ complainant: '', phone: '', type: 'QUALITY', content: '' })

async function load(): Promise<void> {
  loading.value = true
  try {
    d.value = await http.get<TraceDetailData>(`/api/trace/${route.params.code}`)
  } catch (e) {
    error.value = (e as ApiError)?.msg || '请核对二维码是否正确'
  } finally { loading.value = false }
}
const reload = (): void => { error.value = ''; load() }

async function submitComplaint(): Promise<void> {
  if (!cForm.complainant.trim()) {
    ElMessage.warning('请填写投诉人')
    return
  }
  if (!cForm.content.trim()) {
    ElMessage.warning('请填写投诉内容')
    return
  }
  saving.value = true
  try {
    const data = await http.post<ComplaintSubmitResult>('/api/complaint', {
      ...cForm,
      batchId: d.value.batch!.id
    })
    complaintVisible.value = false
    ElMessage.success('投诉已提交')
    // 回到查询页展示查询码
    const kw = cForm.phone || data.id
    location.href = `/app/complaint/query?keyword=${kw}&submitted=1&cid=${data.id}`
  } catch { /* 已提示 */ } finally { saving.value = false }
}

onMounted(load)

/* ===== 信任关系 / 决策翻译 / 下一步动作 ===== */
const PROMISE_TYPES: Record<string, string> = {
  FRESH: '新鲜承诺',
  HARVEST_DATE: '采收期承诺',
  QUALITY: '品质承诺'
}
const promiseTypeText = (t: string): string => PROMISE_TYPES[t] || t

/** 关注类操作需先注册信任者身份 */
function ensureRegistered(): boolean {
  if (d.value.following?.registered) return true
  ElMessage.warning('请先注册成为信任者，才能关注农场、加入常购')
  router.push('/invite/admin')
  return false
}

async function followFarmer(): Promise<void> {
  if (!d.value.farmer?.id) return
  if (!ensureRegistered()) return
  if (d.value.following?.farmerFollowed) return
  try {
    await http.post('/api/consumer/follow', { targetType: 'FARMER', targetId: d.value.farmer.id })
    if (d.value.following) d.value.following.farmerFollowed = true
    ElMessage.success(`已关注 ${d.value.farmer.name} 的农场，上新会提醒你`)
  } catch { /* 已提示 */ }
}

async function followBatch(): Promise<void> {
  if (!d.value.batch) return
  if (!ensureRegistered()) return
  if (d.value.following?.batchFollowed) return
  try {
    await http.post('/api/consumer/follow', { targetType: 'BATCH', targetId: d.value.batch.id })
    if (d.value.following) d.value.following.batchFollowed = true
    ElMessage.success('已关注本批上市提醒')
  } catch { /* 已提示 */ }
}

async function addToBasket(): Promise<void> {
  if (!d.value.batch) return
  if (!ensureRegistered()) return
  try {
    await http.post('/api/consumer/subscription', {
      cropName: d.value.batch.cropName,
      batchId: d.value.batch.id,
      qty: 5,
      cycleDays: 7
    })
    ElMessage.success('已加入常购（默认 5 斤 / 7 天），到点助手会提醒你')
  } catch { /* 已提示 */ }
}

function openAssistant(): void {
  const code = d.value.batch?.traceCode
  router.push(code ? `/assistant/${code}` : '/assistant')
}

/* ===== AI 可信指数 ===== */
const RING_C = 2 * Math.PI * 52
const gaugeOffset = computed<number>(() => RING_C * (1 - (d.value.trust?.score ?? 0) / 100))
const openItem = ref<string>('')
const toggleItem = (k: string): void => { openItem.value = openItem.value === k ? '' : k }
const LEVELS = {
  GREEN: { text: '可信', tag: 'success' },
  YELLOW: { text: '基本可信', tag: 'warning' },
  RED: { text: '需谨慎', tag: 'danger' }
} as const
const levelInfo = computed(() => LEVELS[d.value.trust?.level ?? 'YELLOW'])

/* ===== AI 批次体检报告 ===== */
const healthReport = ref<string>('')
const healthLoading = ref<boolean>(false)
const healthTime = ref<string>('')
async function genHealthReport(): Promise<void> {
  if (!d.value.batch) return
  healthLoading.value = true
  try {
    const r = await http.post<HealthReportData>('/api/ai/health-report',
            { batchId: d.value.batch.id }, { timeout: 90000 })
    healthReport.value = r.report
    healthTime.value = new Date().toLocaleString('zh-CN', { hour12: false })
  } catch {
    ElMessage.error('体检报告生成失败，请稍后再试')
  } finally { healthLoading.value = false }
}
</script>

<style scoped>
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
.t-body { max-width: 860px; margin: 0 auto; padding: 20px 14px 60px; }
.t-card { margin-bottom: 16px; }
.hero-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.hero-crop { font-size: 30px; font-weight: 800; letter-spacing: 2px; }
.hero-sub { margin-top: 10px; display: flex; align-items: center; gap: 10px; }
.hero-code { color: rgba(255, 255, 255, .85); font-size: 14px; font-family: monospace; }
.hero-badge {
  text-align: center; background: rgba(255, 255, 255, .16);
  border: 1px solid rgba(255, 255, 255, .4); border-radius: 14px; padding: 10px 18px;
}
.badge-num { font-size: 26px; font-weight: 800; }
.badge-text { font-size: 12px; color: rgba(255, 255, 255, .85); }
.flow-item { line-height: 1.9; }
.flow-meta { margin-left: 10px; color: #666; font-size: 13px; }
.flow-time { margin-left: 10px; color: #999; font-size: 12.5px; }
.flow-remark { color: #777; font-size: 13px; margin-top: 2px; }
.report-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 4px; border-bottom: 1px dashed #e4efe5;
}
.report-no { font-weight: 600; color: #23422a; }
.report-meta { font-size: 12.5px; color: #8a9a8e; margin-top: 2px; }
.t-foot { text-align: center; color: #9aab9d; font-size: 12px; letter-spacing: 2px; margin-top: 10px; }
/* AI 可信指数 */
.trust-wrap { display: flex; gap: 22px; align-items: flex-start; flex-wrap: wrap; }
.gauge-box { position: relative; width: 132px; height: 132px; flex: none; }
.g-bg { fill: none; stroke: #edf3ee; stroke-width: 11; }
.g-val { fill: none; stroke-width: 11; stroke-linecap: round; transition: stroke-dashoffset .8s ease; }
.g-GREEN { stroke: #22b573; }
.g-YELLOW { stroke: #f5a623; }
.g-RED { stroke: #e74c3c; }
.gauge-center { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.g-num { font-size: 34px; font-weight: 800; color: #23422a; line-height: 1; }
.g-unit { font-size: 11px; color: #8a9a8e; margin-top: 5px; }
.trust-main { flex: 1; min-width: 260px; }
.trust-verdict { display: flex; align-items: center; gap: 8px; color: #3c5642; font-size: 14px; margin-bottom: 10px; }
.trust-items { display: flex; flex-direction: column; }
.t-item { border-bottom: 1px dashed #e4efe5; }
.t-item:last-child { border-bottom: none; }
.t-item-head { display: flex; align-items: center; gap: 10px; padding: 9px 2px; cursor: pointer; user-select: none; }
.t-item-head:hover .t-label { color: #2e7d32; }
.lv-dot { width: 9px; height: 9px; border-radius: 50%; flex: none; }
.dot-GREEN { background: #22b573; }
.dot-YELLOW { background: #f5a623; }
.dot-RED { background: #e74c3c; }
.t-label { width: 110px; color: #23422a; font-size: 13.5px; font-weight: 600; flex: none; }
.t-bar { flex: 1; height: 7px; background: #eef4ef; border-radius: 4px; overflow: hidden; }
.t-bar-in { display: block; height: 100%; border-radius: 4px; transition: width .6s ease; }
.bar-GREEN { background: linear-gradient(90deg, #3dd68c, #1fa45b); }
.bar-YELLOW { background: linear-gradient(90deg, #ffd54f, #f5a623); }
.bar-RED { background: linear-gradient(90deg, #ff8a65, #e74c3c); }
.t-score { width: 46px; text-align: right; font-size: 12.5px; color: #6d7f70; font-variant-numeric: tabular-nums; flex: none; }
.t-arrow { color: #9aab9d; font-size: 12px; transition: transform .2s; }
.t-arrow.open { transform: rotate(180deg); }
.t-evidence { padding: 2px 4px 12px 19px; animation: evIn .25s ease; }
.ev-detail { color: #57705d; font-size: 13px; margin-bottom: 6px; }
.ev-line { color: #8a9a8e; font-size: 12.5px; line-height: 1.8; }
@keyframes evIn { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: none; } }
.trust-note { margin-top: 12px; font-size: 12px; color: #9aab9d; line-height: 1.6; }
/* AI 体检报告 */
.hr-empty { text-align: center; padding: 14px 0 6px; }
.hr-desc { color: #6d7f70; font-size: 13.5px; margin-bottom: 14px; }
.hr-meta { font-size: 12px; color: #9aab9d; margin-bottom: 10px; }
.hr-body {
  white-space: pre-wrap; line-height: 1.9; color: #33493a; font-size: 14px;
  background: #f7faf7; border: 1px solid #e4efe5; border-radius: 10px; padding: 14px 16px;
}
/* 信任关系条 */
.trust-rel {
  display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap;
  background: #fff; border-radius: 12px; padding: 14px 18px; margin-bottom: 16px;
  box-shadow: 0 2px 10px rgba(31, 66, 39, .07);
}
.rel-left { display: flex; align-items: center; gap: 12px; }
.rel-avatar {
  width: 42px; height: 42px; border-radius: 50%; flex: none;
  background: linear-gradient(135deg, #3dd68c, #157a45); color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 700;
}
.rel-name { font-size: 15px; font-weight: 700; color: #1b3a22; display: flex; align-items: center; gap: 8px; }
.rel-meta { font-size: 12.5px; color: #7d8f81; margin-top: 4px; }
.rel-right { display: flex; align-items: center; gap: 12px; }
.rel-stat { font-size: 12.5px; color: #6d7f70; }
.rel-stat b { color: #1b7a45; font-size: 15px; }

/* 决策翻译 */
.decision-card {
  border: 1px solid #dfeee3;
  background: linear-gradient(180deg, #f8fdf9 0%, #fff 40%);
}
.dc-head { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 14px; }
.dc-advice { color: #33513c; font-size: 14.5px; font-weight: 600; }
.dc-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.dc-col { background: #f7fbf8; border-radius: 10px; padding: 12px 14px; }
.dc-title { font-size: 13px; font-weight: 700; color: #2a6b41; margin-bottom: 8px; }
.dc-line { font-size: 13px; color: #415c48; line-height: 1.85; }
.dc-line.warn { color: #b4652a; }
.dc-line.muted { color: #9aab9d; }
.dc-actions { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 16px; }
@media (max-width: 768px) {
  .dc-grid { grid-template-columns: 1fr; }
}

/* 承诺兑现 */
.pm-list { display: flex; flex-direction: column; gap: 10px; }
.pm-item { display: flex; gap: 12px; align-items: flex-start; padding: 10px 12px; background: #f8fbf8; border-radius: 10px; }
.pm-body { flex: 1; }
.pm-type { font-size: 13.5px; font-weight: 600; color: #274a33; }
.pm-remark { font-size: 12.5px; color: #7d8f81; margin-top: 4px; line-height: 1.7; }
.pm-note { margin-top: 12px; font-size: 12px; color: #9aab9d; line-height: 1.6; }

@media (max-width: 768px) {
  .hero-crop { font-size: 24px; }
  .t-links { gap: 12px; }
}
</style>
