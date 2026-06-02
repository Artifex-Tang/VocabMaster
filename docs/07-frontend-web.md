# 07 — Web 前端实现规范

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5+ | Composition API |
| Vite | 5.x | 构建工具 |
| TypeScript | 5.x | 类型 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由 |
| Element Plus | 2.x | UI 库 |
| ECharts | 5.x | 图表（遗忘曲线） |
| axios | 1.x | HTTP |
| dayjs | 1.x | 时间处理 |
| @vueuse/core | 10.x | 组合式工具集 |
| Dexie | 4.x | IndexedDB 封装（离线缓存） |
| Vitest + Playwright | — | 测试 |

## 项目结构

```
wordmate-web/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
├── .env.development
├── .env.production
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── env.d.ts
│   ├── api/
│   │   ├── request.ts                # axios 实例 + 拦截器
│   │   ├── types.ts                  # 接口 TypeScript 类型
│   │   ├── auth.ts
│   │   ├── user.ts
│   │   ├── word.ts
│   │   ├── study.ts
│   │   ├── test.ts
│   │   ├── stats.ts
│   │   ├── checkin.ts
│   │   ├── wrong-word.ts
│   │   ├── sync.ts
│   │   └── admin.ts
│   ├── stores/
│   │   ├── user.ts                   # 当前用户 + token
│   │   ├── settings.ts               # 用户设置
│   │   ├── study.ts                  # 今日计划 + 当前学习会话
│   │   └── offline.ts                # 离线队列
│   ├── router/
│   │   ├── index.ts
│   │   └── guards.ts                 # 登录守卫 + 管理员守卫
│   ├── views/
│   │   ├── auth/
│   │   │   ├── Login.vue
│   │   │   ├── Register.vue
│   │   │   └── ResetPassword.vue
│   │   ├── dashboard/
│   │   │   └── Dashboard.vue         # 首页 + 今日计划
│   │   ├── study/
│   │   │   ├── StudySession.vue      # 学习主界面
│   │   │   └── ReviewSession.vue
│   │   ├── test/
│   │   │   ├── TestEntry.vue
│   │   │   ├── SpellingTest.vue
│   │   │   ├── ChoiceTest.vue
│   │   │   └── ListeningTest.vue
│   │   ├── stats/
│   │   │   ├── StatsOverview.vue
│   │   │   ├── ForgettingCurve.vue
│   │   │   └── Calendar.vue
│   │   ├── wrong-word/
│   │   │   └── WrongWordList.vue
│   │   ├── settings/
│   │   │   └── UserSettings.vue
│   │   ├── levels/
│   │   │   └── LevelSelection.vue
│   │   └── admin/
│   │       ├── WordManage.vue
│   │       ├── UserManage.vue
│   │       └── Dashboard.vue
│   ├── components/
│   │   ├── layout/
│   │   │   ├── AppHeader.vue
│   │   │   ├── AppSidebar.vue
│   │   │   └── AppFooter.vue
│   │   ├── WordCard.vue              # 单词卡片（翻面）
│   │   ├── ProgressBar.vue
│   │   ├── StreakBadge.vue
│   │   ├── EbbinghausChart.vue       # ECharts 遗忘曲线
│   │   ├── CheckinCalendar.vue
│   │   ├── AchievementCard.vue
│   │   └── AudioPlayer.vue
│   ├── composables/
│   │   ├── useTts.ts                 # Web Speech API 封装
│   │   ├── useOfflineSync.ts         # 离线同步 hook
│   │   └── useCountdown.ts
│   ├── utils/
│   │   ├── storage.ts                # localStorage/IndexedDB 统一接口
│   │   ├── tts.ts                    # TTS 底层实现
│   │   ├── device-id.ts              # 设备 ID 生成与持久化
│   │   ├── date.ts                   # dayjs 封装
│   │   └── constants.ts              # 常量（等级列表、错误码等）
│   ├── styles/
│   │   ├── main.scss
│   │   └── variables.scss
│   └── assets/
│       ├── images/
│       └── sounds/
└── public/
    └── favicon.ico
```

## 核心实现

### 1. axios 封装

