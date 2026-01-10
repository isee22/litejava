<template>
  <div class="home">
    <!-- Banner -->
    <div class="banner">
      <div class="banner-slide" :class="{ active: currentSlide === 0 }">
        <div class="banner-content">
          <h2>新年大促</h2>
          <p>全场商品低至5折，限时抢购</p>
          <button @click="$router.push('/products')">立即抢购</button>
        </div>
      </div>
      <div class="banner-slide slide2" :class="{ active: currentSlide === 1 }">
        <div class="banner-content">
          <h2>数码狂欢</h2>
          <p>iPhone、MacBook 限时特惠</p>
          <button @click="$router.push('/products?category=phone')">查看详情</button>
        </div>
      </div>
      <div class="banner-slide slide3" :class="{ active: currentSlide === 2 }">
        <div class="banner-content">
          <h2>新品首发</h2>
          <p>最新潮流单品，抢先体验</p>
          <button @click="$router.push('/products?category=new')">探索新品</button>
        </div>
      </div>
      <div class="banner-dots">
        <span v-for="i in 3" :key="i" :class="{ active: currentSlide === i - 1 }" @click="currentSlide = i - 1"></span>
      </div>
    </div>

    <div class="container">
      <!-- Categories -->
      <div class="categories">
        <div class="category-item" v-for="cat in categories" :key="cat.name" @click="$router.push('/products?category=' + cat.key)">
          <div class="icon">{{ cat.icon }}</div>
          <div class="name">{{ cat.name }}</div>
        </div>
      </div>

      <!-- Flash Sale -->
      <div class="flash-sale">
        <div class="flash-header">
          <h3>⚡ 限时秒杀</h3>
          <div class="countdown">
            <span>{{ countdown.hours }}</span>:<span>{{ countdown.minutes }}</span>:<span>{{ countdown.seconds }}</span>
          </div>
        </div>
        <div class="flash-products">
          <div class="flash-item" v-for="p in flashProducts" :key="p.id" @click="showDetail(p)">
            <img :src="p.image || `https://picsum.photos/seed/${p.id}/150/150`" :alt="p.name">
            <div class="price">¥{{ p.salePrice || p.price }}</div>
            <div class="original" v-if="p.salePrice">¥{{ p.price }}</div>
            <div class="progress"><div class="progress-bar" :style="{ width: (p.sold || 60) + '%' }"></div></div>
          </div>
        </div>
      </div>

      <!-- Hot Products -->
      <div class="section">
        <div class="section-title">
          <h2>热门推荐</h2>
          <a @click="$router.push('/products')">查看更多 ></a>
        </div>
        <div class="products">
          <div class="product-card" v-for="p in hotProducts" :key="p.id" @click="showDetail(p)">
            <img :src="p.image || `https://picsum.photos/seed/${p.id}/200/200`" :alt="p.name">
            <div class="info">
              <div class="name">{{ p.name }}</div>
              <div class="price"><small>¥</small>{{ p.price }}</div>
              <div class="tags">
                <span class="tag" v-if="p.isNew">新品</span>
                <span class="tag" v-if="p.isHot">热卖</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Product Detail Modal -->
    <div class="modal" :class="{ show: showModal }" @click.self="showModal = false">
      <div class="modal-content" v-if="selectedProduct">
        <span class="modal-close" @click="showModal = false">&times;</span>
        <img :src="selectedProduct.image || `https://picsum.photos/seed/${selectedProduct.id}/300/300`" class="product-img">
        <div class="product-info">
          <div class="product-name">{{ selectedProduct.name }}</div>
          <div class="product-price">¥{{ selectedProduct.price }}</div>
          <div class="product-desc">{{ selectedProduct.description || '暂无描述' }}</div>
          <div class="quantity">
            <span>数量</span>
            <button @click="quantity > 1 && quantity--">-</button>
            <input type="number" v-model.number="quantity" min="1">
            <button @click="quantity++">+</button>
          </div>
          <div class="actions">
            <button class="btn-cart" @click="addToCart">加入购物车</button>
            <button class="btn-buy" @click="buyNow">立即购买</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import api from '../api'

const router = useRouter()
const userStore = useUserStore()

const currentSlide = ref(0)
const categories = [
  { key: 'phone', icon: '📱', name: '手机' },
  { key: 'computer', icon: '💻', name: '电脑' },
  { key: 'appliance', icon: '📺', name: '家电' },
  { key: 'clothes', icon: '👔', name: '服饰' },
  { key: 'shoes', icon: '👟', name: '鞋靴' },
  { key: 'beauty', icon: '💄', name: '美妆' },
  { key: 'food', icon: '🍎', name: '生鲜' },
  { key: 'book', icon: '📚', name: '图书' }
]

