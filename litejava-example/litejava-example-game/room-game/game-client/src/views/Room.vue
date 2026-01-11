<template>
  <div class="room">
    <!-- 顶部信息 -->
    <header class="header">
      <div class="room-info">
        <span class="room-id">房间号: {{ gameStore.roomId }}</span>
        <span class="game-type">{{ gameTypeName }}</span>
      </div>
      <button class="exit-btn" @click="exitRoom">退出房间</button>
    </header>

    <!-- 座位区域 -->
    <div class="seats-area">
      <div 
        v-for="(seat, index) in seats" 
        :key="index"
        class="seat"
        :class="{ 
          empty: !seat.userId,
          me: seat.userId === userStore.playerId,
          ready: seat.ready,
          owner: seat.userId === ownerId
        }"
      >
        <div class="seat-index">{{ index + 1 }}号位</div>
        <div class="avatar">
          {{ seat.userId ? '👤' : '➕' }}
        </div>
        <div class="player-name">
          {{ seat.userId ? seat.name : '等待加入' }}
        </div>
        <div v-if="seat.userId" class="status">
          <span v-if="seat.userId === ownerId" class="owner-tag">房主</span>
          <span v-if="seat.ready" class="ready-tag">已准备</span>
          <span v-else-if="seat.userId !== ownerId" class="not-ready-tag">未准备</span>
        </div>
      </div>
    </div>

    <!-- 聊天区域 -->
    <div class="chat-area">
      <div class="chat-messages" ref="chatBox">
        <div v-for="(msg, i) in chatMessages" :key="i" class="chat-msg">
          <span class="sender">{{ msg.name }}:</span>
          <span class="content">{{ msg.content }}</span>
        </div>
      </div>
      <div class="chat-input">
        <input 
          v-model="chatInput" 
          placeholder="输入消息..."
          @keyup.enter="sendChat"
        />
        <button @click="sendChat">发送</button>
      </div>
    </div>

    <!-- 底部操作 -->
    <div class="actions">
      <button 
        v-if="!isOwner && !myReady"
        class="action-btn ready"
        @click="setReady"
      >
        准备
      </button>
      <button 
        v-if="!isOwner && myReady"
        class="action-btn cancel-ready"
        @click="cancelReady"
      >
        取消准备
      </button>
      <button 
        v-if="isOwner"
        class="action-btn start"
        :disabled="!canStart"
        @click="startGame"
      >
        开始游戏
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useGameStore } from '../stores/game'
import { wsManager, Cmd, GameType } from '../utils/websocket'
import { showMessage } from '../utils/message'

const router = useRouter()
const userStore = useUserStore()
const gameStore = useGameStore()

// 状态
const seats = ref([])
const ownerId = ref(0)
const chatMessages = ref([])
const chatInput = ref('')
const chatBox = ref(null)

// 游戏类型名称映射
const gameTypeNames = {
  [GameType.DOUDIZHU]: '斗地主',
  [GameType.MAHJONG]: '麻将',
  [GameType.GOBANG]: '五子棋',
  [GameType.TEXAS]: '德州扑克',
  [GameType.NIUNIU]: '牛牛',
  [GameType.WEREWOLF]: '狼人杀'
}

const gameTypeName = computed(() => gameTypeNames[gameStore.gameType] || '未知游戏')

const isOwner = computed(() => userStore.playerId === ownerId.value)

const myReady = computed(() => {
  const mySeat = seats.value.find(s => s.userId === userStore.playerId)
  return mySeat?.ready || false
})

const canStart = computed(() => {
  const filledSeats = seats.value.filter(s => s.userId)
  if (filledSeats.length < 2) return false
  // 除房主外都要准备
  return filledSeats.every(s => s.userId === ownerId.value || s.ready)
})

// 退出房间
function exitRoom() {
  wsManager.send(Cmd.ROOM_EXIT, {})
}

// 准备
function setReady() {
  wsManager.send(Cmd.READY, { ready: true })
}

// 取消准备
function cancelReady() {
  wsManager.send(Cmd.READY, { ready: false })
}

// 开始游戏
function startGame() {
  wsManager.send(Cmd.GAME_START, {})
}

// 发送聊天
function sendChat() {
  if (!chatInput.value.trim()) return
  wsManager.send(Cmd.CHAT_SEND, { content: chatInput.value })
  chatInput.value = ''
}

// 滚动聊天到底部
function scrollChatToBottom() {
  nextTick(() => {
    if (chatBox.value) {
      chatBox.value.scrollTop = chatBox.value.scrollHeight
    }
  })
}

// === 消息处理 ===

function onLoginResult(data) {
  if (data.code === 0) {
    seats.value = data.seats || []
    ownerId.value = data.ownerId || 0
    gameStore.setSeatIndex(data.seatIndex)
  } else {
    showMessage('进入房间失败')
    router.push('/lobby')
  }
}

function onUserJoin(data) {
  const seat = seats.value.find(s => s.seatIndex === data.seatIndex)
  if (seat) {
    seat.userId = data.userId
    seat.name = data.name
    seat.ready = false
    seat.online = true
  }
  chatMessages.value.push({ name: '系统', content: `${data.name} 加入了房间` })
  scrollChatToBottom()
}

