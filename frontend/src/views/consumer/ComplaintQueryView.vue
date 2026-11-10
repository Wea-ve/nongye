<template>
  <div class="trace-page">
    <header class="t-nav">
      <div class="t-brand"><img class="brand-ico" src="../../assets/logo-icon.svg" alt="安溯·知源" />安溯·知源</div>
      <div class="t-links">
        <router-link to="/assistant">🛡️ 消费助手</router-link>
        <router-link to="/complaint/new">我要投诉</router-link>
        <router-link to="/login">管理入口</router-link>
      </div>
    </header>

    <div class="t-body">
      <el-card shadow="never" class="t-card">
        <template #header>🔍 投诉进度查询</template>
        <div style="display: flex; gap: 10px; flex-wrap: wrap">
          <el-input v-model="keyword" placeholder="输入手机号或查询码" clearable
                    style="max-width: 320px" @keyup.enter="load">
            <template #prefix>🔑</template>
          </el-input>
          <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        </div>
      </el-card>

      <el-alert v-if="justSubmitted" type="success" :closable="false" show-icon class="t-card"
                :title="`投诉提交成功！查询码 ${keyword} ，请截图保存，后续凭此查询进度`" />

      <el-empty v-if="!loading && searched && !list.length"
                description="没有查到相关投诉，请确认手机号或查询码是否正确" />

      <el-card v-for="c in list" :key="c.id" shadow="never" class="t-card">
        <div class="c-head">
          <div>
            <span class="c-no">查询码 {{ c.queryCode || '—' }}</span>
            <el-tag :type="complaintStatusMap[c.status]?.type" size="small" style="margin-left: 10px">
              {{ complaintStatusMap[c.status]?.text }}
            </el-tag>
          </div>
          <span class="c-time">{{ fmtTime(c.createTime) }}</span>
        </div>

        <el-descriptions :column="2" size="small" border style="margin-top: 12px">
          <el-descriptions-item label="类型">{{ complaintTypeMap[c.type || ''] || c.type || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关联批次">
            {{ c.batchNo ? `${c.cropName}（${c.batchNo}）` : '未关联' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="c-block">
          <div class="c-label">您的投诉内容</div>
          <div class="c-content">{{ c.content }}</div>
        </div>

        <div v-if="c.reply" class="c-block admin">
          <div class="c-label">👨‍💼 管理员回复（{{ fmtTime(c.replyTime) }}）</div>
          <div class="c-content">{{ c.reply }}</div>
        </div>

        <!-- SLA 倒计时（未关闭工单显示） -->
        <el-card v-if="c && c.status !== 'CLOSED'" shadow="never" class="t-card sla-card">
          <div class="sla-bar">
            <div class="sla-label" :style="{ color: slaLabel(c).color }">
              {{ slaLabel(c).text }}
            </div>
            <el-progress
              :percentage="Math.max(0, Math.min(100, slaPercentage(c)))"
              :color="slaBarColor(c)"
              :stroke-width="6"
              :show-text="false"
              class="sla-progress"
            />
            <div class="sla-meta">承诺 {{ c.slaHours ?? 24 }} 小时内首次回复 · 自动升级管理员超时即触发</div>
          </div>
        </el-card>

        <div v-if="c.consumerReply" class="c-block consumer">
          <div class="c-label">🙋 您的确认反馈（{{ fmtTime(c.consumerReplyTime) }}）· 工单已办结</div>
          <div class="c-content">{{ c.consumerReply }}</div>
        </div>

        <!-- 处理中：消费者确认 -->
        <template v-if="c.status === 'PROCESSING'">
          <el-alert type="info" :closable="false" style="margin-top: 12px"
                    title="管理员已回复，如问题已解决，请填写查询码并确认反馈以关闭工单" />
          <div style="display: flex; gap: 10px; margin-top: 12px; flex-wrap: wrap">
            <el-input v-model="confirmCode" placeholder="查询码" style="max-width: 160px" />
            <el-input v-model="confirmText" placeholder="如：问题已解决，满意" size="default" style="max-width: 300px" />
            <el-button type="success" :loading="saving" @click="confirm(c)">确认并关闭工单</el-button>
          </div>
        </template>

        <!-- 待处理提示 -->
        <el-alert v-if="c.status === 'PENDING'" type="warning" :closable="false" style="margin-top: 12px"
                  title="工作人员会尽快处理，请耐心等待" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { complaintStatusMap, complaintTypeMap, fmtTime } from '../../utils'
import type { Complaint } from '../../types'

const route = useRoute()
const keyword = ref<string>(route.query.keyword ? String(route.query.keyword) : '')
const justSubmitted: boolean = route.query.submitted === '1'
const list = ref<Complaint[]>([])
// 模板原有引用，保持默认 false，与原行为一致
const searched = ref<boolean>(false)
const loading = ref<boolean>(false)
const saving = ref<boolean>(false)
const confirmText = ref<string>('')
const confirmCode = ref<string>('')

async function load(): Promise<void> {
  const kw = keyword.value.trim()
  if (!kw) {
    ElMessage.warning('请输入手机号或查询码')
    return
  }
  loading.value = true
  try {
    list.value = await http.get<Complaint[]>('/api/complaint/query', { params: { keyword: kw } })
  } finally { loading.value = false }
}

async function confirm(c: Complaint): Promise<void> {
  if (!confirmCode.value.trim()) {
    ElMessage.warning('请填写查询码')
    return
  }
  if (!confirmText.value.trim()) {
    ElMessage.warning('请填写确认内容')
    return
  }
  saving.value = true
  try {
    await http.post(`/api/complaint/${c.id}/confirm`, {
      consumerReply: confirmText.value,
      queryCode: confirmCode.value.trim().toUpperCase()
    })
    ElMessage.success('感谢反馈，工单已关闭')
    load()
  } catch { /* 已提示 */ } finally { saving.value = false }
}

onMounted(() => { if (keyword.value) load(); ticker = window.setInterval(() => { nowTick.value++ }, 1000) })
onUnmounted(() => { if (ticker) window.clearInterval(ticker) })

const nowTick = ref(0)
let ticker: number | null = null

function Integer(v: unknown): number { return v == null ? 0 : Number(v) }

/**
 * 计算剩余 SLA 时间（秒）。
 * 规则：overdue=1 → 返回负数（表示已超时 |overdue| 秒）；
 * firstReplyTime IS NOT NULL → 返回 0（管理员已首次回复，SLA 停止计时）；
 * 否则：(createTime + slaHours*3600) - now
 */
function slaRemaining(c: Complaint): number {
  if (Integer(c.overdue) === 1) {
    const deadline = new Date(c.createTime!).getTime() + ((c.slaHours ?? 24) * 3600 * 1000)
    return Math.round((deadline - Date.now()) / 1000)
  }
  if (c.firstReplyTime) return 0
  const deadline = new Date(c.createTime!).getTime() + ((c.slaHours ?? 24) * 3600 * 1000)
  return Math.round((deadline - Date.now()) / 1000)
}
function slaLabel(c: Complaint): { text: string; color: string } {
  const sec = slaRemaining(c)
  if (c.firstReplyTime) return { text: `管理员已在 ${c.firstReplyTime.slice(5, 16)} 首次回复`, color: '#52c41a' }
  if (Integer(c.overdue) === 1 || sec <= 0) {
    const lateH = Math.abs(Math.floor(sec / 3600))
    return { text: `⚠️ SLA 已超时 ${lateH} 小时，已自动升级管理员紧急处理`, color: '#f56c6c' }
  }
  const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60)
  if (sec < 3600) return { text: `⏳ 剩余 ${m} 分钟 SLA`, color: '#faad14' }
  return { text: `⏳ 剩余 ${h} 小时 ${m} 分钟 SLA`, color: '#1677ff' }
}
function slaPercentage(c: Complaint): number {
  if (!c.createTime) return 50
  const totalSec = (c.slaHours ?? 24) * 3600
  const elapsed = (Date.now() - new Date(c.createTime).getTime()) / 1000
  return Math.round(elapsed / totalSec * 100)
}
function slaBarColor(c: Complaint): string {
  if (c.firstReplyTime) return '#52c41a'
  if (Integer(c.overdue) === 1 || slaRemaining(c) <= 0) return '#f56c6c'
  const pct = slaPercentage(c)
  if (pct >= 90) return '#f56c6c'
  if (pct >= 70) return '#faad14'
  return '#1677ff'
}
</script>

<style scoped>
.trace-page { min-height: 100vh; background: #f4f7f4; }
.t-nav {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; background: #fff; box-shadow: 0 1px 8px rgba(31, 66, 39, .08);
}
.t-brand { font-weight: 700; color: #1b5e20; font-size: 17px; }
.brand-ico { width: 22px; height: 22px; vertical-align: -5px; margin-right: 6px; }
.t-links { display: flex; gap: 18px; }
.t-links a { color: #4a674e; text-decoration: none; font-size: 14px; }
.t-body { max-width: 720px; margin: 0 auto; padding: 24px 14px 60px; }
.t-card { margin-bottom: 16px; }
.c-head { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.c-no { font-weight: 700; color: #23422a; }
.c-time { font-size: 12.5px; color: #999; }
.c-block { border-radius: 10px; padding: 12px 14px; margin-top: 12px; background: #f7f9f7; }
.c-block.admin { background: #f2f8f2; border: 1px solid #dcecdc; }
.c-block.consumer { background: #eef5fd; border: 1px solid #d3e5f7; }
.c-label { font-size: 12.5px; color: #7d8f81; margin-bottom: 6px; }
.c-content { font-size: 14px; color: #333; line-height: 1.6; white-space: pre-wrap; }
.sla-bar { display: flex; flex-direction: column; gap: 8px; }
.sla-label { font-weight: 500; }
.sla-progress { margin-top: 4px; }
.sla-meta { font-size: 12px; color: #8a9a8e; }
</style>
