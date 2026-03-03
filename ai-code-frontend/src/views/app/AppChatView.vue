<template>
  <div id="appChatView">
    <div class="chat-header">
      <div class="header-left">
        <a-button type="text" @click="goBack">
          <arrow-left-outlined />
        </a-button>
        <h2 class="app-name">{{ appInfo?.appName || '未命名应用' }}</h2>
      </div>
      <div class="header-right">
        <a-button type="primary" :loading="deploying" @click="handleDeploy">
          <cloud-upload-outlined />
          部署应用
        </a-button>
      </div>
    </div>

    <div class="chat-container">
      <div class="chat-main">
        <div class="message-list" ref="messageListRef">
          <div
            v-for="(msg, index) in messageList"
            :key="index"
            :class="['message-item', msg.role]"
          >
            <div class="message-avatar">
              <a-avatar v-if="msg.role === 'user'" :src="loginUserStore.loginUser.userAvatar">
                {{ loginUserStore.loginUser.userName?.charAt(0) }}
              </a-avatar>
              <a-avatar v-else style="background-color: #1890ff">
                <robot-outlined />
              </a-avatar>
            </div>
            <div class="message-content">
              <div class="message-text" v-html="renderMarkdown(msg.content)"></div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <a-textarea
            v-model:value="userInput"
            placeholder="输入你的需求，AI 将继续优化网站..."
            :auto-size="{ minRows: 2, maxRows: 4 }"
            :disabled="generating"
            @pressEnter="handleSendMessage"
          />
          <a-button
            type="primary"
            :loading="generating"
            :disabled="!userInput.trim()"
            @click="handleSendMessage"
          >
            发送
          </a-button>
        </div>
      </div>

      <div class="preview-area" v-if="showPreview">
        <div class="preview-header">
          <span>网站预览</span>
          <div class="preview-actions">
            <a-button type="link" size="small" @click="refreshPreview">
              <reload-outlined />
              刷新
            </a-button>
            <a-button type="link" :href="previewUrl" target="_blank" size="small">
              <fullscreen-outlined />
              新窗口打开
            </a-button>
          </div>
        </div>
        <iframe ref="previewIframe" :src="previewUrl" class="preview-iframe" frameborder="0"></iframe>
      </div>
    </div>

    <a-modal
      v-model:open="deployModalVisible"
      title="部署成功"
      :footer="null"
    >
      <div class="deploy-result">
        <a-result status="success" title="应用部署成功">
          <template #extra>
            <div class="deploy-url">
              <p>访问地址：</p>
              <a-input-group compact>
                <a-input :value="deployUrl" style="width: calc(100% - 80px)" readonly />
                <a-button type="primary" @click="copyDeployUrl">复制链接</a-button>
              </a-input-group>
            </div>
            <a-button type="primary" :href="deployUrl" target="_blank" style="margin-top: 16px">
              访问网站
            </a-button>
          </template>
        </a-result>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  CloudUploadOutlined,
  RobotOutlined,
  FullscreenOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'
import { getAppVoById, deployApp } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import { marked } from 'marked'
import hljs from 'highlight.js'

const renderer = new marked.Renderer()
renderer.code = function ({ text, lang }: { text: string; lang?: string }) {
  let highlighted: string
  if (lang && hljs.getLanguage(lang)) {
    highlighted = hljs.highlight(text, { language: lang }).value
  } else {
    highlighted = hljs.highlightAuto(text).value
  }
  return `<pre><code class="hljs">${highlighted}</code></pre>`
}

marked.setOptions({
  renderer,
  breaks: true,
  gfm: true,
})

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appId = route.params.id as string
const appInfo = ref<API.AppVO>()
const messageList = ref<ChatMessage[]>([])
const userInput = ref('')
const generating = ref(false)
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')
const messageListRef = ref<HTMLElement>()
const previewIframe = ref<HTMLIFrameElement>()
const showPreview = ref(false)

const previewUrl = ref('')
let currentAiMessageIndex = -1

