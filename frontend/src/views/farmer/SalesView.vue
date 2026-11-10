<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">销售记录（流通出货）</div>
    </div>

    <el-card shadow="never">
      <el-table :data="records" v-loading="loading">
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column prop="cropName" label="作物" width="90" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ traceTypeMap[row.type]?.text || row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量(斤)" width="100" />
        <el-table-column prop="toUserName" label="接收方" width="120" />
        <el-table-column prop="location" label="地点" min-width="140" show-overflow-tooltip />
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.operateTime) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!loading && !records.length" description="暂无记录，经销商入库/出库您的批次后这里会显示" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '../../api/http'
import { fmtTime, traceTypeMap } from '../../utils'
import type { TraceRecord } from '../../types'

const records = ref<TraceRecord[]>([])
const loading = ref<boolean>(false)

onMounted(async () => {
  loading.value = true
  try {
    records.value = await http.get<TraceRecord[]>('/api/farmer/sales')
  } finally { loading.value = false }
})
</script>
