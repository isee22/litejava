import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 响应拦截器
api.interceptors.response.use(
  res => res.data,
  err => {
    const msg = err.response?.data?.message || err.message
    alert(msg)
    return Promise.reject(err)
  }
)

export default {
  // 图书列表
  getBooks(keyword = '') {
    return api.get('/books', { params: { q: keyword } })
  },
  
  // 获取单本图书
  getBook(id) {
    return api.get(`/books/${id}`)
  },
  
  // 创建图书
  createBook(book) {
    return api.post('/books', book)
  },
  
  // 更新图书
  updateBook(id, book) {
    return api.put(`/books/${id}`, book)
  },
  
  // 删除图书
  deleteBook(id) {
    return api.delete(`/books/${id}`)
  }
}
