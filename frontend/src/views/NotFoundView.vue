<script setup lang="ts">
</script>

<template>
  <div class="nf">
    <div class="nf-icon">🧭</div>
    <div class="nf-code">404</div>
    <div class="nf-msg">页面走丢了，这里什么都没有</div>
    <el-button type="primary" @click="$router.push('/login')">回到首页</el-button>
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
</style>
