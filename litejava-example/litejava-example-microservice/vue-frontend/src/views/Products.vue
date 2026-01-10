<template>
  <div class="products-page">
    <div class="filter-bar">
      <div class="categories">
        <span :class="{ active: !currentCategory }" @click="filterByCategory('')">全部</span>
        <span v-for="cat in categories" :key="cat.key" :class="{ active: currentCategory === cat.key }" @click="filterByCategory(cat.key)">{{ cat.name }}</span>
      </div>
      <div class="search-box">
        <input v-model="keyword" type="text" placeholder="搜索商品..." @keyup.enter="search" />
        <button @click="search">搜索</button>
      </div>
    </div>
    
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="products.length === 0" class="empty">暂无商品</div>
    <template v-else>
      <div class="products-grid">
        <div v-for="p in products" :key="p.id" class="product-card">
          <img :src="p.image || `https://picsum.photos/seed/${p.id}/280/200`" :alt="p.name" />
          <div class="info">
            <div class="name">{{ p.name }}</div>
            <div class="desc">{{ p.description || '暂无描述' }}</div>
            <div class="price"><small>¥</small>{{ p.price?.toFixed(2) }}</div>
            <div class="stock">库存: {{ p.stock }}</div>
            <button class="btn-buy" @click="buy(p)">立即购买</button>
          </div>
        </div>
      </div>
      <div class="pagination">
        <button :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
        <span>第 {{ page }} / {{ totalPages }} 页 (共 {{ total }} 条)</span>
        <button :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { productApi, orderApi, searchApi } from '../api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const products = ref([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(12)
const total = ref(0)
const currentCategory = ref('')

const categories = [
  { key: 'phone', name: '手机' },
  { key: 'computer', name: '电脑' },
  { key: 'appliance', name: '家电' },
  { key: 'clothes', name: '服饰' },
  { key: 'shoes', name: '鞋靴' },
  { key: 'beauty', name: '美妆' },
  { key: 'food', name: '生鲜' },
  { key: 'book', name: '图书' },
  { key: 'new', name: '新品' }
]

const totalPages = computed(() => Math.ceil(total.value / size.value) || 1)

async function loadProducts() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (currentCategory.value) {
      params.category = currentCategory.value
    }
    const res = await productApi.list(params)
    if (res.code === 0) {
      const list = res.data?.list || []
      // 添加图片
      products.value = list.map((p, i) => ({
        ...p,
        image: p.imageUrl || `https://picsum.photos/seed/${p.id || i}/280/200`
      }))
      total.value = res.data?.total || 0
    }
  } catch (e) {
    console.error('加载商品失败:', e)
  } finally {
    loading.value = false
  }
}

function filterByCategory(cat) {
  currentCategory.value = cat
  page.value = 1
  router.push({ query: cat ? { category: cat } : {} })
  loadProducts()
}

async function search() {
  if (!keyword.value.trim()) {
    page.value = 1
    loadProducts()
    return
  }
  loading.value = true
  try {
    const res = await searchApi.query(keyword.value, page.value, size.value)
    if (res.code === 0) {
      const list = res.data?.list || res.data || []
      products.value = list.map((p, i) => ({
        ...p,
        image: p.imageUrl || `https://picsum.photos/seed/${p.id || i}/280/200`
      }))
      total.value = res.data?.total || products.value.length
    }
  } catch (e) {
    console.error('搜索失败:', e)
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  page.value = p
  if (keyword.value.trim()) {
    search()
  } else {
    loadProducts()
  }
}

async function buy(product) {
  if (!userStore.isLoggedIn) {
    alert('请先登录')
    router.push('/login')
    return
  }
  if (!confirm(`确认购买 ${product.name}？\n价格: ¥${product.price}`)) return
  
  try {
    const res = await orderApi.create({
      userId: userStore.user.id,
      items: [{
        productName: product.name,
        price: product.price,
        quantity: 1
      }]
    })
    if (res.code === 0) {
      router.push('/payment/' + res.data.id)
    } else {
      alert('下单失败: ' + res.msg)
    }
  } catch (e) {
    alert('网络错误: ' + e.message)
  }
}

onMounted(() => {
  currentCategory.value = route.query.category || ''
  loadProducts()
})
</script>

<style scoped>
.products-page { padding: 20px; max-width: 1200px; margin: 0 auto; }
.filter-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 15px; }
.categories { display: flex; gap: 10px; flex-wrap: wrap; }
.categories span { padding: 8px 16px; background: #f5f5f5; border-radius: 20px; cursor: pointer; font-size: 14px; transition: all 0.2s; }
.categories span:hover { background: #e6f7ff; color: #1890ff; }
.categories span.active { background: #1890ff; color: white; }
.search-box { display: flex; gap: 10px; }
.search-box input { padding: 10px 15px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; width: 200px; }
.search-box button { background: #1890ff; color: white; border: none; padding: 10px 20px; border-radius: 4px; cursor: pointer; }
.loading, .empty { text-align: center; padding: 60px; color: #999; }
.products-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.product-card { background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); transition: transform 0.2s; }
.product-card:hover { transform: translateY(-4px); }
.product-card img { width: 100%; height: 200px; object-fit: cover; background: #f5f5f5; }
.product-card .info { padding: 15px; }
.product-card .name { font-size: 16px; font-weight: 500; margin-bottom: 8px; }
.product-card .desc { font-size: 13px; color: #999; margin-bottom: 10px; height: 40px; overflow: hidden; }
.product-card .price { color: #ff4d4f; font-size: 20px; font-weight: bold; }
.product-card .price small { font-size: 14px; }
.product-card .stock { font-size: 12px; color: #999; margin-top: 5px; }
.product-card .btn-buy { width: 100%; background: #ff4d4f; color: white; border: none; padding: 10px; cursor: pointer; margin-top: 10px; border-radius: 4px; }
.product-card .btn-buy:hover { background: #ff7875; }
.pagination { display: flex; justify-content: center; align-items: center; gap: 20px; margin-top: 30px; padding: 20px; }
.pagination button { background: #1890ff; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; }
.pagination button:disabled { background: #d9d9d9; cursor: not-allowed; }
.pagination span { color: #666; }
</style>
