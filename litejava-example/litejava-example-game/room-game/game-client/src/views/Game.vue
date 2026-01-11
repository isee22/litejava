<template>
  <div class="game-panel">
    <div class="game-table">
      <!-- 其他玩家 -->
      <PlayerArea 
        v-for="pos in ['top', 'left']" 
        :key="pos"
        :position="pos"
        :player="getPlayerByPosition(pos)"
        :isCurrent="isCurrentPlayer(pos)"
        :isLandlord="isLandlordPlayer(pos)"
        :isReady="isReadyPlayer(pos)"
        :showCardCount="gameStore.gameStatus !== 'waiting'"
        :playedCards="playedCardsMap[pos]"
        :passed="passedMap[pos]"
      />
      
      <!-- 中央出牌区 -->
      <div class="play-area">
        <template v-if="playedCardsMap.bottom">
          <Card v-for="(card, i) in playedCardsMap.bottom" :key="i" :card="card" small />
        </template>
        <span v-else-if="passedMap.bottom" class="pass-text">不出</span>
      </div>
      
      <!-- 底牌 -->
      <div v-if="gameStore.bottomCards.length" class="bottom-cards">
        <Card v-for="(card, i) in gameStore.bottomCards" :key="i" :card="card" small />
      </div>
      
      <!-- 操作按钮 -->
      <div class="action-buttons">
        <button v-if="showReady" class="action-btn success" @click="ready">准备</button>
        <button v-if="showBid" class="action-btn primary" @click="bid(true)">叫地主</button>
        <button v-if="showBid" class="action-btn secondary" @click="bid(false)">不叫</button>
        <button v-if="showPlay" class="action-btn primary" @click="playCards">出牌</button>
        <button v-if="showPass" class="action-btn secondary" @click="pass">不出</button>
      </div>
      
      <!-- 我的区域 -->
      <div class="player-area bottom">
        <div class="player-info">
          <div class="player-name">{{ userStore.playerName }} (我)</div>
          <div v-if="gameStore.gameStatus !== 'waiting'" class="player-cards-count">
            剩余 {{ gameStore.myCards.length }} 张
          </div>
          <div class="player-status" :class="myStatusClass">{{ myStatusText }}</div>
        </div>
        <div class="hand-cards">
          <Card 
            v-for="(card, i) in gameStore.myCards" 
            :key="i"
            :card="card"
            :selected="isSelected(card)"
            @click="gameStore.toggleCard(card)"
          />
        </div>
      </div>
    </div>
    
    <!-- 游戏结果弹窗 -->
    <div v-if="showResult" class="game-result">
      <div class="result-content">
        <h2 :class="resultWin ? 'win' : 'lose'">
          {{ resultWin ? '🎉 胜利！' : '😢 失败' }}
        </h2>
        <p>{{ resultDetail }}</p>
        <button @click="closeResult">继续游戏</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useGameStore } from '../stores/game'
import { wsManager, Cmd } from '../utils/websocket'
import { showMessage } from '../utils/message'
import Card from '../components/Card.vue'
import PlayerArea from '../components/PlayerArea.vue'

const router = useRouter()
const userStore = useUserStore()
const gameStore = useGameStore()

const showBid = ref(false)
const showPlay = ref(false)
const showPass = ref(false)
const showResult = ref(false)
const resultWin = ref(false)
const resultDetail = ref('')
const playedCardsMap = ref({ top: null, left: null, bottom: null })
const passedMap = ref({ top: false, left: false, bottom: false })

const showReady = computed(() => gameStore.gameStatus === 'waiting')

const myStatusText = computed(() => {
  const status = []
  if (gameStore.mySeat === gameStore.landlordSeat) status.push('👑 地主')
  if (gameStore.mySeat === gameStore.currentSeat && gameStore.gameStatus !== 'waiting') {
    status.push('⏰ 出牌中')
  }
  return status.join(' ')
})

const myStatusClass = computed(() => ({
  current: gameStore.mySeat === gameStore.currentSeat,
  landlord: gameStore.mySeat === gameStore.landlordSeat
}))

function getDisplaySeat(seat) {
  if (seat === gameStore.mySeat) return 'bottom'
  return ((seat - gameStore.mySeat + 3) % 3) === 1 ? 'left' : 'top'
}

function getSeatByPosition(pos) {
  if (pos === 'bottom') return gameStore.mySeat
  const offset = pos === 'left' ? 1 : 2
  return (gameStore.mySeat + offset) % 3
}

function getPlayerByPosition(pos) {
  return gameStore.players[getSeatByPosition(pos)] || {}
}

