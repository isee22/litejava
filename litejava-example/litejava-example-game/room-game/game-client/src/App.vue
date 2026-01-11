<template>
  <div class="app">
    <router-view />
    <MessageBox />
  </div>
</template>

<script setup>
import MessageBox from './components/MessageBox.vue'
</script>
