import { ref } from 'vue'

export const message = ref('')
export const messageVisible = ref(false)

let timer = null

export function showMessage(text, duration = 2000) {
  message.value = text
  messageVisible.value = true
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => {
    messageVisible.value = false
  }, duration)
}
