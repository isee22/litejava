<template>
  <div class="werewolf-game">
    <!-- 顶部信息栏 -->
    <header class="game-header">
      <div class="day-info">
        <span class="day">第 {{ day }} 天</span>
        <span class="phase">{{ phaseName }}</span>
      </div>
      <div class="role-info" v-if="myRole !== null">
        <span class="role-icon">{{ roleIcons[myRole] }}</span>
        <span class="role-name">{{ roleNames[myRole] }}</span>
      </div>
      <div class="timer" v-if="timeout > 0">
        ⏱️ {{ timeout }}s
      </div>
    </header>

    <!-- 玩家座位区 -->
    <div class="players-area">
      <div 
        v-for="(player, index) in players" 
        :key="index"
        class="player-seat"
        :class="{ 
          dead: !player.alive,
          me: index === mySeat,
          wolf: isWolf && wolfSeats.includes(index),
          selected: selectedTarget === index,
          canSelect: canSelectTargets.includes(index),
          speaking: currentSpeaker === index
        }"
        @click="selectTarget(index)"
      >
        <div class="seat-number">{{ index + 1 }}号</div>
        <div class="avatar">{{ player.alive ? '👤' : '💀' }}</div>
        <div class="player-name">{{ player.name || '玩家' + (index + 1) }}</div>
        <div class="badges">
          <span v-if="isWolf && wolfSeats.includes(index)" class="wolf-badge">🐺</span>
          <span v-if="player.role !== undefined && gameOver" class="role-badge">
            {{ roleIcons[player.role] }}
          </span>
        </div>
        <div v-if="votes[index] !== undefined" class="vote-count">
          {{ votes[index] }}票
        </div>
      </div>
    </div>

    <!-- 中央信息区 -->
    <div class="center-area">
      <!-- 夜晚遮罩 -->
      <div v-if="isNight" class="night-overlay">
        <div class="moon">🌙</div>
        <div class="night-text">{{ phaseName }}</div>
      </div>

      <!-- 死亡公告 -->
      <div v-if="deathAnnounce" class="death-announce">
        <div class="announce-title">{{ deathAnnounce.reason }}</div>
        <div class="dead-list">
          <span v-for="seat in deathAnnounce.deadSeats" :key="seat">
            {{ seat + 1 }}号玩家
          </span>
          <span v-if="deathAnnounce.deadSeats.length === 0">平安夜</span>
        </div>
      </div>

      <!-- 游戏结束 -->
      <div v-if="gameOver" class="game-over">
        <div class="winner">{{ winnerName }} 胜利!</div>
        <button class="back-btn" @click="backToLobby">返回大厅</button>
      </div>
    </div>

    <!-- 操作面板 -->
    <div class="action-panel" v-if="currentAction && !gameOver">
      <!-- 狼人杀人 -->
      <div v-if="currentAction === 'kill'" class="action-box">
        <h3>🐺 选择要杀的目标</h3>
        <p>点击玩家头像选择目标，-1表示空刀</p>
        <div class="action-buttons">
          <button class="action-btn" @click="doWolfKill" :disabled="selectedTarget === null">
            确认击杀
          </button>
          <button class="action-btn secondary" @click="selectedTarget = -1; doWolfKill()">
            空刀
          </button>
        </div>
      </div>

      <!-- 预言家查验 -->
      <div v-if="currentAction === 'check'" class="action-box">
        <h3>🔮 选择要查验的目标</h3>
        <div class="action-buttons">
          <button class="action-btn" @click="doSeerCheck" :disabled="selectedTarget === null">
            查验身份
          </button>
        </div>
        <div v-if="checkResult !== null" class="check-result">
          查验结果: {{ checkResult ? '🐺 狼人' : '👤 好人' }}
        </div>
      </div>

      <!-- 女巫用药 -->
      <div v-if="currentAction === 'witch'" class="action-box">
        <h3>🧪 女巫行动</h3>
        <div v-if="witchInfo.killedSeat >= 0" class="witch-info">
          今晚 {{ witchInfo.killedSeat + 1 }}号 被杀
        </div>
        <div class="witch-options">
          <button 
            v-if="witchInfo.hasAntidote && witchInfo.killedSeat >= 0"
            class="action-btn save" 
            @click="doWitchSave"
          >
            💊 使用解药救人
          </button>
          <button 
            v-if="witchInfo.hasPoison"
            class="action-btn poison" 
            @click="showPoisonSelect = true"
          >
            ☠️ 使用毒药
          </button>
          <button class="action-btn secondary" @click="doWitchSkip">
            跳过
          </button>
        </div>
      </div>

      <!-- 守卫守护 -->
      <div v-if="currentAction === 'protect'" class="action-box">
        <h3>🛡️ 选择要守护的目标</h3>
        <p v-if="lastGuardTarget >= 0">上一晚守护了 {{ lastGuardTarget + 1 }}号，不能连续守护</p>
        <div class="action-buttons">
          <button class="action-btn" @click="doGuardProtect" :disabled="selectedTarget === null">
            确认守护
          </button>
        </div>
      </div>

      <!-- 猎人开枪 -->
      <div v-if="currentAction === 'shoot'" class="action-box">
        <h3>🔫 猎人开枪</h3>
        <p>你死了！选择一个玩家带走</p>
        <div class="action-buttons">
          <button class="action-btn danger" @click="doHunterShoot" :disabled="selectedTarget === null">
            开枪！
          </button>
          <button class="action-btn secondary" @click="selectedTarget = -1; doHunterShoot()">
            不开枪
          </button>
        </div>
      </div>

      <!-- 发言 -->
      <div v-if="currentAction === 'speak'" class="action-box">
        <h3>🎤 轮到你发言</h3>
        <textarea v-model="speakContent" placeholder="输入发言内容..." rows="3"></textarea>
        <div class="action-buttons">
          <button class="action-btn" @click="doSpeak">发言</button>
          <button class="action-btn secondary" @click="speakContent = ''; doSpeak()">跳过</button>
        </div>
      </div>

      <!-- 投票 -->
      <div v-if="currentAction === 'vote'" class="action-box">
        <h3>🗳️ 投票放逐</h3>
        <p>选择你认为是狼人的玩家</p>
        <div class="action-buttons">
          <button class="action-btn" @click="doVote" :disabled="selectedTarget === null">
            确认投票
          </button>
          <button class="action-btn secondary" @click="selectedTarget = -1; doVote()">
            弃票
          </button>
        </div>
      </div>
    </div>

    <!-- 聊天/发言记录 -->
    <div class="chat-panel">
      <div class="chat-messages" ref="chatBox">
        <div v-for="(msg, i) in messages" :key="i" class="chat-msg" :class="msg.type">
          <span class="sender">{{ msg.sender }}:</span>
          <span class="content">{{ msg.content }}</span>
        </div>
      </div>
    </div>

    <!-- 毒药选择弹窗 -->
    <div v-if="showPoisonSelect" class="modal-overlay" @click="showPoisonSelect = false">
      <div class="modal" @click.stop>
        <h3>选择毒杀目标</h3>
        <div class="poison-targets">
          <button 
            v-for="seat in witchInfo.poisonTargets" 
            :key="seat"
            class="target-btn"
            @click="doWitchPoison(seat)"
          >
            {{ seat + 1 }}号
          </button>
        </div>
        <button class="cancel-btn" @click="showPoisonSelect = false">取消</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useGameStore } from '../stores/game'
