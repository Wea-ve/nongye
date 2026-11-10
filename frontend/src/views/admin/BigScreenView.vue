<template>
  <div class="bigscreen">
    <!-- 顶部标题栏 -->
    <header class="bs-header">
      <div class="hd-side left">
        <span class="hd-chip">AI WEB 赛道 · 可信溯源决策平台</span>
      </div>
      <div class="hd-title">
        <i class="hd-line"></i>
        <h1><img class="hd-logo" src="../../assets/logo-icon.svg" alt="安溯·知源" />安溯·知源 · 全链路数据大屏</h1>
        <i class="hd-line"></i>
      </div>
      <div class="hd-side right">
        <span class="hd-clock">{{ clock }}</span>
        <button class="hd-btn" @click="toggleFull">{{ isFull ? '退出全屏' : '全屏' }}</button>
        <button class="hd-btn" @click="router.push('/admin/dashboard')">返回控制台</button>
      </div>
    </header>

    <!-- KPI 数字翻牌 -->
    <section class="kpi-row">
      <div v-for="k in kpis" :key="k.label" class="kpi-card">
        <div class="kpi-icon">{{ k.icon }}</div>
        <div class="kpi-num" :style="{ color: k.color }">{{ show[k.key] ?? 0 }}</div>
        <div class="kpi-label">{{ k.label }}</div>
      </div>
    </section>

    <!-- 图表阵列 -->
    <section class="chart-grid">
      <div class="panel">
        <div class="panel-title">📱 近 7 天扫码量趋势</div>
        <div class="panel-body"><ChartBox :option="scanLine" height="100%" /></div>
      </div>
      <div class="panel">
        <div class="panel-title">🥬 作物产量分布（斤）</div>
        <div class="panel-body"><ChartBox :option="cropRose" height="100%" /></div>
      </div>
      <div class="panel">
        <div class="panel-title">🧪 检测合格率</div>
        <div class="panel-body"><ChartBox :option="gauge" height="100%" /></div>
      </div>
      <div class="panel">
        <div class="panel-title">🔥 热门批次 TOP5（扫码次数）</div>
        <div class="panel-body"><ChartBox :option="hotBar" height="100%" /></div>
      </div>
      <div class="panel">
        <div class="panel-title">🌾 批次状态 &amp; 投诉工单</div>
        <div class="panel-body"><ChartBox :option="statusRing" height="100%" /></div>
      </div>
      <div class="panel">
        <div class="panel-title">⚡ 实时扫码动态</div>
        <div class="panel-body scan-feed">
          <transition-group name="feed" tag="div" class="feed-list">
            <div v-for="(s, i) in recentScans" :key="s.id ?? i" class="feed-item">
              <span class="feed-dot"></span>
              <span class="feed-crop">{{ s.cropName || '未知作物' }}</span>
              <span class="feed-batch">{{ s.batchNo }}</span>
              <span class="feed-time">{{ fmtTime(s.scanTime) }}</span>
            </div>
          </transition-group>
          <div v-if="!recentScans.length" class="feed-empty">暂无扫码记录，扫码后实时刷新</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import http from '../../api/http'
import ChartBox from '../../components/ChartBox.vue'
import { complaintStatusMap } from '../../utils'
import type { BigScreenData, CountRow, ScanLog, StatRow } from '../../types'

const router = useRouter()

/* ============ 数据加载（30s 自动刷新） ============ */
const d = ref<BigScreenData>({
  batchTotal: 0,
  farmerCount: 0,
  dealerCount: 0,
  scanToday: 0,
  scanTotal: 0,
  pendingCount: 0,
  traceTotal: 0,
  reportTotal: 0,
  qualifiedRate: 100,
  batchStatus: {},
  scan7days: [],
  trace7days: [],
  cropStats: [],
  complaintStatus: [],
  hotBatches: [],
  recentScans: []
})
const recentScans = computed<ScanLog[]>(() => d.value.recentScans || [])

async function load(): Promise<void> {
  try {
    d.value = await http.get<BigScreenData>('/api/admin/bigscreen')
  } catch { /* 拦截器已提示 */ }
}
let refreshTimer: number | undefined

