<template>
  <div id="appEditView">
    <a-card :loading="loading">
      <template #title>
        <div class="card-header">
          <span>{{ isAdmin ? '管理员编辑应用' : '编辑应用' }}</span>
          <a-button type="text" @click="goBack">
            <arrow-left-outlined />
            返回
          </a-button>
        </div>
      </template>

      <a-form
        :model="formData"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 16 }"
        @finish="handleSubmit"
      >
        <a-form-item label="应用名称" name="appName" :rules="[{ required: true, message: '请输入应用名称' }]">
          <a-input v-model:value="formData.appName" placeholder="请输入应用名称" />
        </a-form-item>

        <a-form-item v-if="isAdmin" label="应用封面" name="cover">
          <a-input v-model:value="formData.cover" placeholder="请输入封面图片URL" />
          <div v-if="formData.cover" class="cover-preview">
            <a-image :src="formData.cover" :width="200" />
          </div>
        </a-form-item>

        <a-form-item v-if="isAdmin" label="优先级" name="priority">
          <a-input-number v-model:value="formData.priority" :min="0" :max="99" />
          <span class="priority-hint">（99 为精选应用）</span>
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
          <a-space>
            <a-button type="primary" html-type="submit" :loading="submitting">
              保存
            </a-button>
            <a-button @click="goBack">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-divider />

      <div class="app-info">
        <h4>应用信息</h4>
        <p><strong>应用ID：</strong>{{ appInfo?.id }}</p>
        <p><strong>创建者：</strong>{{ appInfo?.user?.userName || '未知' }}</p>
        <p><strong>创建时间：</strong>{{ dayjs(appInfo?.createTime).format('YYYY-MM-DD HH:mm:ss') }}</p>
        <p><strong>更新时间：</strong>{{ dayjs(appInfo?.updateTime).format('YYYY-MM-DD HH:mm:ss') }}</p>
        <p v-if="appInfo?.deployedTime">
          <strong>部署时间：</strong>{{ dayjs(appInfo?.deployedTime).format('YYYY-MM-DD HH:mm:ss') }}
        </p>
        <p><strong>初始提示词：</strong>{{ appInfo?.initPrompt }}</p>
      </div>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import {
  getAppVoById,
  getAppVoByIdByAdmin,
  updateApp,
  updateAppByAdmin,
} from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appId = route.params.id as string
const isAdmin = ref(false)
const loading = ref(true)
const submitting = ref(false)
const appInfo = ref<API.AppVO>()

const formData = reactive({
  appName: '',
  cover: '',
  priority: 0,
})

const checkPermission = () => {
  const loginUser = loginUserStore.loginUser
  isAdmin.value = loginUser.userRole === 'admin'
}

const fetchAppInfo = async () => {
  loading.value = true
  try {
    let res
    if (isAdmin.value) {
      res = await getAppVoByIdByAdmin({ id: appId })
    } else {
      res = await getAppVoById({ id: appId })
    }

    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      formData.appName = res.data.data.appName || ''
      formData.cover = res.data.data.cover || ''
      formData.priority = res.data.data.priority || 0

      if (!isAdmin.value && appInfo.value?.userId !== loginUserStore.loginUser.id) {
        message.error('您没有权限编辑此应用')
        router.push('/')
      }
    } else {
      message.error('获取应用信息失败：' + res.data.message)
    }
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    let res
    if (isAdmin.value) {
      res = await updateAppByAdmin({
        id: appId,
        appName: formData.appName,
        cover: formData.cover,
        priority: formData.priority,
      })
    } else {
      res = await updateApp({
        id: appId,
        appName: formData.appName,
      })
    }

    if (res.data.code === 0) {
      message.success('保存成功')
      goBack()
    } else {
      message.error('保存失败：' + res.data.message)
    }
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  checkPermission()
  fetchAppInfo()
})
</script>

<style scoped>
#appEditView {
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cover-preview {
  margin-top: 12px;
}

.priority-hint {
  margin-left: 8px;
  color: #999;
}

.app-info {
  background: #fafafa;
  padding: 16px;
  border-radius: 4px;
}

.app-info h4 {
  margin-bottom: 12px;
}

.app-info p {
  margin-bottom: 8px;
  color: #666;
}
</style>
