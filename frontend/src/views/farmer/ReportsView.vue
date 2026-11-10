<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">检测报告</div>
      <el-button type="primary" @click="openAdd"><el-icon><Upload /></el-icon>上传报告</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="reports" v-loading="loading">
        <el-table-column prop="reportNo" label="报告编号" min-width="150" />
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column prop="cropName" label="作物" width="90" />
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

    <el-dialog v-model="addVisible" title="上传检测报告" width="480px">
      <el-form label-width="90px">
        <el-form-item label="批次" required>
          <el-select v-model="addForm.batchId" style="width: 100%">
            <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo} · ${b.cropName}`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报告编号"><el-input v-model="addForm.reportNo" /></el-form-item>
        <el-form-item label="检测机构"><el-input v-model="addForm.institution" placeholder="如：绿源第三方检测中心" /></el-form-item>
        <el-form-item label="检测日期">
          <el-date-picker v-model="addForm.reportDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结果" required>
          <el-radio-group v-model="addForm.result">
            <el-radio value="合格">合格</el-radio>
            <el-radio value="不合格">不合格</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="报告文件">
          <input type="file" accept=".pdf,.png,.jpg,.jpeg" @change="onFileChange" />
          <div style="font-size: 12px; color: #999">支持 PDF/PNG/JPG，10MB 以内</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { fmtDate } from '../../utils'
import type { CropBatch, DetectionReport, FarmerReportsData } from '../../types'

const reports = ref<DetectionReport[]>([])
const batches = ref<CropBatch[]>([])
const loading = ref<boolean>(false)
const saving = ref<boolean>(false)
const addVisible = ref<boolean>(false)
const file = ref<File | null>(null)
const addForm = reactive<{ batchId: number | null; reportNo: string; institution: string; reportDate: string; result: string }>({ batchId: null, reportNo: '', institution: '', reportDate: '', result: '合格' })

async function load(): Promise<void> {
  loading.value = true
  try {
    const d = await http.get<FarmerReportsData>('/api/farmer/reports')
    reports.value = d.reports
    batches.value = d.batches
  } finally { loading.value = false }
}

const openAdd = (): void => {
  Object.assign(addForm, { batchId: null, reportNo: '', institution: '', reportDate: '', result: '合格' })
  file.value = null
  addVisible.value = true
}

function onFileChange(e: Event): void {
  const input = e.target as HTMLInputElement
  file.value = input.files && input.files[0] ? input.files[0] : null
}

async function save(): Promise<void> {
  if (!addForm.batchId) {
    ElMessage.warning('请选择批次')
    return
  }
  if (!addForm.result) {
    ElMessage.warning('请选择检测结果')
    return
  }
  saving.value = true
  try {
    const fd = new FormData()
    Object.entries(addForm).forEach(([k, v]) => v != null && fd.append(k, String(v)))
    if (file.value) fd.append('file', file.value)
    await http.post('/api/farmer/reports', fd)
    ElMessage.success('报告已上传')
    addVisible.value = false
    load()
  } catch { /* 已提示 */ } finally { saving.value = false }
}

onMounted(load)
</script>
