<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/login')
}
</script>

<template>
  <div class="nf">
    <div class="nf-icon">🧭</div>
    <div class="nf-code">404</div>
    <div class="nf-msg">页面走丢了，可能是链接已失效或地址输错了</div>
    <div class="nf-actions">
      <el-button @click="goBack">返回上一页</el-button>
      <el-button type="primary" @click="$router.push('/login')">回到首页</el-button>
    </div>
  </div>
</template>

<style scoped>
.nf {
  min-height: 100vh; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 14px;
  background: #f4f7f4;
}
.nf-icon { font-size: 64px; }
.nf-code { font-size: 56px; font-weight: 800; color: #2e7d32; line-height: 1; }
.nf-msg { color: #7d8f81; margin-bottom: 8px; }
.nf-actions { display: flex; gap: 12px; }
</style>
