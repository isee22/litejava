<template>
  <div class="game-room">
    <!-- 顶部信息栏 -->
    <header class="header">
      <div class="room-info">
        <span class="room-id">房间: {{ gameStore.roomId }}</span>
        <span class="game-type">{{ gameTypeName }}</span>
        <span v-if="gameStatus !== 'waiting'" class="round-info">
          剩余 {{ remainingCards }} 张
        </span>
      </div>
      <button class="exit-btn" @click="exitRoom">退出</button>
    </header>

    <!-- 游戏桌面 -->
    <div class="game-table">
      <!-- 左侧玩家 -->
      <div v-if="displaySeats[2]" class="player-seat left" :class="getPlayerClass(2)">
        <div class="player-avatar">{{ displaySeats[2].userId ? '👤' : '➕' }}</div>
        <div class="player-name">{{ displaySeats[2].name || '等待加入' }}</div>
        <div class="player-tags">
          <span v-if="isLandlordSeat(displaySeats[2])" class="tag landlord">👑地主</span>
          <span v-if="gameStatus === 'waiting' && displaySeats[2].ready" class="tag ready">已准备</span>
          <span v-if="gameStatus !== 'waiting'" class="tag cards">{{ displaySeats[2].cardCount || 0 }}张</span>
        </div>
        <!-- 出牌区 -->
        <div class="play-zone">
          <template v-if="playedCards[2]">
            <Card v-for="(c, i) in decodeCards(playedCards[2])" :key="i" :card="c" small />
          </template>
          <span v-else-if="passedSeats[2]" class="pass">不出</span>
        </div>
      </div>

      <!-- 右侧玩家 -->
      <div v-if="displaySeats[1]" class="player-seat right" :class="getPlayerClass(1)">
        <div class="player-avatar">{{ displaySeats[1].userId ? '👤' : '➕' }}</div>
        <div class="player-name">{{ displaySeats[1].name || '等待加入' }}</div>
        <div class="player-tags">
          <span v-if="isLandlordSeat(displaySeats[1])" class="tag landlord">👑地主</span>
          <span v-if="gameStatus === 'waiting' && displaySeats[1].ready" class="tag ready">已准备</span>
          <span v-if="gameStatus !== 'waiting'" class="tag cards">{{ displaySeats[1].cardCount || 0 }}张</span>
        </div>
        <!-- 出牌区 -->
        <div class="play-zone">
          <template v-if="playedCards[1]">
            <Card v-for="(c, i) in decodeCards(playedCards[1])" :key="i" :card="c" small />
          </template>
          <span v-else-if="passedSeats[1]" class="pass">不出</span>
        </div>
      </div>

      <!-- 中央区域 -->
      <div class="center-area">
        <!-- 底牌 -->
        <div v-if="bottomCards.length" class="bottom-cards">
          <span class="label">底牌</span>
          <div class="cards-row">
            <Card v-for="(c, i) in decodeCards(bottomCards)" :key="i" :card="c" small />
          </div>
        </div>

        <!-- 倒计时 -->
        <div v-if="countdown > 0 && gameStatus !== 'waiting'" class="countdown-clock" :class="{ urgent: countdown <= 5 }">
          <div class="clock-icon">⏰</div>
          <div class="clock-time">{{ countdown }}</div>
        </div>

        <!-- 我的出牌区 -->
        <div class="my-play-zone">
          <template v-if="playedCards[0]">
            <Card v-for="(c, i) in decodeCards(playedCards[0])" :key="i" :card="c" small />
          </template>
          <span v-else-if="passedSeats[0]" class="pass">不出</span>
        </div>
      </div>

      <!-- 底部 - 我的区域 -->
      <div class="my-area">
        <div class="my-info" :class="{ current: isMyTurn }">
          <span class="my-name">{{ userStore.playerName }} (我)</span>
          <span v-if="isLandlordSeat(displaySeats[0])" class="tag landlord">👑地主</span>
          <span v-if="gameStatus === 'waiting' && myReady" class="tag ready">已准备</span>
          <span v-if="gameStatus !== 'waiting'" class="tag cards">{{ myCards.length }}张</span>
        </div>

        <!-- 手牌 -->
        <div class="hand-cards">
          <Card 
            v-for="(card, i) in myCardsDecoded" 
            :key="i"
            :card="card"
            :selected="isCardSelected(card)"
            @click="toggleCard(card)"
          />
        </div>

        <!-- 操作按钮 -->
        <div class="action-bar">
          <!-- 等待阶段 -->
          <template v-if="gameStatus === 'waiting'">
            <button v-if="!isOwner && !myReady" class="btn success" @click="setReady">准备</button>
            <button v-if="!isOwner && myReady" class="btn" @click="cancelReady">取消准备</button>
            <span v-if="isOwner" class="tip">等待玩家准备...</span>
          </template>

          <!-- 叫地主阶段 -->
          <template v-if="gameStatus === 'bidding' && isMyTurn">
            <button class="btn primary" @click="bid(true)">{{ bidButtonText }}</button>
            <button class="btn" @click="bid(false)">{{ noBidButtonText }}</button>
          </template>

          <!-- 出牌阶段 -->
          <template v-if="gameStatus === 'playing' && isMyTurn">
            <button class="btn hint" @click="getHint">💡 提示</button>
            <button class="btn primary" @click="doPlayCards">出牌</button>
            <button v-if="canPass" class="btn" @click="doPass">不出</button>
          </template>

          <!-- 等待其他玩家 -->
          <span v-if="gameStatus !== 'waiting' && !isMyTurn" class="tip">等待其他玩家...</span>
        </div>
      </div>
    </div>

    <!-- 游戏结果弹窗 -->
    <div v-if="showResult" class="result-modal">
      <div class="result-box">
        <h2 :class="resultWin ? 'win' : 'lose'">{{ resultWin ? '🎉 胜利！' : '😢 失败' }}</h2>
        <p>{{ resultDetail }}</p>
        <button class="btn primary" @click="closeResult">继续</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useGameStore } from '../stores/game'
