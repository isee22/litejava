<template>
  <div class="lobby">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="user-info">
        <span class="avatar">👤</span>
        <span class="name">{{ userStore.playerName }}</span>
        <span class="room-cards">🎫 房卡: {{ roomCardCount }}</span>
      </div>
      <button class="logout-btn" @click="logout">退出</button>
    </header>

    <!-- 游戏选择标签 -->
    <div class="game-tabs">
      <button 
        v-for="game in games" 
        :key="game.type"
        class="tab-btn"
        :class="{ active: selectedGame === game.type }"
        @click="selectGame(game.type)"
      >
        <span class="icon">{{ game.icon }}</span>
        <span class="name">{{ game.name }}</span>
      </button>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 左侧：级别场列表 -->
      <div class="room-list-panel">
        <div class="panel-header">
          <h3>{{ currentGameName }} - 选择场次</h3>
        </div>
        
        <div class="level-list">
          <div 
            v-for="level in gameLevels" 
            :key="level.roomLevel"
            class="level-item"
            @click="quickStartLevel(level)"
          >
            <div class="level-info">
              <span class="level-name">{{ level.roomName }}</span>
              <span class="level-desc">底分: {{ level.baseScore }} | 准入: {{ level.minCoins }}金币</span>
            </div>
            <div class="level-status">
              <span class="player-count">👥 在线中</span>
            </div>
            <button class="join-btn">快速开始</button>
          </div>
          <div v-if="gameLevels.length === 0" class="empty-tip">
            暂无场次配置
          </div>
        </div>
      </div>

      <!-- 右侧：操作面板 -->
      <div class="action-panel">
        <!-- 创建好友房 -->
        <div class="action-card">
          <h4>🏠 创建好友房</h4>
          <p>消耗1张房卡，邀请好友</p>
          <div class="room-card-info">
            <span>房卡: {{ roomCardCount }}</span>
          </div>
          <button 
            class="action-btn success" 
            @click="createFriendRoom"
            :disabled="roomCardCount < 1"
          >
            创建好友房
          </button>
        </div>

        <!-- 加入好友房 -->
        <div class="action-card">
          <h4>🚪 加入好友房</h4>
          <p>输入6位房间号</p>
          <button class="action-btn info" @click="showRoomIdInput">
            输入房间号
          </button>
        </div>
      </div>
    </div>

    <!-- 九宫格输入房间号弹窗 -->
    <div v-if="showInput" class="modal-overlay" @click="closeRoomIdInput">
      <div class="modal-content" @click.stop>
        <h3>输入房间号</h3>
        <div class="room-id-display">{{ inputRoomId || '______' }}</div>
        <div class="numpad">
          <button v-for="n in 9" :key="n" @click="inputDigit(n)" class="num-btn">{{ n }}</button>
          <button @click="clearInput" class="num-btn">清空</button>
          <button @click="inputDigit(0)" class="num-btn">0</button>
          <button @click="deleteDigit" class="num-btn">删除</button>
        </div>
        <div class="modal-actions">
          <button @click="closeRoomIdInput" class="cancel-btn">取消</button>
          <button @click="confirmRoomId" :disabled="inputRoomId.length !== 6" class="confirm-btn">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useGameStore } from '../stores/game'
import { hallApi, wsManager, GameType } from '../utils/websocket'
import { showMessage } from '../utils/message'

const router = useRouter()
const userStore = useUserStore()
const gameStore = useGameStore()

// 游戏列表
const games = [
  { type: GameType.DOUDIZHU, name: '斗地主', icon: '🃏' },
  { type: GameType.MAHJONG, name: '麻将', icon: '🀄' },
  { type: GameType.GOBANG, name: '五子棋', icon: '⚫' },
  { type: GameType.TEXAS, name: '德州扑克', icon: '♠️' },
  { type: GameType.NIUNIU, name: '牛牛', icon: '🐂' },
  { type: GameType.WEREWOLF, name: '狼人杀', icon: '🐺' }
]

// 状态
const selectedGame = ref(GameType.DOUDIZHU)
const gameLevels = ref([])
const roomCardCount = ref(0)
const showInput = ref(false)
const inputRoomId = ref('')
const matching = ref(false)

// 计算属性
const currentGameName = computed(() => {
  const game = games.find(g => g.type === selectedGame.value)
  return game ? game.name : ''
})

// 选择游戏
function selectGame(type) {
  selectedGame.value = type
  loadGameLevels()
}

// 加载游戏级别场配置
async function loadGameLevels() {
  try {
    // 优先从 localStorage 读取
    const cached = localStorage.getItem('roomConfigs')
    if (cached) {
      const configs = JSON.parse(cached)
      gameLevels.value = configs.filter(c => c.gameType === selectedGame.value)
      return
    }
    
    // 缓存不存在时才请求
    const result = await hallApi.getRoomConfigs()
    if (result.code === 0 && result.data) {
      localStorage.setItem('roomConfigs', JSON.stringify(result.data))
      gameLevels.value = result.data.filter(c => c.gameType === selectedGame.value)
    }
  } catch (e) {
    console.error('加载场次配置失败:', e)
  }
}

