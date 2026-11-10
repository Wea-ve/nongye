<template>
  <div class="trace-page">
    <header class="t-nav">
      <div class="t-brand"><img class="brand-ico" src="../../assets/logo-icon.svg" alt="安溯·知源" />安溯·知源</div>
      <div class="t-links">
        <router-link to="/assistant">🛡️ 消费助手</router-link>
        <router-link to="/complaint/query">投诉查询</router-link>
        <router-link to="/login">管理入口</router-link>
      </div>
    </header>

    <div class="t-body">
      <el-card shadow="never" class="t-card">
        <template #header>📢 我要投诉</template>
        <el-form label-width="90px">
          <el-form-item label="溯源码">
            <el-input v-model="traceCode" placeholder="选填：包装上的溯源码，如 TZ3F2A..." clearable>
              <template #append>
                <el-button @click="resolveBatch" :loading="resolving">识别</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-alert v-if="batch" type="success" :closable="false" style="margin-bottom: 16px"
                    :title="`已关联批次：${batch.cropName}（${batch.batchNo}）`" />
          <el-form-item label="投诉人" required><el-input v-model="form.complainant" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="用于查询处理进度" /></el-form-item>
          <el-form-item label="类型">
            <el-select v-model="form.type" style="width: 100%">
              <el-option v-for="(v, k) in complaintTypeMap" :key="k" :label="v" :value="k" />
            </el-select>
          </el-form-item>
          <el-form-item label="内容" required>
            <el-input v-model="form.content" type="textarea" :rows="4"
                      placeholder="请描述您遇到的问题（变质、缺斤少两、信息不实等）" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="saving" @click="submit">提交投诉</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <div class="t-foot">提交后将生成查询码，可随时在「投诉查询」页跟踪处理进度</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { complaintTypeMap } from '../../utils'
import type { ComplaintSubmitResult, CropBatch, TraceDetailData } from '../../types'

const traceCode = ref<string>('')
const batch = ref<CropBatch | null>(null)
const resolving = ref<boolean>(false)
const saving = ref<boolean>(false)
const form = reactive({ complainant: '', phone: '', type: 'QUALITY', content: '' })

async function resolveBatch(): Promise<void> {
  const code = traceCode.value.trim()
  if (!code) {
    ElMessage.warning('请输入溯源码')
    return
  }
  resolving.value = true
  try {
    const d = await http.get<Partial<TraceDetailData>>(`/api/trace/${code}`)
    batch.value = d.batch || null
    ElMessage.success('已识别到对应批次')
  } catch { batch.value = null } finally { resolving.value = false }
}

async function submit(): Promise<void> {
  if (!form.complainant.trim()) {
    ElMessage.warning('请填写投诉人')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请填写投诉内容')
    return
  }
  saving.value = true
  try {
    const data = await http.post<ComplaintSubmitResult>('/api/complaint', {
      ...form,
      batchId: batch.value?.id
    })
    ElMessage.success('投诉已提交，查询码：' + data.queryCode)
    location.href = `/app/complaint/query?keyword=${encodeURIComponent(data.queryCode || '')}&submitted=1`
  } catch { /* 已提示 */ } finally { saving.value = false }
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
.t-body { max-width: 640px; margin: 0 auto; padding: 24px 14px 60px; }
.t-card { margin-bottom: 16px; }
.t-foot { text-align: center; color: #9aab9d; font-size: 12px; letter-spacing: 1px; }
</style>
