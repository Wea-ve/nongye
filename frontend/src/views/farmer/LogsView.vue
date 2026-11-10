<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">农事记录（全部批次）</div>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="batchId" placeholder="按批次筛选" clearable style="width: 300px" @change="filter">
          <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo} · ${b.cropName}`" :value="b.id" />
        </el-select>
        <el-button @click="$router.push('/farmer/batch')">前往批次管理</el-button>
      </div>

      <el-table :data="filteredList" v-loading="loading">
        <el-table-column prop="logDate" label="日期" width="110" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ logTypeMap[row.logType]?.text || row.logType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column prop="cropName" label="作物" width="90" />
        <el-table-column prop="material" label="物料" width="120" />
        <el-table-column prop="dosage" label="用量" width="110" />
        <el-table-column prop="content" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary"
                       @click="$router.push(`/farmer/batch/${row.batchId}/logs`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import http from '../../api/http'
import { logTypeMap } from '../../utils'
import type { CropBatch, FarmerLogsData, FarmingLog } from '../../types'

const logs = ref<FarmingLog[]>([])
const batches = ref<CropBatch[]>([])
const batchId = ref<number | null>(null)
const loading = ref<boolean>(false)

const filteredList = computed<FarmingLog[]>(() =>
  batchId.value ? logs.value.filter(l => l.batchId === batchId.value) : logs.value
)

const filter = (): void => { /* computed 自动过滤 */ }

onMounted(async () => {
  loading.value = true
  try {
    const d = await http.get<FarmerLogsData>('/api/farmer/logs')
    logs.value = d.logs
    batches.value = d.batches
  } finally { loading.value = false }
})
</script>