// 加载房卡数量
async function loadRoomCards() {
  try {
    // 优先从 localStorage 读取
    const cached = localStorage.getItem('playerItems')
    if (cached) {
      const items = JSON.parse(cached)
      const roomCard = items.find(item => item.itemId === 5001)
      roomCardCount.value = roomCard ? roomCard.count : 0
      return
    }
    
    // 缓存不存在时才请求
    const resp = await fetch(`/api/account/bag/${userStore.playerId}`)
    const result = await resp.json()
    if (result.code === 0 && result.data) {
      localStorage.setItem('playerItems', JSON.stringify(result.data))
      const roomCard = result.data.find(item => item.itemId === 5001)
      roomCardCount.value = roomCard ? roomCard.count : 0
    }
  } catch (e) {
    console.error('加载房卡失败:', e)
  }
}

// 快速开始（级别场）- 有房间就加入，没房间就创建
async function quickStartLevel(level) {
  if (matching.value) return
  
  try {
    // 调用快速开始API
    const result = await hallApi.quickStart(
      userStore.playerId,
      selectedGame.value,
      { maxPlayers: level.maxPlayers || 4, roomLevel: level.roomLevel }
    )
    
    if (result.code !== 0) {
      showMessage(result.msg || '快速开始失败')
      return
    }
    
    showMessage('进入房间成功')
    await enterGameRoom(result.data)
  } catch (e) {
    showMessage('快速开始失败，请重试')
  }
}

// 不再需要轮询匹配
// async function pollMatchResult(levelNum) { ... }

// 不再需要取消匹配
// async function cancelMatch() { ... }

// 创建好友房
async function createFriendRoom() {
  if (roomCardCount.value < 1) {
    showMessage('房卡不足')
    return
  }
  
  try {
    const result = await hallApi.createRoom(
      userStore.playerId,
      selectedGame.value,
      { maxPlayers: 4 }
    )
    
    if (result.code !== 0) {
      showMessage(result.msg || '创建失败')
      return
    }
    
    // 创建成功，扣除房卡
    roomCardCount.value--
    updateLocalRoomCards(-1)
    
    showMessage('好友房创建成功')
    await enterGameRoom(result.data)
  } catch (e) {
    showMessage('创建失败，请重试')
  }
}

// 更新本地房卡数量
function updateLocalRoomCards(delta) {
  const cached = localStorage.getItem('playerItems')
  if (cached) {
    const items = JSON.parse(cached)
    const roomCard = items.find(item => item.itemId === 5001)
    if (roomCard) {
      roomCard.count += delta
      localStorage.setItem('playerItems', JSON.stringify(items))
    }
  }
}

// 显示房间号输入
function showRoomIdInput() {
  showInput.value = true
  inputRoomId.value = ''
}

// 关闭房间号输入
function closeRoomIdInput() {
  showInput.value = false
  inputRoomId.value = ''
}

// 输入数字
function inputDigit(digit) {
  if (inputRoomId.value.length < 6) {
    inputRoomId.value += digit
  }
}

// 删除数字
function deleteDigit() {
  if (inputRoomId.value.length > 0) {
    inputRoomId.value = inputRoomId.value.slice(0, -1)
  }
}

// 清空输入
function clearInput() {
  inputRoomId.value = ''
}

// 确认房间号
async function confirmRoomId() {
  if (inputRoomId.value.length !== 6) {
    showMessage('请输入6位房间号')
    return
  }
  
  // 先保存房间号，再关闭弹窗
  const roomId = inputRoomId.value
  closeRoomIdInput()
  
  try {
    const result = await hallApi.enterRoom(
      userStore.playerId,
      roomId,
      userStore.playerName
    )
    
    if (result.code !== 0) {
      showMessage(result.msg || '加入失败')
      return
    }
    
    showMessage('加入成功')
    await enterGameRoom(result.data)
  } catch (e) {
    showMessage('加入失败，请重试')
  }
}

// 进入游戏房间
async function enterGameRoom(data) {
  const roomId = data.roomId || data.roomid
  const { ip, port, gameType, token, time, sign } = data
  
  let wsUrl = data.wsUrl
  if (!wsUrl && ip && port) {
    wsUrl = `ws://${ip}:${port}/game`
  }
  
  if (!wsUrl) {
    showMessage('无法获取游戏服务器地址')
    return
  }
  
  gameStore.setRoom(roomId, -1)
  gameStore.setGameType(gameType)
  
  // 构造登录参数 (带签名)
  const loginParams = { token, roomid: roomId, time, sign }
  
  try {
    await wsManager.connect(wsUrl, loginParams)
    router.push('/room')
  } catch (e) {
    showMessage('连接游戏服务器失败: ' + (e.message || '未知错误'))
    gameStore.reset()
  }
}

