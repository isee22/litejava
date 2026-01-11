<template>
  <Transition name="fade">
    <div v-if="messageVisible" class="message-box">{{ message }}</div>
  </Transition>
</template>

<script setup>
import { message, messageVisible } from '../utils/message'
</script>

<style scoped>
.message-box {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 15px 30px;
  border-radius: 8px;
  z-index: 1000;
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