import { wsManager } from '../utils/websocket'
import { showMessage } from '../utils/message'

// 狼人杀命令号
const WwCmd = {
  WOLF_KILL: 701,
  WOLF_KILL_RESULT: 702,
  SEER_CHECK: 703,
  SEER_CHECK_RESULT: 704,
  WITCH_USE: 705,
  WITCH_USE_RESULT: 706,
  GUARD_PROTECT: 707,
  GUARD_PROTECT_RESULT: 708,
  HUNTER_SHOOT: 709,
  HUNTER_SHOOT_RESULT: 710,
  SPEAK: 720,
  SPEAK_BROADCAST: 721,
  VOTE: 722,
  VOTE_RESULT: 723,
  PHASE_CHANGE: 730,
  DEATH_ANNOUNCE: 731,
  GAME_OVER: 732,
  ROLE_ASSIGN: 733,
  YOUR_TURN: 734,
  WOLF_TEAMMATES: 735
}

// 角色定义
const ROLE = { VILLAGER: 0, WOLF: 1, SEER: 2, WITCH: 3, GUARD: 4, HUNTER: 5 }
const roleNames = { 0: '村民', 1: '狼人', 2: '预言家', 3: '女巫', 4: '守卫', 5: '猎人' }
const roleIcons = { 0: '👤', 1: '🐺', 2: '🔮', 3: '🧪', 4: '🛡️', 5: '🔫' }

