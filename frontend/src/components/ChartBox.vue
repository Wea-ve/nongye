<template>
  <div ref="el" :style="{ height, width: '100%' }"></div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import type { EChartsOption, ECharts } from 'echarts'
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'

const props = withDefaults(defineProps<{ option: EChartsOption | null; height?: string }>(), {
  height: '300px'
})

const el = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null

const render = (): void => {
  if (chart && props.option) chart.setOption(props.option, true)
}

const resize = (): void => {
  if (chart) chart.resize()
}

onMounted(() => {
  if (el.value) chart = echarts.init(el.value)
  render()
  window.addEventListener('resize', resize)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) chart.dispose()
})
</script>