```typescript
// src/api/request.ts
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getDeviceId } from '@/utils/device-id'

export interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
  request_id?: string
}

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api/v1',
  timeout: 15000,
})

instance.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.accessToken) {
    config.headers.Authorization = `Bearer ${userStore.accessToken}`
  }
  config.headers['X-Device-Id'] = getDeviceId()
  config.headers['X-Device-Type'] = 'web'
  config.headers['X-Client-Version'] = import.meta.env.VITE_APP_VERSION
  return config
})

let isRefreshing = false
let refreshQueue: Array<() => void> = []

instance.interceptors.response.use(
  (res: AxiosResponse<ApiResponse>) => {
    const { code, msg, data } = res.data
    if (code === 0) return data
    // 业务错误
    if (code === 20002) {
      // token 过期，触发刷新
      return handleTokenRefresh(res.config)
    }
    ElMessage.error(msg || '请求失败')
    return Promise.reject(new Error(msg))
  },
  err => {
    if (err.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      window.location.href = '/login'
    } else if (err.response?.status === 429) {
      ElMessage.warning('请求太频繁，请稍后再试')
    } else {
      ElMessage.error(err.message || '网络错误')
    }
    return Promise.reject(err)
  }
)

async function handleTokenRefresh(config: AxiosRequestConfig) {
  const userStore = useUserStore()
  if (!isRefreshing) {
    isRefreshing = true
    try {
      await userStore.refreshAccessToken()
      refreshQueue.forEach(cb => cb())
      refreshQueue = []
    } catch (e) {
      userStore.logout()
      window.location.href = '/login'
      return Promise.reject(e)
    } finally {
      isRefreshing = false
    }
  }
  return new Promise(resolve => {
    refreshQueue.push(() => resolve(instance(config)))
  })
}

export default instance

// 便捷方法
export const http = {
  get: <T>(url: string, params?: any) => instance.get<any, T>(url, { params }),
  post: <T>(url: string, data?: any) => instance.post<any, T>(url, data),
  patch: <T>(url: string, data?: any) => instance.patch<any, T>(url, data),
  del: <T>(url: string) => instance.delete<any, T>(url),
}
```

### 2. Pinia 用户 store

```typescript
// src/stores/user.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'

interface User {
  uuid: string
  nickname: string
  email?: string
  phoneMasked?: string
  avatarUrl?: string
  locale: string
  timezone: string
}

export const useUserStore = defineStore('user', () => {
  const accessToken = ref<string>(localStorage.getItem('access_token') || '')
  const refreshToken = ref<string>(localStorage.getItem('refresh_token') || '')
  const user = ref<User | null>(null)
  const isAdmin = ref(false)
  
  const isLoggedIn = computed(() => !!accessToken.value)
  
  async function login(type: string, identifier: string, password: string) {
    const data = await authApi.login({ type, identifier, password })
    setAuth(data)
  }
  
  async function loginByCode(type: string, identifier: string, code: string) { ... }
  
  async function refreshAccessToken() {
    const data = await authApi.refresh({ refresh_token: refreshToken.value })
    accessToken.value = data.access_token
    localStorage.setItem('access_token', data.access_token)
  }
  
  function setAuth(data: any) {
    accessToken.value = data.access_token
    refreshToken.value = data.refresh_token
    user.value = data.user
    localStorage.setItem('access_token', data.access_token)
    localStorage.setItem('refresh_token', data.refresh_token)
  }
  
  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
  }
  
  async function fetchMe() {
    const data = await authApi.me()
    user.value = data
  }
  
  return { accessToken, refreshToken, user, isAdmin, isLoggedIn, 
           login, loginByCode, logout, fetchMe, refreshAccessToken }
})
```

### 3. 单词卡片组件

> **已实现**，参见 `wordmate-web/src/components/WordCard.vue`

**卡片正面**：
- Emoji 示意图（`word.emoji`，按 topic_code 填充）或 `word.image_url`（Wikimedia Commons）
- 单词文本
- 词性标签（从 `pos` 字段解析，如 n./v./adj.）
- 音标（UK/US 切换）+ 发音按钮
- 词形变化提示（从 `zh_definition` 提取复数、过去式等）
- topic_code 标签

**卡片反面**：
- 英文释义 / 中文释义
- 例句（`example_en` / `example_zh`，Free Dictionary API 获取）
- 近义词（`related_words.synonyms`）
- 衍生词（`related_words.derived`，WordNet 获取）

**交互**：
- **双向翻转**：点击或 Space 键切换正⇄反
- **自动发音**：翻到反面时自动播放（可在设置中关闭）

