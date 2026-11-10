<template>
  <div class="page">
    <div class="page-head"><div class="page-title">数据大屏</div></div>

    <div class="stat-grid">
      <StatCard :value="d.batchTotal" label="种植批次" icon="🌾" />
      <StatCard :value="d.farmerCount" label="农户账号" icon="🧑‍🌾" color="linear-gradient(135deg, #1e88e5, #1565c0)" />
      <StatCard :value="d.dealerCount" label="经销商账号" icon="🚚" color="linear-gradient(135deg, #fb8c00, #ef6c00)" />
      <StatCard :value="d.pendingCount" label="待处理投诉" icon="📨" color="linear-gradient(135deg, #e53935, #c62828)" />
      <StatCard :value="d.complaintTotal" label="投诉总数" icon="📬" color="linear-gradient(135deg, #8e24aa, #6a1b9a)" />
      <StatCard :value="d.scanToday" label="今日扫码" icon="📱" color="linear-gradient(135deg, #43a047, #1b5e20)" />
    </div>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>🥬 各作物产量分布（斤）</template>
          <ChartBox :option="cropPie" height="300px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>📨 投诉状态分布</template>
          <ChartBox :option="complaintPie" height="300px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12" style="margin-top: 16px">
        <el-card shadow="never">
          <template #header>📈 近 7 天流转量</template>
          <ChartBox :option="traceLine" height="300px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12" style="margin-top: 16px">
        <el-card shadow="never">
          <template #header>📱 近 7 天扫码量</template>
          <ChartBox :option="scanLine" height="300px" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import http from '../../api/http'
import StatCard from '../../components/StatCard.vue'
import ChartBox from '../../components/ChartBox.vue'
import { complaintStatusMap, type DictItem } from '../../utils'
import type { AdminDashboard, CountRow, StatRow } from '../../types'

const d = ref<AdminDashboard>({
  batchTotal: 0,
  farmerCount: 0,
  dealerCount: 0,
  pendingCount: 0,
  complaintTotal: 0,
  scanToday: 0,
  cropStats: [],
  complaintStatus: [],
  trace7days: [],
  scan7days: []
})

const toPieData = (rows: StatRow[] | undefined, map?: Record<string, DictItem>) => (rows || []).map(r => ({
  name: map?.[r.status || '']?.text || r.crop_name || r.status,
  value: Number(r.cnt || r.total || 0)
}))

const cropPie = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 斤 ({d}%)' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie', radius: ['38%', '66%'], center: ['50%', '45%'],
    itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
    data: toPieData(d.value.cropStats)
  }]
}))

const complaintPie = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie', radius: ['38%', '66%'], center: ['50%', '45%'],
    itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
    data: toPieData(d.value.complaintStatus, complaintStatusMap)
  }]
}))

const mkLine = (rows: CountRow[] | undefined, name: string): EChartsOption => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 30, bottom: 30 },
  xAxis: { type: 'category', data: (rows || []).map(r => String(r.day).slice(5)) },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    name, type: 'line', smooth: true, symbolSize: 7,
    areaStyle: { opacity: 0.12 },
    lineStyle: { width: 3, color: '#2e7d32' },
    itemStyle: { color: '#2e7d32' },
    data: (rows || []).map(r => Number(r.cnt))
  }]
})

const traceLine = computed<EChartsOption>(() => mkLine(d.value.trace7days, '流转量'))
const scanLine = computed<EChartsOption>(() => mkLine(d.value.scan7days, '扫码量'))

onMounted(async () => {
  d.value = await http.get<AdminDashboard>('/api/admin/dashboard')
})
</script>

<style scoped>
.stat-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 14px; }
@media (max-width: 1200px) { .stat-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) { .stat-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
