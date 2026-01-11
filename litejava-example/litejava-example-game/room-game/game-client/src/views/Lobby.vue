<template>
  <div class="lobby">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="user-info">
        <span class="avatar">👤</span>
        <span class="name">{{ userStore.playerName }}</span>
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
      <!-- 左侧：房间列表 -->
      <div class="room-list-panel">
        <div class="panel-header">
          <h3>{{ currentGameName }} - 房间列表</h3>
          <button class="refresh-btn" @click="refreshRooms" :disabled="loading">
            🔄 刷新
          </button>
        </div>
        
        <div class="room-list" v-if="!loading">
          <div v-if="rooms.length === 0" class="empty-tip">
            暂无房间，快来创建一个吧！
          </div>
          <div 
            v-for="room in rooms" 
            :key="room.roomId"
            class="room-item"
            :class="{ full: room.playerCount >= room.maxPlayers, gaming: room.gaming }"
          >
            <div class="room-info">
              <span class="room-id">房间 {{ room.roomId }}</span>
              <span class="room-owner">房主: {{ room.ownerName || '未知' }}</span>
            </div>
            <div class="room-status">
              <span class="player-count">
                👥 {{ room.playerCount }}/{{ room.maxPlayers }}
              </span>
              <span v-if="room.gaming" class="gaming-tag">游戏中</span>
            </div>
            <button 
              class="join-btn"
              :disabled="room.playerCount >= room.maxPlayers || room.gaming"
              @click="joinRoom(room.roomId)"
            >
              {{ room.gaming ? '游戏中' : (room.playerCount >= room.maxPlayers ? '已满' : '加入') }}
            </button>
          </div>
        </div>
        <div v-else class="loading">加载中...</div>
      </div>

      <!-- 右侧：操作面板 -->
      <div class="action-panel">
        <!-- 快速开始 -->
        <div class="action-card">
          <h4>⚡ 快速开始</h4>
          <p>自动匹配玩家，快速进入游戏</p>
          <button 
            class="action-btn primary"
            :disabled="matching"
            @click="quickStart"
          >
            {{ matching ? '匹配中...' : '快速匹配' }}
          </button>
          <button 
            v-if="matching"
            class="action-btn cancel"
            @click="cancelMatch"
          >
            取消匹配
          </button>
          <div v-if="matchStatus" class="match-status">{{ matchStatus }}</div>
        </div>

        <!-- 创建房间 -->
        <div class="action-card">
          <h4>🏠 创建房间</h4>
          <p>创建私人房间，邀请好友</p>
          <div class="create-options">
            <label>
              人数:
              <select v-model="createPlayerCount">
                <option v-for="n in playerCountOptions" :key="n" :value="n">{{ n }}人</option>
              </select>
            </label>
          </div>
          <button class="action-btn success" @click="createRoom">
            创建房间
          </button>
        </div>

        <!-- 加入房间 -->
        <div class="action-card">
          <h4>🚪 加入房间</h4>
          <p>输入房间号直接加入</p>
          <input 
            v-model="joinRoomId" 
            placeholder="输入房间号"
            class="room-input"
          />
          <button 
            class="action-btn info"
            :disabled="!joinRoomId"
            @click="joinRoomById"
          >
            加入
          </button>
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
  { type: GameType.DOUDIZHU, name: '斗地主', icon: '🃏', maxPlayers: [3] },
  { type: GameType.MAHJONG, name: '麻将', icon: '🀄', maxPlayers: [4] },
  { type: GameType.GOBANG, name: '五子棋', icon: '⚫', maxPlayers: [2] },
  { type: GameType.TEXAS, name: '德州扑克', icon: '♠️', maxPlayers: [6, 9] },
  { type: GameType.NIUNIU, name: '牛牛', icon: '🐂', maxPlayers: [6, 8] },
  { type: GameType.WEREWOLF, name: '狼人杀', icon: '🐺', maxPlayers: [6, 8, 9, 12] }
]

// 状态
const selectedGame = ref(GameType.DOUDIZHU)
const rooms = ref([])
const loading = ref(false)
const matching = ref(false)
const matchStatus = ref('')
const createPlayerCount = ref(3)
const joinRoomId = ref('')

// 计算属性
const currentGameName = computed(() => {
  const game = games.find(g => g.type === selectedGame.value)
  return game ? game.name : ''
})

const playerCountOptions = computed(() => {
  const game = games.find(g => g.type === selectedGame.value)
  return game ? game.maxPlayers : [4]
})

// 选择游戏
function selectGame(type) {
  selectedGame.value = type
  const game = games.find(g => g.type === type)
  if (game && game.maxPlayers.length > 0) {
    createPlayerCount.value = game.maxPlayers[0]
  }
  refreshRooms()
}

// 刷新房间列表
async function refreshRooms() {
  loading.value = true
  try {
    const result = await hallApi.getRoomConfigs()
    if (result.code === 0) {
      rooms.value = result.data || []
    }
  } catch (e) {
    console.error('获取房间列表失败:', e)
  } finally {
    loading.value = false
  }
}