**关键实现**：
```ts
// 双向翻转
function handleFlip() {
  flipped.value = !flipped.value
  if (flipped.value) {
    emit('flip')
    if (settingsStore.settings.auto_play_audio) playAudio()
  }
}

// 词性标签
const posLabel = computed(() => {
  const code = props.word.pos?.split(':')[0]  // "n:100" → "n"
  const map = { n:'n.', v:'v.', j:'adj.', r:'adv.', ... }
  return code ? (map[code] || code) : ''
})

// 词形提示（从 zh_definition 括号提取）
const formHint = computed(() => {
  const m = props.word.zh_definition?.match(/[（(](.+?(?:复数|过去式|比较级).+?)[）)]/)
  return m ? m[1] : ''
})
```

### 4. 学习会话（Study Session）

```vue
<!-- src/views/study/StudySession.vue -->
<template>
  <div class="study-session">
    <div class="header">
      <el-button text @click="$router.back()">← 返回</el-button>
      <div class="progress-info">
        {{ currentIdx + 1 }} / {{ queue.length }}
      </div>
      <div class="correct-count">✓ {{ correctCount }}</div>
    </div>
    <el-progress :percentage="progressPercentage" :show-text="false" />
    
    <WordCard 
      v-if="currentWord"
      ref="cardRef"
      :key="currentWord.id"
      :word="currentWord"
      @flip="isFlipped = true"
    />
    
    <div class="actions" v-if="isFlipped">
      <el-button type="danger" size="large" @click="handleAnswer('wrong')">
        不认识
      </el-button>
      <el-button type="success" size="large" @click="handleAnswer('correct')">
        认识 ✓
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import * as studyApi from '@/api/study'
import { useOfflineSync } from '@/composables/useOfflineSync'
import WordCard from '@/components/WordCard.vue'

const route = useRoute()
const level = computed(() => route.params.level as string)
const mode = computed(() => route.query.mode as string || 'today')

const queue = ref<WordBank[]>([])
const currentIdx = ref(0)
const isFlipped = ref(false)
const correctCount = ref(0)
const startTime = ref(Date.now())
const cardRef = ref<InstanceType<typeof WordCard> | null>(null)

const { submitAnswer } = useOfflineSync()

const currentWord = computed(() => queue.value[currentIdx.value])
const progressPercentage = computed(() => 
  queue.value.length ? Math.round(currentIdx.value / queue.value.length * 100) : 0
)

onMounted(async () => {
  const plan = await studyApi.today(level.value)
  // 合并复习词和新学词，复习词在前
  queue.value = [...plan.reviewWords, ...plan.newWords]
  startTime.value = Date.now()
})

async function handleAnswer(result: 'correct' | 'wrong') {
  const word = currentWord.value
  const duration = Date.now() - startTime.value
  
  if (result === 'correct') correctCount.value++
  
  await submitAnswer({
    word_id: word.id,
    level_code: word.levelCode,
    result,
    mode: 'card',
    duration_ms: duration,
    client_ts: new Date().toISOString(),
  })
  
  if (currentIdx.value + 1 >= queue.value.length) {
    // 完成
    await finishSession()
  } else {
    currentIdx.value++
    isFlipped.value = false
    startTime.value = Date.now()
    cardRef.value?.reset()
  }
}

async function finishSession() {
  // 跳转完成页
  router.push(`/study/done?level=${level.value}&correct=${correctCount.value}&total=${queue.value.length}`)
}
</script>
```

### 5. 学习会话导航

> **已实现**，参见 `wordmate-web/src/views/study/StudySession.vue`

导航栏：返回 | ← | 进度条 | N/M | → | ✓数量

- `←` `→` 按钮和键盘箭头键切换单词（不提交答案，仅浏览）
- 切换时自动重置卡片为正面
- 快捷键：`←/→` 切换，`Space` 翻面，`1/2/3` 答题，`P` 发音
- Store 提供 `goTo(idx)` 方法支持跳转

### 6. 词库检索页

> **已实现**，参见 `wordmate-web/src/views/word/WordSearch.vue`

路由：`/word-search`

功能：
- 搜索框（输入单词或释义）+ 等级下拉筛选
- `el-table` 展示结果：单词(emoji)、释义、音标、等级标签
- `el-pagination` 分页
- 点击行 → `el-dialog` 弹出 WordCard 卡片（可翻转）