import { wsManager, Cmd, GameType, hallApi, getErrMsg } from '../utils/websocket'
import { showMessage } from '../utils/message'
import Card from '../components/Card.vue'

const router = useRouter()
const userStore = useUserStore()
const gameStore = useGameStore()

// 房间状态
const seats = ref([])
const ownerId = ref(0)
const mySeatIndex = ref(-1)

// 游戏状态
const gameStatus = ref('waiting') // waiting, bidding, playing
const myCards = ref([])           // 服务端编码 (0-53)
const selectedCards = ref([])     // 客户端格式 {suit, rank}
const bottomCards = ref([])
const landlordSeat = ref(-1)
const currentSeat = ref(-1)
const lastCards = ref([])
const lastPlaySeat = ref(-1)
const playedCards = ref({})       // { displayIndex: [服务端编码] }
const passedSeats = ref({})
const showResult = ref(false)
const resultWin = ref(false)
const resultDetail = ref('')
const countdown = ref(0)
const countdownTimer = ref(null)
const bidCount = ref(0)
const hintIndex = ref(0)
const remainingCards = ref(54)

// 游戏类型名称
const gameTypeNames = {
  [GameType.DOUDIZHU]: '斗地主',
  [GameType.MAHJONG]: '麻将',
  [GameType.GOBANG]: '五子棋'
}
const gameTypeName = computed(() => gameTypeNames[gameStore.gameType] || '游戏')

// 计算属性
const isOwner = computed(() => userStore.playerId === ownerId.value)
const myReady = computed(() => seats.value.find(s => s.userId === userStore.playerId)?.ready || false)
const isMyTurn = computed(() => mySeatIndex.value >= 0 && currentSeat.value === mySeatIndex.value)
const canPass = computed(() => lastCards.value.length > 0 && lastPlaySeat.value !== mySeatIndex.value)
const bidButtonText = computed(() => bidCount.value === 0 ? '叫地主' : '抢地主')
const noBidButtonText = computed(() => bidCount.value === 0 ? '不叫' : '不抢')

// 手牌解码后的格式
const myCardsDecoded = computed(() => decodeCards(myCards.value))

// 座位显示顺序 (自己在底部)
const displaySeats = computed(() => {
  if (seats.value.length === 0) return []
  const myIdx = seats.value.findIndex(s => s.userId === userStore.playerId)
  if (myIdx < 0) return seats.value
  const result = []
  for (let i = 0; i < seats.value.length; i++) {
    result.push(seats.value[(myIdx + i) % seats.value.length])
  }
  return result
})

