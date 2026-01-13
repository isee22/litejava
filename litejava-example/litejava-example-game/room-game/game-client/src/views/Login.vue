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
    
    const { userId, name, roomId, serverId } = result.data
    
    // 保存用户信息
    localStorage.setItem('userId', userId)
    localStorage.setItem('username', username.value)
    localStorage.setItem('playerName', name)
    
    // 保存房间配置和道具列表
    if (result.data.roomConfigs) {
      localStorage.setItem('roomConfigs', JSON.stringify(result.data.roomConfigs))
    }
    if (result.data.items) {
      localStorage.setItem('playerItems', JSON.stringify(result.data.items))
    }
    
    userStore.setPlayer(userId, name)
    
    // 检查断线重连
    if (roomId && serverId) {
      // 弹出选择框让用户决定是否重连
      const shouldReconnect = confirm('检测到未完成的游戏，是否重新连接？\n\n点击"确定"重连，点击"取消"放弃游戏')
      
      if (shouldReconnect) {
        showMessage('正在重连...')
        // 调用 enterRoom 获取新的 token 和签名
        const reconnectResult = await hallApi.enterRoom(userId, roomId, name)
        if (reconnectResult.code === 0) {
          // 重连成功，进入房间
          const { useGameStore } = await import('../stores/game')
          const { wsManager } = await import('../utils/websocket')
          const gameStore = useGameStore()
          
          const data = reconnectResult.data
          const wsUrl = data.wsUrl || `ws://${data.ip}:${data.port}/game`
          const loginParams = { 
            token: data.token, 
            roomid: data.roomId || data.roomid, 
            time: data.time, 
            sign: data.sign 
          }
          
          gameStore.setRoom(data.roomId || data.roomid, -1)
          gameStore.setGameType(data.gameType)
          
          await wsManager.connect(wsUrl, loginParams)
          router.push('/room')
          return
        }
        // 重连失败
        showMessage('重连失败，进入大厅')
      } else {
        // 用户选择不重连，清理房间状态
        showMessage('已放弃游戏')
        await hallApi.clearUserRoom(userId)
      }
    } else {
      showMessage('欢迎回来！')
    }
    
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
    
    // 保存房间配置和道具列表
    if (result.data.roomConfigs) {
      localStorage.setItem('roomConfigs', JSON.stringify(result.data.roomConfigs))
    }
    if (result.data.items) {
      localStorage.setItem('playerItems', JSON.stringify(result.data.items))
    }
    
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
