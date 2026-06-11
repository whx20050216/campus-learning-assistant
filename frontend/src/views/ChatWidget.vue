<template>
  <div v-if="showButton" class="chat-float-btn" @click="togglePanel">
    <el-icon :size="28"><ChatDotRound /></el-icon>
  </div>

  <Transition name="chat-slide">
    <div v-if="visible" class="chat-panel">
      <!-- 顶部栏 -->
      <div class="chat-header">
        <div class="chat-header-left">
          <el-icon :size="20"><ChatDotRound /></el-icon>
          <span>AI 学习助手</span>
        </div>
        <div class="chat-header-right">
          <el-button size="small" text @click="createNewSession">
            <el-icon><Plus /></el-icon> 新对话
          </el-button>
          <el-button size="small" text @click="togglePanel">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>

      <div class="chat-body">
        <!-- 左侧会话列表 -->
        <div class="chat-sidebar" :class="{ collapsed: sidebarCollapsed }">
          <div class="session-list">
            <div
              v-for="s in sessions"
              :key="s.id"
              class="session-item"
              :class="{ active: s.id === currentSessionId }"
              @click="switchSession(s.id)"
            >
              <div class="session-title">{{ s.title }}</div>
              <el-icon class="session-delete" @click.stop="handleDeleteSession(s.id)"><Delete /></el-icon>
            </div>
          </div>
          <div class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
            <el-icon><ArrowLeft v-if="!sidebarCollapsed" /><ArrowRight v-else /></el-icon>
          </div>
        </div>

        <!-- 右侧消息区 -->
        <div class="chat-main">
          <div ref="msgListRef" class="message-list">
            <div v-if="messages.length === 0" class="chat-welcome">
              <el-icon :size="48" color="var(--primary-300)"><ChatDotRound /></el-icon>
              <p>我是你的 AI 学习助手</p>
              <p class="chat-welcome-tip">可以问我关于学习资料的问题</p>
            </div>

            <div
              v-for="(msg, idx) in messages"
              :key="idx"
              class="message-item"
              :class="msg.role"
            >
              <div class="message-bubble">{{ msg.content }}</div>
            </div>

            <!-- 流式输出中的消息 -->
            <div v-if="isStreaming" class="message-item assistant">
              <div class="message-bubble streaming">
                {{ streamingContent }}<span class="cursor">|</span>
              </div>
            </div>
          </div>

          <div class="chat-input-area">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="2"
              placeholder="输入问题，按 Enter 发送..."
              :disabled="isStreaming"
              @keydown.enter.prevent="sendMessage"
            />
            <el-button
              type="primary"
              :disabled="!inputMessage.trim() || isStreaming"
              @click="sendMessage"
            >
              <el-icon><Promotion /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound,
  Plus,
  Close,
  Delete,
  ArrowLeft,
  ArrowRight,
  Promotion,
} from '@element-plus/icons-vue'
import {
  getSessions,
  getMessages,
  deleteSession,
  createSession,
  createStreamUrl,
  type ChatSessionVO,
  type ChatMessageVO,
} from '@/api/chat'

const visible = ref(false)
const sidebarCollapsed = ref(false)
const sessions = ref<ChatSessionVO[]>([])
const currentSessionId = ref<number | undefined>(undefined)
const messages = ref<ChatMessageVO[]>([])
const inputMessage = ref('')
const isStreaming = ref(false)
const streamingContent = ref('')
const msgListRef = ref<HTMLDivElement | null>(null)

const showButton = computed(() => {
  const token = localStorage.getItem('token')
  return !!token
})

function togglePanel() {
  visible.value = !visible.value
  if (visible.value) {
    loadSessions()
  }
}

async function loadSessions() {
  try {
    const res = await getSessions()
    sessions.value = res.data || []
  } catch {
    // ignore
  }
}

async function switchSession(id: number) {
  currentSessionId.value = id
  try {
    const res = await getMessages(id)
    messages.value = res.data || []
    await nextTick()
    scrollToBottom()
  } catch {
    ElMessage.error('加载消息失败')
  }
}

async function createNewSession() {
  try {
    const res = await createSession('新对话')
    const session = res.data
    sessions.value.unshift(session)
    currentSessionId.value = session.id
    messages.value = []
  } catch {
    ElMessage.error('创建会话失败')
  }
}