/* ============ 实时时钟 ============ */
const clock = ref<string>('')
let clockTimer: number | undefined
function tick(): void {
  const t = new Date()
  const p = (n: number): string => String(n).padStart(2, '0')
  clock.value = `${t.getFullYear()}-${p(t.getMonth() + 1)}-${p(t.getDate())} ${p(t.getHours())}:${p(t.getMinutes())}:${p(t.getSeconds())}`
}

/* ============ 全屏 ============ */
const isFull = ref<boolean>(false)
function toggleFull(): void {
  if (document.fullscreenElement) document.exitFullscreen()
  else document.documentElement.requestFullscreen?.()
}
function onFullChange(): void { isFull.value = !!document.fullscreenElement }

/* ============ KPI 数字滚动动画 ============ */
type KpiKey = 'batchTotal' | 'farmerCount' | 'dealerCount' | 'scanToday' | 'scanTotal' | 'pendingCount'
interface KpiDef {
  key: KpiKey
  label: string
  icon: string
  color: string
}
const KPI_DEF: KpiDef[] = [
  { key: 'batchTotal', label: '种植批次', icon: '🌾', color: '#7cffb2' },
  { key: 'farmerCount', label: '入驻农户', icon: '🧑‍🌾', color: '#38bdf8' },
  { key: 'dealerCount', label: '经销商家', icon: '🚚', color: '#ffc53d' },
  { key: 'scanToday', label: '今日扫码', icon: '📱', color: '#00e5c3' },
  { key: 'scanTotal', label: '累计扫码', icon: '📊', color: '#00e5c3' },
  { key: 'pendingCount', label: '待处理投诉', icon: '📨', color: '#ff6b6b' }
]
const show = reactive<Record<string, number>>(Object.fromEntries(KPI_DEF.map(k => [k.key, 0])))
const anims: Record<string, number> = {}
function animateTo(key: KpiKey, target: number | undefined): void {
  const to = Number(target) || 0
  cancelAnimationFrame(anims[key])
  const from = Number(show[key]) || 0
  if (from === to) { show[key] = to; return }
  const start = performance.now()
  const dur = 900
  const step = (now: number): void => {
    const p = Math.min((now - start) / dur, 1)
    const eased = 1 - Math.pow(1 - p, 3)
    show[key] = Math.round(from + (to - from) * eased)
    if (p < 1) anims[key] = requestAnimationFrame(step)
  }
  anims[key] = requestAnimationFrame(step)
}
watch(() => d.value, (v) => KPI_DEF.forEach(k => animateTo(k.key, v?.[k.key])), { deep: false })

const kpis = computed(() => KPI_DEF.map(k => ({ ...k, value: d.value?.[k.key] })))

/* ============ 图表公共样式 ============ */
const AXIS = { axisLine: { lineStyle: { color: 'rgba(126,164,191,.35)' } }, axisLabel: { color: '#7ea4bf' }, axisTick: { show: false } }
const SPLIT = { splitLine: { lineStyle: { color: 'rgba(126,164,191,.12)' } } }

/* 近 7 天扫码量（面积折线） */
const scanLine = computed<EChartsOption>(() => {
  const rows = d.value.scan7days || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 24, top: 26, bottom: 30 },
    xAxis: { type: 'category', boundaryGap: false, data: rows.map(r => String(r.day).slice(5)), ...AXIS },
    yAxis: { type: 'value', minInterval: 1, ...AXIS, ...SPLIT },
    series: [{
      name: '扫码量', type: 'line', smooth: true, symbol: 'circle', symbolSize: 8,
      data: rows.map(r => Number(r.cnt)),
      lineStyle: { width: 3, color: '#00e5c3', shadowColor: 'rgba(0,229,195,.6)', shadowBlur: 12 },
      itemStyle: { color: '#00e5c3', borderColor: '#fff', borderWidth: 1 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(0,229,195,.35)' }, { offset: 1, color: 'rgba(0,229,195,0)' }]
        }
      }
    }]
  }
})

