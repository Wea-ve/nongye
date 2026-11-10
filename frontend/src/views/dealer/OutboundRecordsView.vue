<template>
  <div class="page">
    <div class="page-head"><div class="page-title">出库记录</div></div>
    <el-card shadow="never">
      <el-table :data="records" v-loading="loading">
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column prop="cropName" label="作物" width="90" />
        <el-table-column prop="quantity" label="数量(斤)" width="100" />
        <el-table-column prop="location" label="发货地点" min-width="140" show-overflow-tooltip />
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.operateTime) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!loading && !records.length" description="暂无出库记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '../../api/http'
import { fmtTime } from '../../utils'
import type { TraceRecord } from '../../types'

const records = ref<TraceRecord[]>([])
const loading = ref<boolean>(false)

onMounted(async () => {
  loading.value = true
  try { records.value = await http.get<TraceRecord[]>('/api/dealer/records/outbound') } finally { loading.value = false }
})
</script>
