<template>
  <div class="orders-page">
    <h2>我的订单</h2>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="orders.length === 0" class="empty">暂无订单</div>
    <template v-else>
      <div class="order-list">
        <div v-for="o in orders" :key="o.id" class="order-item">
          <div class="header">
            <span class="order-no">订单号: {{ o.orderNo }}</span>
            <span :class="['status', statusClass(o.status)]">{{ statusText(o.status) }}</span>
          </div>
          <div class="time">创建时间: {{ o.createdAt }}</div>
          <div class="footer">
            <span class="amount">¥{{ o.totalAmount?.toFixed(2) }}</span>
            <button v-if="o.status === 0" class="btn-pay" @click="$router.push('/payment/' + o.id)">去支付</button>
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
import { ref, computed, onMounted } from 'vue'
import { orderApi } from '../api'

const orders = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const totalPages = computed(() => Math.ceil(total.value / size.value) || 1)

const statusMap = {
  0: ['待支付', 'pending'],
  1: ['已支付', 'paid'],
  2: ['已发货', 'paid'],
  3: ['已完成', 'completed'],
  4: ['已取消', 'pending']
}

function statusText(status) {
  return statusMap[status]?.[0] || '未知'
}

function statusClass(status) {
  return statusMap[status]?.[1] || 'pending'
}

async function loadOrders() {
  loading.value = true
  try {
    const res = await orderApi.list({ page: page.value, size: size.value })
    if (res.code === 0) {
      orders.value = res.data?.list || []
      total.value = res.data?.total || orders.value.length
    }
  } catch (e) {
    console.error('加载订单失败:', e)
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  page.value = p
  loadOrders()
}

onMounted(loadOrders)
</script>

<style scoped>
.orders-page { padding: 20px; max-width: 800px; margin: 0 auto; }
.orders-page h2 { margin-bottom: 20px; }
.loading, .empty { text-align: center; padding: 60px; color: #999; }
.order-list { background: white; border-radius: 8px; padding: 20px; }
.order-item { border-bottom: 1px solid #f0f0f0; padding: 15px 0; }
.order-item:last-child { border-bottom: none; }
.order-item .header { display: flex; justify-content: space-between; margin-bottom: 10px; }
.order-item .order-no { color: #666; font-size: 14px; }
.order-item .status { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.order-item .status.pending { background: #fff7e6; color: #fa8c16; }
.order-item .status.paid { background: #e6f7ff; color: #1890ff; }
.order-item .status.completed { background: #f6ffed; color: #52c41a; }
.order-item .time { font-size: 13px; color: #999; }
.order-item .footer { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.order-item .amount { font-size: 18px; color: #ff4d4f; font-weight: bold; }
.order-item .btn-pay { background: #ff4d4f; color: white; border: none; padding: 8px 20px; border-radius: 4px; cursor: pointer; }
.order-item .btn-pay:hover { background: #ff7875; }
.pagination { display: flex; justify-content: center; align-items: center; gap: 20px; margin-top: 20px; padding: 20px; }
.pagination button { background: #1890ff; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; }
.pagination button:disabled { background: #d9d9d9; cursor: not-allowed; }
.pagination span { color: #666; }
</style>
