<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">农事记录 · {{ batch?.batchNo }}</div>
      <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon>添加记录</el-button>
    </div>

    <el-card shadow="never" style="margin-bottom: 14px">
      <el-descriptions :column="3" size="large">
        <el-descriptions-item label="作物">{{ batch?.cropName }}</el-descriptions-item>
        <el-descriptions-item label="品种">{{ batch?.variety || '-' }}</el-descriptions-item>
        <el-descriptions-item label="产地">{{ batch?.landLocation || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <template #header>📓 记录时间线</template>
      <el-timeline v-if="logs.length">
        <el-timeline-item v-for="l in logs" :key="l.id" :timestamp="fmtDate(l.logDate)"
                          :color="logTypeMap[l.logType]?.color" placement="top">
          <div class="log-item">
            <el-tag size="small" :color="logTypeMap[l.logType]?.color" effect="dark"
                    style="border: none; margin-right: 8px">{{ logTypeMap[l.logType]?.text || l.logType }}</el-tag>
            <span v-if="l.material" class="log-line">物料：{{ l.material }}</span>
            <span v-if="l.dosage" class="log-line">用量：{{ l.dosage }}</span>
            <div v-if="l.content" class="log-content">{{ l.content }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无农事记录，点击右上角添加" />
    </el-card>

    <el-dialog v-model="addVisible" title="添加农事记录" width="460px">
      <el-form label-width="80px">
        <el-form-item label="类型" required>
          <el-select v-model="addForm.logType" style="width: 100%">
            <el-option v-for="(v, k) in logTypeMap" :key="k" :label="v.text" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" required>
          <el-date-picker v-model="addForm.logDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="物料"><el-input v-model="addForm.material" placeholder="如：有机肥 / 吡虫啉" /></el-form-item>
        <el-form-item label="用量"><el-input v-model="addForm.dosage" placeholder="如：20kg / 800倍液" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="addForm.content" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAdd">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { fmtDate, logTypeMap } from '../../utils'
import type { BatchLogsData, CropBatch, FarmingLog } from '../../types'

const route = useRoute()
const batchId = route.params.id
const batch = ref<CropBatch | null>(null)
const logs = ref<FarmingLog[]>([])
const addVisible = ref<boolean>(false)
const saving = ref<boolean>(false)
const addForm = reactive({ logType: '', logDate: '', material: '', dosage: '', content: '' })

async function load(): Promise<void> {
  const d = await http.get<BatchLogsData>(`/api/farmer/batches/${batchId}/logs`)
  batch.value = d.batch
  logs.value = d.logs
}

const openAdd = (): void => {
  Object.assign(addForm, { logType: '', logDate: new Date().toISOString().slice(0, 10), material: '', dosage: '', content: '' })
  addVisible.value = true
}

async function saveAdd(): Promise<void> {
  if (!addForm.logType) {
    ElMessage.warning('请选择农事类型')
    return
  }
  if (!addForm.logDate) {
    ElMessage.warning('请选择记录日期')
    return
  }
  saving.value = true
  try {
    await http.post(`/api/farmer/batches/${batchId}/logs`, addForm)
    ElMessage.success('记录已保存')
    addVisible.value = false
    load()
  } catch { /* 已提示 */ } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.log-line { margin-right: 12px; font-size: 13px; color: #555; }
.log-content { margin-top: 4px; color: #666; font-size: 13px; }
</style>
