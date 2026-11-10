import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// SPA 部署在 Spring Boot 的 /app/ 路径下
export default defineConfig({
  base: '/app/',
  plugins: [vue()],
  build: {
    outDir: '../src/main/resources/static/app',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        // 第三方依赖单独分包，利用浏览器长缓存
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'axios'],
          'vendor-element': ['element-plus', '@element-plus/icons-vue'],
          'vendor-echarts': ['echarts']
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/qrcode': 'http://localhost:8080',
      '/uploads': 'http://localhost:8080',
      '/admin/batches/export': 'http://localhost:8080',
      '/admin/complaints/export': 'http://localhost:8080',
      '/img': 'http://localhost:8080'
    }
  }
})