// 阶段定义
const PHASE = {
  NIGHT_START: 0, WOLF_TURN: 1, SEER_TURN: 2, WITCH_TURN: 3, GUARD_TURN: 4,
  DAY_START: 5, SPEAK: 6, VOTE: 7, LAST_WORDS: 8, GAME_OVER: 9
}
const phaseNames = {
  0: '夜晚降临', 1: '狼人请睁眼', 2: '预言家请睁眼', 3: '女巫请睁眼',
  4: '守卫请睁眼', 5: '天亮了', 6: '发言阶段', 7: '投票阶段', 8: '遗言阶段', 9: '游戏结束'
}

const router = useRouter()
const userStore = useUserStore()
const gameStore = useGameStore()

// 游戏状态
const day = ref(1)
const phase = ref(0)
const myRole = ref(null)
const mySeat = ref(-1)
const players = ref([])
const wolfSeats = ref([])
const currentSpeaker = ref(-1)
const timeout = ref(0)
const gameOver = ref(false)
const winner = ref(0)

// 操作状态
const currentAction = ref(null)
const selectedTarget = ref(null)
const canSelectTargets = ref([])
const checkResult = ref(null)
const witchInfo = ref({ killedSeat: -1, hasAntidote: false, hasPoison: false, poisonTargets: [] })
const lastGuardTarget = ref(-1)
const showPoisonSelect = ref(false)
const speakContent = ref('')
const votes = ref({})
const deathAnnounce = ref(null)
const messages = ref([])
const chatBox = ref(null)

// 计算属性
const phaseName = computed(() => phaseNames[phase.value] || '未知')
const isNight = computed(() => phase.value >= 0 && phase.value <= 4)
const isWolf = computed(() => myRole.value === ROLE.WOLF)
const winnerName = computed(() => winner.value === 1 ? '好人阵营' : '狼人阵营')

// 选择目标
function selectTarget(index) {
  if (!canSelectTargets.value.includes(index)) return
  selectedTarget.value = selectedTarget.value === index ? null : index
}

// 狼人杀人
function doWolfKill() {
  wsManager.send(WwCmd.WOLF_KILL, { targetSeat: selectedTarget.value ?? -1 })
  currentAction.value = null
}

// 预言家查验
function doSeerCheck() {
  if (selectedTarget.value === null) return
  wsManager.send(WwCmd.SEER_CHECK, { targetSeat: selectedTarget.value })
}

// 女巫救人
function doWitchSave() {
  wsManager.send(WwCmd.WITCH_USE, { useAntidote: true, usePoison: false, poisonTarget: -1 })
  currentAction.value = null
}

// 女巫毒人
function doWitchPoison(target) {
  wsManager.send(WwCmd.WITCH_USE, { useAntidote: false, usePoison: true, poisonTarget: target })
  showPoisonSelect.value = false
  currentAction.value = null
}

// 女巫跳过
function doWitchSkip() {
  wsManager.send(WwCmd.WITCH_USE, { useAntidote: false, usePoison: false, poisonTarget: -1 })
  currentAction.value = null
}

// 守卫守护
function doGuardProtect() {
  if (selectedTarget.value === null) return
  wsManager.send(WwCmd.GUARD_PROTECT, { targetSeat: selectedTarget.value })
  currentAction.value = null
}

// 猎人开枪
function doHunterShoot() {
  wsManager.send(WwCmd.HUNTER_SHOOT, { targetSeat: selectedTarget.value ?? -1 })
  currentAction.value = null
}

// 发言
function doSpeak() {
  wsManager.send(WwCmd.SPEAK, { content: speakContent.value })
  speakContent.value = ''
  currentAction.value = null
}

