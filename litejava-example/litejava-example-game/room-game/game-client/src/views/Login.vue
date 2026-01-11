<template>
  <div class="login-panel">
    <h1>🎮 房间制游戏</h1>
    <div class="form">
      <input v-model="username" type="text" placeholder="用户名" maxlength="16" @keypress.enter="handleSubmit">
      <input v-model="password" type="password" placeholder="密码" maxlength="32" @keypress.enter="handleSubmit">
      <div class="buttons">
        <button @click="login" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
        <button @click="register" :disabled="loading" class="secondary">注册</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { hallApi } from '../utils/websocket'
import { showMessage } from '../utils/message'

const router = useRouter()
const userStore = useUserStore()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function login() {
  if (!validate()) return
  loading.value = true
  
  try {
    const result = await hallApi.login(username.value, password.value)
    
    if (result.code !== 0) {
      showMessage(result.msg || '登录失败')
      loading.value = false
      return
    }
    
    const { userId, name } = result.data
    
    // 保存用户信息
    localStorage.setItem('userId', userId)
    localStorage.setItem('username', username.value)
    localStorage.setItem('playerName', name)
    
    userStore.setPlayer(userId, name)
    showMessage('欢迎回来！')
    router.push('/lobby')
    
  } catch (e) {
    showMessage('网络错误，请重试')
  } finally {
    loading.value = false
  }
}

async function register() {
  if (!validate()) return
  loading.value = true
  
  try {
    const result = await hallApi.register(username.value, password.value, username.value)
    
    if (result.code !== 0) {
      showMessage(result.msg || '注册失败')
      loading.value = false
      return
    }
    
    // 注册成功，自动登录
    const { userId, name } = result.data
    localStorage.setItem('userId', userId)
    localStorage.setItem('username', username.value)
    localStorage.setItem('playerName', name)
    
    userStore.setPlayer(userId, name)
    showMessage('注册成功！')
    router.push('/lobby')
    
  } catch (e) {
    showMessage('网络错误，请重试')
  } finally {
    loading.value = false
  }
}

function validate() {
  if (!username.value || username.value.length < 2) {
    showMessage('用户名至少2个字符')
    return false
  }
  if (!password.value || password.value.length < 4) {
    showMessage('密码至少4个字符')
    return false
  }
  return true
}

function handleSubmit() {
  login()
}
</script>

<style scoped>
.login-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
}
.login-panel h1 {
  font-size: 48px;
  margin-bottom: 40px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
  color: white;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.form input {
  width: 300px;
  padding: 15px 20px;
  font-size: 16px;
  border: none;
  border-radius: 8px;
}
.buttons {
  display: flex;
  gap: 10px;
}
.buttons button {
  flex: 1;
  padding: 15px;
  font-size: 16px;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}
.buttons button:hover { background: #c0392b; }
.buttons button:disabled { background: #95a5a6; cursor: not-allowed; }
.buttons button.secondary { background: #3498db; }
.buttons button.secondary:hover { background: #2980b9; }
</style>
