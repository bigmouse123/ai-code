<template>
  <div id="homeView">
    <div class="hero-section">
      <h1 class="site-title">AI 应用生成平台</h1>
      <p class="site-subtitle">通过 AI 对话，快速生成你的网站应用</p>
      <div class="prompt-input-wrapper">
        <a-textarea
          v-model:value="promptInput"
          placeholder="输入你想要生成的网站描述，例如：帮我生成一个个人博客网站..."
          :auto-size="{ minRows: 3, maxRows: 6 }"
          @pressEnter="handleCreateApp"
        />
        <a-button type="primary" size="large" :loading="creating" @click="handleCreateApp">
          创建应用
        </a-button>
      </div>
    </div>

    <a-divider />

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="my" tab="我的应用">
        <div class="search-bar">
          <a-input-search
            v-model:value="mySearchParams.appName"
            placeholder="搜索应用名称"
            enter-button
            @search="searchMyApps"
          />
        </div>
        <a-list
          :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 3, xl: 4, xxl: 4 }"
          :data-source="myAppList"
          :loading="myLoading"
          :pagination="myPagination"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-card hoverable @click="goToAppChat(item.id)">
                <template #cover>
                  <img
                    :alt="item.appName"
                    :src="item.cover || defaultCover"
                    class="app-cover"
                  />
                </template>
                <template #actions>
                  <edit-outlined @click.stop="goToEditApp(item.id)" />
                  <delete-outlined @click.stop="handleDeleteApp(item.id)" />
                </template>
                <a-card-meta :title="item.appName || '未命名应用'">
                  <template #description>
                    <div class="app-description">
                      {{ item.initPrompt?.substring(0, 50) || '暂无描述' }}...
                    </div>
                    <div class="app-time">
                      {{ dayjs(item.createTime).format('YYYY-MM-DD HH:mm') }}
                    </div>
                  </template>
                </a-card-meta>
              </a-card>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>

      <a-tab-pane key="good" tab="精选应用">
        <div class="search-bar">
          <a-input-search
            v-model:value="goodSearchParams.appName"
            placeholder="搜索应用名称"
            enter-button
            @search="searchGoodApps"
          />
        </div>
        <a-list
          :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 3, xl: 4, xxl: 4 }"
          :data-source="goodAppList"
          :loading="goodLoading"
          :pagination="goodPagination"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-card hoverable @click="goToAppChat(item.id)">
                <template #cover>
                  <img
                    :alt="item.appName"
                    :src="item.cover || defaultCover"
                    class="app-cover"
                  />
                </template>
                <a-card-meta :title="item.appName || '未命名应用'">
                  <template #description>
                    <div class="app-description">
                      {{ item.initPrompt?.substring(0, 50) || '暂无描述' }}...
                    </div>
                    <div class="app-author">
                      作者：{{ item.user?.userName || '未知' }}
                    </div>
                  </template>
                </a-card-meta>
              </a-card>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import {
  addApp,
  deleteApp,
  listMyAppVoByPage,
  listGoodAppVoByPage,
} from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const defaultCover = 'https://via.placeholder.com/300x200?text=AI+App'

const promptInput = ref('')
const creating = ref(false)
const activeTab = ref('my')

const myAppList = ref<API.AppVO[]>([])
const myTotal = ref<number | string>(0)
const myLoading = ref(false)
const mySearchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 20,
})

const goodAppList = ref<API.AppVO[]>([])
const goodTotal = ref<number | string>(0)
const goodLoading = ref(false)
const goodSearchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 20,
})

const myPagination = computed(() => ({
  current: mySearchParams.pageNum ?? 1,
  pageSize: mySearchParams.pageSize ?? 20,
  total: myTotal.value,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
  onChange: (page: number, pageSize: number) => {
    mySearchParams.pageNum = page
    mySearchParams.pageSize = pageSize
    fetchMyApps()
  },
}))

const goodPagination = computed(() => ({
  current: goodSearchParams.pageNum ?? 1,
  pageSize: goodSearchParams.pageSize ?? 20,
  total: goodTotal.value,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
  onChange: (page: number, pageSize: number) => {
    goodSearchParams.pageNum = page
    goodSearchParams.pageSize = pageSize
    fetchGoodApps()
  },
}))

const fetchMyApps = async () => {
  myLoading.value = true
  try {
    const res = await listMyAppVoByPage(mySearchParams)
    if (res.data.code === 0 && res.data.data) {
      myAppList.value = res.data.data.records ?? []
      myTotal.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取我的应用失败：' + res.data.message)
    }
  } finally {
    myLoading.value = false
  }
}

const fetchGoodApps = async () => {
  goodLoading.value = true
  try {
    const res = await listGoodAppVoByPage(goodSearchParams)
    if (res.data.code === 0 && res.data.data) {
      goodAppList.value = res.data.data.records ?? []
      goodTotal.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取精选应用失败：' + res.data.message)
    }
  } finally {
    goodLoading.value = false
  }
}

const searchMyApps = () => {
  mySearchParams.pageNum = 1
  fetchMyApps()
}

const searchGoodApps = () => {
  goodSearchParams.pageNum = 1
  fetchGoodApps()
}

const handleCreateApp = async () => {
  if (!promptInput.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({ initPrompt: promptInput.value })
    if (res.data.code === 0 && res.data.data) {
      message.success('创建应用成功')
      router.push(`/app/chat/${res.data.data}`)
    } else {
      message.error('创建应用失败：' + res.data.message)
    }
  } finally {
    creating.value = false
  }
}

const handleDeleteApp = (id: number | string) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个应用吗？删除后无法恢复。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteApp({ id })
      if (res.data.code === 0) {
        message.success('删除成功')
        fetchMyApps()
      } else {
        message.error('删除失败：' + res.data.message)
      }
    },
  })
}

const goToAppChat = (id: number | string) => {
  router.push(`/app/chat/${id}`)
}

const goToEditApp = (id: number | string) => {
  router.push(`/app/edit/${id}`)
}

onMounted(() => {
  if (loginUserStore.loginUser.id) {
    fetchMyApps()
  }
  fetchGoodApps()
})
</script>

<style scoped>
#homeView {
  padding: 0;
}

.hero-section {
  text-align: center;
  padding: 40px 0;
}

.site-title {
  font-size: 36px;
  font-weight: bold;
  color: #1890ff;
  margin-bottom: 12px;
}

.site-subtitle {
  font-size: 16px;
  color: #666;
  margin-bottom: 24px;
}

.prompt-input-wrapper {
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-bar {
  margin-bottom: 16px;
  max-width: 300px;
}

.app-cover {
  height: 160px;
  object-fit: cover;
}

.app-description {
  color: #666;
  font-size: 12px;
  margin-bottom: 8px;
}

.app-time,
.app-author {
  color: #999;
  font-size: 12px;
}
</style>
