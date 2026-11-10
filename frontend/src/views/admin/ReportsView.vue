<template>
  <div class="page">
    <div class="page-head"><div class="page-title">检测报告监管</div></div>

    <div class="stat-grid" style="margin-bottom: 16px">
      <StatCard :value="d.qualifiedCount" label="合格报告" icon="✅" />
      <StatCard :value="d.unqualifiedCount" label="不合格报告" icon="❌" color="linear-gradient(135deg, #e53935, #c62828)" />
    </div>

    <el-card shadow="never">
      <el-table :data="d.list" v-loading="loading">
        <el-table-column prop="reportNo" label="报告编号" min-width="150" />
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column prop="cropName" label="作物" width="90" />
        <el-table-column prop="farmerName" label="农户" width="100" />
        <el-table-column prop="institution" label="检测机构" min-width="140" show-overflow-tooltip />
        <el-table-column label="检测日期" width="110">
          <template #default="{ row }">{{ fmtDate(row.reportDate) }}</template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.result === '合格' ? 'success' : 'danger'" size="small">{{ row.result }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="附件" width="100" fixed="right">
          <template #default="{ row }">
            <el-link v-if="row.reportUrl" type="primary" :href="row.reportUrl" target="_blank">查看</el-link>
            <span v-else style="color: #999">无</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '../../api/http'
import StatCard from '../../components/StatCard.vue'
import { fmtDate } from '../../utils'
import type { AdminReportsData } from '../../types'

const d = ref<AdminReportsData>({ list: [], qualifiedCount: 0, unqualifiedCount: 0 })
const loading = ref<boolean>(false)

onMounted(async () => {
  loading.value = true
  try { d.value = await http.get<AdminReportsData>('/api/admin/reports') } finally { loading.value = false }
})
</script>

<style scoped>
.stat-grid { display: grid; grid-template-columns: repeat(2, 240px); gap: 14px; }
@media (max-width: 768px) { .stat-grid { grid-template-columns: 1fr; } }
</style>
