import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as Icons from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
// 全局注册图标组件
for (const [name, component] of Object.entries(Icons)) {
  app.component(name, component)
}
app.mount('#app')