function onUserExit(data) {
  const seat = seats.value.find(s => s.userId === data.userId)
  if (seat) {
    const name = seat.name
    seat.userId = 0
    seat.name = ''
    seat.ready = false
    chatMessages.value.push({ name: '系统', content: `${name} 离开了房间` })
    scrollChatToBottom()
  }
}

function onUserReady(data) {
  const seat = seats.value.find(s => s.seatIndex === data.seatIndex)
  if (seat) {
    seat.ready = data.ready !== false
  }
}

function onUserState(data) {
  const seat = seats.value.find(s => s.userId === data.userId)
  if (seat) {
    seat.online = data.online
  }
}

function onChatMsg(data) {
  const seat = seats.value.find(s => s.userId === data.userId)
  chatMessages.value.push({
    name: seat?.name || '未知',
    content: data.content
  })
  scrollChatToBottom()
}

function onRoomExitResult(data) {
  if (data.kicked) {
    showMessage('你被踢出了房间')
  }
  router.push('/lobby')
}

function onGameStart(data) {
  showMessage('游戏开始！')
  // 根据游戏类型跳转到对应页面
  if (gameStore.gameType === GameType.WEREWOLF) {
    router.push('/game/werewolf')
  } else {
    router.push('/game')
  }
}

onMounted(() => {
  wsManager.on(Cmd.LOGIN, onLoginResult)
  wsManager.on(Cmd.USER_JOIN, onUserJoin)
  wsManager.on(Cmd.USER_EXIT, onUserExit)
  wsManager.on(Cmd.USER_READY, onUserReady)
  wsManager.on(Cmd.USER_STATE, onUserState)
  wsManager.on(Cmd.CHAT_MSG, onChatMsg)
  wsManager.on(Cmd.ROOM_EXIT, onRoomExitResult)
  wsManager.on(Cmd.GAME_START, onGameStart)
  
  // Gateway 创建/加入房间成功后会自动连接 GameServer 并发送 LOGIN
  // 客户端只需等待 LOGIN 响应即可
})

onUnmounted(() => {
  wsManager.off(Cmd.LOGIN, onLoginResult)
  wsManager.off(Cmd.USER_JOIN, onUserJoin)
  wsManager.off(Cmd.USER_EXIT, onUserExit)
  wsManager.off(Cmd.USER_READY, onUserReady)
  wsManager.off(Cmd.USER_STATE, onUserState)
  wsManager.off(Cmd.CHAT_MSG, onChatMsg)
  wsManager.off(Cmd.ROOM_EXIT, onRoomExitResult)
  wsManager.off(Cmd.GAME_START, onGameStart)
})
</script>

<style scoped>
.room {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: white;
  display: flex;
  flex-direction: column;
}

/* 顶部 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 30px;
  background: rgba(0, 0, 0, 0.3);
}
.room-info {
  display: flex;
  align-items: center;
  gap: 20px;
}
.room-id {
  font-size: 18px;
  font-weight: bold;
}
.game-type {
  padding: 5px 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 15px;
  font-size: 14px;
}
.exit-btn {
  padding: 8px 20px;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}
.exit-btn:hover {
  background: #c0392b;
}

/* 座位区域 */
.seats-area {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  padding: 30px;
}

.seat {
  width: 150px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 15px;
  text-align: center;
  transition: all 0.3s;
  border: 2px solid transparent;
}
.seat.me {
  border-color: #3498db;
  background: rgba(52, 152, 219, 0.2);
}
.seat.ready {
  border-color: #27ae60;
}
.seat.empty {
  opacity: 0.5;
}

.seat-index {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 10px;
}
.avatar {
  font-size: 48px;
  margin-bottom: 10px;
}
.player-name {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 10px;
}
.status {
  display: flex;
  justify-content: center;
  gap: 5px;
}
.owner-tag {
  padding: 3px 8px;
  background: #f39c12;
  border-radius: 3px;
  font-size: 11px;
}
.ready-tag {
  padding: 3px 8px;
  background: #27ae60;
  border-radius: 3px;
  font-size: 11px;
}
.not-ready-tag {
  padding: 3px 8px;
  background: #7f8c8d;
  border-radius: 3px;
  font-size: 11px;
}

/* 聊天区域 */
.chat-area {
  margin: 0 30px 20px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 10px;
  overflow: hidden;
}
.chat-messages {
  height: 120px;
  overflow-y: auto;
  padding: 10px 15px;
}
.chat-msg {
  margin-bottom: 5px;
  font-size: 13px;
}
.chat-msg .sender {
  color: #3498db;
  margin-right: 5px;
}
.chat-input {
  display: flex;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}
.chat-input input {
  flex: 1;
  padding: 10px 15px;
  background: transparent;
  color: white;
  border: none;
  outline: none;
}
.chat-input input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}
.chat-input button {
  padding: 10px 20px;
  background: #3498db;
  color: white;
  border: none;
  cursor: pointer;
}
.chat-input button:hover {
  background: #2980b9;
}

/* 底部操作 */
.actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  padding: 20px;
  background: rgba(0, 0, 0, 0.3);
}

.action-btn {
  padding: 15px 50px;
  font-size: 18px;
  font-weight: bold;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
}
.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.ready {
  background: linear-gradient(135deg, #27ae60 0%, #1e8449 100%);
  color: white;
}
.action-btn.ready:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(39, 174, 96, 0.4);
}

.action-btn.cancel-ready {
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.action-btn.start {
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
  color: white;
}
.action-btn.start:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(231, 76, 60, 0.4);
}
</style>
