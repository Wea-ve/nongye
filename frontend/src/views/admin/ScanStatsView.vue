<template>
  <div class="page">
    <div class="page-head"><div class="page-title">扫码统计</div></div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="10">
        <el-card shadow="never" style="margin-bottom: 16px">
          <template #header>📱 今日扫码量</template>
          <div style="text-align: center; padding: 16px 0">
            <div style="font-size: 52px; font-weight: 800; color: #2e7d32">{{ d.todayCount }}</div>
            <div style="color: #8a9a8e">较昨日 {{ trend > 0 ? '+' + trend : trend }} 次</div>
          </div>
        </el-card>
        <el-card shadow="never">
          <template #header>🔥 热门批次 TOP10</template>
          <el-table :data="d.hotBatches" size="small">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="crop_name" label="作物" width="100" />
            <el-table-column prop="batch_no" label="批次号" min-width="160" />
            <el-table-column prop="cnt" label="扫码次数" width="90" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="14">
        <el-card shadow="never" style="margin-bottom: 16px">
          <template #header>📈 近 7 天扫码量</template>
          <ChartBox :option="lineOption" height="260px" />
        </el-card>
        <el-card shadow="never">
          <template #header>📋 扫码明细（最近 200 条）</template>
          <el-table :data="d.scanLogs" size="small" max-height="360">
            <el-table-column prop="trace_code" label="溯源码" min-width="170" />
            <el-table-column prop="crop_name" label="作物" width="90" />
            <el-table-column prop="ip" label="IP" width="130" />
            <el-table-column label="时间" width="160">
              <template #default="{ row }">{{ fmtTime(row.scanTime) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import http from '../../api/http'
import ChartBox from '../../components/ChartBox.vue'
import { fmtTime } from '../../utils'
import type { ScanStatsData } from '../../types'

const d = ref<ScanStatsData>({ todayCount: 0, hotBatches: [], scanLogs: [], scan7days: [] })
const trend = computed<number>(() => {
  const rows = d.value.scan7days || []
  if (rows.length < 2) return 0
  return Number(rows[rows.length - 1].cnt) - Number(rows[rows.length - 2].cnt)
})

const lineOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 30, bottom: 30 },
  xAxis: { type: 'category', data: (d.value.scan7days || []).map(r => String(r.day).slice(5)) },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    name: '扫码量', type: 'bar', barWidth: 18,
    itemStyle: { borderRadius: [6, 6, 0, 0], color: '#43a047' },
    data: (d.value.scan7days || []).map(r => Number(r.cnt))
  }]
}))

onMounted(async () => {
  d.value = await http.get<ScanStatsData>('/api/admin/scan-stats')
})
</script>
