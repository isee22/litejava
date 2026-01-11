import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/lobby', name: 'Lobby', component: () => import('../views/Lobby.vue'), meta: { requiresAuth: true } },
  { path: '/room', name: 'Room', component: () => import('../views/Room.vue'), meta: { requiresAuth: true } },
  { path: '/game', name: 'Game', component: () => import('../views/Game.vue'), meta: { requiresAuth: true } },
  { path: '/game/werewolf', name: 'WerewolfGame', component: () => import('../views/WerewolfGame.vue'), meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/')
  } else {
    next()
  }
})

export default router