API: `wordApi.searchWords(q, level, page, page_size)`

### 7. 侧边栏折叠

> **已实现**，参见 `wordmate-web/src/components/layout/AppLayout.vue`

- 点击 logo 区域切换收窄/展开
- 展开 220px（图标+文字），收窄 64px（仅图标，hover tooltip）
- `el-menu :collapse` 原生支持

### 8. 遗忘曲线增强

> **已实现**，参见 `wordmate-web/src/views/stats/ForgettingCurve.vue`

- 搜索改为 `el-autocomplete` 下拉提示，每条显示 `level_code` 标签区分同名单词
- ECharts 加 `dataZoom`（滚轮 + 滑块缩放）

### 9. 离线队列 + 同步

```typescript
// src/utils/storage.ts
import Dexie, { Table } from 'dexie'

interface PendingAnswer {
  id?: number
  word_id: number
  level_code: string
  result: 'correct' | 'wrong' | 'skip'
  mode: string
  duration_ms: number
  client_ts: string
  synced: 0 | 1
}

interface CachedWord {
  id: number
  level_code: string
  data: any  // 完整词条
  cached_at: string
}

class VocabDB extends Dexie {
  pendingAnswers!: Table<PendingAnswer>
  cachedWords!: Table<CachedWord>
  
  constructor() {
    super('VocabMaster')
    this.version(1).stores({
      pendingAnswers: '++id, synced, client_ts',
      cachedWords: 'id, level_code',
    })
  }
}

export const db = new VocabDB()
```

```typescript
// src/composables/useOfflineSync.ts
import { db } from '@/utils/storage'
import * as studyApi from '@/api/study'
import * as syncApi from '@/api/sync'
import { useOnline } from '@vueuse/core'
import { watch } from 'vue'
import { getDeviceId } from '@/utils/device-id'

const online = useOnline()

// 监听联网状态，上线时触发同步
watch(online, (isOnline) => {
  if (isOnline) flushPendingQueue()
})

export function useOfflineSync() {
  async function submitAnswer(answer: any) {
    if (online.value) {
      try {
        return await studyApi.answer(answer)
      } catch (e) {
        // 失败也入队
        await db.pendingAnswers.add({ ...answer, synced: 0 })
        throw e
      }
    } else {
      await db.pendingAnswers.add({ ...answer, synced: 0 })
      return { queued: true }
    }
  }
  
  return { submitAnswer }
}

async function flushPendingQueue() {
  const pending = await db.pendingAnswers.where({ synced: 0 }).toArray()
  if (pending.length === 0) return
  
  try {
    await syncApi.push({
      device_id: getDeviceId(),
      answers: pending.map(p => ({
        word_id: p.word_id,
        level_code: p.level_code,
        result: p.result,
        mode: p.mode,
        duration_ms: p.duration_ms,
        client_ts: p.client_ts,
      }))
    })
    // 成功后标记 synced
    const ids = pending.map(p => p.id!).filter(Boolean)
    await db.pendingAnswers.where('id').anyOf(ids).modify({ synced: 1 })
    // 同步后清理已同步的
    await db.pendingAnswers.where({ synced: 1 }).delete()
  } catch (e) {
    console.error('flush queue failed', e)
  }
}
```

### 6. TTS 封装

```typescript
// src/composables/useTts.ts
export function useTts() {
  const synth = window.speechSynthesis
  
  function speak(text: string, accent: 'uk' | 'us' = 'us', rate = 1) {
    if (!synth) {
      console.warn('Web Speech API not supported')
      return
    }
    synth.cancel()
    const utter = new SpeechSynthesisUtterance(text)
    utter.lang = accent === 'uk' ? 'en-GB' : 'en-US'
    utter.rate = rate
    
    // 尝试选择合适的 voice
    const voices = synth.getVoices()
    const voice = voices.find(v => v.lang === utter.lang)
    if (voice) utter.voice = voice
    
    synth.speak(utter)
  }
  
  return { speak }
}
```

### 7. 遗忘曲线图表

