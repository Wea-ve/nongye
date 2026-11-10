<template>
  <div class="page">
    <div class="page-head"><div class="page-title">农事监管（全平台）</div></div>

    <el-card shadow="never">
      <el-table :data="logs" v-loading="loading">
        <el-table-column prop="logDate" label="日期" width="110" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ logTypeMap[row.logType]?.text || row.logType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column prop="cropName" label="作物" width="90" />
        <el-table-column prop="farmerName" label="农户" width="100" />
        <el-table-column prop="material" label="物料" width="120" />
        <el-table-column prop="dosage" label="用量" width="110" />
        <el-table-column prop="content" label="备注" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '../../api/http'
import { logTypeMap } from '../../utils'
import type { FarmingLog } from '../../types'

const logs = ref<FarmingLog[]>([])
const loading = ref<boolean>(false)

onMounted(async () => {
  loading.value = true
  try { logs.value = await http.get<FarmingLog[]>('/api/admin/farming-logs') } finally { loading.value = false }
})
</script>
