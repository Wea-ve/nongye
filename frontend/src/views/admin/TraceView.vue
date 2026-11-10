<template>
  <div class="page">
    <div class="page-head"><div class="page-title">溯源查询</div></div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="traceCode" placeholder="输入溯源码" clearable style="width: 280px" @keyup.enter="query">
          <template #prefix>🔍</template>
        </el-input>
        <el-button type="primary" @click="query">查询</el-button>
        <el-button v-if="batch" @click="openPublic">打开消费者页面</el-button>
      </div>

      <el-descriptions v-if="batch" :column="3" border size="large">
        <el-descriptions-item label="批次号">{{ batch.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="作物">{{ batch.cropName }}</el-descriptions-item>
        <el-descriptions-item label="品种">{{ batch.variety || '-' }}</el-descriptions-item>
        <el-descriptions-item label="农户">{{ batch.farmerName }}</el-descriptions-item>
        <el-descriptions-item label="产地">{{ batch.landLocation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="batchStatusMap[batch.status]?.type" size="small">{{ batchStatusMap[batch.status]?.text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="溯源码">{{ batch.traceCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="采收日期">{{ fmtDate(batch.actualHarvestDate) }}</el-descriptions-item>
        <el-descriptions-item label="采收重量">{{ batch.harvestWeight ? batch.harvestWeight + ' 斤' : '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else-if="searched" description="未找到该批次，请核对溯源码" />
      <el-empty v-else description="输入溯源码查询批次信息" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import http from '../../api/http'
import { batchStatusMap, fmtDate } from '../../utils'
import type { AdminTraceData, CropBatch } from '../../types'

const traceCode = ref<string>('')
const batch = computed<CropBatch | undefined>(() => result.value.batch ?? undefined)
const searched = ref<boolean>(false)
const result = ref<AdminTraceData>({})

async function query(): Promise<void> {
  const code = traceCode.value.trim()
  if (!code) return
  result.value = await http.get<AdminTraceData>('/api/admin/trace', { params: { traceCode: code } })
  searched.value = true
}

const openPublic = (): Window | null => window.open(`/app/trace/${batch.value!.traceCode}`, '_blank')
</script>
