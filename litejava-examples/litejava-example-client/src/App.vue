<template>
  <div class="container">
    <h1>📚 图书管理系统</h1>
    
    <!-- 搜索栏 -->
    <div class="search-bar">
      <input v-model="keyword" placeholder="搜索图书..." @keyup.enter="search" />
      <button @click="search">搜索</button>
      <button class="primary" @click="showAdd">+ 添加图书</button>
    </div>
    
    <!-- 图书列表 -->
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>书名</th>
          <th>作者</th>
          <th>ISBN</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="book in books" :key="book.id">
          <td>{{ book.id }}</td>
          <td>{{ book.title }}</td>
          <td>{{ book.author }}</td>
          <td>{{ book.isbn }}</td>
          <td>
            <button @click="showEdit(book)">编辑</button>
            <button class="danger" @click="remove(book.id)">删除</button>
          </td>
        </tr>
        <tr v-if="books.length === 0">
          <td colspan="5" class="empty">暂无数据</td>
        </tr>
      </tbody>
    </table>
    
    <!-- 弹窗 -->
    <div v-if="showModal" class="modal" @click.self="closeModal">
      <div class="modal-content">
        <h2>{{ isEdit ? '编辑图书' : '添加图书' }}</h2>
        <form @submit.prevent="save">
          <div class="form-item">
            <label>书名</label>
            <input v-model="form.title" required />
          </div>
          <div class="form-item">
            <label>作者</label>
            <input v-model="form.author" />
          </div>
          <div class="form-item">
            <label>ISBN</label>
            <input v-model="form.isbn" />
          </div>
          <div class="form-item">
            <label>简介</label>
            <textarea v-model="form.description" rows="3"></textarea>
          </div>
          <div class="form-actions">
            <button type="button" @click="closeModal">取消</button>
            <button type="submit" class="primary">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from './api'

const books = ref([])
const keyword = ref('')
const showModal = ref(false)
const isEdit = ref(false)
const form = ref({})

const loadBooks = async () => {
  const res = await api.getBooks(keyword.value)
  books.value = res.data?.books || []
}

const search = () => loadBooks()

const showAdd = () => {
  form.value = { title: '', author: '', isbn: '', description: '' }
  isEdit.value = false
  showModal.value = true
}

const showEdit = (book) => {
  form.value = { ...book }
  isEdit.value = true
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const save = async () => {
  if (isEdit.value) {
    await api.updateBook(form.value.id, form.value)
  } else {
    await api.createBook(form.value)
  }
  closeModal()
  loadBooks()
}

const remove = async (id) => {
  if (confirm('确定删除？')) {
    await api.deleteBook(id)
    loadBooks()
  }
}

onMounted(loadBooks)
</script>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f5f5f5; }
.container { max-width: 1000px; margin: 0 auto; padding: 20px; }
h1 { margin-bottom: 20px; color: #333; }

.search-bar { display: flex; gap: 10px; margin-bottom: 20px; }
.search-bar input { flex: 1; padding: 8px 12px; border: 1px solid #ddd; border-radius: 4px; }

button { padding: 8px 16px; border: 1px solid #ddd; border-radius: 4px; background: #fff; cursor: pointer; }
button:hover { background: #f0f0f0; }
button.primary { background: #1890ff; color: #fff; border-color: #1890ff; }
button.primary:hover { background: #40a9ff; }
button.danger { color: #ff4d4f; border-color: #ff4d4f; }
button.danger:hover { background: #fff1f0; }

table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
th, td { padding: 12px 16px; text-align: left; border-bottom: 1px solid #f0f0f0; }
th { background: #fafafa; font-weight: 500; }
td button { margin-right: 8px; padding: 4px 8px; font-size: 12px; }
.empty { text-align: center; color: #999; padding: 40px; }

.modal { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; }
.modal-content { background: #fff; padding: 24px; border-radius: 8px; width: 400px; }
.modal-content h2 { margin-bottom: 20px; }
.form-item { margin-bottom: 16px; }
.form-item label { display: block; margin-bottom: 4px; color: #666; }
.form-item input, .form-item textarea { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px; }
.form-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px; }
</style>
