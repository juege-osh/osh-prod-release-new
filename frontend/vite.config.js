import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [vue()],
  base: process.env.VITE_BASE_PATH || '/',
  server: {
    port: 5173,
    proxy: {
      '/api': process.env.VITE_DEV_API_TARGET || 'http://127.0.0.1:18080'
    }
  }
})
