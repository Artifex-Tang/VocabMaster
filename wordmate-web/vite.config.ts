import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { viteMockServe } from 'vite-plugin-mock'
import { readFileSync } from 'node:fs'
import path from 'node:path'

const pkg = JSON.parse(readFileSync('./package.json', 'utf-8'))

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const useMock = env.VITE_USE_MOCK === 'true'

  return {
    plugins: [
      vue(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
        imports: ['vue', 'vue-router', 'pinia'],
        dts: 'src/auto-imports.d.ts',
      }),
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/components.d.ts',
      }),
      viteMockServe({
        mockPath: 'mock',
        enable: useMock,
        logger: false,
      }),
    ],
    resolve: {
      alias: { '@': path.resolve(__dirname, 'src') },
    },
    server: {
      port: 3100,
      proxy: useMock
        ? {}
        : { '/api': { target: 'http://localhost:8080', changeOrigin: true } },
    },
    define: {
      'import.meta.env.VITE_APP_VERSION': JSON.stringify(pkg.version),
    },
    css: {
      preprocessorOptions: {
        scss: { api: 'modern-compiler' },
      },
    },
    build: {
      chunkSizeWarningLimit: 600,
      rollupOptions: {
        output: {
          manualChunks: (id: string) => {
            if (id.includes('node_modules/echarts') || id.includes('node_modules/zrender')) return 'echarts'
            if (id.includes('node_modules/element-plus')) return 'element-plus'
            if (id.includes('node_modules/vue') || id.includes('node_modules/@vue') || id.includes('node_modules/pinia') || id.includes('node_modules/vue-router')) return 'vue-vendor'
            if (id.includes('node_modules/dexie')) return 'dexie'
            if (id.includes('node_modules/axios')) return 'axios'
            if (id.includes('node_modules/dayjs')) return 'dayjs'
            if (id.includes('node_modules/@iconify')) return 'iconify'
          },
        },
      },
    },
  }
})
