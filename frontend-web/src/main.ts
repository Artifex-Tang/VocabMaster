import { createApp } from 'vue'
import { createPinia } from 'pinia'
// Element Plus global CSS; components are auto-imported on-demand by unplugin-vue-components.
// ElMessage/ElMessageBox/ElNotification are plain functions — no app.use() needed.
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'
import { initOfflineSync } from './composables/useOfflineSync'
import '@/styles/main.scss'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Trigger store setup early so registerAuthHooks() runs before the first API request
useUserStore()

// Called once here — sets up the global online/offline watcher
initOfflineSync()

app.mount('#app')