// 快速匹配
async function quickStart() {
  matching.value = true
  matchStatus.value = '正在匹配玩家...'
  
  try {
    const result = await hallApi.startMatch(
      userStore.playerId,
      selectedGame.value,
      'normal',
      userStore.playerName
    )
    
    if (result.code !== 0) {
      showMessage(result.msg || '匹配失败')
      matching.value = false
      matchStatus.value = ''
      return
    }
    
    if (result.data.status === 'matched') {
      matchStatus.value = '匹配成功！'
      await enterGameRoom(result.data)
    } else {
      // 继续轮询
      pollMatchResult()
    }
  } catch (e) {
    showMessage('匹配失败，请重试')
    matching.value = false
    matchStatus.value = ''
  }
}

// 轮询匹配结果
async function pollMatchResult() {
  if (!matching.value) return
  
  try {
    const result = await hallApi.pollMatch(
      userStore.playerId,
      selectedGame.value,
      'normal',
      userStore.playerName
    )
    
    if (result.data?.status === 'matched') {
      matchStatus.value = '匹配成功！'
      await enterGameRoom(result.data)
    } else if (result.data?.status === 'cancelled') {
      matching.value = false
      matchStatus.value = ''
    } else {
      // 继续轮询
      setTimeout(pollMatchResult, 1000)
    }
  } catch (e) {
    console.error('轮询匹配失败:', e)
    setTimeout(pollMatchResult, 2000)
  }
}

// 取消匹配
async function cancelMatch() {
  try {
    await hallApi.cancelMatch(userStore.playerId)
  } catch (e) {
    console.error('取消匹配失败:', e)
  }
  matching.value = false
  matchStatus.value = ''
}

// 创建房间
async function createRoom() {
  try {
    const result = await hallApi.createRoom(
      userStore.playerId,
      selectedGame.value,
      { maxPlayers: createPlayerCount.value }
    )
    
    if (result.code !== 0) {
      showMessage(result.msg || '创建失败')
      return
    }
    
    showMessage('房间创建成功')
    await enterGameRoom(result.data)
  } catch (e) {
    showMessage('创建失败，请重试')
  }
}

// 加入房间
async function joinRoom(roomId) {
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

// 通过房间号加入
function joinRoomById() {
  if (joinRoomId.value) {
    joinRoom(joinRoomId.value)
  }
}

// 进入游戏房间 (通过 Nginx 代理连接 GameServer WebSocket)
async function enterGameRoom(data) {
  const { roomid, serverId, gameType, token } = data
  
  // 生产环境: 通过 Nginx 代理，不暴露后端 IP/端口
  // 开发环境: 也可以直连 (需要配置 API_CONFIG.wsProxy)
  const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${wsProtocol}//${location.host}/ws/game/${gameType}?server=${serverId}&token=${token}`
  
  gameStore.setRoom(roomid, -1)
  gameStore.setGameType(gameType)
  
  try {
    await wsManager.connect(wsUrl, token)
    router.push('/room')
  } catch (e) {
    showMessage('连接游戏服务器失败')
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
  // 检查登录状态
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
  
  refreshRooms()
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
  gap: 10px;
}
.user-info .avatar {
  font-size: 24px;
}
.user-info .name {
  font-size: 18px;
  font-weight: bold;
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

/* 房间列表面板 */
.room-list-panel {
  flex: 1;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 15px;
  padding: 20px;
  min-height: 500px;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.panel-header h3 {
  margin: 0;
  font-size: 18px;
}
.refresh-btn {
  padding: 8px 15px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}
.refresh-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}
.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.room-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.empty-tip {
  text-align: center;
  padding: 50px;
  color: rgba(255, 255, 255, 0.5);
}
.loading {
  text-align: center;
  padding: 50px;
}

.room-item {
  display: flex;
  align-items: center;
  padding: 15px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  transition: all 0.3s;
}
.room-item:hover {
  background: rgba(255, 255, 255, 0.12);
}
.room-item.full {
  opacity: 0.6;
}
.room-item.gaming {
  border-left: 3px solid #f39c12;
}

.room-info {
  flex: 1;
}
.room-id {
  font-weight: bold;
  display: block;
  margin-bottom: 5px;
}
.room-owner {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.room-status {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-right: 15px;
}
.player-count {
  font-size: 14px;
}
.gaming-tag {
  padding: 3px 8px;
  background: #f39c12;
  border-radius: 3px;
  font-size: 12px;
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
.join-btn:hover:not(:disabled) {
  background: #2980b9;
}
.join-btn:disabled {
  background: #7f8c8d;
  cursor: not-allowed;
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

.create-options {
  margin-bottom: 15px;
}
.create-options label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}
.create-options select {
  padding: 8px 15px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 5px;
  flex: 1;
}

.room-input {
  width: 100%;
  padding: 10px 15px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 5px;
  margin-bottom: 15px;
  box-sizing: border-box;
}
.room-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
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
  margin-bottom: 10px;
}
.action-btn:last-child {
  margin-bottom: 0;
}
.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-btn.primary {
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
  color: white;
}
.action-btn.primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(231, 76, 60, 0.4);
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

.action-btn.cancel {
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.match-status {
  text-align: center;
  margin-top: 10px;
  font-size: 14px;
  color: #f39c12;
}
</style>
