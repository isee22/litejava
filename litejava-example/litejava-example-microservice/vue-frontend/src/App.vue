<template>
  <div id="app">
    <nav class="navbar">
      <h1 @click="$router.push('/')">🛒 LiteShop</h1>
      <div class="nav-links">
        <router-link to="/">首页</router-link>
        <router-link to="/products">商品</router-link>
        <router-link v-if="userStore.isLoggedIn" to="/orders">我的订单</router-link>
        <span v-if="userStore.isLoggedIn" class="welcome">欢迎, {{ userStore.user?.username }}</span>
        <button v-if="userStore.isLoggedIn" @click="logout">退出</button>
        <router-link v-else to="/login" class="btn">登录</router-link>
      </div>
    </nav>
    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from './stores/user'

const router = useRouter()
const userStore = useUserStore()

function logout() {
  userStore.logout()
  router.push('/products')
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f0f2f5; min-height: 100vh; }
a { text-decoration: none; color: inherit; }

.navbar {
  background: #1890ff;
  color: white;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
}
.navbar h1 { font-size: 20px; cursor: pointer; }
.nav-links { display: flex; align-items: center; gap: 20px; }
.nav-links a { color: white; padding: 8px 12px; border-radius: 4px; }
.nav-links a:hover, .nav-links a.router-link-active { background: rgba(255,255,255,0.2); }
.nav-links .welcome { font-size: 14px; }
.nav-links button, .nav-links .btn {
  background: rgba(255,255,255,0.2);
  border: none;
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}
.nav-links button:hover, .nav-links .btn:hover { background: rgba(255,255,255,0.3); }

main { padding-top: 60px; min-height: calc(100vh - 60px); }
</style>
