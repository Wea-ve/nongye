<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">投诉工单</div>
      <el-button type="success" @click="exportExcel"><el-icon><Download /></el-icon>导出 Excel</el-button>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索投诉人 / 手机号 / 作物" clearable style="width: 240px"
                  @keyup.enter="load" @clear="load">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button @click="load">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading" :row-class-name="complaintRowClass">
        <el-table-column prop="id" label="工单号" width="80" />
        <el-table-column prop="complainant" label="投诉人" width="100" />
        <el-table-column prop="phone" label="电话" width="125" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ complaintTypeMap[row.type || ''] || row.type || '-' }}</template>
        </el-table-column>
        <el-table-column prop="cropName" label="关联作物" width="100" />
        <el-table-column prop="content" label="投诉内容" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="complaintStatusMap[row.status]?.type" size="small">
              {{ complaintStatusMap[row.status]?.text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="SLA 剩余" width="150">
          <template #default="{ row }">
            <div v-if="row.status === 'CLOSED'" style="color:#52c41a">已办结</div>
            <div v-else-if="row.firstReplyTime" style="color:#52c41a">已回复</div>
            <div v-else-if="row.overdue === 1" style="color:#f56c6c;font-weight:600">
              ⚠️ 超时 {{ Math.floor(Math.abs(slaRemaining(row))/3600) }}h
              <el-tag type="danger" size="small" effect="dark">已升级</el-tag>
            </div>
            <div v-else :style="{ color: slaRemaining(row) < 3600 ? '#faad14' : '#1677ff' }">
              {{ slaRemainingLabel(row) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openReply(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 处理工单：按状态机区分表单 -->
    <el-dialog v-model="replyVisible" width="560px" :title="`处理工单 #${row?.id}`">
      <template v-if="row">
        <!-- 已关闭：办结横幅 + 全程信息展示 -->
        <el-alert v-if="row.status === 'CLOSED'" type="success" :closable="false" show-icon
                  title="✅ 该工单已办结" style="margin-bottom: 14px" />

        <el-descriptions :column="2" border size="small" style="margin-bottom: 14px">
          <el-descriptions-item label="投诉人">{{ row.complainant }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ row.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关联批次">{{ row.batchNo || '无' }}<span v-if="row.cropName">（{{ row.cropName }}）</span></el-descriptions-item>
          <el-descriptions-item label="类型">{{ complaintTypeMap[row.type || ''] || row.type || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-alert type="warning" :closable="false" :title="row.content" style="margin-bottom: 14px" />

        <!-- 管理员回复 -->
        <template v-if="row.reply">
          <div class="reply-block">
            <div class="reply-label">👨‍💼 管理员回复（{{ fmtTime(row.replyTime) }}）</div>
            <div class="reply-content">{{ row.reply }}</div>
          </div>
        </template>

        <!-- 消费者确认（若有） -->
        <div v-if="row.consumerReply" class="reply-block consumer">
          <div class="reply-label">🙋 消费者确认反馈（{{ fmtTime(row.consumerReplyTime) }}）</div>
          <div class="reply-content">{{ row.consumerReply }}</div>
        </div>

        <!-- 待处理：回复并转处理中 -->
        <template v-if="row.status === 'PENDING'">
          <el-alert v-if="row.overdue === 1" type="error" :closable="false" show-icon title="该工单已自动升级管理员紧急处理，请优先回复！" style="margin-bottom:12px" />
          <el-divider content-position="left">回复工单</el-divider>
          <el-input v-model="replyText" type="textarea" :rows="3"
                    placeholder="填写处理方案，提交后工单转为「处理中」，等待消费者确认" />
          <div style="text-align: right; margin-top: 12px">
            <el-button type="primary" :loading="saving" @click="submitReply('PROCESSING')">
              回复并标记处理中
            </el-button>
          </div>
        </template>

        <!-- 处理中：引导关闭 -->
        <template v-else-if="row.status === 'PROCESSING'">
          <el-alert v-if="row.overdue === 1" type="error" :closable="false" show-icon title="该工单已自动升级管理员紧急处理，请优先回复！" style="margin-bottom:12px" />
          <el-divider content-position="left">办结工单</el-divider>
          <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px"
                    title="已回复，等待消费者确认；也可直接办结关闭本工单" />
          <el-input v-model="replyText" type="textarea" :rows="2"
                    placeholder="如需补充处理方案再填写（可选）" />
          <div style="text-align: right; margin-top: 12px">
            <el-button type="success" :loading="saving" @click="submitReply('CLOSED')">回复并关闭工单</el-button>
          </div>
        </template>

        <!-- 已关闭：无表单 -->
        <el-alert v-else type="info" :closable="false"
                  title="工单已关闭，回复表单已隐藏。如消费者对处理结果满意，会在此显示确认反馈。" />
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { downloadBlob, complaintStatusMap, complaintTypeMap, fmtTime } from '../../utils'
import type { Complaint } from '../../types'

const list = ref<Complaint[]>([])
const keyword = ref<string>('')
const loading = ref<boolean>(false)
const saving = ref<boolean>(false)
const replyVisible = ref<boolean>(false)
const replyText = ref<string>('')
const row = ref<Complaint | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    list.value = await http.get<Complaint[]>('/api/admin/complaints', { params: { keyword: keyword.value || undefined } })
  } finally { loading.value = false }
}

const openReply = (r: Complaint): void => {
  row.value = r
  replyText.value = ''
  replyVisible.value = true
}

async function submitReply(status: string): Promise<void> {
  if (!replyText.value.trim()) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  saving.value = true
  try {
    await http.post(`/api/admin/complaints/${row.value!.id}/reply`, { reply: replyText.value, status })
    ElMessage.success(status === 'CLOSED' ? '工单已办结关闭' : '已回复，工单转为处理中')
    replyVisible.value = false
    load()
  } catch { /* 已提示 */ } finally { saving.value = false }
}

async function exportExcel(): Promise<void> {
  const res = await http.get<{ data: BlobPart }>('/admin/complaints/export', { responseType: 'blob' })
  downloadBlob(res, '投诉工单.xlsx')
}

onMounted(() => { load(); ticker = window.setInterval(() => { nowTick.value++ }, 1000) })
onUnmounted(() => { if (ticker) window.clearInterval(ticker) })

const nowTick = ref(0)
let ticker: number | null = null

function slaRemaining(c: Complaint): number {
  if (c.firstReplyTime) return 0
  const deadline = new Date(c.createTime!).getTime() + ((c.slaHours ?? 24) * 3600 * 1000)
  return Math.round((deadline - Date.now()) / 1000)
}
function slaRemainingLabel(c: Complaint): string {
  const s = slaRemaining(c)
  if (s <= 0) return `超时 ${Math.floor(Math.abs(s)/3600)}h${Math.floor(Math.abs(s)%3600/60)}m`
  if (s < 3600) return `${Math.floor(s/60)}m 后超时`
  return `${Math.floor(s/3600)}h${Math.floor(s%3600/60)}m`
}
function complaintRowClass({ row }: { row: Complaint }): string {
  if (row.status === 'CLOSED') return ''
  if (row.overdue === 1) return 'row-overdue'
  if (slaRemaining(row) < 0) return 'row-overdue'
  if (slaRemaining(row) > 0 && slaRemaining(row) < 3600) return 'row-warn'
  return ''
}
</script>

<style scoped>
.reply-block {
  border-radius: 10px; padding: 12px 14px; margin-bottom: 10px;
  background: #f2f8f2; border: 1px solid #dcecdc;
}
.reply-block.consumer { background: #eef5fd; border-color: #d3e5f7; }
.reply-label { font-size: 12.5px; color: #7d8f81; margin-bottom: 6px; }
.reply-content { font-size: 14px; color: #333; line-height: 1.6; white-space: pre-wrap; }
:deep(.el-table .row-overdue) { background: #fef0f0 !important; }
:deep(.el-table .row-warn) { background: #fdf6ec !important; }
</style>
