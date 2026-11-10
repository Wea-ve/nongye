<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">信任承诺</div>
      <el-button type="primary" @click="openAdd">新增承诺</el-button>
    </div>

    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 14px"
              title="承诺会在消费者扫码页公开显示，并由系统按真实流转时间自动判定是否兑现。请只承诺能做到的事。" />

    <el-card shadow="never">
      <el-table :data="promises" v-loading="loading">
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column label="承诺类型" width="130">
          <template #default="{ row }">{{ typeText(row.promiseType) }}</template>
        </el-table-column>
        <el-table-column prop="promiseValue" label="承诺内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="evidencePath" label="证据" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.evidencePath || '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !promises.length" description="还没有设置承诺，建议至少设置一条新鲜承诺" :image-size="80" />
    </el-card>

    <el-dialog v-model="addVisible" title="新增信任承诺" width="500px">
      <el-form label-width="90px">
        <el-form-item label="批次" required>
          <el-select v-model="form.batchId" style="width: 100%" placeholder="选择要承诺的批次">
            <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo} · ${b.cropName}`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="承诺类型" required>
          <el-select v-model="form.promiseType" style="width: 100%" @change="onTypeChange">
            <el-option v-for="t in types" :key="t.type" :label="t.label" :value="t.type" />
          </el-select>
          <div v-if="currentHint" class="hint">{{ currentHint }}</div>
        </el-form-item>
        <el-form-item label="承诺内容" required>
          <el-input v-model="form.promiseValue" :placeholder="valuePlaceholder" />
        </el-form-item>
        <el-form-item label="证据链接">
          <el-input v-model="form.evidencePath" placeholder="选填，如检测报告编号或链接" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存承诺</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api/http'
import type { CropBatch } from '../../types'

interface PromiseTypeOption {
  type: string
  label: string
  hint: string
}

interface PromiseRow {
  id: number
  batchId: number
  batchNo?: string
  promiseType: string
  promiseValue: string
  evidencePath?: string
  createTime?: string
}

const promises = ref<PromiseRow[]>([])
const batches = ref<CropBatch[]>([])
const types = ref<PromiseTypeOption[]>([])
const loading = ref<boolean>(false)
const saving = ref<boolean>(false)
const addVisible = ref<boolean>(false)
const form = reactive({ batchId: null as number | null, promiseType: 'FRESH', promiseValue: '', evidencePath: '' })

const typeMap = computed<Record<string, string>>(() => {
  const m: Record<string, string> = {}
  types.value.forEach(t => { m[t.type] = t.label })
  return m
})
const typeText = (t: string): string => typeMap.value[t] || t
const currentHint = computed<string>(() => types.value.find(t => t.type === form.promiseType)?.hint || '')
const valuePlaceholder = computed<string>(() => {
  if (form.promiseType === 'FRESH') return '如：24h'
  if (form.promiseType === 'HARVEST_DATE') return '如：2026-11-15'
  return '如：无农残、不打蜡，已通过第三方检测'
})

function onTypeChange(): void {
  form.promiseValue = ''
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const data = await http.get<{ promises: PromiseRow[]; batches: CropBatch[]; types: PromiseTypeOption[] }>('/api/farmer/promises')
    promises.value = mergeBatchNo(data.promises, data.batches)
    batches.value = data.batches
    types.value = data.types
  } finally { loading.value = false }
}

/** 承诺记录只带 batchId，补上批次号便于展示 */
function mergeBatchNo(list: PromiseRow[], bs: CropBatch[]): PromiseRow[] {
  const map: Record<number, string> = {}
  bs.forEach(b => { map[b.id] = b.batchNo })
  return list.map(p => ({ ...p, batchNo: map[p.batchId] || `#${p.batchId}` }))
}

function openAdd(): void {
  form.batchId = null
  form.promiseType = 'FRESH'
  form.promiseValue = ''
  form.evidencePath = ''
  addVisible.value = true
}

async function save(): Promise<void> {
  if (!form.batchId) { ElMessage.warning('请选择批次'); return }
  if (!form.promiseValue.trim()) { ElMessage.warning('请填写承诺内容'); return }
  saving.value = true
  try {
    await http.post('/api/farmer/promises', { ...form })
    ElMessage.success('承诺已公示，消费者扫码即可看到')
    addVisible.value = false
    await load()
  } catch { /* 已提示 */ } finally { saving.value = false }
}

async function remove(row: PromiseRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除这条「${typeText(row.promiseType)}」承诺吗？`, '删除承诺', { type: 'warning' })
  } catch { return }
  await http.delete(`/api/farmer/promises/${row.id}`)
  ElMessage.success('已删除')
  await load()
}

function fmtDate(v?: string): string {
  return v ? v.replace('T', ' ').slice(0, 16) : '-'
}

onMounted(load)
</script>

<style scoped>
.page { padding: 20px 24px; }
.page-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 20px; font-weight: 700; color: #1b3a22; }
.hint { font-size: 12px; color: #8a9a8e; line-height: 1.6; margin-top: 4px; }
</style>