// 投票
function doVote() {
  wsManager.send(WwCmd.VOTE, { targetSeat: selectedTarget.value ?? -1 })
  currentAction.value = null
}

// 返回大厅
function backToLobby() {
  router.push('/lobby')
}

// 添加消息
function addMessage(sender, content, type = 'normal') {
  messages.value.push({ sender, content, type })
  nextTick(() => {
    if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
  })
}

// === 消息处理 ===

function onRoleAssign(data) {
  myRole.value = data.role
  addMessage('系统', `你的身份是: ${roleNames[data.role]} ${roleIcons[data.role]}`, 'system')
}

function onWolfTeammates(data) {
  wolfSeats.value = data.wolfSeats || []
  const teammates = wolfSeats.value.map(s => `${s + 1}号`).join(', ')
  addMessage('系统', `你的狼人同伴: ${teammates}`, 'system')
}

function onPhaseChange(data) {
  phase.value = data.phase
  day.value = data.day
  currentSpeaker.value = data.currentSpeaker ?? -1
  deathAnnounce.value = null
  selectedTarget.value = null
  checkResult.value = null
  addMessage('系统', phaseNames[data.phase], 'phase')
}

function onYourTurn(data) {
  currentAction.value = data.action
  canSelectTargets.value = data.targets || []
  timeout.value = data.timeout || 30
  
  // 女巫特殊信息
  if (data.action === 'witch' || data.killedSeat !== undefined) {
    currentAction.value = 'witch'
    witchInfo.value = {
      killedSeat: data.killedSeat ?? -1,
      hasAntidote: data.hasAntidote ?? false,
      hasPoison: data.hasPoison ?? false,
      poisonTargets: data.poisonTargets || []
    }
  }
  
  // 倒计时
  const timer = setInterval(() => {
    timeout.value--
    if (timeout.value <= 0) clearInterval(timer)
  }, 1000)
}

function onWolfKillResult(data) {
  if (data.confirmed) {
    const target = data.finalTarget >= 0 ? `${data.finalTarget + 1}号` : '空刀'
    addMessage('狼人', `决定击杀: ${target}`, 'wolf')
  }
}

function onSeerCheckResult(data) {
  checkResult.value = data.isWolf
  const result = data.isWolf ? '🐺 狼人' : '👤 好人'
  addMessage('系统', `${data.targetSeat + 1}号 是 ${result}`, 'system')
  currentAction.value = null
}

function onDeathAnnounce(data) {
  deathAnnounce.value = data
  // 更新玩家存活状态
  for (const seat of data.deadSeats) {
    if (players.value[seat]) players.value[seat].alive = false
  }
  const deadStr = data.deadSeats.length > 0 
    ? data.deadSeats.map(s => `${s + 1}号`).join(', ') 
    : '无人'
  addMessage('系统', `${data.reason}: ${deadStr}`, 'death')
}

function onSpeakBroadcast(data) {
  addMessage(`${data.seatIndex + 1}号`, data.content, 'speak')
}

function onVoteResult(data) {
  votes.value = {}
  // 统计每个人获得的票数
  for (const [voter, target] of Object.entries(data.votes)) {
    if (target >= 0) {
      votes.value[target] = (votes.value[target] || 0) + 1
    }
  }
  
  if (data.eliminatedSeat >= 0) {
    addMessage('系统', `${data.eliminatedSeat + 1}号 被投票出局`, 'death')
    if (players.value[data.eliminatedSeat]) {
      players.value[data.eliminatedSeat].alive = false
    }
  } else {
    addMessage('系统', '平票，无人出局', 'system')
  }
}

function onHunterShootResult(data) {
  if (data.targetSeat >= 0) {
    addMessage('系统', `猎人 ${data.hunterSeat + 1}号 开枪带走了 ${data.targetSeat + 1}号`, 'death')
    if (players.value[data.targetSeat]) {
      players.value[data.targetSeat].alive = false
    }
  }
}

function onGameOver(data) {
  gameOver.value = true
  winner.value = data.winner
  // 显示所有角色
  for (const [seat, role] of Object.entries(data.roles)) {
    if (players.value[seat]) players.value[seat].role = role
  }
  addMessage('系统', `游戏结束! ${data.winnerName} 胜利!`, 'gameover')
}

