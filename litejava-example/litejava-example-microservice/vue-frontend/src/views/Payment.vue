<template>
  <div class="payment-page">
    <div class="payment-card">
      <h2>订单支付</h2>
      
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="!order" class="error">订单不存在</div>
      <div v-else>
        <div class="order-info">
          <div class="row"><span>订单号:</span><span>{{ order.orderNo }}</span></div>
          <div class="row"><span>商品:</span><span>{{ orderItems }}</span></div>
          <div class="row amount"><span>支付金额:</span><span class="price">¥{{ order.totalAmount?.toFixed(2) }}</span></div>
        </div>
        
        <div class="pay-methods">
          <h3>选择支付方式</h3>
          <div class="methods">
            <label :class="{ active: payMethod === 'alipay' }">
              <input type="radio" v-model="payMethod" value="alipay" />
              <span>💳 支付宝</span>
            </label>
            <label :class="{ active: payMethod === 'wechat' }">
              <input type="radio" v-model="payMethod" value="wechat" />
              <span>💚 微信支付</span>
            </label>
            <label :class="{ active: payMethod === 'balance' }">
              <input type="radio" v-model="payMethod" value="balance" />
              <span>💰 余额支付</span>
            </label>
          </div>
        </div>
        
        <button class="btn-pay" @click="handlePay" :disabled="paying">
          {{ paying ? '支付中...' : '确认支付 ¥' + order.totalAmount?.toFixed(2) }}
        </button>
        
        <p v-if="error" class="error-msg">{{ error }}</p>
        <p v-if="success" class="success-msg">{{ success }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const loading = ref(true)
const paying = ref(false)
const payMethod = ref('alipay')
const error = ref('')
const success = ref('')

const orderItems = computed(() => {
  if (!order.value?.items) return '-'
  return order.value.items.map(i => `${i.productName} x${i.quantity}`).join(', ')
})

async function loadOrder() {
  loading.value = true
  try {
    const res = await api.post('/order/detail', { id: Number(route.params.id) })
    if (res.code === 0) {
      order.value = res.data
    }
  } catch (e) {
    console.error('加载订单失败:', e)
  } finally {
    loading.value = false
  }
}

async function handlePay() {
  paying.value = true
  error.value = ''
  success.value = ''
  
  try {
    // 1. 先创建支付单
    const createRes = await api.post('/payment/create', {
      orderNo: order.value.orderNo,
      userId: order.value.userId,
      amount: order.value.totalAmount,
      channel: payMethod.value
    })
    
    if (createRes.code !== 0) {
      error.value = createRes.msg || '创建支付单失败'
      return
    }
    
    // 2. 执行支付
    const payRes = await api.post('/payment/pay', {
      paymentNo: createRes.data.paymentNo
    })
    
    if (payRes.code === 0) {
      success.value = '支付成功！'
      setTimeout(() => router.push('/orders'), 1500)
    } else {
      error.value = payRes.msg || '支付失败'
    }
  } catch (e) {
    error.value = '网络错误: ' + e.message
  } finally {
    paying.value = false
  }
}

onMounted(loadOrder)
</script>

<style scoped>
.payment-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f0f2f5; padding: 20px; }
.payment-card { background: white; padding: 40px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); width: 100%; max-width: 500px; }
.payment-card h2 { text-align: center; margin-bottom: 30px; color: #333; }
.loading, .error { text-align: center; padding: 40px; color: #999; }

.order-info { background: #fafafa; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
.order-info .row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; }
.order-info .row:last-child { border-bottom: none; }
.order-info .row span:first-child { color: #666; }
.order-info .amount { font-size: 18px; font-weight: bold; }
.order-info .price { color: #ff4d4f; }

.pay-methods { margin-bottom: 20px; }
.pay-methods h3 { font-size: 14px; color: #666; margin-bottom: 15px; }
.methods { display: flex; flex-direction: column; gap: 10px; }
.methods label { display: flex; align-items: center; padding: 15px; border: 2px solid #e8e8e8; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
.methods label:hover { border-color: #1890ff; }
.methods label.active { border-color: #1890ff; background: #e6f7ff; }
.methods label input { display: none; }
.methods label span { font-size: 16px; }

.btn-pay { width: 100%; background: #ff4d4f; color: white; border: none; padding: 15px; border-radius: 8px; font-size: 18px; cursor: pointer; }
.btn-pay:hover { background: #ff7875; }
.btn-pay:disabled { background: #ccc; cursor: not-allowed; }

.error-msg { color: #ff4d4f; text-align: center; margin-top: 15px; }
.success-msg { color: #52c41a; text-align: center; margin-top: 15px; }
</style>