// ========== 牌编码转换 ==========
function decodeCard(code) {
  if (code === 52) return { suit: 0, rank: 16 }  // 小王
  if (code === 53) return { suit: 0, rank: 17 }  // 大王
  const suit = Math.floor(code / 13)
  const rank = (code % 13) + 3
  return { suit, rank }
}

function decodeCards(codes) {
  if (!codes || !Array.isArray(codes)) return []
  return codes.map(decodeCard)
}

function encodeCard(card) {
  if (card.rank === 16) return 52
  if (card.rank === 17) return 53
  return card.suit * 13 + (card.rank - 3)
}

function encodeCards(cards) {
  return cards.map(encodeCard)
}

// ========== UI 辅助 ==========
function getPlayerClass(displayIdx) {
  const seat = displaySeats.value[displayIdx]
  if (!seat) return {}
  return {
    empty: !seat.userId,
    current: gameStatus.value !== 'waiting' && seat.seatIndex === currentSeat.value,
    landlord: seat.seatIndex === landlordSeat.value
  }
}

function isLandlordSeat(seat) {
  return seat && landlordSeat.value >= 0 && seat.seatIndex === landlordSeat.value
}

function isCardSelected(card) {
  return selectedCards.value.some(c => c.suit === card.suit && c.rank === card.rank)
}

function toggleCard(card) {
  if (gameStatus.value !== 'playing' || !isMyTurn.value) return
  const idx = selectedCards.value.findIndex(c => c.suit === card.suit && c.rank === card.rank)
  if (idx >= 0) {
    selectedCards.value.splice(idx, 1)
  } else {
    selectedCards.value.push(card)
  }
}

function getDisplayIndex(seatIndex) {
  const myIdx = seats.value.findIndex(s => s.userId === userStore.playerId)
  if (myIdx < 0) return seatIndex
  return (seatIndex - myIdx + seats.value.length) % seats.value.length
}

function clearPlayedCards() {
  playedCards.value = {}
  passedSeats.value = {}
}

// ========== 倒计时 ==========
function startCountdown(seconds) {
  stopCountdown()
  countdown.value = seconds
  countdownTimer.value = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) stopCountdown()
  }, 1000)
}

function stopCountdown() {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
    countdownTimer.value = null
  }
  countdown.value = 0
}

// ========== 房间操作 ==========
async function exitRoom() {
  wsManager.send(Cmd.ROOM_EXIT, {})
  try { await hallApi.clearUserRoom(userStore.playerId) } catch (e) {}
}

function setReady() { wsManager.send(Cmd.READY, {}) }
function cancelReady() { wsManager.send(Cmd.CANCEL_READY, {}) }

// ========== 游戏操作 ==========
function bid(wantBid) {
  wsManager.send(Cmd.DDZ_BID, { bid: wantBid })
  stopCountdown()
}

function doPlayCards() {
  if (selectedCards.value.length === 0) {
    showMessage('请选择要出的牌')
    return
  }
  const codes = encodeCards(selectedCards.value)
  wsManager.send(Cmd.DDZ_PLAY, { cards: codes })
  selectedCards.value = []
  stopCountdown()
  hintIndex.value = 0
}

function doPass() {
  wsManager.send(Cmd.DDZ_PASS, {})
  stopCountdown()
}

function closeResult() {
  showResult.value = false
}


