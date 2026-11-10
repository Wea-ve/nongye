<template>
  <div class="ai-chat">
    <!-- 浮窗按钮 -->
    <div v-if="!open" class="ai-fab" @click="open = true">
      <span class="ai-fab-icon">🤖</span>
      <span class="ai-fab-text">AI 问答</span>
    </div>

    <!-- 问答面板 -->
    <transition name="ai-pop">
      <div v-if="open" class="ai-panel">
        <div class="ai-head">
          <span>🛡️ 消费助手问答</span>
          <div class="ai-head-btns">
            <button class="ai-head-btn" title="清空对话" @click="clearChat" v-if="messages.length">🗑️</button>
            <button class="ai-head-btn" title="关闭" @click="open = false">✕</button>
          </div>
        </div>
        <div class="ai-body" ref="bodyEl">
          <div class="ai-msg bot">您好！我是「安溯·知源」的消费助手，可以问我：农残合格吗？打过几次农药？怎么保存？也可以告诉我你常买什么，我提醒你下次该买～</div>
          <div v-for="(m, i) in messages" :key="i" class="ai-msg" :class="m.role === 'user' ? 'user' : 'bot'">
            <div class="ai-bubble">
              <img v-if="m.image" :src="m.image" class="ai-msg-img" @click="preview(m.image)" />
              <div>{{ m.text }}<span v-if="m.streaming" class="ai-cursor"></span></div>
              <!-- 本轮调用的工具徽章 -->
              <div v-if="m.tools && m.tools.length" class="ai-tools">
                <span v-for="(t, ti) in m.tools" :key="ti" class="ai-tool-chip" :title="t.summary">
                  🔧 {{ t.label }}
                </span>
              </div>
              <!-- 答案依据：让回答可核验 -->
              <div v-if="m.sources && m.sources.length" class="ai-sources">
                <div class="ai-sources-title">📎 答案依据</div>
                <div v-for="(s, si) in m.sources" :key="si" class="ai-source-item">
                  <span class="ai-source-type">{{ sourceTypeMap[s.type] || '资料' }}</span>
                  <span class="ai-source-label">{{ s.label }}</span>
                  <span v-if="s.detail" class="ai-source-detail">{{ s.detail }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-if="loading && !hasStreaming" class="ai-msg bot">
            <div class="ai-bubble typing">
              <div v-if="!steps.length">正在思考…</div>
              <div v-for="(s, si) in steps" :key="si" class="ai-step">
                <span class="ai-step-dot">✓</span>{{ s.summary }}
              </div>
            </div>
          </div>
        </div>
        <!-- 待发送图片预览 -->
        <div v-if="imagePending" class="ai-img-preview">
          <img :src="imagePending" />
          <el-button text circle size="small" class="ai-img-remove" @click="imagePending = ''">✕</el-button>
        </div>
        <div class="ai-input">
          <input ref="fileEl" type="file" accept="image/*" class="ai-file" @change="onPickImage" />
          <button class="ai-cam" title="上传照片提问" @click="fileEl && fileEl.click()" :disabled="loading">📷</button>
          <el-input v-model="input" placeholder="输入问题，回车发送" :disabled="loading"
                    @keyup.enter="send" clearable />
          <el-button type="primary" :loading="loading" @click="send">发送</el-button>
        </div>
      </div>
    </transition>

    <!-- 图片放大预览 -->
    <div v-if="previewUrl" class="ai-preview-mask" @click="previewUrl = ''">
      <img :src="previewUrl" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import http from '../api/http'

interface ToolBadge {
  label: string
  summary?: string
}

interface SourceItem {
  type: string
  label: string
  detail?: string
  score?: number
}

interface ChatMessage {
  role: 'user' | 'bot'
  text: string
  image?: string
  tools?: ToolBadge[]
  sources?: SourceItem[]
  streaming?: boolean
}

interface ChatReply {
  reply: string
  tools?: ToolBadge[]
  sources?: SourceItem[]
}

interface ChatPayload {
  batchId?: number
  question: string
  sessionId: string
  image?: string
}

interface StreamEvent {
  type: 'step' | 'delta' | 'reply'
  label?: string
  summary?: string
  delta?: string
  reply?: string
  tools?: ToolBadge[]
  sources?: SourceItem[]
}

const props = defineProps<{ batchId?: number }>()

const open = ref<boolean>(false)
const input = ref<string>('')
const loading = ref<boolean>(false)
const messages = ref<ChatMessage[]>([])
const bodyEl = ref<HTMLElement | null>(null)
const fileEl = ref<HTMLInputElement | null>(null)
const imagePending = ref<string>('')   // 待发送图片 data URL
const previewUrl = ref<string>('')     // 放大预览
const steps = ref<StreamEvent[]>([])   // 实时工具/识别步骤
const hasStreaming = computed<boolean>(() => messages.value.some(m => m.streaming))

/** 依据类型 → 展示文案 */
const sourceTypeMap: Record<string, string> = {
  data: '批次数据',
  record: '农事记录',
  report: '检测报告',
  trace: '流转记录',
  knowledge: '知识库',
  image: '图片识别',
  basket: '我的常购',
  history: '我的历史',
  recommend: '当季推荐'
}

// 会话 ID：页面内保持多轮上下文
let sessionId = `s-${Date.now()}-${Math.floor(Math.random() * 100000)}`

/** 清空对话：重置消息列表并换新会话，后端上下文随之作废 */
function clearChat() {
  messages.value = []
  input.value = ''
  imagePending.value = ''
  sessionId = `s-${Date.now()}-${Math.floor(Math.random() * 100000)}`
}

const scrollBottom = () => nextTick(() => {
  if (bodyEl.value) bodyEl.value.scrollTop = bodyEl.value.scrollHeight
})

/** 选择图片：canvas 压缩到最长边 1024px、JPEG 0.8，控制请求体积 */
function onPickImage(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  target.value = ''
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    const img = new Image()
    img.onload = () => {
      const max = 1024
      let { width, height } = img
      if (width > max || height > max) {
        const scale = max / Math.max(width, height)
        width = Math.round(width * scale)
        height = Math.round(height * scale)
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
}

/** 当前正在逐字输出的气泡索引；-1 表示尚未创建 */
let streamIndex = -1

/** 首个 delta 到达时才创建回答气泡，避免出现空气泡 */
function ensureStreamBubble(): ChatMessage {
  if (streamIndex < 0) {
    messages.value.push({ role: 'bot', text: '', tools: [], sources: [], streaming: true })
    streamIndex = messages.value.length - 1
  }
  return messages.value[streamIndex]
}

async function send(): Promise<void> {
  const q = input.value.trim()
  const img = imagePending.value
  if ((!q && !img) || loading.value) return
  messages.value.push({ role: 'user', text: q || '请帮我看看这张照片', image: img })
  input.value = ''
  imagePending.value = ''
  loading.value = true
  steps.value = []
  streamIndex = -1
  scrollBottom()
  const payload: ChatPayload = { batchId: props.batchId, question: q || '请分析这张照片', sessionId, image: img }
  try {
    const data = await chatStream(payload)
    if (streamIndex >= 0) {
      // 流式气泡已展示部分内容，用最终结果补全工具与依据
      const m = messages.value[streamIndex]
      m.text = data.reply || m.text
      m.tools = data.tools || []
      m.sources = data.sources || []
      m.streaming = false
      streamIndex = -1
    } else {
      messages.value.push({
        role: 'bot',
        text: data.reply || 'AI 暂时无法回答',
        tools: data.tools || [],
        sources: data.sources || []
      })
    }
  } catch {
    // 流式中断：丢弃半截气泡，退回普通接口重试
    if (streamIndex >= 0) {
      messages.value.splice(streamIndex, 1)
      streamIndex = -1
    }
    try {
      const data = await http.post<ChatReply>('/api/ai/chat', payload, { timeout: 90000 })
      messages.value.push({
        role: 'bot',
        text: data.reply || 'AI 暂时无法回答',
        tools: data.tools || [],
        sources: data.sources || []
      })
    } catch {
      messages.value.push({ role: 'bot', text: 'AI 服务暂时不可用，请稍后再试', tools: [] })
    }
  } finally {
    loading.value = false
    steps.value = []
    scrollBottom()
  }
}

/** 读取 SSE 流：step 步骤 / delta 逐字文本 / reply 最终结果 */
async function chatStream(payload: ChatPayload): Promise<ChatReply> {
  const resp = await fetch('/api/ai/chat/stream', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify(payload)
  })
  if (!resp.ok || !resp.body) throw new Error('stream unavailable')

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let finalData: ChatReply | null = null
  for (;;) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const chunks = buffer.split('\n\n')
    buffer = chunks.pop() || ''
    for (const chunk of chunks) {
      const dataLine = chunk.split('\n').find((l: string) => l.startsWith('data:'))
      if (!dataLine) continue
      let event: StreamEvent
      try {
        event = JSON.parse(dataLine.slice(5).trim()) as StreamEvent
      } catch {
        continue
      }
      if (event.type === 'step') {
        steps.value.push(event)
        scrollBottom()
      } else if (event.type === 'delta') {
        ensureStreamBubble().text += event.delta || ''
        scrollBottom()
      } else if (event.type === 'reply') {
        finalData = { reply: event.reply || '', tools: event.tools, sources: event.sources }
      }
    }
  }
  if (!finalData) throw new Error('no reply')
  return finalData
}

function preview(url: string): void { previewUrl.value = url }
</script>

<script lang="ts">
/**
 * 对外导出：类型 + 纯 SSE 读取函数
 * AssistantView 等页面可以复用这套对话流，无需依赖 AiChat 浮窗组件
 */

export interface ToolBadge {
  label: string
  summary?: string
}

export interface SourceItem {
  type: string
  label: string
  detail?: string
  score?: number
}

export interface ChatMessage {
  role: 'user' | 'bot'
  text: string
  image?: string
  tools?: ToolBadge[]
  sources?: SourceItem[]
  streaming?: boolean
}

export interface ChatReply {
  reply: string
  tools?: ToolBadge[]
  sources?: SourceItem[]
}

export interface ChatPayload {
  batchId?: number | null
  question: string
  sessionId: string
  image?: string
}

export interface StreamEvent {
  type: 'step' | 'delta' | 'reply'
  label?: string
  summary?: string
  delta?: string
  reply?: string
  tools?: ToolBadge[]
  sources?: SourceItem[]
}

/** 依据类型 → 展示文案映射（可在外部复用） */
export const sourceTypeMap: Record<string, string> = {
  data: '批次数据',
  record: '农事记录',
  report: '检测报告',
  trace: '流转记录',
  knowledge: '知识库',
  image: '图片识别'
}

/**
 * 流式对话：纯函数，不依赖组件状态
 * @param payload 提问参数
 * @param onEvent 每个 SSE 事件回调（step / delta / reply 都会触发）
 * @returns 最终完整回复（包含最终工具徽章和依据）
 */
export async function chatStream(
  payload: ChatPayload,
  onEvent?: (event: StreamEvent) => void
): Promise<ChatReply> {
  const resp = await fetch('/api/ai/chat/stream', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify(payload)
  })
  if (!resp.ok || !resp.body) throw new Error('stream unavailable')

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let finalData: ChatReply | null = null
  for (;;) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const chunks = buffer.split('\n\n')
    buffer = chunks.pop() || ''
    for (const chunk of chunks) {
      const dataLine = chunk.split('\n').find((l: string) => l.startsWith('data:'))
      if (!dataLine) continue
      let event: StreamEvent
      try {
        event = JSON.parse(dataLine.slice(5).trim()) as StreamEvent
      } catch {
        continue
      }
      if (event.type === 'step') {
        onEvent?.(event)
      } else if (event.type === 'delta') {
        onEvent?.(event)
      } else if (event.type === 'reply') {
        finalData = { reply: event.reply || '', tools: event.tools, sources: event.sources }
        onEvent?.(event)
      }
    }
  }
  if (!finalData) throw new Error('no reply')
  return finalData
}
</script>

