<template>
  <div 
    class="card" 
    :class="{ red: isRed, black: !isRed, small, selected, joker: isJoker }"
    @click="$emit('click')"
  >
    <div class="card-inner">
      <!-- 大小王 -->
      <template v-if="card.rank === 16">
        <div class="joker-icon">🃏</div>
        <div class="joker-text">小王</div>
      </template>
      <template v-else-if="card.rank === 17">
        <div class="joker-icon big">🃏</div>
        <div class="joker-text">大王</div>
      </template>
      <!-- 普通牌 -->
      <template v-else>
        <div class="card-corner top-left">
          <span class="card-rank">{{ rank }}</span>
          <span class="card-suit">{{ suit }}</span>
        </div>
        <div class="card-center">
          <span class="card-suit-big">{{ suit }}</span>
        </div>
        <div class="card-corner bottom-right">
          <span class="card-rank">{{ rank }}</span>
          <span class="card-suit">{{ suit }}</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  card: { type: Object, required: true },
  small: { type: Boolean, default: false },
  selected: { type: Boolean, default: false }
})

defineEmits(['click'])

const suits = ['♦', '♣', '♥', '♠']
// rank: 3-15 对应 3,4,5,6,7,8,9,10,J,Q,K,A,2
const ranks = { 3: '3', 4: '4', 5: '5', 6: '6', 7: '7', 8: '8', 9: '9', 10: '10', 11: 'J', 12: 'Q', 13: 'K', 14: 'A', 15: '2' }

const suit = computed(() => suits[props.card.suit] || '')
const rank = computed(() => ranks[props.card.rank] || '')
const isRed = computed(() => props.card.suit === 0 || props.card.suit === 2 || props.card.rank >= 16)
const isJoker = computed(() => props.card.rank >= 16)
</script>

<style scoped>
.card {
  width: 70px;
  height: 100px;
  background: linear-gradient(145deg, #ffffff, #f0f0f0);
  border-radius: 8px;
  margin: 0 -18px;
  cursor: pointer;
  transition: transform 0.2s, margin-top 0.2s, box-shadow 0.2s;
  position: relative;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.3), inset 0 1px 0 rgba(255,255,255,0.8);
  border: 1px solid #ddd;
}
.card:hover { 
  transform: translateY(-12px); 
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.4);
}
.card.selected { 
  margin-top: -25px; 
  background: linear-gradient(145deg, #fffacd, #fff8b0);
  box-shadow: 0 5px 15px rgba(241, 196, 60, 0.5);
}
.card.red { color: #e74c3c; }
.card.black { color: #2c3e50; }
.card.joker { 
  background: linear-gradient(145deg, #fff8e1, #ffe082);
}
.card.joker.red { color: #e74c3c; }

.card-inner {
  width: 100%;
  height: 100%;
  position: relative;
}

.card-corner {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1;
}
.card-corner.top-left { top: 5px; left: 6px; }
.card-corner.bottom-right { 
  bottom: 5px; 
  right: 6px; 
  transform: rotate(180deg);
}

.card-rank { font-size: 16px; font-weight: bold; }
.card-suit { font-size: 12px; }

.card-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.card-suit-big { font-size: 32px; }

/* 大小王样式 */
.joker-icon { 
  font-size: 36px; 
  text-align: center;
  margin-top: 15px;
}
.joker-icon.big { font-size: 40px; }
.joker-text { 
  font-size: 14px; 
  font-weight: bold; 
  text-align: center;
  margin-top: 5px;
}

/* 小牌样式 */
.card.small { 
  width: 50px; 
  height: 72px; 
  margin: 0 -12px;
  border-radius: 5px;
}
.card.small .card-rank { font-size: 12px; }
.card.small .card-suit { font-size: 9px; }
.card.small .card-suit-big { font-size: 22px; }
.card.small .joker-icon { font-size: 24px; margin-top: 8px; }
.card.small .joker-text { font-size: 10px; }
.card.small .card-corner.top-left { top: 3px; left: 4px; }
.card.small .card-corner.bottom-right { bottom: 3px; right: 4px; }
</style>