function isCurrentPlayer(pos) {
  return getSeatByPosition(pos) === gameStore.currentSeat && gameStore.gameStatus !== 'waiting'
}

function isLandlordPlayer(pos) {
  return getSeatByPosition(pos) === gameStore.landlordSeat
}

function isReadyPlayer(pos) {
  const player = getPlayerByPosition(pos)
  return player.ready && gameStore.gameStatus === 'waiting'
}

function isSelected(card) {
  return gameStore.selectedCards.some(c => c.suit === card.suit && c.rank === card.rank)
}

function ready() {
  wsManager.send(Cmd.ROOM_READY, {})
}

function bid(wantBid) {
  wsManager.send(Cmd.DDZ_BID, { bid: wantBid })
  showBid.value = false
}

function playCards() {
  if (gameStore.selectedCards.length === 0) {
    showMessage('请选择要出的牌')
    return
  }
  wsManager.send(Cmd.DDZ_PLAY, { cards: gameStore.selectedCards })
}

function pass() {
  wsManager.send(Cmd.DDZ_PASS, {})
}

function closeResult() {
  showResult.value = false
}

function clearPlayedCards() {
  playedCardsMap.value = { top: null, left: null, bottom: null }
  passedMap.value = { top: false, left: false, bottom: false }
}

// 消息处理
function onRoomInfo(data) {
  const playerList = data.players || []
  for (const p of playerList) {
    gameStore.updatePlayer(p.seat, p)
    if (p.id === userStore.playerId) {
      gameStore.mySeat = p.seat
    }
  }
}

function onPlayerReady(data) {
  gameStore.updatePlayer(data.seat, { ready: data.ready })
}

function onPlayerLeave(data) {
  if (data.seat !== undefined) {
    gameStore.players[data.seat] = {}
  }
}

function onGameStart(data) {
  gameStore.gameStatus = 'bidding'
  showMessage('游戏开始！')
  clearPlayedCards()
  gameStore.bottomCards = []
  
  for (let i = 0; i < 3; i++) {
    if (gameStore.players[i].id) {
      gameStore.updatePlayer(i, { ready: false, cardCount: 17 })
    }
  }
  
  wsManager.send(Cmd.DDZ_DEAL, {})
  if (data.bidSeat === gameStore.mySeat) {
    showBid.value = true
  }
}

function onDeal(data) {
  gameStore.setCards(data.cards || [])
}

function onBidResult(data) {
  showBid.value = false
  
  if (data.landlordSeat !== undefined) {
    gameStore.landlordSeat = data.landlordSeat
    gameStore.gameStatus = 'playing'
    gameStore.currentSeat = data.currentSeat
    gameStore.bottomCards = data.bottomCards || []
    
    showMessage(`座位${data.landlordSeat + 1} 成为地主`)
    
    if (data.landlordSeat === gameStore.mySeat) {
      wsManager.send(Cmd.DDZ_DEAL, {})
    }
    
    if (data.currentSeat === gameStore.mySeat) {
      showPlay.value = true
      showPass.value = false
    }
  } else if (data.redeal) {
    showMessage('无人叫地主，重新发牌')
    if (data.nextBidSeat === gameStore.mySeat) {
      showBid.value = true
    }
  } else {
    showMessage(`座位${data.seat + 1} ${data.bid ? '叫地主' : '不叫'}`)
    if (data.nextBidSeat === gameStore.mySeat) {
      showBid.value = true
    }
  }
}

function onPlayResult(data) {
  showPlay.value = false
  showPass.value = false
  
  const seat = data.seat
  const displayPos = getDisplaySeat(seat)
  
  if (data.pass) {
    passedMap.value[displayPos] = true
    playedCardsMap.value[displayPos] = null
  } else {
    const cards = data.cards || []
    playedCardsMap.value[displayPos] = cards
    passedMap.value[displayPos] = false
    gameStore.lastCards = cards
    gameStore.lastSeat = seat
    
    if (gameStore.players[seat]) {
      gameStore.updatePlayer(seat, { cardCount: data.remainCount })
    }
    
    if (seat === gameStore.mySeat) {
      gameStore.removeCards(cards)
    }
  }
  
  if (data.clearLast) {
    gameStore.lastCards = []
    gameStore.lastSeat = -1
    clearPlayedCards()
  }
  
  if (data.gameOver) {
    onGameOver(data)
    return
  }
  
  if (data.nextSeat !== undefined) {
    gameStore.currentSeat = data.nextSeat
    if (data.nextSeat === gameStore.mySeat) {
      showPlay.value = true
      showPass.value = gameStore.lastCards.length > 0 && gameStore.lastSeat !== gameStore.mySeat
    }
  }
}

