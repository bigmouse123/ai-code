import axios from 'axios'
import { message } from 'ant-design-vue'

const MAX_SAFE_INTEGER = Number.MAX_SAFE_INTEGER

function reviveJson(key: string, value: unknown) {
  if (typeof value === 'number' && value > MAX_SAFE_INTEGER) {
    return String(value)
  }
  return value
}

function transformResponse(data: string) {
  try {
    return JSON.parse(data, reviveJson)
  } catch {
    return data
  }
}

const myAxios = axios.create({
  baseURL: 'http://localhost:8123/api',
  timeout: 60000,
  withCredentials: true,
  transformResponse: [transformResponse],
})

myAxios.interceptors.request.use(
  function (config) {
    return config
  },
  function (error) {
    return Promise.reject(error)
  },
)

myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    if (data.code === 40100) {
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${window.location.href}`
      }
    }
    return response
  },
  function (error) {
    return Promise.reject(error)
  },
)

export default myAxios