// ========== 提示功能 ==========
function analyzeHand(cards) {
  const hints = []
  const rankCount = {}
  
  for (const card of cards) {
    rankCount[card.rank] = (rankCount[card.rank] || [])
    rankCount[card.rank].push(card)
  }
  
  const ranks = Object.keys(rankCount).map(Number).sort((a, b) => a - b)
  
  // 单张
  for (const rank of ranks) {
    hints.push({ type: '单张', cards: [rankCount[rank][0]], power: rank })
  }
  
  // 对子
  for (const rank of ranks) {
    if (rankCount[rank].length >= 2) {
      hints.push({ type: '对子', cards: rankCount[rank].slice(0, 2), power: rank })
    }
  }
  
  // 三张
  for (const rank of ranks) {
    if (rankCount[rank].length >= 3) {
      hints.push({ type: '三张', cards: rankCount[rank].slice(0, 3), power: rank })
    }
  }
  
  // 三带一
  for (const rank of ranks) {
    if (rankCount[rank].length >= 3) {
      for (const otherRank of ranks) {
        if (otherRank !== rank && rankCount[otherRank].length >= 1) {
          hints.push({ type: '三带一', cards: [...rankCount[rank].slice(0, 3), rankCount[otherRank][0]], power: rank })
          break
        }
      }
    }
  }
  
  // 炸弹
  for (const rank of ranks) {
    if (rankCount[rank].length === 4) {
      hints.push({ type: '炸弹', cards: rankCount[rank].slice(0, 4), power: rank + 100 })
    }
  }
  
  // 王炸
  if (rankCount[16] && rankCount[17]) {
    hints.push({ type: '王炸', cards: [rankCount[16][0], rankCount[17][0]], power: 200 })
  }
  
  // 顺子
  const seqRanks = ranks.filter(r => r >= 3 && r <= 14)
  for (let len = 5; len <= seqRanks.length; len++) {
    for (let i = 0; i <= seqRanks.length - len; i++) {
      let isSeq = true
      for (let j = 1; j < len; j++) {
        if (seqRanks[i + j] !== seqRanks[i] + j) { isSeq = false; break }
      }
      if (isSeq) {
        const seqCards = []
        for (let j = 0; j < len; j++) seqCards.push(rankCount[seqRanks[i + j]][0])
        hints.push({ type: '顺子', cards: seqCards, power: seqRanks[i] })
      }
    }
  }
  
  return hints
}

function filterHints(hints, lastCardsCodes) {
  if (!lastCardsCodes || lastCardsCodes.length === 0) return hints
  
  const lastDecoded = decodeCards(lastCardsCodes)
  const lastType = detectCardType(lastDecoded)
  if (!lastType) return []
  
  return hints.filter(hint => {
    if (hint.type === '王炸') return true
    if (hint.type === '炸弹') {
      if (lastType.type !== '炸弹') return true
      return hint.power > lastType.power
    }
    if (hint.cards.length !== lastDecoded.length) return false
    if (hint.type !== lastType.type) return false
    return hint.power > lastType.power
  })
}

function detectCardType(cards) {
  if (!cards || cards.length === 0) return null
  
  const rankCount = {}
  for (const card of cards) {
    rankCount[card.rank] = (rankCount[card.rank] || 0) + 1
  }
  const ranks = Object.keys(rankCount).map(Number)
  const counts = Object.values(rankCount)
  
  if (cards.length === 2 && rankCount[16] && rankCount[17]) return { type: '王炸', power: 200 }
  if (cards.length === 4 && counts[0] === 4) return { type: '炸弹', power: ranks[0] + 100 }
  if (cards.length === 1) return { type: '单张', power: ranks[0] }
  if (cards.length === 2 && counts[0] === 2) return { type: '对子', power: ranks[0] }
  if (cards.length === 3 && counts[0] === 3) return { type: '三张', power: ranks[0] }
  if (cards.length === 4 && counts.includes(3)) return { type: '三带一', power: ranks.find(r => rankCount[r] === 3) }
  
  if (cards.length >= 5 && counts.every(c => c === 1)) {
    ranks.sort((a, b) => a - b)
    let isSeq = true
    for (let i = 1; i < ranks.length; i++) {
      if (ranks[i] !== ranks[i-1] + 1 || ranks[i] > 14) { isSeq = false; break }
    }
    if (isSeq) return { type: '顺子', power: ranks[0] }
  }
  
  return null
}

function getHint() {
  const allHints = analyzeHand(myCardsDecoded.value)
  const validHints = filterHints(allHints, lastCards.value)
  
  if (validHints.length === 0) {
    showMessage('没有能出的牌')
    return
  }
  
  hintIndex.value = hintIndex.value % validHints.length
  const hint = validHints[hintIndex.value]
  selectedCards.value = [...hint.cards]
  showMessage(`提示: ${hint.type}`)
  hintIndex.value++
}

