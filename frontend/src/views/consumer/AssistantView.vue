<template>
  <div class="trace-page">
    <!-- 顶部导航条 -->
    <header class="t-nav">
      <div class="t-brand"><img class="brand-ico" src="../../assets/logo-icon.svg" alt="安溯·知源" />安溯·知源</div>
      <div class="t-links">
        <router-link to="/consumer/home">我的首页</router-link>
        <router-link to="/login">管理入口</router-link>
      </div>
    </header>

    <div class="assistant-layout">
      <!-- 左侧：上下文栏 -->
      <aside class="assistant-side">
        <el-card shadow="never" class="t-card side-profile">
          <div class="sp-avatar">🌾</div>
          <div class="sp-info">
            <div class="sp-name">{{ nickname }}</div>
            <el-tag v-if="consumer.profile?.trustLevel" size="small" effect="dark" type="success" style="border:none">
              {{ trustLevelText(consumer.profile.trustLevel) }}
            </el-tag>
            <div class="sp-stats">关注 {{ sideStats.followCount }} 个农场 · 常买 {{ sideStats.buyCount }} 种菜</div>
          </div>
        </el-card>

        <el-card shadow="never" class="t-card">
          <template #header>⚡ 快捷问句</template>
          <div class="quick-q-list">
            <div v-for="q in quickQuestions" :key="q" class="quick-q" @click="sendQuick(q)">
              {{ q }}
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="t-card">
          <template #header>🛒 常购清单</template>
          <div v-if="subscriptions.length" class="sub-list">
            <div v-for="(s, i) in subscriptions" :key="i" class="sub-item">
              <span class="sub-crop">{{ s.cropName }}</span>
              <span v-if="s.nextDate" class="sub-next">下次 {{ s.nextDate }}</span>
              <span class="sub-cycle">每 {{ s.cycleDays || 7 }} 天</span>
            </div>
          </div>
          <el-empty v-else description="还没有常购清单" :image-size="60" />
        </el-card>

        <el-card shadow="never" class="t-card">
          <template #header>📰 最近动态</template>
          <div v-if="events.length" class="event-list">
            <div v-for="(e, i) in events" :key="i" class="event-item">
              <span class="event-ico">{{ e.icon }}</span>
              <div class="event-body">
                <div class="event-text">{{ e.text }}</div>
                <div class="event-time">{{ e.time }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无动态" :image-size="60" />
        </el-card>
      </aside>

      <!-- 右侧：聊天主区 -->
      <main class="assistant-main">
        <el-card shadow="never" class="t-card chat-card">
          <template #header>
            <div class="chat-head">
              <span>🤖 消费助手</span>
              <div class="chat-head-btns">
                <el-tag v-if="batchId" size="small" effect="plain">批次 #{{ batchId }}</el-tag>
                <el-button text size="small" @click="clearChat" v-if="messages.length">🗑️ 清空</el-button>
              </div>
            </div>
          </template>

          <div class="chat-body" ref="bodyEl">
            <!-- 开场白 -->
            <div class="chat-msg bot" v-if="!messages.length">
              <div class="chat-bubble">
                {{ greeting }}
              </div>
            </div>

            <!-- 消息列表 -->
            <div v-for="(m, i) in messages" :key="i" class="chat-msg" :class="m.role">
              <div class="chat-bubble">
                <img v-if="m.image" :src="m.image" class="chat-msg-img" alt="上传的图片" />
                <div>{{ m.text }}<span v-if="m.streaming" class="chat-cursor"></span></div>
                <!-- 工具徽章 -->
                <div v-if="m.tools && m.tools.length" class="chat-tools">
                  <span v-for="(t, ti) in m.tools" :key="ti" class="chat-tool-chip" :title="t.summary">
                    🔧 {{ t.label }}
                  </span>
                </div>
                <!-- 答案依据 -->
                <div v-if="m.sources && m.sources.length" class="chat-sources">
                  <div class="chat-sources-title">📎 答案依据</div>
                  <div v-for="(s, si) in m.sources" :key="si" class="chat-source-item">
                    <span class="chat-source-type">{{ sourceTypeMap[s.type] || '资料' }}</span>
                    <span class="chat-source-label">{{ s.label }}</span>
                    <span v-if="s.detail" class="chat-source-detail">{{ s.detail }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 正在思考 / 步骤 -->
            <div v-if="loading && !hasStreaming" class="chat-msg bot">
              <div class="chat-bubble chat-thinking">
                <div v-if="!steps.length">正在思考…</div>
                <div v-for="(s, si) in steps" :key="si" class="chat-step">
                  <span class="chat-step-dot">✓</span>{{ s.summary }}
                </div>
              </div>
            </div>
          </div>

          <div v-if="imagePending" class="chat-img-preview">
            <img :src="imagePending" alt="待发送图片" />
            <span class="chat-img-tip">将识别照片中的农产品 / 农药包装后作答</span>
            <el-button text circle size="small" @click="imagePending = ''">✕</el-button>
          </div>
          <div class="chat-input">
            <input ref="fileEl" type="file" accept="image/*" class="hidden-file" @change="onPickImage" />
            <el-tooltip content="拍照或上传图片，AI 先识别再结合批次数据回答" placement="top">
              <el-button class="img-btn" size="large" :disabled="loading" @click="fileEl?.click()">📷</el-button>
            </el-tooltip>
            <el-input v-model="input" placeholder="问我：这箱菜怎么存？什么时候再买？"
                      :disabled="loading" @keyup.enter="send" clearable
                      size="large" />
            <el-button type="primary" size="large" :loading="loading" @click="send">发送</el-button>
          </div>
        </el-card>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { consumer, fetchConsumerMe } from '../../store/auth'
import { chatStream, sourceTypeMap, type ChatMessage, type ChatPayload, type StreamEvent } from '../../components/AiChat.vue'

const route = useRoute()
const batchId = computed<number | null>(() => {
  const v = route.params.batchId
  return v ? Number(v) : null
})

const input = ref<string>('')
const loading = ref<boolean>(false)
const messages = ref<ChatMessage[]>([])
const bodyEl = ref<HTMLElement | null>(null)
const steps = ref<StreamEvent[]>([])
const hasStreaming = computed<boolean>(() => messages.value.some(m => m.streaming))

let sessionId = `s-${Date.now()}-${Math.floor(Math.random() * 100000)}`
let streamIndex = -1

const quickQuestions: string[] = [
  '这箱菜怎么存？',
  '我该什么时候再买？',
  '最近有什么能买的？',
  '我家上次那个西红柿怎么样？'
]

function trustLevelText(lv?: string): string {
  const m: Record<string, string> = {
    FOLLOWER: '关注者',
    TRUSTER: '信任者',
    FAMILY: '家庭用户'
  }
  return m[lv || ''] || '信任者'
}

const nickname = computed<string>(() => consumer.profile?.nickname || '朋友')

/* ===== 开场白 ===== */
const greeting = computed<string>(() => {
  const nick = nickname.value
  const fc = sideStats.value.followCount
  const crops = topCrops.value
  const base = `您好！我是你的消费助手，正在为 ${nick} 服务`
  const parts = [base, `已关注 ${fc} 个农场`]
  if (crops.length) parts.push(`常买 ${crops.join('、')}`)
  return parts.join(' · ')
})

const sideStats = ref<{ followCount: number; buyCount: number }>({ followCount: 0, buyCount: 0 })
const topCrops = ref<string[]>([])

/* ===== 侧栏数据 ===== */
interface SubscriptionRow {
  cropName: string
  nextDate?: string | null
  cycleDays?: number
}
interface EventRow { icon: string; text: string; time: string }
const subscriptions = ref<SubscriptionRow[]>([])
const events = ref<EventRow[]>([])

async function loadSide(): Promise<void> {
  // 尝试加载常购清单和动态（后端接口可能还未开放，静默失败）
  try {
    subscriptions.value = await http.get<SubscriptionRow[]>('/api/consumer/subscriptions')
    sideStats.value.buyCount = subscriptions.value.length
    topCrops.value = subscriptions.value.slice(0, 3).map(s => s.cropName)
  } catch { /* 忽略 */ }
  try {
    const list = await http.get<any[]>('/api/consumer/event?limit=5')
    events.value = (list || []).map(raw => ({
      icon: raw.icon || '📌',
      text: raw.text || raw.content || String(raw),
      time: raw.time || raw.date || ''
    }))
  } catch { /* 忽略 */ }
}

/* ===== 气泡管理 ===== */
function ensureStreamBubble(): ChatMessage {
  if (streamIndex < 0) {
    messages.value.push({ role: 'bot', text: '', tools: [], sources: [], streaming: true })
    streamIndex = messages.value.length - 1
  }
  return messages.value[streamIndex]
}

const scrollBottom = () => nextTick(() => {
  if (bodyEl.value) bodyEl.value.scrollTop = bodyEl.value.scrollHeight
})

function clearChat(): void {
  messages.value = []
  input.value = ''
  steps.value = []
  sessionId = `s-${Date.now()}-${Math.floor(Math.random() * 100000)}`
  streamIndex = -1
}

function sendQuick(q: string): void {
  input.value = q
  send()
}

/* ===== 拍照/上传：canvas 压缩到最长边 1024px，JPEG 0.8 ===== */
const fileEl = ref<HTMLInputElement | null>(null)
const imagePending = ref<string>('')

function onPickImage(e: Event): void {
  const input = e.target as HTMLInputElement
  const file = input.files && input.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    const img = new Image()
    img.onload = () => {
      const max = 1024
      let { width, height } = img
      if (width > max || height > max) {
        const ratio = Math.min(max / width, max / height)
        width = Math.round(width * ratio)
        height = Math.round(height * ratio)
      }
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      canvas.getContext('2d')?.drawImage(img, 0, 0, width, height)
      imagePending.value = canvas.toDataURL('image/jpeg', 0.8)
    }
    img.src = reader.result as string
  }
  reader.readAsDataURL(file)
  input.value = ''
}

async function send(): Promise<void> {
  const q = input.value.trim()
  const img = imagePending.value
  if ((!q && !img) || loading.value) return
  const text = q || '请帮我看看这张照片'
  messages.value.push({ role: 'user', text, image: img || undefined })
  input.value = ''
  imagePending.value = ''
  loading.value = true
  steps.value = []
  streamIndex = -1
  scrollBottom()

  const payload: ChatPayload = {
    batchId: batchId.value,
    question: text,
    sessionId
  }
  if (img) payload.image = img

  try {
    const data = await chatStream(payload, (event) => {
      if (event.type === 'step') {
        steps.value.push(event)
        scrollBottom()
      } else if (event.type === 'delta') {
        ensureStreamBubble().text += event.delta || ''
        scrollBottom()
      } else if (event.type === 'reply') {
        // reply 事件已经返回完整数据，chatStream 会在最终返回完整对象
      }
    })
    if (streamIndex >= 0) {
      const m = messages.value[streamIndex]
      m.text = data.reply || m.text
      m.tools = data.tools || []
      m.sources = data.sources || []
      m.streaming = false
      streamIndex = -1
    } else {
      messages.value.push({
        role: 'bot',
        text: data.reply || '我暂时无法回答这个问题',
        tools: data.tools || [],
        sources: data.sources || []
      })
    }
  } catch {
    if (streamIndex >= 0) {
      messages.value.splice(streamIndex, 1)
      streamIndex = -1
    }
    messages.value.push({ role: 'bot', text: 'AI 服务暂时不可用，请稍后再试' })
  } finally {
    loading.value = false
    steps.value = []
    scrollBottom()
  }
}

onMounted(async () => {
  await fetchConsumerMe()
  await loadSide()
})
</script>

<style scoped>
/* 复用 consumer 公共样式 */
.trace-page { min-height: 100vh; background: #f4f7f4; }
.t-nav {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; background: #fff;
  box-shadow: 0 1px 8px rgba(31, 66, 39, .08);
  position: sticky; top: 0; z-index: 100;
}
.t-brand { font-weight: 700; color: #1b5e20; font-size: 17px; letter-spacing: 1px; }
.brand-ico { width: 22px; height: 22px; vertical-align: -5px; margin-right: 6px; }
.t-links { display: flex; gap: 18px; }
.t-links a { color: #4a674e; text-decoration: none; font-size: 14px; }
.t-links a:hover { color: #2e7d32; }
.t-card { margin-bottom: 16px; }

/* 左右双栏布局 */
.assistant-layout {
  display: grid; grid-template-columns: 280px 1fr; gap: 20px;
  max-width: 1100px; margin: 0 auto; padding: 20px 14px 60px;
  align-items: start;
}
.assistant-side { position: sticky; top: 64px; }
.assistant-main { min-width: 0; }

/* 侧栏头像 */
.side-profile {
  display: flex; gap: 12px; align-items: center;
}
.sp-avatar {
  width: 52px; height: 52px; border-radius: 50%;
  background: linear-gradient(135deg, #43a047, #1b5e20);
  color: #fff; font-size: 26px;
  display: flex; align-items: center; justify-content: center; flex: none;
}
.sp-info { flex: 1; min-width: 0; }
.sp-name { font-size: 15px; font-weight: 700; color: #23422a; margin-bottom: 4px; }
.sp-stats { font-size: 12px; color: #8a9a8e; margin-top: 6px; }

/* 快捷问句 */
.quick-q-list { display: flex; flex-direction: column; gap: 8px; }
.quick-q {
  padding: 10px 12px; background: #f7faf7;
  border: 1px solid #e8f1e8; border-radius: 8px;
  font-size: 13px; color: #23422a; cursor: pointer;
  transition: all .15s;
}
.quick-q:hover { background: #e8f5e9; border-color: #c8e6c9; color: #1b5e20; }

/* 常购清单 */
.sub-list { display: flex; flex-direction: column; gap: 8px; }
.sub-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 10px; background: #f7faf7; border-radius: 6px;
  font-size: 13px; flex-wrap: wrap; gap: 6px;
}
.sub-crop { font-weight: 600; color: #23422a; }
.sub-next { font-size: 12px; color: #2e7d32; background: #e8f5e9; border-radius: 4px; padding: 0 6px; }
.sub-cycle { font-size: 12px; color: #8a9a8e; }

/* 动态 */
.event-list { display: flex; flex-direction: column; gap: 10px; }
.event-item { display: flex; gap: 8px; align-items: flex-start; }
.event-ico { font-size: 16px; flex: none; }
.event-body { flex: 1; min-width: 0; }
.event-text { font-size: 13px; color: #33493a; line-height: 1.5; }
.event-time { font-size: 11px; color: #8a9a8e; margin-top: 2px; }

/* ===== 聊天区 ===== */
.chat-card {
  display: flex; flex-direction: column; height: calc(100vh - 120px);
  min-height: 480px;
}
.chat-card :deep(.el-card__body) { flex: 1; padding: 0; display: flex; flex-direction: column; }
.chat-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.chat-head-btns { display: flex; gap: 6px; align-items: center; }

.chat-body {
  flex: 1; overflow-y: auto; padding: 14px 16px;
  background: #f6f9f6; display: flex; flex-direction: column; gap: 12px;
}
.chat-msg { display: flex; }
.chat-msg.user { justify-content: flex-end; }
.chat-bubble {
  max-width: 78%; padding: 10px 14px; border-radius: 12px;
  font-size: 14px; line-height: 1.7; white-space: pre-wrap; word-break: break-word;
}
.chat-msg.bot .chat-bubble { background: #fff; color: #333; border: 1px solid #e3eee4; }
.chat-msg.user .chat-bubble { background: #2e7d32; color: #fff; }
.chat-cursor {
  display: inline-block; width: 2px; height: 14px; margin-left: 2px;
  background: #2e7d32; vertical-align: -2px; animation: chat-blink .8s steps(1) infinite;
}
@keyframes chat-blink { 50% { opacity: 0; } }
.chat-thinking { color: #999; }
.chat-step { display: flex; align-items: flex-start; gap: 4px; color: #4a674e; font-size: 13px; }
.chat-step-dot { color: #2e7d32; font-weight: 700; }

/* 工具徽章 */
.chat-tools { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; }
.chat-tool-chip {
  font-size: 11px; color: #1b5e20; background: #e8f5e9;
  border: 1px solid #c8e6c9; border-radius: 10px; padding: 1px 8px;
}
/* 依据 */
.chat-sources { margin-top: 8px; padding-top: 8px; border-top: 1px dashed #dcecdc; }
.chat-sources-title { font-size: 11px; color: #7d8f81; margin-bottom: 4px; }
.chat-source-item { font-size: 12px; color: #4a674e; line-height: 1.7; }
.chat-source-type {
  background: #eef5ee; border: 1px solid #d5e8d6; color: #1b5e20;
  border-radius: 4px; padding: 0 4px; margin-right: 4px; font-size: 11px;
}
.chat-source-label { font-weight: 600; }
.chat-source-detail { color: #8a9c8d; margin-left: 4px; }

/* 输入区 */
.chat-input { display: flex; gap: 10px; padding: 12px 14px; border-top: 1px solid #eee; }
.chat-input .el-input { flex: 1; }
.hidden-file { display: none; }
.img-btn { padding: 0 14px; font-size: 18px; }
.chat-img-preview {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 12px; border-top: 1px dashed #e2ece5; background: #f8fbf8;
}
.chat-img-preview img { width: 56px; height: 56px; object-fit: cover; border-radius: 8px; }
.chat-img-tip { font-size: 12.5px; color: #7d8f81; flex: 1; }
.chat-msg-img { max-width: 180px; border-radius: 8px; display: block; margin-bottom: 6px; }

/* 移动端 */
@media (max-width: 768px) {
  .assistant-layout { grid-template-columns: 1fr; padding: 12px 10px 40px; }
  .assistant-side { position: static; order: 2; }
  .assistant-main { order: 1; }
  .chat-card { height: auto; min-height: 60vh; }
  .t-links { gap: 12px; }
}
</style>