async function handleDeleteSession(id: number) {
  try {
    await deleteSession(id)
    sessions.value = sessions.value.filter((s) => s.id !== id)
    if (currentSessionId.value === id) {
      currentSessionId.value = undefined
      messages.value = []
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

function sendMessage() {
  const text = inputMessage.value.trim()
  if (!text || isStreaming.value) return

  // 如果没有当前会话，先创建一个
  if (!currentSessionId.value) {
    createSession(text.length > 20 ? text.substring(0, 20) + '...' : text)
      .then((res) => {
        const session = res.data
        sessions.value.unshift(session)
        currentSessionId.value = session.id
        doSend(text)
      })
      .catch(() => ElMessage.error('创建会话失败'))
    return
  }

  doSend(text)
}

function doSend(text: string) {
  // 先显示用户消息
  messages.value.push({
    id: 0,
    sessionId: currentSessionId.value || 0,
    role: 'user',
    content: text,
    createdAt: new Date().toISOString(),
  })
  inputMessage.value = ''
  isStreaming.value = true
  streamingContent.value = ''
  nextTick().then(scrollToBottom)

  // SSE 连接
  const url = createStreamUrl(text, currentSessionId.value)
  const es = new EventSource(url)

  es.addEventListener('message', (e) => {
    try {
      const data = JSON.parse(e.data)
      if (data.content) {
        streamingContent.value += data.content
        nextTick().then(scrollToBottom)
      }
    } catch {
      // ignore parse error
    }
  })

  es.addEventListener('done', () => {
    es.close()
    isStreaming.value = false
    if (streamingContent.value) {
      messages.value.push({
        id: 0,
        sessionId: currentSessionId.value || 0,
        role: 'assistant',
        content: streamingContent.value,
        createdAt: new Date().toISOString(),
      })
    }
    streamingContent.value = ''
    nextTick().then(scrollToBottom)
  })

  es.addEventListener('error', (e: any) => {
    es.close()
    isStreaming.value = false
    const data = e.data || '问答服务异常'
    messages.value.push({
      id: 0,
      sessionId: currentSessionId.value || 0,
      role: 'assistant',
      content: '⚠️ ' + data,
      createdAt: new Date().toISOString(),
    })
    streamingContent.value = ''
    nextTick().then(scrollToBottom)
  })

  es.onerror = () => {
    es.close()
    isStreaming.value = false
    streamingContent.value = ''
  }
}

function scrollToBottom() {
  const el = msgListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

onMounted(() => {
  if (showButton.value) loadSessions()
})
</script>

<style scoped>
.chat-float-btn {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-500), var(--primary-600));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4);
  z-index: 2000;
  transition: all 0.3s ease;
}
.chat-float-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 24px rgba(59, 130, 246, 0.5);
}

.chat-panel {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 520px;
  background: var(--bg-card);
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.1);
  z-index: 2001;
  display: flex;
  flex-direction: column;
}

.chat-slide-enter-active,
.chat-slide-leave-active {
  transition: transform 0.3s ease;
}
.chat-slide-enter-from,
.chat-slide-leave-to {
  transform: translateX(100%);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-card);
}
.chat-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-weight: 600;
  font-size: 16px;
  color: var(--text-primary);
}
.chat-header-right {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.chat-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.chat-sidebar {
  width: 180px;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width 0.2s;
}
.chat-sidebar.collapsed {
  width: 36px;
}
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-2);
}
.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  color: var(--text-secondary);
  transition: all 0.2s;
  margin-bottom: var(--space-1);
}
.session-item:hover {
  background: var(--bg-hover);
}
.session-item.active {
  background: var(--primary-50);
  color: var(--primary-600);
  font-weight: 500;
}
.session-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.session-delete {
  opacity: 0;
  font-size: 12px;
  color: var(--danger-500);
  cursor: pointer;
}
.session-item:hover .session-delete {
  opacity: 1;
}
.sidebar-toggle {
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid var(--border-color);
  cursor: pointer;
  color: var(--text-tertiary);
  font-size: 12px;
}
.sidebar-toggle:hover {
  background: var(--bg-hover);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.chat-welcome {
  text-align: center;
  padding: var(--space-12) var(--space-4);
  color: var(--text-secondary);
}
.chat-welcome p {
  margin: var(--space-2) 0 0;
  font-size: 15px;
  font-weight: 500;
}
.chat-welcome-tip {
  font-size: 13px;
  color: var(--text-tertiary);
  font-weight: 400;
}

.message-item {
  display: flex;
}
.message-item.user {
  justify-content: flex-end;
}
.message-item.assistant {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 80%;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.message-item.user .message-bubble {
  background: var(--primary-500);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.message-item.assistant .message-bubble {
  background: var(--bg-hover);
  color: var(--text-primary);
  border-bottom-left-radius: 4px;
}

.streaming .cursor {
  animation: blink 1s infinite;
  color: var(--primary-500);
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.chat-input-area {
  display: flex;
  align-items: flex-end;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  border-top: 1px solid var(--border-color);
  background: var(--bg-card);
}
.chat-input-area :deep(.el-textarea__inner) {
  resize: none;
}

@media (max-width: 768px) {
  .chat-panel {
    width: 100%;
  }
  .chat-sidebar {
    display: none;
  }
}
</style>