// ========== 消息处理 ==========
function onLoginResult(data) {
  console.log('[Room] LOGIN:', data)
  const code = data.code !== undefined ? data.code : 0
  if (code !== 0) {
    showMessage('进入房间失败')
    router.push('/lobby')
    return
  }
  
  seats.value = data.seats || []
  ownerId.value = data.ownerId || 0
  mySeatIndex.value = data.seatIndex !== undefined ? data.seatIndex : -1
  gameStore.setSeatIndex(mySeatIndex.value)
  seats.value.forEach(s => { s.cardCount = 17 })
  
  // 断线重连：恢复游戏状态
  const game = data.game
  if (game && game.status > 0) {
    console.log('[Room] 恢复游戏状态:', game)
    
    // status: 1=叫地主, 2=出牌
    if (game.status === 1) {
      gameStatus.value = 'bidding'
    } else if (game.status === 2) {
      gameStatus.value = 'playing'
    }
    
    currentSeat.value = game.currentSeat !== undefined ? game.currentSeat : -1
    landlordSeat.value = game.landlordSeat !== undefined ? game.landlordSeat : -1
    
    // 恢复手牌
    if (game.myCards && game.myCards.length > 0) {
      myCards.value = game.myCards.sort((a, b) => b - a)
    }
    
    // 恢复上家出的牌
    if (game.lastCards && game.lastCards.length > 0) {
      lastCards.value = game.lastCards
      lastPlaySeat.value = game.lastPlaySeat !== undefined ? game.lastPlaySeat : -1
    }
    
    // 恢复底牌
    if (game.bottomCards && game.bottomCards.length > 0) {
      bottomCards.value = game.bottomCards
    }
    
    // 更新地主手牌数量
    if (landlordSeat.value >= 0) {
      const landlordSeatObj = seats.value.find(s => s.seatIndex === landlordSeat.value)
      if (landlordSeatObj) landlordSeatObj.cardCount = 20
    }
    
    // 开始倒计时
    startCountdown(15)
    showMessage('已重连到游戏')
  }
}

function onUserJoin(data) {
  const seat = seats.value.find(s => s.seatIndex === data.seatIndex)
  if (seat) {
    seat.userId = data.userId
    seat.name = data.name
    seat.ready = false
    seat.online = true
    seat.cardCount = 17
  }
}

function onUserExit(data) {
  const seat = seats.value.find(s => s.userId === data.userId)
  if (seat) {
    seat.userId = 0
    seat.name = ''
    seat.ready = false
  }
}

function onReady(data) {
  const seat = seats.value.find(s => s.userId === data.userId)
  if (seat) seat.ready = data.ready !== false
}

function onRoomExitResult(data) {
  if (data.kicked) showMessage('你被踢出了房间')
  hallApi.clearUserRoom(userStore.playerId).catch(() => {})
  router.push('/lobby')
}

function onGameStart(data) {
  console.log('[Room] GAME_START:', data)
  gameStatus.value = 'bidding'
  currentSeat.value = data.bidSeat
  clearPlayedCards()
  bottomCards.value = []
  landlordSeat.value = -1
  lastCards.value = []
  lastPlaySeat.value = -1
  bidCount.value = 0
  seats.value.forEach(s => { if (s.userId) s.cardCount = 17 })
  remainingCards.value = 54
  startCountdown(data.timeout || 15)
  showMessage('游戏开始！')
}

function onDeal(data) {
  console.log('[Room] DEAL:', data)
  myCards.value = (data.cards || []).sort((a, b) => b - a)
  selectedCards.value = []
}

function onBidResult(data) {
  console.log('[Room] BID_RESULT:', data)
  stopCountdown()
  
  if (data.landlordSeat !== undefined && data.landlordSeat >= 0) {
    landlordSeat.value = data.landlordSeat
    gameStatus.value = 'playing'
    currentSeat.value = data.landlordSeat
    bottomCards.value = data.bottomCards || []
    
    if (data.landlordSeat === mySeatIndex.value && bottomCards.value.length) {
      myCards.value = [...myCards.value, ...bottomCards.value].sort((a, b) => b - a)
    }
    
    const landlordSeatObj = seats.value.find(s => s.seatIndex === data.landlordSeat)
    if (landlordSeatObj) landlordSeatObj.cardCount = 20
    
    showMessage(`座位${data.landlordSeat + 1} 成为地主`)
    startCountdown(15)
  } else if (data.redeal) {
    showMessage('无人叫地主，重新发牌')
    bidCount.value = 0
    currentSeat.value = data.nextSeat !== undefined ? data.nextSeat : data.nextBidSeat
    startCountdown(15)
  } else {
    if (data.bid) bidCount.value++
    const bidText = bidCount.value <= 1 ? (data.bid ? '叫地主' : '不叫') : (data.bid ? '抢地主' : '不抢')
    const seatIdx = data.seatIndex !== undefined ? data.seatIndex : data.seat
    showMessage(`座位${seatIdx + 1} ${bidText}`)
    currentSeat.value = data.nextSeat !== undefined ? data.nextSeat : data.nextBidSeat
    startCountdown(15)
  }
}

