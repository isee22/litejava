import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const playerId = ref(null)
  const playerName = ref('')
  
  const isLoggedIn = computed(() => !!playerId.value)
  
  function setPlayer(id, name) {
    playerId.value = id
    playerName.value = name
  }
  
  function logout() {
    playerId.value = null
    playerName.value = ''
  }
  
  return { playerId, playerName, isLoggedIn, setPlayer, logout }
})
