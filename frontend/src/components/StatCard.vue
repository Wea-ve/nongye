<template>
  <div class="stat-card">
    <div class="stat-icon" :style="{ background: color }">{{ icon }}</div>
    <div>
      <div class="stat-value">{{ value }}</div>
      <div class="stat-label">{{ label }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  value?: number | string
  label?: string
  icon?: string
  color?: string
}>(), {
  value: 0,
  label: '',
  icon: '🌱',
  color: 'linear-gradient(135deg, #43a047, #2e7d32)'
})
</script>
