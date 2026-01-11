<template>
  <div class="player-area" :class="position">
    <div class="player-info">
      <div class="player-name">{{ displayName }}</div>
      <div v-if="showCardCount" class="player-cards-count">剩余 {{ player.cardCount }} 张</div>
      <div class="player-status" :class="statusClass">{{ statusText }}</div>
    </div>
    <div class="played-cards">
      <template v-if="playedCards">
        <Card v-for="(card, i) in playedCards" :key="i" :card="card" small />
      </template>
      <span v-else-if="passed" class="pass-text">不出</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Card from './Card.vue'

const props = defineProps({
  position: { type: String, required: true },
  player: { type: Object, default: () => ({}) },
  isMe: { type: Boolean, default: false },
  isCurrent: { type: Boolean, default: false },
  isLandlord: { type: Boolean, default: false },
  isReady: { type: Boolean, default: false },
  showCardCount: { type: Boolean, default: false },
  playedCards: { type: Array, default: null },
  passed: { type: Boolean, default: false }
})

const displayName = computed(() => {
  if (!props.player.id) return '等待玩家...'
  return props.player.name + (props.isMe ? ' (我)' : '')
})

const statusText = computed(() => {
  const status = []
  if (props.isReady) status.push('已准备')
  if (props.isLandlord) status.push('👑 地主')
  if (props.isCurrent) status.push('⏰ 出牌中')
  return status.join(' ')
})

const statusClass = computed(() => ({
  current: props.isCurrent,
  landlord: props.isLandlord,
  ready: props.isReady
}))
</script>

<style scoped>
.player-area { position: absolute; text-align: center; }
.player-area.top { top: 20px; left: 50%; transform: translateX(-50%); }
.player-area.left { left: 20px; top: 50%; transform: translateY(-50%); }
.player-area.right { right: 20px; top: 50%; transform: translateY(-50%); }
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
.player-status.ready { color: #2ecc71; }
.player-status.landlord { color: #f1c40f; }
.player-status.current { color: #e74c3c; animation: pulse 1s infinite; }

.played-cards { display: flex; justify-content: center; }
.pass-text { font-size: 24px; color: #aaa; }

@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
</style>