```vue
<!-- src/components/EbbinghausChart.vue -->
<template>
  <div ref="chartRef" style="width: 100%; height: 320px;"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

const props = defineProps<{
  reviews: Array<{ ts: string; result: string; stage_after: number }>
  firstLearnedAt: string
}>()

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

function drawChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  
  const start = dayjs(props.firstLearnedAt)
  const now = dayjs()
  const totalHours = now.diff(start, 'hour', true)
  
  // 生成理论遗忘曲线（简化：R = e^(-t/S)，每次复习后 S 翻倍）
  const theoreticalPoints: [number, number][] = []
  let S = 24 // 初始记忆强度（小时）
  let lastT = 0
  
  for (const review of props.reviews) {
    const t = dayjs(review.ts).diff(start, 'hour', true)
    // 从 lastT 到 t 的衰减
    const steps = Math.max(1, Math.floor((t - lastT) / 0.5))
    for (let i = 0; i <= steps; i++) {
      const ti = lastT + (t - lastT) * (i / steps)
      const elapsedSinceLastReview = ti - lastT
      theoreticalPoints.push([ti, Math.exp(-elapsedSinceLastReview / S)])
    }
    // 复习后记忆强度提升
    S = S * (review.result === 'correct' ? 2 : 0.5)
    lastT = t
  }
  // 最后一段到 now
  for (let ti = lastT; ti <= totalHours; ti += 0.5) {
    theoreticalPoints.push([ti, Math.exp(-(ti - lastT) / S)])
  }
  
  // 实际复习点
  const reviewPoints = props.reviews.map(r => {
    const t = dayjs(r.ts).diff(start, 'hour', true)
    return { value: [t, r.result === 'correct' ? 1 : 0.3], 
             itemStyle: { color: r.result === 'correct' ? '#67C23A' : '#F56C6C' } }
  })
  
  chart.setOption({
    grid: { top: 30, left: 50, right: 20, bottom: 40 },
    xAxis: { type: 'value', name: '小时', axisLabel: { formatter: (v: number) => 
      v < 24 ? `${v.toFixed(0)}h` : `${(v/24).toFixed(0)}d` } },
    yAxis: { type: 'value', name: '记忆保留率', min: 0, max: 1, axisLabel: { formatter: '{value}' } },
    series: [
      { name: '理论曲线', type: 'line', data: theoreticalPoints, smooth: true, showSymbol: false, lineStyle: { color: '#409EFF' } },
      { name: '复习节点', type: 'scatter', data: reviewPoints, symbolSize: 10 }
    ],
    tooltip: { trigger: 'axis' },
    legend: { top: 0 }
  })
}

onMounted(drawChart)
watch(() => props.reviews, drawChart, { deep: true })
onUnmounted(() => chart?.dispose())
</script>
```

### 8. Vite 配置

```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] }),
  ],
  resolve: {
    alias: { '@': path.resolve(__dirname, 'src') }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
  },
  define: {
    'import.meta.env.VITE_APP_VERSION': JSON.stringify(require('./package.json').version)
  }
})
```

### 9. 路由守卫

```typescript
// src/router/guards.ts
import { useUserStore } from '@/stores/user'

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to) => {
    const userStore = useUserStore()
    
    if (to.meta.public) return true
    
    if (!userStore.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    
    if (!userStore.user) {
      try { await userStore.fetchMe() } catch { return '/login' }
    }
    
    if (to.meta.adminOnly && !userStore.isAdmin) {
      return '/403'
    }
    
    return true
  })
}
```

## UI 设计规范

- **主色**：教育场景推荐 `#1890FF`（Element Plus `primary`）或 `#4F46E5`（紫蓝）
- **学习状态色**：未学（灰）、学习中（蓝）、已掌握（绿）、答错（红）
- **字体**：中文 `PingFang SC/Microsoft YaHei`，英文单词用 `Inter` 或系统默认无衬线，音标用 `Charis SIL`（有 IPA 字符支持）
- **卡片翻面**：使用 CSS 3D `transform: rotateY(180deg)` + `transition`，翻面动画 0.4s
- **响应式**：桌面宽屏三栏布局（侧栏+主内容+辅助信息），小屏（<768px）折叠为单栏
- **深色模式**：Element Plus 3.x 内置暗色主题，跟随系统或用户设置

## 性能要点

- **代码分割**：路由级懒加载（`const X = () => import('...')`）
- **图片**：词卡配图用 WebP，懒加载（`loading="lazy"`）
- **词库缓存**：下载后存 IndexedDB，首次访问走网络，后续走本地
- **Service Worker**：用 Vite PWA 插件，离线也能打开应用
- **虚拟滚动**：词表超过 100 项时用 `el-virtual-list`