const fetchAppInfo = async () => {
  const res = await getAppVoById({ id: appId })
  if (res.data.code === 0 && res.data.data) {
    appInfo.value = res.data.data
    if (appInfo.value?.initPrompt) {
      messageList.value.push({
        role: 'user',
        content: appInfo.value.initPrompt,
      })
      startChat(appInfo.value.initPrompt)
    }
  } else {
    message.error('获取应用信息失败：' + res.data.message)
  }
}

const parseSseData = (rawData: string): string => {
  try {
    const json = JSON.parse(rawData)
    if (json.d !== undefined) {
      return json.d
    }
    return rawData
  } catch {
    return rawData
  }
}

const startChat = async (prompt: string) => {
  generating.value = true
  showPreview.value = true
  previewUrl.value = `http://localhost:8123/api/static/${appInfo.value?.codeGenType}_${appId}/`

  messageList.value.push({
    role: 'assistant',
    content: '',
  })
  currentAiMessageIndex = messageList.value.length - 1
  scrollToBottom()

  try {
    const eventSource = new EventSource(
      `http://localhost:8123/api/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(prompt)}`,
      { withCredentials: true }
    )

    eventSource.addEventListener('end', () => {
      eventSource.close()
      generating.value = false
      if (currentAiMessageIndex >= 0 && !messageList.value[currentAiMessageIndex].content) {
        messageList.value[currentAiMessageIndex].content = '网站已生成完成！'
      }
      refreshPreview()
      scrollToBottom()
    })

    eventSource.onmessage = (event) => {
      const data = event.data
      const text = parseSseData(data)
      if (currentAiMessageIndex >= 0) {
        messageList.value[currentAiMessageIndex].content += text
      }
      scrollToBottom()
    }

    eventSource.onerror = () => {
      eventSource.close()
      generating.value = false
      if (currentAiMessageIndex >= 0 && !messageList.value[currentAiMessageIndex].content) {
        messageList.value[currentAiMessageIndex].content = '生成过程中发生错误'
      }
      message.error('生成过程中发生错误')
    }
  } catch {
    generating.value = false
    message.error('生成失败')
  }
}

const handleSendMessage = () => {
  if (!userInput.value.trim() || generating.value) return

  const prompt = userInput.value.trim()
  messageList.value.push({
    role: 'user',
    content: prompt,
  })
  userInput.value = ''
  scrollToBottom()
  startChat(prompt)
}

const handleDeploy = async () => {
  deploying.value = true
  try {
    const res = await deployApp({ appId })
    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } finally {
    deploying.value = false
  }
}

const copyDeployUrl = () => {
  navigator.clipboard.writeText(deployUrl.value)
  message.success('链接已复制到剪贴板')
}

const renderMarkdown = (content: string): string => {
  if (!content) return ''
  return marked.parse(content) as string
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const refreshPreview = () => {
  if (previewIframe.value) {
    previewIframe.value.src = previewUrl.value
  }
}

const goBack = () => {
  router.push('/')
}

onMounted(() => {
  fetchAppInfo()
})
</script>

<style scoped>
@import 'highlight.js/styles/github.css';

#appChatView {
  height: calc(100vh - 64px - 56px - 32px);
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  margin: -24px;
  padding: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-name {
  margin: 0;
  font-size: 18px;
}

.chat-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 60%;
  border-right: 1px solid #e8e8e8;
  background: #fff;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-item.user .message-content {
  background: #1890ff;
  color: #fff;
  border-radius: 12px 12px 0 12px;
}

.message-item.assistant .message-content {
  background: #f0f0f0;
  border-radius: 12px 12px 12px 0;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
}

.message-text {
  word-break: break-word;
  line-height: 1.6;
}

.message-text :deep(pre) {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
}

.message-text :deep(code) {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.message-text :deep(p) {
  margin: 8px 0;
}

.message-text :deep(p:first-child) {
  margin-top: 0;
}

.message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.input-area {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-top: 1px solid #e8e8e8;
  background: #fafafa;
}

.input-area .ant-input {
  flex: 1;
}

.preview-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
  font-weight: 500;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-iframe {
  flex: 1;
  width: 100%;
  height: 100%;
}

.deploy-result {
  text-align: center;
}

.deploy-url {
  margin: 16px 0;
}
</style>