const flashProducts = ref([])
const hotProducts = ref([])
const countdown = ref({ hours: '02', minutes: '30', seconds: '00' })

const showModal = ref(false)
const selectedProduct = ref(null)
const quantity = ref(1)

let slideTimer = null
let countdownTimer = null

onMounted(async () => {
  // 轮播
  slideTimer = setInterval(() => {
    currentSlide.value = (currentSlide.value + 1) % 3
  }, 4000)

  // 倒计时
  let total = 2 * 3600 + 30 * 60
  countdownTimer = setInterval(() => {
    if (total > 0) {
      total--
      const h = Math.floor(total / 3600)
      const m = Math.floor((total % 3600) / 60)
      const s = total % 60
      countdown.value = {
        hours: String(h).padStart(2, '0'),
        minutes: String(m).padStart(2, '0'),
        seconds: String(s).padStart(2, '0')
      }
    }
  }, 1000)

  // 加载商品
  try {
    const res = await api.post('/product/list', {})
    if (res.code === 0) {
      const products = res.data?.list || []
      // 为商品添加图片 (使用 picsum.photos)
      const withImages = products.map((p, i) => ({
        ...p,
        image: p.imageUrl || `https://picsum.photos/seed/${p.id || i}/300/300`
      }))
      flashProducts.value = withImages.slice(0, 5).map(p => ({ ...p, salePrice: Math.floor(p.price * 0.7), sold: Math.floor(Math.random() * 40 + 50) }))
      hotProducts.value = withImages.slice(0, 10).map(p => ({ ...p, isHot: Math.random() > 0.5, isNew: Math.random() > 0.7 }))
    }
  } catch (e) {
    console.error('加载商品失败', e)
  }
})

onUnmounted(() => {
  clearInterval(slideTimer)
  clearInterval(countdownTimer)
})

function showDetail(product) {
  selectedProduct.value = product
  quantity.value = 1
  showModal.value = true
}

async function addToCart() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  alert(`已添加 ${quantity.value} 件 ${selectedProduct.value.name} 到购物车`)
  showModal.value = false
}

async function buyNow() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const res = await api.post('/order/create', {
      userId: userStore.user.id,
      items: [{
        productName: selectedProduct.value.name,
        price: selectedProduct.value.price,
        quantity: quantity.value
      }]
    })
    if (res.code === 0) {
      showModal.value = false
      router.push('/payment/' + res.data.id)
    } else {
      alert(res.msg || '下单失败')
    }
  } catch (e) {
    alert('下单失败: ' + e.message)
  }
}
</script>


