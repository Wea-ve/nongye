<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">溯源码管理</div>
      <el-tag effect="plain" size="large">当前局域网地址：{{ baseUrl }}</el-tag>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom: 14px"
              title="二维码自动使用当前局域网 IP 生成，切换 WiFi/热点后刷新本页即可更新，手机需与电脑同一局域网" />

    <el-row :gutter="16">
      <el-col v-for="b in withTrace" :key="b.id" :xs="24" :sm="12" :md="8" style="margin-bottom: 16px">
        <el-card shadow="never">
          <div class="qr-card">
            <div class="qr-crop">🥬 {{ b.cropName }}<span class="qr-variety">{{ b.variety }}</span></div>
            <img v-if="b.traceCode" :src="`/qrcode?code=${b.traceCode}`" class="qr-img" alt="二维码" />
            <div class="qr-code">{{ b.traceCode }}</div>
            <div class="qr-actions">
              <el-button size="small" @click="copyLink(b)">复制链接</el-button>
              <el-button size="small" type="primary" @click="openPage(b)">打开页面</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!withTrace.length" description="暂无溯源码，先在批次管理中完成采收登记" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import type { CropBatch, TraceCodesData } from '../../types'

const baseUrl = ref<string>('')
const batches = ref<CropBatch[]>([])

const withTrace = computed<CropBatch[]>(() => batches.value.filter(b => b.traceCode))

async function copyLink(b: CropBatch): Promise<void> {
  try {
    await navigator.clipboard.writeText(`${baseUrl.value}/trace/${b.traceCode}`)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制：' + baseUrl.value + '/trace/' + b.traceCode)
  }
}
const openPage = (b: CropBatch): Window | null => window.open(`/app/trace/${b.traceCode}`, '_blank')

onMounted(async () => {
  const d = await http.get<TraceCodesData>('/api/farmer/trace-codes')
  batches.value = d.batches
  baseUrl.value = d.baseUrl
})
</script>

<style scoped>
.qr-card { text-align: center; }
.qr-crop { font-size: 17px; font-weight: 700; color: #23422a; margin-bottom: 12px; }
.qr-variety { margin-left: 8px; font-size: 12px; color: #8a9a8e; font-weight: 400; }
.qr-img { width: 180px; height: 180px; }
.qr-code { margin-top: 8px; font-weight: 600; letter-spacing: 1px; color: #2e7d32; }
.qr-actions { margin-top: 10px; }
</style>