function onLoginResult(data) {
  if (data.code === 0) {
    mySeat.value = data.seatIndex ?? gameStore.mySeat
    // 初始化玩家列表
    players.value = (data.seats || []).map((s, i) => ({
      name: s.name || `玩家${i + 1}`,
      alive: true,
      seatIndex: i
    }))
    // 恢复游戏状态
    if (data.game) {
      day.value = data.game.day || 1
      phase.value = data.game.phase || 0
      myRole.value = data.game.myRole
      if (data.game.alive) {
        data.game.alive.forEach((a, i) => {
          if (players.value[i]) players.value[i].alive = a
        })
      }
      if (data.game.wolfSeats) wolfSeats.value = data.game.wolfSeats
    }
  }
}

onMounted(() => {
  mySeat.value = gameStore.mySeat
  
  // 注册消息处理
  wsManager.on(WwCmd.ROLE_ASSIGN, onRoleAssign)
  wsManager.on(WwCmd.WOLF_TEAMMATES, onWolfTeammates)
  wsManager.on(WwCmd.PHASE_CHANGE, onPhaseChange)
  wsManager.on(WwCmd.YOUR_TURN, onYourTurn)
  wsManager.on(WwCmd.WOLF_KILL_RESULT, onWolfKillResult)
  wsManager.on(WwCmd.SEER_CHECK_RESULT, onSeerCheckResult)
  wsManager.on(WwCmd.DEATH_ANNOUNCE, onDeathAnnounce)
  wsManager.on(WwCmd.SPEAK_BROADCAST, onSpeakBroadcast)
  wsManager.on(WwCmd.VOTE_RESULT, onVoteResult)
  wsManager.on(WwCmd.HUNTER_SHOOT_RESULT, onHunterShootResult)
  wsManager.on(WwCmd.GAME_OVER, onGameOver)
  wsManager.on(2, onLoginResult) // LOGIN_RESULT
})

onUnmounted(() => {
  wsManager.off(WwCmd.ROLE_ASSIGN, onRoleAssign)
  wsManager.off(WwCmd.WOLF_TEAMMATES, onWolfTeammates)
  wsManager.off(WwCmd.PHASE_CHANGE, onPhaseChange)
  wsManager.off(WwCmd.YOUR_TURN, onYourTurn)
  wsManager.off(WwCmd.WOLF_KILL_RESULT, onWolfKillResult)
  wsManager.off(WwCmd.SEER_CHECK_RESULT, onSeerCheckResult)
  wsManager.off(WwCmd.DEATH_ANNOUNCE, onDeathAnnounce)
  wsManager.off(WwCmd.SPEAK_BROADCAST, onSpeakBroadcast)
  wsManager.off(WwCmd.VOTE_RESULT, onVoteResult)
  wsManager.off(WwCmd.HUNTER_SHOOT_RESULT, onHunterShootResult)
  wsManager.off(WwCmd.GAME_OVER, onGameOver)
  wsManager.off(2, onLoginResult)
})
</script>

<style scoped>
.werewolf-game {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: white;
  display: flex;
  flex-direction: column;
}

