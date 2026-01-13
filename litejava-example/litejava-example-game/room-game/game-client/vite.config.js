import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * 开发模式：Vite 代理到各服务端口（无需 Nginx）
 * 生产模式：通过 Nginx 统一入口
 */
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      // Account Server
      '/api/account': {
        target: 'http://localhost:8101',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api\/account/, '')
      },
      // Hall Server
      '/api/hall': {
        target: 'http://localhost:8201',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api\/hall/, '')
      }
    }
  }
})
