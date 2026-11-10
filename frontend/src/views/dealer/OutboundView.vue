<template>
  <div class="page">
    <div class="page-head"><div class="page-title">发货出库</div></div>

    <el-card shadow="never">
      <el-alert type="info" :closable="false" style="margin-bottom: 12px"
                title="仅已入库的批次可以出库；出库信息将写入溯源流转链路，消费者扫码可见" />
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column prop="cropName" label="作物" width="90" />
        <el-table-column prop="farmerName" label="农户" width="100" />
        <el-table-column prop="harvestWeight" label="采收重量(斤)" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="batchStatusMap[row.status]?.type" size="small">{{ batchStatusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入库状态" width="100">
          <template #default="{ row }">
            <el-tag :type="inboundIds.has(row.id) ? 'success' : 'info'" size="small">
              {{ inboundIds.has(row.id) ? '已入库' : '未入库' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :disabled="!inboundIds.has(row.id)" @click="openOut(row)">出库</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" title="发货出库" width="460px">
      <el-alert type="info" :closable="false" style="margin-bottom: 14px"
                :title="`批次：${row?.batchNo} · ${row?.cropName}`" />
      <el-form label-width="90px">
        <el-form-item label="出库数量" required>
          <el-input-number v-model="form.quantity" :min="0.1" :precision="1" style="width: 100%" />
          <span style="margin-left: 8px; color: #999">斤</span>
        </el-form-item>
        <el-form-item label="收货方" required><el-input v-model="form.toName" placeholder="如：惠民农贸市场" /></el-form-item>
        <el-form-item label="发货地点" required><el-input v-model="form.location" placeholder="如：物流园 3 号仓" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确认出库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { batchStatusMap } from '../../utils'
import type { CropBatch, OutboundData } from '../../types'

const list = ref<CropBatch[]>([])
const inboundIds = ref<Set<number>>(new Set())
const loading = ref<boolean>(false)
const saving = ref<boolean>(false)
const visible = ref<boolean>(false)
const row = ref<CropBatch | null>(null)
const form = reactive<{ quantity: number | null; toName: string; location: string }>({ quantity: null, toName: '', location: '' })

async function load(): Promise<void> {
  loading.value = true
  try {
    const d = await http.get<OutboundData>('/api/dealer/outbound-data')
    list.value = d.list
    inboundIds.value = new Set(d.inboundIds)
  } finally { loading.value = false }
}

const openOut = (r: CropBatch): void => {
  row.value = r
  form.quantity = null
  form.toName = ''
  form.location = ''
  visible.value = true
}

async function submit(): Promise<void> {
  if (!form.quantity) {
    ElMessage.warning('请填写出库数量')
    return
  }
  if (!form.toName.trim() || !form.location.trim()) {
    ElMessage.warning('请填写收货方和发货地点')
    return
  }
  saving.value = true
  try {
    await http.post('/api/dealer/outbound', { batchId: row.value!.id, ...form })
    ElMessage.success('出库成功，已写入流转记录')
    visible.value = false
    load()
  } catch { /* 已提示 */ } finally { saving.value = false }
}

onMounted(load)
</script>
