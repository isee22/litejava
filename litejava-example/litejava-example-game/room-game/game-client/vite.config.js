import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      // 所有 API 和 WebSocket 都走 Nginx
      '/api': {
        target: 'http://localhost:80',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:80',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
