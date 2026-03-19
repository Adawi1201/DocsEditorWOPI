import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')
  
  return {
    plugins: [
      vue(),
      vueDevTools(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    // 开发服务器配置
    server: {
      port: Number(env.VITE_FRONTEND_PORT) || 5231,
      proxy: {
        // 将/cool和/lool请求转发到Collabora（解决跨域问题）
        '/cool': {
          target: env.VITE_COLLABORA_SERVER || 'http://localhost:9980',
          changeOrigin: true,
          secure: false
        },
        '/lool': {
          target: env.VITE_COLLABORA_SERVER || 'http://localhost:9980',
          changeOrigin: true,
          secure: false
        },
        // 将API请求转发到后端（解决跨域问题）
        '/api': {
          target: env.VITE_API_SERVER || 'http://localhost:8808',
          changeOrigin: true,
          secure: false
        },
        // 将WOPI请求转发到后端
        '/wopi': {
          target: env.VITE_API_SERVER || 'http://localhost:8808',
          changeOrigin: true,
          secure: false
        }
      }
    }
  }
})
