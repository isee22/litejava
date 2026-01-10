<template>
  <div class="auth-container">
    <div class="auth-card">
      <h2>用户注册</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="form.username" type="text" placeholder="请输入用户名" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="form.password" type="password" placeholder="请输入密码" required />
        </div>
        <div class="form-group">
          <label>确认密码</label>
          <input v-model="form.password2" type="password" placeholder="请再次输入密码" required />
        </div>
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="form.email" type="email" placeholder="请输入邮箱" />
        </div>
        <div class="form-group">
          <label>手机号</label>
          <input v-model="form.phone" type="tel" placeholder="请输入手机号" />
        </div>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>
      </form>
      <p class="auth-link">已有账号？<router-link to="/login">立即登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({ username: '', password: '', password2: '', email: '', phone: '' })
const loading = ref(false)
const error = ref('')
const success = ref('')

async function handleRegister() {
  if (form.password !== form.password2) {
    error.value = '两次密码输入不一致'
    return
  }
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const result = await userStore.register({
      username: form.username,
      password: form.password,
      email: form.email,
      phone: form.phone
    })
    if (result.success) {
      success.value = '注册成功！即将跳转到登录页...'
      setTimeout(() => router.push('/login'), 1500)
    } else {
      error.value = result.msg || '注册失败'
    }
  } catch (e) {
    error.value = '网络错误: ' + e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f0f2f5; }
.auth-card { background: white; padding: 40px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); width: 100%; max-width: 400px; }
.auth-card h2 { text-align: center; margin-bottom: 30px; color: #333; }
.form-group { margin-bottom: 20px; }
.form-group label { display: block; margin-bottom: 8px; color: #666; }
.form-group input { width: 100%; padding: 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.form-group input:focus { border-color: #1890ff; outline: none; }
.btn-primary { width: 100%; background: #1890ff; color: white; border: none; padding: 12px; border-radius: 4px; font-size: 16px; cursor: pointer; }
.btn-primary:hover { background: #40a9ff; }
.btn-primary:disabled { background: #ccc; cursor: not-allowed; }
.error { color: #ff4d4f; text-align: center; margin-top: 15px; }
.success { color: #52c41a; text-align: center; margin-top: 15px; }
.auth-link { text-align: center; margin-top: 20px; color: #666; }
.auth-link a { color: #1890ff; }
</style>
