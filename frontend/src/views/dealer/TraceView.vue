<template>
  <div class="page">
    <div class="page-head"><div class="page-title">溯源查询</div></div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="traceCode" placeholder="输入溯源码" clearable style="width: 280px" @keyup.enter="query">
          <template #prefix>🔍</template>
        </el-input>
        <el-button type="primary" @click="query">查询</el-button>
        <el-button v-if="d.batch" @click="openPublic">打开消费者页面</el-button>
      </div>

      <template v-if="d.batch">
        <el-descriptions :column="3" border size="large" style="margin-bottom: 18px">
          <el-descriptions-item label="批次号">{{ d.batch.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="作物">{{ d.batch.cropName }}</el-descriptions-item>
          <el-descriptions-item label="品种">{{ d.batch.variety || '-' }}</el-descriptions-item>
          <el-descriptions-item label="农户">{{ d.batch.farmerName }}</el-descriptions-item>
          <el-descriptions-item label="采收日期">{{ fmtDate(d.batch.actualHarvestDate) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="batchStatusMap[d.batch.status]?.type" size="small">{{ batchStatusMap[d.batch.status]?.text }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-card shadow="never">
              <template #header>📓 农事记录</template>
              <el-timeline v-if="d.logs?.length">
                <el-timeline-item v-for="l in d.logs" :key="l.id" :timestamp="fmtDate(l.logDate)">
                  <b>{{ logTypeMap[l.logType]?.text || l.logType }}</b>
                  <span v-if="l.material" style="margin-left: 8px; color: #666">{{ l.material }} {{ l.dosage || '' }}</span>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="无农事记录" :image-size="60" />
            </el-card>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-card shadow="never">
              <template #header>🔄 流转链路</template>
              <el-timeline v-if="d.records?.length">
                <el-timeline-item v-for="r in d.records" :key="r.id" :timestamp="fmtTime(r.operateTime)">
                  <b>{{ traceTypeMap[r.type]?.text || r.type }}</b>
                  <span style="margin-left: 8px; color: #666">{{ r.fromUserName }} → {{ r.toUserName || '-' }}</span>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="无流转记录" :image-size="60" />
            </el-card>
          </el-col>
        </el-row>
      </template>
      <el-empty v-else-if="searched" description="未找到该批次" />
      <el-empty v-else description="输入溯源码查询批次全链路信息" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import http from '../../api/http'
import { batchStatusMap, fmtDate, fmtTime, logTypeMap, traceTypeMap } from '../../utils'
import type { DealerTraceData } from '../../types'

const traceCode = ref<string>('')
const searched = ref<boolean>(false)
const d = ref<DealerTraceData>({})

async function query(): Promise<void> {
  const code = traceCode.value.trim()
  if (!code) return
  d.value = await http.get<DealerTraceData>('/api/dealer/trace', { params: { traceCode: code } })
  searched.value = true
}

const openPublic = (): Window | null => window.open(`/app/trace/${d.value.batch?.traceCode}`, '_blank')
</script>
