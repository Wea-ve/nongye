<template>
  <div class="page">
    <div class="page-head"><div class="page-title">流转记录（全平台）</div></div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索批次号 / 作物名" clearable style="width: 240px"
                  @keyup.enter="load" @clear="load">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button @click="load">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading">
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :color="traceTypeMap[row.type]?.color" style="border: none">
              {{ traceTypeMap[row.type]?.text || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column prop="cropName" label="作物" width="90" />
        <el-table-column prop="quantity" label="数量(斤)" width="100" />
        <el-table-column prop="fromUserName" label="流转方" width="110" />
        <el-table-column prop="toUserName" label="接收方" width="110" />
        <el-table-column prop="location" label="地点" min-width="130" show-overflow-tooltip />
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.operateTime) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '../../api/http'
import { fmtTime, traceTypeMap } from '../../utils'
import type { TraceRecord } from '../../types'

const list = ref<TraceRecord[]>([])
const keyword = ref<string>('')
const loading = ref<boolean>(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    list.value = await http.get<TraceRecord[]>('/api/admin/trace-records', { params: { keyword: keyword.value || undefined } })
  } finally { loading.value = false }
}

onMounted(load)
</script>