/* 顶部信息栏 */
.game-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 30px;
  background: rgba(0, 0, 0, 0.4);
}
.day-info { display: flex; gap: 15px; align-items: center; }
.day { font-size: 20px; font-weight: bold; }
.phase { padding: 5px 15px; background: #8e44ad; border-radius: 15px; }
.role-info { display: flex; align-items: center; gap: 10px; font-size: 18px; }
.role-icon { font-size: 28px; }
.timer { font-size: 18px; color: #f39c12; }

/* 玩家座位区 */
.players-area {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 15px;
  padding: 20px;
}
.player-seat {
  width: 100px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
  position: relative;
}
.player-seat:hover { background: rgba(255, 255, 255, 0.15); }
.player-seat.me { border-color: #3498db; }
.player-seat.dead { opacity: 0.4; }
.player-seat.wolf { border-color: #c0392b; }
.player-seat.selected { border-color: #f39c12; background: rgba(243, 156, 18, 0.2); }
.player-seat.canSelect { cursor: pointer; box-shadow: 0 0 10px rgba(46, 204, 113, 0.5); }
.player-seat.speaking { border-color: #27ae60; animation: pulse 1s infinite; }
@keyframes pulse { 0%, 100% { box-shadow: 0 0 5px #27ae60; } 50% { box-shadow: 0 0 20px #27ae60; } }

.seat-number { font-size: 12px; color: rgba(255,255,255,0.6); margin-bottom: 5px; }
.avatar { font-size: 36px; margin-bottom: 5px; }
.player-name { font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.badges { position: absolute; top: 5px; right: 5px; }
.wolf-badge, .role-badge { font-size: 16px; }
.vote-count { position: absolute; bottom: -10px; left: 50%; transform: translateX(-50%);
  background: #e74c3c; padding: 2px 8px; border-radius: 10px; font-size: 11px; }

/* 中央信息区 */
.center-area {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}
.night-overlay {
  text-align: center;
  animation: fadeIn 0.5s;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.moon { font-size: 80px; margin-bottom: 20px; }
.night-text { font-size: 24px; color: #9b59b6; }

.death-announce {
  text-align: center;
  padding: 30px;
  background: rgba(231, 76, 60, 0.2);
  border-radius: 15px;
}
.announce-title { font-size: 20px; margin-bottom: 15px; }
.dead-list { font-size: 24px; color: #e74c3c; }

.game-over {
  text-align: center;
  padding: 40px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 20px;
}
.winner { font-size: 32px; margin-bottom: 20px; }
.back-btn {
  padding: 15px 40px;
  font-size: 18px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
}

/* 操作面板 */
.action-panel {
  padding: 20px;
  background: rgba(0, 0, 0, 0.4);
}
.action-box {
  max-width: 500px;
  margin: 0 auto;
  text-align: center;
}
.action-box h3 { margin-bottom: 10px; }
.action-box p { color: rgba(255,255,255,0.7); margin-bottom: 15px; font-size: 14px; }
.action-buttons { display: flex; justify-content: center; gap: 10px; flex-wrap: wrap; }
.action-btn {
  padding: 12px 30px;
  font-size: 16px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}
.action-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.action-btn:not(.secondary):not(.save):not(.poison):not(.danger) { background: #3498db; color: white; }
.action-btn.secondary { background: rgba(255,255,255,0.1); color: white; }
.action-btn.save { background: #27ae60; color: white; }
.action-btn.poison { background: #8e44ad; color: white; }
.action-btn.danger { background: #e74c3c; color: white; }

.witch-info { margin-bottom: 15px; color: #e74c3c; }
.witch-options { display: flex; justify-content: center; gap: 10px; flex-wrap: wrap; }
.check-result { margin-top: 15px; font-size: 18px; padding: 10px; background: rgba(255,255,255,0.1); border-radius: 8px; }

textarea {
  width: 100%;
  padding: 10px;
  background: rgba(255,255,255,0.1);
  color: white;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 8px;
  resize: none;
  margin-bottom: 10px;
}

/* 聊天面板 */
.chat-panel {
  height: 150px;
  background: rgba(0, 0, 0, 0.3);
  border-top: 1px solid rgba(255,255,255,0.1);
}
.chat-messages {
  height: 100%;
  overflow-y: auto;
  padding: 10px 20px;
}
.chat-msg { margin-bottom: 5px; font-size: 13px; }
.chat-msg .sender { color: #3498db; margin-right: 5px; }
.chat-msg.system .sender { color: #f39c12; }
.chat-msg.phase .sender { color: #9b59b6; }
.chat-msg.death .sender { color: #e74c3c; }
.chat-msg.wolf .sender { color: #c0392b; }
.chat-msg.speak .sender { color: #27ae60; }
.chat-msg.gameover { font-size: 16px; font-weight: bold; color: #f39c12; }

/* 弹窗 */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
}
.modal {
  background: #2c3e50;
  padding: 30px;
  border-radius: 15px;
  text-align: center;
  min-width: 300px;
}
.modal h3 { margin-bottom: 20px; }
.poison-targets {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-bottom: 20px;
}
.target-btn {
  padding: 10px 20px;
  background: #8e44ad;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
.target-btn:hover { background: #9b59b6; }
.cancel-btn {
  padding: 10px 30px;
  background: rgba(255,255,255,0.1);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
</style>