function onPlayResult(data) {
  console.log('[Room] PLAY_RESULT:', data)
  stopCountdown()
  
  const seatIdx = data.seatIndex !== undefined ? data.seatIndex : data.seat
  const displayIdx = getDisplayIndex(seatIdx)
  
  if (data.pass) {
    passedSeats.value[displayIdx] = true
    playedCards.value[displayIdx] = null
  } else {
    const cards = data.cards || []
    playedCards.value[displayIdx] = cards
    passedSeats.value[displayIdx] = false
    lastCards.value = cards
    lastPlaySeat.value = seatIdx
    
    const seat = seats.value.find(s => s.seatIndex === seatIdx)
    if (seat) seat.cardCount = data.remainCards !== undefined ? data.remainCards : (seat.cardCount - cards.length)
    
    if (seatIdx === mySeatIndex.value) {
      myCards.value = myCards.value.filter(c => !cards.includes(c))
    }
    
    remainingCards.value -= cards.length
  }
  
  if (data.clearLast) {
    lastCards.value = []
    lastPlaySeat.value = -1
    clearPlayedCards()
  }
  
  if (data.gameOver) {
    onGameOver(data)
    return
  }
  
  currentSeat.value = data.nextSeat
  startCountdown(15)
}

function onGameOver(data) {
  console.log('[Room] GAME_OVER:', data)
  stopCountdown()
  gameStatus.value = 'waiting'
  currentSeat.value = -1
  
  const isWinner = (data.landlordWin && mySeatIndex.value === landlordSeat.value) ||
                   (!data.landlordWin && mySeatIndex.value !== landlordSeat.value)
  
  resultWin.value = isWinner
  resultDetail.value = data.landlordWin ? '地主获胜' : '农民获胜'
  showResult.value = true
  
  landlordSeat.value = -1
  myCards.value = []
  bottomCards.value = []
  bidCount.value = 0
}

function onPlayError(data) {
  if (data.code && data.code !== 0) {
    showMessage(getErrMsg(data.code))
  }
}

onMounted(() => {
  wsManager.on(Cmd.LOGIN, onLoginResult)
  wsManager.on(Cmd.USER_JOIN, onUserJoin)
  wsManager.on(Cmd.USER_EXIT, onUserExit)
  wsManager.on(Cmd.READY, onReady)
  wsManager.on(Cmd.ROOM_EXIT, onRoomExitResult)
  wsManager.on(Cmd.GAME_START, onGameStart)
  wsManager.on(Cmd.DEAL, onDeal)
  wsManager.on(Cmd.DDZ_BID_RESULT, onBidResult)
  wsManager.on(Cmd.DDZ_PLAY, onPlayError)
  wsManager.on(Cmd.DDZ_PLAY_RESULT, onPlayResult)
  wsManager.on(Cmd.GAME_OVER, onGameOver)
  
  if (wsManager.lastLoginData) {
    console.log('[Room] 使用缓存的 LOGIN 数据')
    onLoginResult(wsManager.lastLoginData)
  }
})

onUnmounted(() => {
  stopCountdown()
  wsManager.off(Cmd.LOGIN, onLoginResult)
  wsManager.off(Cmd.USER_JOIN, onUserJoin)
  wsManager.off(Cmd.USER_EXIT, onUserExit)
  wsManager.off(Cmd.READY, onReady)
  wsManager.off(Cmd.ROOM_EXIT, onRoomExitResult)
  wsManager.off(Cmd.GAME_START, onGameStart)
  wsManager.off(Cmd.DEAL, onDeal)
  wsManager.off(Cmd.DDZ_BID_RESULT, onBidResult)
  wsManager.off(Cmd.DDZ_PLAY, onPlayError)
  wsManager.off(Cmd.DDZ_PLAY_RESULT, onPlayResult)
  wsManager.off(Cmd.GAME_OVER, onGameOver)
})
</script>