// 退出登录
function logout() {
  wsManager.disconnect()
  userStore.logout()
  localStorage.clear()
  router.push('/')
}

onMounted(() => {
  if (!userStore.isLoggedIn) {
    const userId = localStorage.getItem('userId')
    const playerName = localStorage.getItem('playerName')
    if (userId && playerName) {
      userStore.setPlayer(parseInt(userId), playerName)
    } else {
      router.push('/')
      return
    }
  }
  
  loadGameLevels()
  loadRoomCards()
})
</script>

<style scoped>
.lobby {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: white;
}

/* 顶部导航 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 30px;
  background: rgba(0, 0, 0, 0.3);
}
.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}
.user-info .avatar {
  font-size: 24px;
}
.user-info .name {
  font-size: 18px;
  font-weight: bold;
}
.user-info .room-cards {
  padding: 5px 15px;
  background: rgba(255, 215, 0, 0.2);
  border: 1px solid rgba(255, 215, 0, 0.5);
  border-radius: 15px;
  font-size: 14px;
}
.logout-btn {
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 5px;
  cursor: pointer;
}
.logout-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* 游戏标签 */
.game-tabs {
  display: flex;
  justify-content: center;
  gap: 10px;
  padding: 20px;
  flex-wrap: wrap;
}
.tab-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 15px 25px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: 2px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
}
.tab-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}
.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
}
.tab-btn .icon {
  font-size: 28px;
  margin-bottom: 5px;
}
.tab-btn .name {
  font-size: 14px;
}

/* 主内容区 */
.main-content {
  display: flex;
  gap: 20px;
  padding: 0 30px 30px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 级别场列表 */
.room-list-panel {
  flex: 1;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 15px;
  padding: 20px;
  min-height: 500px;
}
.panel-header {
  margin-bottom: 15px;
}
.panel-header h3 {
  margin: 0;
  font-size: 18px;
}

.level-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.empty-tip {
  text-align: center;
  padding: 50px;
  color: rgba(255, 255, 255, 0.5);
}

.level-item {
  display: flex;
  align-items: center;
  padding: 15px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  transition: all 0.3s;
  cursor: pointer;
}
.level-item:hover {
  background: rgba(255, 255, 255, 0.12);
  transform: translateX(5px);
}

.level-info {
  flex: 1;
}
.level-name {
  font-weight: bold;
  display: block;
  margin-bottom: 5px;
  font-size: 16px;
}
.level-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.level-status {
  margin-right: 15px;
}
.player-count {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.join-btn {
  padding: 8px 20px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.3s;
}
.join-btn:hover {
  background: #2980b9;
}

.join-btn {
  padding: 8px 20px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.3s;
}
.join-btn:hover {
  background: #2980b9;
}

/* 操作面板 */
.action-panel {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.action-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 15px;
  padding: 20px;
}
.action-card h4 {
  margin: 0 0 10px;
  font-size: 16px;
}
.action-card p {
  margin: 0 0 15px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.room-card-info {
  margin-bottom: 15px;
  padding: 10px;
  background: rgba(255, 215, 0, 0.1);
  border: 1px solid rgba(255, 215, 0, 0.3);
  border-radius: 5px;
  text-align: center;
  font-size: 14px;
}

.action-btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s;
}
.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.success {
  background: linear-gradient(135deg, #27ae60 0%, #1e8449 100%);
  color: white;
}
.action-btn.success:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(39, 174, 96, 0.4);
}

.action-btn.info {
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  color: white;
}
.action-btn.info:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(52, 152, 219, 0.4);
}

/* 九宫格输入弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  border-radius: 20px;
  padding: 30px;
  min-width: 350px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
}

.modal-content h3 {
  margin: 0 0 20px;
  text-align: center;
  font-size: 20px;
}

.room-id-display {
  text-align: center;
  font-size: 32px;
  font-weight: bold;
  letter-spacing: 8px;
  padding: 20px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 10px;
  margin-bottom: 20px;
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.numpad {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}

.num-btn {
  padding: 20px;
  font-size: 24px;
  font-weight: bold;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.num-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.05);
}
.num-btn:active {
  transform: scale(0.95);
}

.modal-actions {
  display: flex;
  gap: 10px;
}

.cancel-btn, .confirm-btn {
  flex: 1;
  padding: 12px;
  font-size: 16px;
  font-weight: bold;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}
.cancel-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.confirm-btn {
  background: linear-gradient(135deg, #27ae60 0%, #1e8449 100%);
  color: white;
}
.confirm-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(39, 174, 96, 0.4);
}
.confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