/* 作物产量玫瑰图 */
const cropRose = computed<EChartsOption>(() => {
  const rows = (d.value.cropStats || []).map(r => ({ name: r.crop_name, value: Number(r.total || 0) }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} 斤 ({d}%)' },
    legend: { bottom: 4, textStyle: { color: '#7ea4bf' }, itemWidth: 12, itemHeight: 8 },
    series: [{
      type: 'pie', roseType: 'radius', radius: ['18%', '68%'], center: ['50%', '44%'],
      itemStyle: { borderRadius: 6, borderColor: 'rgba(5,11,20,.8)', borderWidth: 2 },
      label: { color: '#a8c6da', fontSize: 12 },
      data: rows.length ? rows : [{ name: '暂无数据', value: 0, itemStyle: { color: 'rgba(126,164,191,.2)' } }],
      color: ['#00e5c3', '#38bdf8', '#ffc53d', '#7cffb2', '#b37feb', '#ff9f6b', '#5b8ff9']
    }]
  }
})

/* 检测合格率仪表盘 */
const gauge = computed<EChartsOption>(() => {
  const rate = Number(d.value.qualifiedRate ?? 100)
  return {
    series: [{
      type: 'gauge', startAngle: 210, endAngle: -30, min: 0, max: 100,
      radius: '92%', center: ['50%', '58%'],
      progress: { show: true, width: 14, roundCap: true, itemStyle: { color: '#7cffb2' } },
      axisLine: { lineStyle: { width: 14, color: [[1, 'rgba(126,164,191,.15)']] } },
      pointer: { show: false }, axisTick: { show: false }, splitLine: { show: false },
      axisLabel: { distance: 20, color: '#7ea4bf', fontSize: 11 },
      detail: {
        valueAnimation: true, offsetCenter: [0, '-5%'],
        formatter: '{value}%', color: '#7cffb2', fontSize: 30, fontWeight: 700
      },
      title: { offsetCenter: [0, '28%'], color: '#7ea4bf', fontSize: 12 },
      data: [{ value: rate, name: `共 ${d.value.reportTotal ?? 0} 份检测报告` }]
    }]
  }
})

/* 热门批次 TOP5 横向条形 */
const hotBar = computed<EChartsOption>(() => {
  const rows = (d.value.hotBatches || []).slice(0, 5)
  const sorted = [...rows].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 120, right: 40, top: 16, bottom: 26 },
    xAxis: { type: 'value', minInterval: 1, ...AXIS, ...SPLIT },
    yAxis: {
      type: 'category',
      data: sorted.map(r => `${r.batch_no || ''} ${r.crop_name || ''}`),
      ...AXIS, axisLabel: { color: '#a8c6da', fontSize: 11, width: 110, overflow: 'truncate' }
    },
    series: [{
      type: 'bar', barWidth: 12, roundCap: true,
      data: sorted.map(r => Number(r.cnt)),
      itemStyle: {
        borderRadius: 6,
        color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#00e5c3' }, { offset: 1, color: '#38bdf8' }] }
      },
      label: { show: true, position: 'right', color: '#00e5c3', fontSize: 12 }
    }]
  }
})

