<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">种植批次</div>
      <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon>新增批次</el-button>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索批次号 / 作物名" clearable style="width: 240px"
                  @keyup.enter="load" @clear="load">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button @click="load">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading">
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column prop="cropName" label="作物" width="90" />
        <el-table-column prop="variety" label="品种" width="100" />
        <el-table-column prop="landLocation" label="产地" min-width="130" show-overflow-tooltip />
        <el-table-column label="播种日期" width="110">
          <template #default="{ row }">{{ fmtDate(row.plantDate) }}</template>
        </el-table-column>
        <el-table-column label="预计采收" width="110">
          <template #default="{ row }">{{ fmtDate(row.expectHarvestDate) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="batchStatusMap[row.status]?.type" size="small">{{ batchStatusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/farmer/batch/${row.id}/logs`)">农事记录</el-button>
            <el-button v-if="row.status === 'PLANTING'" size="small" type="primary" @click="openHarvest(row)">采收登记</el-button>
            <el-button v-if="row.traceCode" size="small" success @click="showQr(row)">溯源码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增批次 -->
    <el-dialog v-model="addVisible" title="新增种植批次" width="460px">
      <el-form label-width="90px">
        <el-form-item label="作物名称" required><el-input v-model="addForm.cropName" placeholder="如：西红柿" /></el-form-item>
        <el-form-item label="品种"><el-input v-model="addForm.variety" placeholder="如：粉果大番茄" /></el-form-item>
        <el-form-item label="产地地块" required><el-input v-model="addForm.landLocation" placeholder="如：1号大棚" /></el-form-item>
        <el-form-item label="播种日期" required>
          <el-date-picker v-model="addForm.plantDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预计采收">
          <el-date-picker v-model="addForm.expectHarvestDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAdd">保存</el-button>
      </template>
    </el-dialog>

    <!-- 采收登记 -->
    <el-dialog v-model="harvestVisible" title="采收登记（生成溯源码）" width="440px">
      <el-alert type="info" :closable="false" style="margin-bottom: 14px"
                :title="`批次：${harvestBatch?.batchNo} · ${harvestBatch?.cropName}`" />
      <el-form label-width="90px">
        <el-form-item label="采收日期" required>
          <el-date-picker v-model="harvestForm.actualHarvestDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="采收重量" required>
          <el-input-number v-model="harvestForm.harvestWeight" :min="0.1" :precision="1" style="width: 100%" />
          <span style="margin-left: 8px; color: #999">斤</span>
        </el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false" title="登记后自动生成溯源码与二维码，批次状态变为已采收" />
      <template #footer>
        <el-button @click="harvestVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveHarvest">确认采收</el-button>
      </template>
    </el-dialog>

    <!-- 溯源码 -->
    <el-dialog v-model="qrVisible" title="批次溯源码" width="340px" align-center>
      <div style="text-align: center">
        <img v-if="qrBatch?.traceCode" :src="`/qrcode?code=${qrBatch.traceCode}`"
             alt="溯源二维码" style="width: 220px; height: 220px" />
        <div style="margin-top: 10px; font-size: 15px; font-weight: 600; letter-spacing: 1px">
          {{ qrBatch?.traceCode }}
        </div>
        <div style="color: #999; font-size: 12px; margin-top: 4px">手机扫码或访问 /trace/{{ qrBatch?.traceCode }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { batchStatusMap, fmtDate } from '../../utils'
import type { CropBatch } from '../../types'

const list = ref<CropBatch[]>([])
const keyword = ref<string>('')
const loading = ref<boolean>(false)
const saving = ref<boolean>(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    list.value = await http.get<CropBatch[]>('/api/farmer/batches', { params: { keyword: keyword.value || undefined } })
  } finally { loading.value = false }
}

// 新增批次
const addVisible = ref<boolean>(false)
const addForm = reactive({ cropName: '', variety: '', landLocation: '', plantDate: '', expectHarvestDate: '' })
const openAdd = (): void => {
  Object.assign(addForm, { cropName: '', variety: '', landLocation: '', plantDate: '', expectHarvestDate: '' })
  addVisible.value = true
}
async function saveAdd(): Promise<void> {
  if (!addForm.cropName.trim()) {
    ElMessage.warning('请填写作物名称')
    return
  }
  if (!addForm.landLocation.trim()) {
    ElMessage.warning('请填写产地地块')
    return
  }
  if (!addForm.plantDate) {
    ElMessage.warning('请选择播种日期')
    return
  }
  saving.value = true
  try {
    await http.post('/api/farmer/batches', addForm)
    ElMessage.success('批次已创建')
    addVisible.value = false
    load()
  } catch { /* 已提示 */ } finally { saving.value = false }
}

// 采收登记
const harvestVisible = ref<boolean>(false)
const harvestBatch = ref<CropBatch | null>(null)
const harvestForm = reactive<{ actualHarvestDate: string; harvestWeight: number | null }>({ actualHarvestDate: '', harvestWeight: null })
const openHarvest = (row: CropBatch): void => {
  harvestBatch.value = row
  harvestForm.actualHarvestDate = new Date().toISOString().slice(0, 10)
  harvestForm.harvestWeight = null
  harvestVisible.value = true
}
async function saveHarvest(): Promise<void> {
  if (!harvestForm.actualHarvestDate) {
    ElMessage.warning('请选择采收日期')
    return
  }
  if (!harvestForm.harvestWeight) {
    ElMessage.warning('请填写采收重量')
    return
  }
  saving.value = true
  try {
    const batch = await http.post<CropBatch>(`/api/farmer/batches/${harvestBatch.value!.id}/harvest`, harvestForm)
    ElMessage.success(`采收完成，溯源码：${batch.traceCode}`)
    harvestVisible.value = false
    load()
  } catch { /* 已提示 */ } finally { saving.value = false }
}

// 溯源码弹窗
const qrVisible = ref<boolean>(false)
const qrBatch = ref<CropBatch | null>(null)
const showQr = (row: CropBatch): void => {
  qrBatch.value = row
  qrVisible.value = true
}

onMounted(load)
</script>