<style scoped>
.home { background: #f5f5f5; min-height: 100vh; }

/* Banner */
.banner { position: relative; height: 400px; overflow: hidden; }
.banner-slide { position: absolute; width: 100%; height: 100%; opacity: 0; transition: opacity 0.5s; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.banner-slide.slide2 { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.banner-slide.slide3 { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.banner-slide.active { opacity: 1; }
.banner-content { position: absolute; top: 50%; left: 10%; transform: translateY(-50%); color: white; text-shadow: 2px 2px 4px rgba(0,0,0,0.3); }
.banner-content h2 { font-size: 48px; margin-bottom: 15px; }
.banner-content p { font-size: 20px; margin-bottom: 25px; }
.banner-content button { padding: 15px 40px; font-size: 16px; background: #ff6b6b; border: none; color: white; border-radius: 30px; cursor: pointer; }
.banner-content button:hover { background: #ee5a5a; }
.banner-dots { position: absolute; bottom: 20px; left: 50%; transform: translateX(-50%); display: flex; gap: 10px; }
.banner-dots span { width: 12px; height: 12px; background: rgba(255,255,255,0.5); border-radius: 50%; cursor: pointer; }
.banner-dots span.active { background: white; }

.container { max-width: 1400px; margin: 0 auto; padding: 30px 20px; }

/* Categories */
.categories { display: grid; grid-template-columns: repeat(8, 1fr); gap: 15px; margin-bottom: 40px; }
.category-item { background: white; border-radius: 12px; padding: 20px 10px; text-align: center; cursor: pointer; transition: all 0.3s; }
.category-item:hover { transform: translateY(-5px); box-shadow: 0 10px 30px rgba(0,0,0,0.1); }
.category-item .icon { font-size: 36px; margin-bottom: 10px; }
.category-item .name { font-size: 14px; color: #333; }

/* Flash Sale */
.flash-sale { background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%); border-radius: 16px; padding: 25px; margin-bottom: 40px; }
.flash-header { display: flex; align-items: center; gap: 20px; margin-bottom: 20px; color: white; }
.flash-header h3 { font-size: 24px; }
.countdown { display: flex; gap: 5px; }
.countdown span { background: #333; padding: 5px 10px; border-radius: 4px; font-weight: bold; }
.flash-products { display: grid; grid-template-columns: repeat(5, 1fr); gap: 15px; }
.flash-item { background: white; border-radius: 12px; padding: 15px; text-align: center; cursor: pointer; transition: transform 0.3s; }
.flash-item:hover { transform: scale(1.05); }
.flash-item img { width: 100%; height: 120px; object-fit: contain; margin-bottom: 10px; }
.flash-item .price { color: #ff6b6b; font-size: 20px; font-weight: bold; }
.flash-item .original { color: #999; text-decoration: line-through; font-size: 12px; }
.flash-item .progress { height: 6px; background: #eee; border-radius: 3px; margin-top: 10px; overflow: hidden; }
.flash-item .progress-bar { height: 100%; background: linear-gradient(90deg, #ff6b6b, #ff8e53); border-radius: 3px; }

/* Section */
.section { margin-bottom: 40px; }
.section-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; }
.section-title h2 { font-size: 24px; color: #333; position: relative; padding-left: 15px; }
.section-title h2::before { content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 4px; height: 24px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 2px; }
.section-title a { color: #667eea; cursor: pointer; font-size: 14px; }

/* Products */
.products { display: grid; grid-template-columns: repeat(5, 1fr); gap: 20px; }
.product-card { background: white; border-radius: 12px; overflow: hidden; cursor: pointer; transition: all 0.3s; }
.product-card:hover { transform: translateY(-5px); box-shadow: 0 15px 40px rgba(0,0,0,0.15); }
.product-card img { width: 100%; height: 200px; object-fit: contain; padding: 20px; background: #fafafa; }
.product-card .info { padding: 15px; }
.product-card .name { font-size: 14px; color: #333; margin-bottom: 10px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; height: 40px; }
.product-card .price { color: #ff6b6b; font-size: 22px; font-weight: bold; }
.product-card .price small { font-size: 14px; }
.product-card .tags { margin-top: 10px; }
.product-card .tag { display: inline-block; padding: 2px 8px; background: #fff0f0; color: #ff6b6b; font-size: 11px; border-radius: 3px; margin-right: 5px; }

/* Modal */
.modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 1000; justify-content: center; align-items: center; }
.modal.show { display: flex; }
.modal-content { background: white; border-radius: 16px; padding: 30px; width: 800px; max-width: 90%; display: flex; gap: 30px; position: relative; }
.modal-close { position: absolute; top: 15px; right: 20px; font-size: 28px; cursor: pointer; color: #999; }
.product-img { width: 300px; height: 300px; object-fit: contain; background: #fafafa; border-radius: 8px; }
.product-info { flex: 1; }
.product-name { font-size: 20px; color: #333; margin-bottom: 15px; }
.product-price { font-size: 32px; color: #ff6b6b; font-weight: bold; margin-bottom: 20px; }
.product-desc { color: #666; font-size: 14px; line-height: 1.8; margin-bottom: 20px; }
.quantity { display: flex; align-items: center; gap: 15px; margin-bottom: 20px; }
.quantity button { width: 36px; height: 36px; border: 1px solid #ddd; background: white; cursor: pointer; font-size: 18px; border-radius: 4px; }
.quantity input { width: 60px; height: 36px; text-align: center; border: 1px solid #ddd; border-radius: 4px; }
.actions { display: flex; gap: 15px; }
.btn-cart { flex: 1; padding: 15px; background: #ff6b6b; border: none; color: white; border-radius: 8px; font-size: 16px; cursor: pointer; }
.btn-cart:hover { background: #ee5a5a; }
.btn-buy { flex: 1; padding: 15px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border: none; color: white; border-radius: 8px; font-size: 16px; cursor: pointer; }
.btn-buy:hover { opacity: 0.9; }

@media (max-width: 1200px) {
  .categories { grid-template-columns: repeat(4, 1fr); }
  .flash-products, .products { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 768px) {
  .categories { grid-template-columns: repeat(4, 1fr); }
  .flash-products, .products { grid-template-columns: repeat(2, 1fr); }
  .banner-content h2 { font-size: 28px; }
  .modal-content { flex-direction: column; }
  .product-img { width: 100%; height: 200px; }
}
</style>
