import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useGameStore = defineStore('game', () => {
  const roomId = ref(null)
  const gameType = ref(null)
  const mySeat = ref(-1)
  const players = ref([{}, {}, {}])
  const myCards = ref([])
  const selectedCards = ref([])
  const gameStatus = ref('waiting') // waiting, bidding, playing
  const currentSeat = ref(-1)
  const landlordSeat = ref(-1)
  const lastCards = ref([])
  const lastSeat = ref(-1)
  const bottomCards = ref([])
  
  function reset() {
    roomId.value = null
    gameType.value = null
    mySeat.value = -1
    players.value = [{}, {}, {}]
    myCards.value = []
    selectedCards.value = []
    gameStatus.value = 'waiting'
    currentSeat.value = -1
    landlordSeat.value = -1
    lastCards.value = []
    lastSeat.value = -1
    bottomCards.value = []
  }
  
  function setRoom(id, seat) {
    roomId.value = id
    mySeat.value = seat
  }
  
  function setGameType(type) {
    gameType.value = type
  }
  
  function setSeatIndex(seat) {
    mySeat.value = seat
  }
  
  function updatePlayer(seat, data) {
    players.value[seat] = { ...players.value[seat], ...data }
  }
  
  function setCards(cards) {
    myCards.value = cards.sort((a, b) => b.rank - a.rank)
    selectedCards.value = []
  }
  
  function toggleCard(card) {
    const idx = selectedCards.value.findIndex(c => c.suit === card.suit && c.rank === card.rank)
    if (idx >= 0) {
      selectedCards.value.splice(idx, 1)
    } else {
      selectedCards.value.push(card)
    }
  }
  
  function removeCards(cards) {
    for (const played of cards) {
      const idx = myCards.value.findIndex(c => c.suit === played.suit && c.rank === played.rank)
      if (idx >= 0) myCards.value.splice(idx, 1)
    }
    selectedCards.value = []
  }
  
  return {
    roomId, gameType, mySeat, players, myCards, selectedCards, gameStatus,
    currentSeat, landlordSeat, lastCards, lastSeat, bottomCards,
    reset, setRoom, setGameType, setSeatIndex, updatePlayer, setCards, toggleCard, removeCards
  }
})
