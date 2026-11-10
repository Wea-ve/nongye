<template>
  <div class="page">
    <div class="page-head"><div class="page-title">经销商工作台</div></div>

    <div class="stat-grid">
      <StatCard :value="d.availableCount" label="可入库批次" icon="📥" />
      <StatCard :value="d.inboundCount" label="累计入库" icon="📦" color="linear-gradient(135deg, #1e88e5, #1565c0)" />
      <StatCard :value="d.outboundCount" label="累计出库" icon="📤" color="linear-gradient(135deg, #fb8c00, #ef6c00)" />
      <StatCard :value="d.involvedCount" label="经手批次" icon="🚚" color="linear-gradient(135deg, #8e24aa, #6a1b9a)" />
    </div>

    <el-alert v-if="(d.staleOutbound || 0) > 0" type="warning" show-icon :closable="false"
              style="margin-top: 16px"
              :title="`有 ${d.staleOutbound} 个批次入库超过 48 小时仍未出库，请尽快安排发货，避免影响新鲜度与农户承诺`" />
    <el-alert v-else type="success" show-icon :closable="false" style="margin-top: 16px"
              title="周转正常：无超 48 小时未出库的批次" />

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>📌 平台约束（出库前请确认）</template>
      <div class="rule-list">
        <div class="rule-item">1. 只有已完成入库的批次才能出库；</div>
        <div class="rule-item">2. 出库数量不得超过该批次在本店的剩余库存；</div>
        <div class="rule-item">3. 出库后批次状态将置为已售，不可重复出库；</div>
        <div class="rule-item">4. 入库超过 48 小时未出库将计入周转异常，影响店铺可信评分。</div>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>🥬 待入库批次（已采收）</span>
          <el-button size="small" type="primary" @click="$router.push('/dealer/inbound')">去入库</el-button>
        </div>
      </template>
      <el-table :data="d.available">
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column prop="cropName" label="作物" width="90" />
        <el-table-column prop="variety" label="品种" width="100" />
        <el-table-column prop="farmerName" label="农户" width="100" />
        <el-table-column label="采收日期" width="110">
          <template #default="{ row }">{{ fmtDate(row.actualHarvestDate) }}</template>
        </el-table-column>
        <el-table-column prop="harvestWeight" label="重量(斤)" width="100" />
      </el-table>
      <el-empty v-if="!d.available?.length" description="暂无可入库批次" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '../../api/http'
import StatCard from '../../components/StatCard.vue'
import { fmtDate } from '../../utils'
import type { DealerDashboard } from '../../types'

const d = ref<DealerDashboard>({
  availableCount: 0,
  inboundCount: 0,
  outboundCount: 0,
  involvedCount: 0,
  available: []
})

onMounted(async () => {
  d.value = await http.get<DealerDashboard>('/api/dealer/dashboard')
})
</script>

<style scoped>
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.rule-list { display: flex; flex-direction: column; gap: 8px; }
.rule-item { font-size: 13px; color: #4b5f51; line-height: 1.7; }
@media (max-width: 768px) { .stat-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