<style scoped>
.game-room {
  min-height: 100vh;
  background: linear-gradient(135deg, #0d4f3c 0%, #1a5c3e 50%, #0d4f3c 100%);
  color: white;
  display: flex;
  flex-direction: column;
}

/* 顶部 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: rgba(0, 0, 0, 0.4);
}
.room-info { display: flex; align-items: center; gap: 15px; }
.room-id { font-weight: bold; }
.game-type { padding: 4px 12px; background: #e74c3c; border-radius: 12px; font-size: 13px; }
.round-info { color: #f1c40f; }
.exit-btn { padding: 6px 16px; background: #c0392b; color: white; border: none; border-radius: 4px; cursor: pointer; }

/* 游戏桌面 */
.game-table {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 20px;
}

/* 玩家座位 */
.player-seat {
  position: absolute;
  width: 150px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 12px;
  text-align: center;
  border: 2px solid transparent;
}
.player-seat.current { border-color: #f1c40f; box-shadow: 0 0 15px rgba(241, 196, 15, 0.5); }
.player-seat.landlord .player-avatar { color: #f1c40f; }
.player-seat.left { left: 30px; top: 50%; transform: translateY(-50%); }
.player-seat.right { right: 30px; top: 50%; transform: translateY(-50%); }

.player-avatar { font-size: 40px; margin-bottom: 8px; }
.player-name { font-size: 14px; font-weight: bold; margin-bottom: 6px; }
.player-tags { display: flex; justify-content: center; gap: 4px; flex-wrap: wrap; }
.tag { padding: 2px 8px; border-radius: 10px; font-size: 11px; }
.tag.landlord { background: #f39c12; }
.tag.ready { background: #27ae60; }
.tag.cards { background: rgba(255,255,255,0.2); }

.play-zone { margin-top: 10px; min-height: 80px; display: flex; justify-content: center; flex-wrap: wrap; }
.pass { color: #aaa; font-size: 18px; }

/* 中央区域 */
.center-area {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.bottom-cards { text-align: center; }
.bottom-cards .label { font-size: 12px; color: #aaa; margin-bottom: 5px; display: block; }
.cards-row { display: flex; justify-content: center; }

/* 倒计时 */
.countdown-clock {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  width: 70px;
  height: 70px;
  justify-content: center;
  border: 3px solid #f1c40f;
  box-shadow: 0 0 15px rgba(241, 196, 60, 0.5);
}
.countdown-clock.urgent { border-color: #e74c3c; box-shadow: 0 0 20px rgba(231, 76, 60, 0.8); animation: shake 0.5s infinite; }
.clock-icon { font-size: 20px; }
.clock-time { font-size: 22px; font-weight: bold; color: #f1c40f; }
.countdown-clock.urgent .clock-time { color: #e74c3c; }

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-2px); }
  75% { transform: translateX(2px); }
}

.my-play-zone { min-height: 100px; display: flex; justify-content: center; }

/* 我的区域 */
.my-area {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.4);
  padding: 15px 20px;
}

.my-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 10px;
  padding: 8px;
  border-radius: 8px;
}
.my-info.current { background: rgba(241, 196, 15, 0.2); border: 1px solid #f1c40f; }
.my-name { font-weight: bold; }

.hand-cards {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 15px;
  min-height: 110px;
}

/* 操作按钮 */
.action-bar {
  display: flex;
  justify-content: center;
  gap: 15px;
  align-items: center;
}

.btn {
  padding: 10px 30px;
  font-size: 16px;
  font-weight: bold;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  transition: all 0.2s;
}
.btn:hover { background: rgba(255, 255, 255, 0.3); }
.btn.primary { background: #e74c3c; }
.btn.primary:hover { background: #c0392b; }
.btn.success { background: #27ae60; }
.btn.success:hover { background: #1e8449; }
.btn.hint { background: #f39c12; }
.btn.hint:hover { background: #d68910; }

.tip { color: rgba(255, 255, 255, 0.6); font-size: 14px; }

/* 结果弹窗 */
.result-modal {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.result-box {
  background: linear-gradient(135deg, #2c3e50, #1a252f);
  padding: 40px 60px;
  border-radius: 20px;
  text-align: center;
}
.result-box h2 { font-size: 32px; margin-bottom: 15px; }
.result-box .win { color: #f1c40f; }
.result-box .lose { color: #e74c3c; }
.result-box p { margin-bottom: 25px; color: #aaa; }
</style>