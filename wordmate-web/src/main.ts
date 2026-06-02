import { createApp } from 'vue'
import { createPinia } from 'pinia'
// Element Plus global CSS; components are auto-imported on-demand by unplugin-vue-components.
// ElMessage/ElMessageBox/ElNotification are plain functions — no app.use() needed.
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'
import { useSettingsStore } from './stores/settings'
import { initOfflineSync } from './composables/useOfflineSync'
import '@/styles/main.scss'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Trigger store setup early so registerAuthHooks() runs before the first API request
useUserStore()

// Apply theme from settings
const settingsStore = useSettingsStore()
function applyTheme(theme: string) {
  const html = document.documentElement
  if (theme === 'dark') {
    html.classList.add('dark')
  } else if (theme === 'system') {
    const preferDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    html.classList.toggle('dark', preferDark)
  } else {
    html.classList.remove('dark')
  }
}
// Watch theme changes reactively
import { watch } from 'vue'
watch(() => settingsStore.settings.theme, applyTheme, { immediate: true })

// Called once here — sets up the global online/offline watcher
initOfflineSync()

app.mount('#app')
