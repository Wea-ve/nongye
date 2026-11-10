<template>
  <div class="page">
    <div class="page-head"><div class="page-title">收货入库</div></div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="traceCode" placeholder="输入溯源码查询批次" clearable style="width: 280px"
                  @keyup.enter="query">
          <template #prefix>🔖</template>
        </el-input>
        <el-button type="primary" @click="query">查询</el-button>
      </div>

      <template v-if="batch">
        <el-descriptions :column="3" border size="large" style="margin-bottom: 18px">
          <el-descriptions-item label="批次号">{{ batch.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="作物">{{ batch.cropName }}</el-descriptions-item>
          <el-descriptions-item label="品种">{{ batch.variety || '-' }}</el-descriptions-item>
          <el-descriptions-item label="农户">{{ batch.farmerName }}</el-descriptions-item>
          <el-descriptions-item label="产地">{{ batch.landLocation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="batchStatusMap[batch.status]?.type" size="small">{{ batchStatusMap[batch.status]?.text }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <el-form inline>
          <el-form-item label="入库数量(斤)">
            <el-input-number v-model="quantity" :min="0.1" :precision="1" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="submit">确认入库</el-button>
          </el-form-item>
        </el-form>
      </template>
      <el-empty v-else-if="searched" description="未找到该批次，请核对溯源码" />
      <el-empty v-else description="输入溯源码查询要入库的批次" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { batchStatusMap } from '../../utils'
import type { CropBatch, DealerTraceData } from '../../types'

const traceCode = ref<string>('')
const batch = ref<CropBatch | null>(null)
const searched = ref<boolean>(false)
const quantity = ref<number | null>(null)
const saving = ref<boolean>(false)

async function query(): Promise<void> {
  const code = traceCode.value.trim()
  if (!code) {
    ElMessage.warning('请输入溯源码')
    return
  }
  const d = await http.get<DealerTraceData>('/api/dealer/trace', { params: { traceCode: code } })
  searched.value = true
  if (d.batch) {
    batch.value = d.batch
    quantity.value = d.batch.harvestWeight ? Number(d.batch.harvestWeight) : null
  } else {
    batch.value = null
  }
}

async function submit(): Promise<void> {
  if (!quantity.value) {
    ElMessage.warning('请填写入库数量')
    return
  }
  saving.value = true
  try {
    await http.post('/api/dealer/inbound', { batchId: batch.value!.id, quantity: quantity.value })
    ElMessage.success('入库成功，已写入流转记录')
    batch.value = null
    traceCode.value = ''
    searched.value = false
  } catch { /* 已提示 */ } finally { saving.value = false }
}
</script>