function onGameOver(data) {
  gameStore.gameStatus = 'waiting'
  gameStore.currentSeat = -1
  gameStore.lastCards = []
  gameStore.lastSeat = -1
  
  const isWinner = (data.landlordWin && gameStore.mySeat === gameStore.landlordSeat) ||
                   (!data.landlordWin && gameStore.mySeat !== gameStore.landlordSeat)
  
  resultWin.value = isWinner
  resultDetail.value = data.landlordWin ? '地主获胜' : '农民获胜'
  showResult.value = true
  
  showPlay.value = false
  showPass.value = false
  showBid.value = false
  gameStore.landlordSeat = -1
}

function onRoomDissolved() {
  showMessage('房间已解散')
  backToLobby()
}

function backToLobby() {
  gameStore.reset()
  router.push('/lobby')
}

onMounted(() => {
  wsManager.on(Cmd.ROOM_INFO, onRoomInfo)
  wsManager.on(Cmd.ROOM_PLAYER_READY, onPlayerReady)
  wsManager.on(Cmd.ROOM_PLAYER_LEAVE, onPlayerLeave)
  wsManager.on(Cmd.ROOM_DISSOLVED, onRoomDissolved)
  wsManager.on(Cmd.GAME_START, onGameStart)
  wsManager.on(Cmd.DDZ_DEAL, onDeal)
  wsManager.on(Cmd.DDZ_BID_RESULT, onBidResult)
  wsManager.on(Cmd.DDZ_PLAY_RESULT, onPlayResult)
  wsManager.on(Cmd.GAME_OVER, onGameOver)
})

onUnmounted(() => {
  wsManager.off(Cmd.ROOM_INFO, onRoomInfo)
  wsManager.off(Cmd.ROOM_PLAYER_READY, onPlayerReady)
  wsManager.off(Cmd.ROOM_PLAYER_LEAVE, onPlayerLeave)
  wsManager.off(Cmd.ROOM_DISSOLVED, onRoomDissolved)
  wsManager.off(Cmd.GAME_START, onGameStart)
  wsManager.off(Cmd.DDZ_DEAL, onDeal)
  wsManager.off(Cmd.DDZ_BID_RESULT, onBidResult)
  wsManager.off(Cmd.DDZ_PLAY_RESULT, onPlayResult)
  wsManager.off(Cmd.GAME_OVER, onGameOver)
})
</script>


<style scoped>
.game-panel { padding: 20px; }

.game-table {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 20px;
  padding: 20px;
  min-height: 600px;
  position: relative;
}

.player-area { position: absolute; text-align: center; }
.player-area.bottom { bottom: 20px; left: 50%; transform: translateX(-50%); }

.player-info {
  background: rgba(255, 255, 255, 0.1);
  padding: 10px 20px;
  border-radius: 10px;
  margin-bottom: 10px;
}
.player-name { font-weight: bold; color: white; }
.player-cards-count { font-size: 14px; color: #aaa; }
.player-status { font-size: 12px; margin-top: 5px; color: #aaa; }
.player-status.current { color: #e74c3c; animation: pulse 1s infinite; }
.player-status.landlord { color: #f1c40f; }

.hand-cards { display: flex; justify-content: center; flex-wrap: wrap; margin-top: 20px; }

.play-area {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  min-width: 300px;
  min-height: 100px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.pass-text { font-size: 24px; color: #aaa; }

.bottom-cards {
  position: absolute;
  top: 20px;
  right: 20px;
  display: flex;
}

.action-buttons {
  position: absolute;
  bottom: 120px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 20px;
}
.action-btn {
  padding: 12px 30px;
  font-size: 16px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: opacity 0.2s;
}
.action-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.action-btn.primary { background: #e74c3c; color: white; }
.action-btn.secondary { background: #95a5a6; color: white; }
.action-btn.success { background: #2ecc71; color: white; }

.game-result {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.result-content {
  background: linear-gradient(135deg, #2c3e50, #1a252f);
  padding: 40px 60px;
  border-radius: 20px;
  text-align: center;
  color: white;
}
.result-content h2 { font-size: 36px; margin-bottom: 20px; }
.result-content .win { color: #f1c40f; }
.result-content .lose { color: #e74c3c; }
.result-content button {
  margin-top: 30px;
  padding: 15px 40px;
  font-size: 18px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
</style>