/* 批次状态 + 投诉状态 双环图 */
const batchStatusMap: Record<string, string> = { PLANTING: '种植中', HARVESTED: '已采收', SOLD: '已售罄' }
const statusRing = computed<EChartsOption>(() => {
  const batchRows = Object.entries(d.value.batchStatus || {})
    .map(([k, v]) => ({ name: batchStatusMap[k] || k, value: Number(v) }))
  const complaintRows = (d.value.complaintStatus || [])
    .map(r => ({ name: complaintStatusMap[r.status || '']?.text || r.status, value: Number(r.cnt) }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 4, textStyle: { color: '#7ea4bf' }, itemWidth: 12, itemHeight: 8 },
    series: [
      {
        name: '批次状态', type: 'pie', radius: ['52%', '72%'], center: ['28%', '44%'],
        itemStyle: { borderRadius: 6, borderColor: 'rgba(5,11,20,.8)', borderWidth: 2 },
        label: { show: false },
        data: batchRows.length ? batchRows : [{ name: '暂无', value: 0 }],
        color: ['#7cffb2', '#38bdf8', '#ffc53d']
      },
      {
        name: '投诉状态', type: 'pie', radius: ['52%', '72%'], center: ['74%', '44%'],
        itemStyle: { borderRadius: 6, borderColor: 'rgba(5,11,20,.8)', borderWidth: 2 },
        label: { show: false },
        data: complaintRows.length ? complaintRows : [{ name: '暂无', value: 0 }],
        color: ['#ff6b6b', '#ffc53d', '#7cffb2']
      }
    ],
    graphic: [
      { type: 'text', left: '28%', top: '82%', style: { text: '批次状态', fill: '#a8c6da', fontSize: 13, textAlign: 'center' } },
      { type: 'text', left: '74%', top: '82%', style: { text: '投诉工单', fill: '#a8c6da', fontSize: 13, textAlign: 'center' } }
    ]
  }
})

/* ============ 工具 ============ */
function fmtTime(t: string | null | undefined): string {
  if (!t) return ''
  const s = String(t).replace('T', ' ')
  return s.length >= 16 ? s.slice(11, 16) : s
}
function onKey(e: KeyboardEvent): void { if (e.key === 'Escape' && !document.fullscreenElement) router.push('/admin/dashboard') }

