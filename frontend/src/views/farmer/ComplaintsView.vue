<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">售后工单</div>
      <el-button icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat-box">
        <div class="stat-num">{{ pendingCount }}</div>
        <div class="stat-label">待处理</div>
      </div>
      <div class="stat-box danger">
        <div class="stat-num">{{ overdueCount }}</div>
        <div class="stat-label">已超时（已自动升级平台）</div>
      </div>
      <div class="stat-box">
        <div class="stat-num">{{ rows.length }}</div>
        <div class="stat-label">全部工单</div>
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 14px"
              title="这里只显示你名下批次的投诉。承诺 24 小时内首次回复，超时将自动升级为平台介入并影响可信评分。回复后消费者可在「投诉查询」页凭查询码查看。" />

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" :row-class-name="rowClass">
        <el-table-column prop="queryCode" label="查询码" width="120" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column label="关联批次" min-width="190">
          <template #default="{ row }">
            <span v-if="row.batchNo">{{ row.cropName }}（{{ row.batchNo }}）</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="投诉内容" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="SLA" width="150">
          <template #default="{ row }">
            <span v-if="row.status === 'CLOSED'" class="sla-done">已办结</span>
            <span v-else-if="overdue(row)" class="sla-bad">已超时 {{ remainText(row) }}</span>
            <span v-else class="sla-ok">剩余 {{ remainText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status === 'CLOSED'" @click="openReply(row)">
              {{ row.reply ? '再次回复' : '处理' }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="detail">
              <div class="detail-row"><b>投诉人：</b>{{ row.complainant }}（{{ maskPhone(row.phone) }}）</div>
              <div class="detail-row"><b>投诉内容：</b>{{ row.content }}</div>
              <div v-if="row.reply" class="detail-row"><b>我的回复：</b>{{ row.reply }}（{{ fmtDate(row.replyTime) }}）</div>
              <div v-if="row.consumerReply" class="detail-row"><b>消费者确认：</b>{{ row.consumerReply }}</div>
              <div v-if="!row.reply" class="detail-row muted">尚未回复</div>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="暂无售后工单" :image-size="80" />
    </el-card>

    <el-dialog v-model="replyVisible" title="处理售后工单" width="520px">
      <div class="reply-head">
        <el-tag size="small" type="info">{{ current?.queryCode }}</el-tag>
        <span class="reply-crop">{{ current?.cropName }} · {{ typeText(current?.type || '') }}</span>
      </div>
      <div class="reply-content">消费者反馈：{{ current?.content }}</div>
      <el-input v-model="replyText" type="textarea" :rows="4" maxlength="300" show-word-limit
                placeholder="请说明核实结果与处理方案，例如：已核实为运输挤压，可全额退款，请联系 138xxxx" />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReply">提交处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'

interface ComplaintRow {
  id: number
  queryCode?: string
  type?: string
  batchNo?: string
  cropName?: string
  complainant?: string
  phone?: string
  content?: string
  status?: string
  reply?: string
  replyTime?: string
  consumerReply?: string
  slaHours?: number
  createTime?: string
  overdue?: number
}

const rows = ref<ComplaintRow[]>([])
const pendingCount = ref<number>(0)
const overdueCount = ref<number>(0)
const loading = ref<boolean>(false)
const saving = ref<boolean>(false)
const replyVisible = ref<boolean>(false)
const replyText = ref<string>('')
const current = ref<ComplaintRow | null>(null)
const now = ref<number>(Date.now())

const typeMap: Record<string, string> = { QUALITY: '质量问题', FRESHNESS: '新鲜度', SERVICE: '服务态度', OTHER: '其他' }
const typeText = (t?: string): string => (t ? typeMap[t] || t : '-')

function statusText(s?: string): string {
  if (s === 'PENDING') return '待处理'
  if (s === 'PROCESSING') return '处理中'
  if (s === 'CLOSED') return '已关闭'
  return s || '-'
}
function statusType(s?: string): 'warning' | 'primary' | 'success' | 'info' {
  if (s === 'PENDING') return 'warning'
  if (s === 'PROCESSING') return 'primary'
  if (s === 'CLOSED') return 'success'
  return 'info'
}

function deadline(row: ComplaintRow): number {
  const base = row.createTime ? new Date(row.createTime.replace(' ', 'T')).getTime() : now.value
  return base + (row.slaHours || 24) * 3600 * 1000
}
function overdue(row: ComplaintRow): boolean {
  return now.value > deadline(row)
}
function remainText(row: ComplaintRow): string {
  const diff = Math.abs(deadline(row) - now.value)
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  return `${h} 小时 ${m} 分`
}
function rowClass({ row }: { row: ComplaintRow }): string {
  if (row.status !== 'CLOSED' && overdue(row)) return 'row-overdue'
  return ''
}

function maskPhone(p?: string): string {
  if (!p || p.length < 7) return p || '-'
  return p.slice(0, 3) + '****' + p.slice(-4)
}
function fmtDate(v?: string): string {
  return v ? v.replace('T', ' ').slice(0, 16) : '-'
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const data = await http.get<{ complaints: ComplaintRow[]; pendingCount: number; overdueCount: number }>('/api/farmer/complaints')
    rows.value = data.complaints || []
    pendingCount.value = data.pendingCount || 0
    overdueCount.value = data.overdueCount || 0
  } finally { loading.value = false }
}

function openReply(row: ComplaintRow): void {
  current.value = row
  replyText.value = row.reply || ''
  replyVisible.value = true
}

async function submitReply(): Promise<void> {
  if (!current.value) return
  if (!replyText.value.trim()) { ElMessage.warning('请填写处理说明'); return }
  saving.value = true
  try {
    await http.post(`/api/farmer/complaints/${current.value.id}/reply`, { reply: replyText.value.trim() })
    ElMessage.success('处理结果已提交，消费者可凭查询码查看')
    replyVisible.value = false
    await load()
  } catch { /* 已提示 */ } finally { saving.value = false }
}

let ticker: number | undefined
onMounted(() => {
  load()
  ticker = window.setInterval(() => { now.value = Date.now() }, 30000)
})
onUnmounted(() => { if (ticker) window.clearInterval(ticker) })
</script>

<style scoped>
.page { padding: 20px 24px; }
.page-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 20px; font-weight: 700; color: #1b3a22; }
.stat-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 16px; }
.stat-box { background: #fff; border-radius: 10px; padding: 16px 18px; box-shadow: 0 1px 4px rgba(0,0,0,.05); }
.stat-box.danger .stat-num { color: #c62828; }
.stat-num { font-size: 26px; font-weight: 700; color: #1b3a22; }
.stat-label { font-size: 12.5px; color: #8a9a8e; margin-top: 4px; }
.sla-ok { color: #2e7d32; font-size: 12.5px; }
.sla-bad { color: #c62828; font-weight: 600; font-size: 12.5px; }
.sla-done { color: #999; font-size: 12.5px; }
.detail { padding: 8px 16px; display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: #4b5f51; }
.muted { color: #9aa79d; }
.reply-head { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.reply-crop { font-weight: 600; color: #23422a; }
.reply-content { background: #f7faf7; border-radius: 8px; padding: 10px 12px; font-size: 13px; color: #4b5f51; margin-bottom: 12px; }
:deep(.row-overdue) { background: #fff5f5; }
@media (max-width: 768px) { .stat-grid { grid-template-columns: 1fr; } }
</style>
