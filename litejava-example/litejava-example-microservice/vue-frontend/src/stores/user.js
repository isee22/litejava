import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)

  async function login(username, password) {
    const res = await authApi.login(username, password)
    if (res.code === 0) {
      token.value = res.data.token
      user.value = { id: res.data.userId, username: res.data.username }
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(user.value))
      return { success: true }
    }
    return { success: false, msg: res.msg }
  }

  async function register(data) {
    const res = await authApi.register(data)
    if (res.code === 0) {
      return { success: true }
    }
    return { success: false, msg: res.msg }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, isLoggedIn, login, register, logout }
})
