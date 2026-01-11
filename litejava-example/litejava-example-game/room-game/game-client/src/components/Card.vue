<template>
  <div 
    class="card" 
    :class="{ red: isRed, black: !isRed, small, selected }"
    @click="$emit('click')"
  >
    <template v-if="card.rank === 16">🃏<br>小</template>
    <template v-else-if="card.rank === 17">🃏<br>大</template>
    <template v-else>{{ suit }}<br>{{ rank }}</template>
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
const ranks = ['', '', '', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K', 'A', '2']

const suit = computed(() => suits[props.card.suit])
const rank = computed(() => ranks[props.card.rank])
const isRed = computed(() => props.card.suit === 0 || props.card.suit === 2 || props.card.rank >= 16)
</script>

<style scoped>
.card {
  width: 60px;
  height: 84px;
  background: white;
  border-radius: 6px;
  margin: 0 -15px;
  cursor: pointer;
  transition: transform 0.2s, margin-top 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.3);
}
.card:hover { transform: translateY(-10px); }
.card.selected { margin-top: -20px; background: #fffacd; }
.card.red { color: #e74c3c; }
.card.black { color: #2c3e50; }
.card.small { width: 45px; height: 63px; font-size: 14px; margin: 0 -10px; }
</style>