<style scoped>
.ai-chat { position: fixed; right: 18px; bottom: 18px; z-index: 2000; }
.ai-fab {
  display: flex; align-items: center; gap: 6px;
  background: linear-gradient(135deg, #43a047, #1b5e20);
  color: #fff; padding: 10px 18px; border-radius: 28px;
  cursor: pointer; box-shadow: 0 6px 18px rgba(27, 94, 32, .4);
  transition: transform .2s;
}
.ai-fab:hover { transform: translateY(-2px); }
.ai-fab-icon { font-size: 18px; }
.ai-panel {
  width: 340px; max-width: calc(100vw - 36px); height: 480px;
  background: #fff; border-radius: 14px; overflow: hidden;
  box-shadow: 0 12px 40px rgba(0, 0, 0, .18);
  display: flex; flex-direction: column;
}
.ai-head {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 14px; color: #fff; font-weight: 600;
  background: linear-gradient(135deg, #43a047, #1b5e20);
}
.ai-head-btns { display: flex; align-items: center; gap: 2px; }
.ai-head-btn {
  border: none; background: transparent; color: #fff; cursor: pointer;
  font-size: 13px; padding: 2px 6px; border-radius: 6px; opacity: .85;
}
.ai-head-btn:hover { background: rgba(255,255,255,.18); opacity: 1; }
.ai-body { flex: 1; overflow-y: auto; padding: 12px; background: #f6f9f6; }
.ai-msg { display: flex; margin-bottom: 10px; }
.ai-msg.user { justify-content: flex-end; }
.ai-bubble {
  max-width: 80%; padding: 8px 12px; border-radius: 10px;
  font-size: 13px; line-height: 1.6; white-space: pre-wrap; word-break: break-word;
}
.ai-msg.bot .ai-bubble { background: #fff; color: #333; border: 1px solid #e3eee4; }
.ai-msg.user .ai-bubble { background: #2e7d32; color: #fff; }
.typing { color: #999; }
.ai-cursor {
  display: inline-block; width: 2px; height: 12px; margin-left: 2px;
  background: #2e7d32; vertical-align: -1px; animation: ai-blink .8s steps(1) infinite;
}
@keyframes ai-blink { 50% { opacity: 0; } }
.ai-msg-img { width: 140px; border-radius: 8px; margin-bottom: 6px; display: block; cursor: zoom-in; }
.ai-tools { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; }
.ai-tool-chip {
  font-size: 11px; color: #1b5e20; background: #e8f5e9;
  border: 1px solid #c8e6c9; border-radius: 10px; padding: 1px 8px;
}
.ai-step { display: flex; align-items: flex-start; gap: 4px; color: #4a674e; }
.ai-step-dot { color: #2e7d32; font-weight: 700; }
.ai-sources { margin-top: 8px; padding-top: 8px; border-top: 1px dashed #dcecdc; }
.ai-sources-title { font-size: 11px; color: #7d8f81; margin-bottom: 4px; }
.ai-source-item { font-size: 11px; color: #4a674e; line-height: 1.6; }
.ai-source-type {
  background: #eef5ee; border: 1px solid #d5e8d6; color: #1b5e20;
  border-radius: 4px; padding: 0 4px; margin-right: 4px;
}
.ai-source-label { font-weight: 600; }
.ai-source-detail { color: #8a9c8d; margin-left: 4px; }
.ai-img-preview {
  position: relative; margin: 8px 12px 0;
  border: 1px solid #e3eee4; border-radius: 8px; width: 72px; height: 72px; overflow: hidden;
}
.ai-img-preview img { width: 100%; height: 100%; object-fit: cover; }
.ai-img-remove {
  position: absolute; top: 0; right: 0; background: rgba(0,0,0,.45) !important; color: #fff;
}
.ai-input { display: flex; gap: 8px; padding: 10px; border-top: 1px solid #eee; }
.ai-file { display: none; }
.ai-cam {
  border: 1px solid #dcdfe6; background: #fff; border-radius: 6px;
  font-size: 16px; cursor: pointer; padding: 0 10px;
}
.ai-cam:disabled { opacity: .5; cursor: not-allowed; }
.ai-preview-mask {
  position: fixed; inset: 0; z-index: 3000; background: rgba(0,0,0,.7);
  display: flex; align-items: center; justify-content: center; cursor: zoom-out;
}
.ai-preview-mask img { max-width: 90vw; max-height: 90vh; border-radius: 8px; }
.ai-pop-enter-active, .ai-pop-leave-active { transition: all .22s ease; }
.ai-pop-enter-from, .ai-pop-leave-to { opacity: 0; transform: translateY(12px) scale(.96); }
</style>
