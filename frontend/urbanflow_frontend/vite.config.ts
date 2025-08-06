// vite.config.ts
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'

const HTTP_LOCALHOST = 'http://localhost'
const WS_LOCALHOST = 'ws://localhost'

export default defineConfig({
  base: '/',
  plugins: [vue(), vueJsx(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['vue', 'vue-router', 'pinia'],
          echarts: ['echarts', 'vue-echarts'],
          utils: ['axios', '@vueuse/core', 'date-fns']
        }
      }
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api/traffic/suggestion': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {

          });
        }
      },
      '/api/traffic': {
        target: 'http://localhost:8083',
        changeOrigin: true
      },
      '/api-status': {
        target: `${HTTP_LOCALHOST}:8087`,
        changeOrigin: true,
        rewrite: path => path,
      },
      '/api/dashboard':{
        target: 'http://localhost:8087',
        changeOrigin: true,
      },
      '/api/events': {
        target: `${HTTP_LOCALHOST}:8085`,
        changeOrigin: true,
        rewrite: path => path,
      },
      '/api/emergency-vehicles': {
        target: `${HTTP_LOCALHOST}:8085`,
        changeOrigin: true,
        rewrite: path => path,
      },
      '/ws/tracking': {
        target: `${WS_LOCALHOST}:8085`,
        ws: true,
        changeOrigin: true,
      },
      '/api/signalcontrol': {
        target: `${HTTP_LOCALHOST}:8082`,
        changeOrigin: true,
        rewrite: path => path,
      },
      '/api/auth': {
        target: `${HTTP_LOCALHOST}:8081`,
        changeOrigin: true,
        rewrite: path => path,
      },
      '/api/area-permission': {
        target: `${HTTP_LOCALHOST}:8081`,
        changeOrigin: true,
        rewrite: path => path,
      },
      '/api/areas': {
        target: `${HTTP_LOCALHOST}:8081`,
        changeOrigin: true,
        rewrite: path => path,
      },
      '/api/users': {
        target: `${HTTP_LOCALHOST}:8081`,
        changeOrigin: true,
        rewrite: path => path,
      },
      '/api/logs': {
        target: `${HTTP_LOCALHOST}:8086`,
        changeOrigin: true,
        rewrite: path => path,
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.log('proxy error', err);
          });
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('Sending Request to the Target:', req.method, req.url);
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('Received Response from the Target:', proxyRes.statusCode, req.url);
          });
        }
      }
    },
  },
})