onMounted(() => {
  tick()
  clockTimer = setInterval(tick, 1000)
  load()
  refreshTimer = setInterval(load, 30000)
  document.addEventListener('fullscreenchange', onFullChange)
  window.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => {
  clearInterval(clockTimer)
  clearInterval(refreshTimer)
  document.removeEventListener('fullscreenchange', onFullChange)
  window.removeEventListener('keydown', onKey)
  Object.values(anims).forEach(id => cancelAnimationFrame(id))
})
</script>

<style scoped>
.bigscreen {
  height: 100vh; display: flex; flex-direction: column; overflow: hidden;
  background:
    radial-gradient(1200px 500px at 15% -10%, rgba(0, 229, 195, .12), transparent 60%),
    radial-gradient(1000px 500px at 85% 110%, rgba(56, 189, 248, .10), transparent 60%),
    linear-gradient(160deg, #050b14 0%, #081524 55%, #0a1a2b 100%);
  color: #cfe6f2;
  padding: 12px 16px 14px; gap: 12px;
}

/* ===== 顶部标题栏 ===== */
.bs-header { display: flex; align-items: center; height: 46px; flex-shrink: 0; }
.hd-side { flex: 1; display: flex; align-items: center; gap: 10px; }
.hd-side.right { justify-content: flex-end; }
.hd-chip {
  font-size: 12px; color: #5f8aa6; border: 1px solid rgba(0, 229, 195, .25);
  border-radius: 20px; padding: 3px 12px; letter-spacing: 1px;
  background: rgba(0, 229, 195, .06);
}
.hd-title { display: flex; align-items: center; gap: 18px; }
.hd-logo { width: 34px; height: 34px; vertical-align: -8px; margin-right: 10px; filter: drop-shadow(0 0 8px rgba(61, 214, 140, .45)); }
.hd-title h1 {
  margin: 0; font-size: 24px; letter-spacing: 4px; white-space: nowrap;
  background: linear-gradient(90deg, #7cffb2, #00e5c3 45%, #38bdf8);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  text-shadow: 0 0 26px rgba(0, 229, 195, .25);
}
.hd-line { width: 180px; height: 2px; background: linear-gradient(90deg, transparent, rgba(0, 229, 195, .6)); }
.hd-line + h1 + .hd-line { transform: scaleX(-1); }
.hd-clock { font-size: 13px; color: #7ea4bf; font-variant-numeric: tabular-nums; }
.hd-btn {
  cursor: pointer; font-size: 12.5px; color: #9fd8c8;
  background: rgba(0, 229, 195, .08); border: 1px solid rgba(0, 229, 195, .3);
  border-radius: 6px; padding: 5px 12px; transition: all .18s;
}
.hd-btn:hover { background: rgba(0, 229, 195, .18); color: #fff; }

/* ===== KPI 数字翻牌 ===== */
.kpi-row { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; flex-shrink: 0; }
.kpi-card {
  position: relative; display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 10px 6px 8px; border-radius: 12px;
  background: linear-gradient(180deg, rgba(0, 229, 195, .07), rgba(10, 30, 50, .5));
  border: 1px solid rgba(0, 229, 195, .16);
  box-shadow: inset 0 0 24px rgba(0, 229, 195, .04);
}
.kpi-icon { position: absolute; left: 12px; top: 8px; font-size: 18px; opacity: .85; }
.kpi-num { font-size: 30px; font-weight: 800; font-variant-numeric: tabular-nums; line-height: 1.15; text-shadow: 0 0 18px currentColor; }
.kpi-label { font-size: 12.5px; color: #7ea4bf; letter-spacing: 2px; }

/* ===== 图表阵列 ===== */
.chart-grid {
  flex: 1; min-height: 0;
  display: grid; grid-template-columns: repeat(3, 1fr); grid-template-rows: 1fr 1fr; gap: 12px;
}
.panel {
  position: relative; display: flex; flex-direction: column; min-height: 0;
  background: rgba(10, 30, 50, .5);
  border: 1px solid rgba(0, 229, 195, .16); border-radius: 12px;
  backdrop-filter: blur(4px);
}
.panel::before, .panel::after {
  content: ''; position: absolute; width: 14px; height: 14px; pointer-events: none;
}
.panel::before {
  left: -1px; top: -1px; border-left: 2px solid #00e5c3; border-top: 2px solid #00e5c3;
  border-top-left-radius: 12px;
}
.panel::after {
  right: -1px; bottom: -1px; border-right: 2px solid #00e5c3; border-bottom: 2px solid #00e5c3;
  border-bottom-right-radius: 12px;
}
.panel-title {
  flex-shrink: 0; padding: 10px 14px 6px; font-size: 14px; font-weight: 600; color: #bfe9dc;
  letter-spacing: 1px;
}
.panel-title::before {
  content: ''; display: inline-block; width: 4px; height: 13px; margin-right: 8px;
  border-radius: 2px; vertical-align: -1px;
  background: linear-gradient(180deg, #00e5c3, #38bdf8);
}
.panel-body { flex: 1; min-height: 0; padding: 2px 8px 8px; }

/* ===== 实时扫码动态 ===== */
.scan-feed { overflow: hidden; }
.feed-list { display: flex; flex-direction: column; gap: 6px; padding: 4px 6px; }
.feed-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 12px; border-radius: 8px; font-size: 12.5px;
  background: rgba(0, 229, 195, .05); border: 1px solid rgba(0, 229, 195, .1);
}
.feed-dot { width: 7px; height: 7px; border-radius: 50%; background: #00e5c3; box-shadow: 0 0 8px #00e5c3; flex-shrink: 0; }
.feed-crop { color: #e6f7f1; font-weight: 600; flex-shrink: 0; }
.feed-batch { color: #7ea4bf; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-family: Consolas, monospace; }
.feed-time { color: #5f8aa6; font-variant-numeric: tabular-nums; flex-shrink: 0; }
.feed-empty { padding: 24px; text-align: center; color: #5f8aa6; font-size: 13px; }
.feed-enter-active { transition: all .4s ease; }
.feed-enter-from { opacity: 0; transform: translateY(-12px); }
.feed-move { transition: transform .4s ease; }

/* 小屏适配（演示一般 1080p，简单兜底） */
@media (max-width: 1100px) {
  .bigscreen { overflow: auto; height: auto; min-height: 100vh; }
  .kpi-row { grid-template-columns: repeat(3, 1fr); }
  .chart-grid { grid-template-columns: 1fr; grid-template-rows: repeat(6, 300px); }
  .hd-line, .hd-chip { display: none; }
}
</style>
