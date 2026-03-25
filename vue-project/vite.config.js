import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 根据模式加载不同的环境文件
  const envFile = mode === 'author' ? '.env.author' : mode === 'admin' ? '.env.admin' : '.env.development'

  return {
    plugins: [vue(), vueDevTools()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    envDir: './',
    envPrefix: 'VITE_',
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },
    build: {
      // 代码分割配置
      rollupOptions: {
        output: {
          // 手动代码分割
          manualChunks: {
            // Element Plus 单独打包
            'element-plus': ['element-plus'],
            // Vue 生态单独打包
            'vue-vendor': ['vue', 'vue-router', 'pinia'],
            // 工具库单独打包
            'utils': ['axios']
          },
          // 入口文件命名
          entryFileNames: 'assets/[name]-[hash].js',
          // chunk 文件命名
          chunkFileNames: 'assets/[name]-[hash].js',
          // 资源文件命名
          assetFileNames: (assetInfo) => {
            const info = assetInfo.name.split('.')
            const ext = info[info.length - 1]
            if (/\.(png|jpe?g|gif|svg|webp|ico)$/i.test(assetInfo.name)) {
              return 'assets/images/[name]-[hash][extname]'
            }
            if (/\.(woff2?|eot|ttf|otf)$/i.test(assetInfo.name)) {
              return 'assets/fonts/[name]-[hash][extname]'
            }
            return 'assets/[name]-[hash][extname]'
          }
        }
      },
      // 压缩配置
      minify: 'terser',
      terserOptions: {
        compress: {
          // 删除 console 和 debugger
          drop_console: mode === 'production',
          drop_debugger: mode === 'production'
        }
      },
      // 生成 source map
      sourcemap: mode !== 'production',
      // 目标浏览器
      target: 'es2015',
      // CSS 代码分割
      cssCodeSplit: true,
      // 资源内联限制（小于 4kb 的资源会内联）
      assetsInlineLimit: 4096
    },
    // 优化依赖预构建
    optimizeDeps: {
      include: ['vue', 'vue-router', 'pinia', 'element-plus', 'axios'],
      exclude: []
    },
    // CSS 配置
    css: {
      devSourcemap: true,
      // CSS 预处理器配置
      preprocessorOptions: {
        scss: {
          additionalData: `@use "@/styles/variables.scss" as *;`
        }
      }
    }
  }
})
