import axios from 'axios'

const api = axios.create({
  baseURL: '',  // 使用 vite proxy
  timeout: 10000
})

// 请求拦截器 - 添加 token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器
api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// Auth API - 透明路由，和后端路径一致
export const authApi = {
  login: (username, password) => api.post('/auth/login', { username, password }),
  register: (data) => api.post('/auth/register', data)
}

// Product API
export const productApi = {
  list: (params = {}) => api.post('/product/list', params)
}

// Order API
export const orderApi = {
  list: (params = {}) => api.post('/order/list', params),
  create: (data) => api.post('/order/create', data)
}

// Search API
export const searchApi = {
  query: (keyword, page = 1, size = 12) => api.post('/search/query', { keyword, page, size })
}

export default api
