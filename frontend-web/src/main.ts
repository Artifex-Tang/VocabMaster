import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
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
app.use(ElementPlus)

// Trigger store setup early so registerAuthHooks() runs before the first API request
useUserStore()

// Called once here — sets up the global online/offline watcher
initOfflineSync()

app.mount('#app')
