// vite.config.ts
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [vue(), vueJsx(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api-status': {
        target: 'http://localhost:8087',
        changeOrigin: true,
        rewrite: path => path,
      },
      '/api/signalcontrol': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: path => path,
      },
      '/api/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: path => path,
      },
      '/api/area-permission': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: path => path,
      },
      '/api/areas': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: path => path,
      },
      '/api/users': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: path => path,
      },
      '/api/emergency-vehicles': {
        target: 'http://localhost:8085', // 您的Java后端地址
        changeOrigin: true, // 必须设置为 true，以支持跨域
        rewrite: path => path,
      },
      '/api/logs': {
        target: 'http://localhost:8086',
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
