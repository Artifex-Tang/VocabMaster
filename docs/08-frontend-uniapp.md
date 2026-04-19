# 08 — Uni-app 前端实现规范（小程序 + Android）

> Uni-app 一套代码编译多端：微信小程序、Android APK、iOS、H5。我们主攻前两端，其他端留作编译选项。

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Uni-app | 3.x（Vue 3 版） | 跨端框架 |
| Vue | 3.5+ | Composition API |
| Vite | 5.x | 构建（Uni-app Vue3 版基于 Vite） |
| TypeScript | 5.x | 类型（小程序端部分语法受限） |
| Pinia | 2.x | 状态管理 |
| uview-plus | 3.x | Uni-app UI 库（兼容 Vue3） |
| uCharts | 2.x | 图表（跨端） |
| luch-request | 3.x | 跨端 HTTP 封装（或手写 uni.request 封装） |

## 开发环境

- **IDE**：HBuilderX（官方工具，可视化真机调试、云打包）或 VS Code + `@dcloudio/uni-helper`
- **小程序开发工具**：微信开发者工具（与 HBuilderX 联动）
- **Android 真机/模拟器**：Android Studio + AVD 或真机 USB 调试
- **iOS**：HBuilderX 的"云打包"功能（无需 Mac 编译），或自备 Mac + Xcode

## 项目结构

```
frontend-uniapp/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── pages.json                     # 页面路由+小程序配置
│   ├── manifest.json                  # 多端打包配置
│   ├── uni.scss                       # 全局 scss 变量
│   ├── pages/
│   │   ├── index/
│   │   │   └── index.vue              # 启动页（根据登录态跳转）
│   │   ├── auth/
│   │   │   ├── login.vue
│   │   │   └── register.vue
│   │   ├── dashboard/
│   │   │   └── index.vue              # tabBar 首页
│   │   ├── study/
│   │   │   ├── session.vue            # 学习主界面
│   │   │   └── done.vue               # 完成页
│   │   ├── test/
│   │   │   ├── entry.vue
│   │   │   ├── spelling.vue
│   │   │   ├── choice.vue
│   │   │   └── listening.vue
│   │   ├── stats/
│   │   │   ├── overview.vue           # tabBar 报表
│   │   │   ├── forgetting-curve.vue
│   │   │   └── calendar.vue
│   │   ├── wrong-word/
│   │   │   └── list.vue
│   │   ├── levels/
│   │   │   └── select.vue
│   │   └── profile/
│   │       ├── index.vue              # tabBar 我的
│   │       ├── settings.vue
│   │       └── about.vue
│   ├── components/
│   │   ├── word-card/
│   │   │   └── word-card.vue
│   │   ├── audio-btn/
│   │   │   └── audio-btn.vue
│   │   ├── streak-badge/
│   │   ├── progress-bar/
│   │   └── ebbinghaus-chart/
│   │       └── ebbinghaus-chart.vue   # 基于 uCharts
│   ├── api/                            # 与 Web 端同接口，代码尽量共享
│   │   ├── request.ts
│   │   ├── types.ts
│   │   ├── auth.ts
│   │   ├── word.ts
│   │   ├── study.ts
│   │   └── ...
│   ├── stores/                         # Pinia，尽量与 Web 共享
│   │   ├── user.ts
│   │   ├── settings.ts
│   │   └── study.ts
│   ├── utils/
│   │   ├── request.ts                  # uni.request 封装
│   │   ├── storage.ts                  # 跨端存储（uni.setStorage / localStorage）
│   │   ├── tts.ts                      # 跨端 TTS（小程序用音频 URL，Android 用原生 TextToSpeech）
│   │   ├── platform.ts                 # 平台判断
│   │   ├── device-id.ts
│   │   └── date.ts
│   └── static/
│       ├── logo.png
│       ├── tab-icons/
│       └── sounds/
└── uni_modules/                        # uview-plus 等插件
```

## 关键配置

### `pages.json`

```json
{
  "pages": [
    { "path": "pages/index/index", "style": { "navigationBarTitleText": "VocabMaster" } },
    { "path": "pages/auth/login", "style": { "navigationBarTitleText": "登录" } },
    { "path": "pages/auth/register", "style": { "navigationBarTitleText": "注册" } },
    { "path": "pages/dashboard/index", "style": { "navigationBarTitleText": "今日" } },
    { "path": "pages/study/session", "style": { "navigationBarTitleText": "学习中", "disableScroll": true } },
    { "path": "pages/study/done", "style": { "navigationBarTitleText": "完成" } },
    { "path": "pages/stats/overview", "style": { "navigationBarTitleText": "报表" } },
    { "path": "pages/stats/forgetting-curve", "style": { "navigationBarTitleText": "遗忘曲线" } },
    { "path": "pages/wrong-word/list", "style": { "navigationBarTitleText": "错题本" } },
    { "path": "pages/levels/select", "style": { "navigationBarTitleText": "选择等级" } },
    { "path": "pages/profile/index", "style": { "navigationBarTitleText": "我的" } },
    { "path": "pages/profile/settings", "style": { "navigationBarTitleText": "设置" } },
    { "path": "pages/test/entry", "style": { "navigationBarTitleText": "测试" } },
    { "path": "pages/test/spelling", "style": { "navigationBarTitleText": "拼写测试" } },
    { "path": "pages/test/choice", "style": { "navigationBarTitleText": "选择题" } },
    { "path": "pages/test/listening", "style": { "navigationBarTitleText": "听写" } }
  ],
  "globalStyle": {
    "navigationBarTextStyle": "black",
    "navigationBarTitleText": "VocabMaster",
    "navigationBarBackgroundColor": "#FFFFFF",
    "backgroundColor": "#F5F7FA"
  },
  "tabBar": {
    "color": "#8F8F8F",
    "selectedColor": "#1890FF",
    "backgroundColor": "#FFFFFF",
    "borderStyle": "black",
    "list": [
      { "pagePath": "pages/dashboard/index", "text": "今日", "iconPath": "static/tab-icons/home.png", "selectedIconPath": "static/tab-icons/home-active.png" },
      { "pagePath": "pages/stats/overview", "text": "报表", "iconPath": "static/tab-icons/stats.png", "selectedIconPath": "static/tab-icons/stats-active.png" },
      { "pagePath": "pages/profile/index", "text": "我的", "iconPath": "static/tab-icons/profile.png", "selectedIconPath": "static/tab-icons/profile-active.png" }
    ]
  }
}
```

### `manifest.json`（核心片段）

```jsonc
{
  "name": "VocabMaster",
  "appid": "",                                  // HBuilderX 申请的 DCloud AppID
  "description": "艾宾浩斯背单词",
  "versionName": "1.0.0",
  "versionCode": 1,
  "mp-weixin": {
    "appid": "wx1234567890abcdef",             // 微信小程序 AppID
    "setting": { "urlCheck": false, "es6": true, "minified": true },
    "permission": {
      "scope.userInfo": { "desc": "用于个性化学习体验" }
    },
    "requiredBackgroundModes": ["audio"]
  },
  "app-plus": {
    "distribute": {
      "android": {
        "permissions": [
          "<uses-permission android:name=\"android.permission.INTERNET\"/>",
          "<uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\"/>",
          "<uses-permission android:name=\"android.permission.WAKE_LOCK\"/>",
          "<uses-permission android:name=\"android.permission.RECORD_AUDIO\"/>",
          "<uses-permission android:name=\"android.permission.MODIFY_AUDIO_SETTINGS\"/>",
          "<uses-permission android:name=\"android.permission.VIBRATE\"/>"
        ],
        "minSdkVersion": 26,
        "targetSdkVersion": 34,
        "abiFilters": ["armeabi-v7a", "arm64-v8a"]
      },
      "ios": { "idfa": false }
    }
  },
  "h5": {
    "title": "VocabMaster",
    "router": { "mode": "history", "base": "/" },
    "devServer": {
      "port": 3001,
      "proxy": {
        "/api": { "target": "http://localhost:8080", "changeOrigin": true }
      }
    }
  },
  "vueVersion": "3"
}
```

## 核心跨端适配层

### 1. HTTP 封装（`utils/request.ts`）

```typescript
import { useUserStore } from '@/stores/user'
import { getDeviceId } from '@/utils/device-id'
import { getPlatform } from '@/utils/platform'

const BASE_URL = import.meta.env.VITE_API_BASE || 'https://api.vocabmaster.com/api/v1'

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE'
  data?: any
  params?: Record<string, any>
  header?: Record<string, string>
  noAuth?: boolean
}

export function request<T = any>(opts: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const userStore = useUserStore()
    const header: Record<string, string> = {
      'Content-Type': 'application/json',
      'X-Device-Id': getDeviceId(),
      'X-Device-Type': getPlatform(),       // miniprogram / android / h5 / ios
      'X-Client-Version': '1.0.0',
      ...opts.header,
    }
    if (!opts.noAuth && userStore.accessToken) {
      header.Authorization = `Bearer ${userStore.accessToken}`
    }
    
    let url = BASE_URL + opts.url
    if (opts.params) {
      const qs = Object.entries(opts.params)
        .filter(([, v]) => v !== undefined && v !== null)
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
        .join('&')
      if (qs) url += '?' + qs
    }
    
    uni.request({
      url,
      method: opts.method || 'GET',
      data: opts.data,
      header,
      timeout: 15000,
      success: async (res: any) => {
        const body = res.data as { code: number; msg: string; data: T }
        if (body.code === 0) return resolve(body.data)
        
        if (body.code === 20002) {
          // token 过期，刷新后重试
          try {
            await userStore.refreshAccessToken()
            const retry = await request<T>(opts)
            return resolve(retry)
          } catch {
            userStore.logout()
            uni.reLaunch({ url: '/pages/auth/login' })
            return reject(new Error('token refresh failed'))
          }
        }
        
        if (body.code === 20001) {
          userStore.logout()
          uni.reLaunch({ url: '/pages/auth/login' })
        } else {
          uni.showToast({ title: body.msg, icon: 'none' })
        }
        reject(new Error(body.msg))
      },
      fail: (err) => {
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

export const http = {
  get: <T>(url: string, params?: any) => request<T>({ url, method: 'GET', params }),
  post: <T>(url: string, data?: any) => request<T>({ url, method: 'POST', data }),
  patch: <T>(url: string, data?: any) => request<T>({ url, method: 'PATCH', data }),
  del: <T>(url: string) => request<T>({ url, method: 'DELETE' }),
}
```

### 2. 平台判断（`utils/platform.ts`）

```typescript
export function getPlatform(): 'miniprogram' | 'android' | 'ios' | 'h5' {
  // #ifdef MP-WEIXIN
  return 'miniprogram'
  // #endif
  
  // #ifdef APP-PLUS
  const sys = uni.getSystemInfoSync()
  return sys.platform === 'ios' ? 'ios' : 'android'
  // #endif
  
  // #ifdef H5
  return 'h5'
  // #endif
}

export function isMiniProgram(): boolean {
  return getPlatform() === 'miniprogram'
}

export function isApp(): boolean {
  // #ifdef APP-PLUS
  return true
  // #endif
  return false
}
```

### 3. 存储适配（`utils/storage.ts`）

```typescript
export const storage = {
  get<T>(key: string): T | null {
    try {
      const v = uni.getStorageSync(key)
      if (!v) return null
      return typeof v === 'string' ? JSON.parse(v) : v
    } catch { return null }
  },
  set(key: string, value: any): void {
    uni.setStorageSync(key, typeof value === 'string' ? value : JSON.stringify(value))
  },
  remove(key: string): void {
    uni.removeStorageSync(key)
  },
  clear(): void {
    uni.clearStorageSync()
  },
}

// 小型"表"封装，用于离线队列等场景
export class LocalTable<T extends { id?: number | string }> {
  constructor(private readonly name: string) {}
  
  all(): T[] {
    return storage.get<T[]>(this.name) || []
  }
  add(item: T): void {
    const list = this.all()
    if (!item.id) (item as any).id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
    list.push(item)
    storage.set(this.name, list)
  }
  remove(id: T['id']): void {
    const list = this.all().filter(x => x.id !== id)
    storage.set(this.name, list)
  }
  update(id: T['id'], patch: Partial<T>): void {
    const list = this.all().map(x => x.id === id ? { ...x, ...patch } : x)
    storage.set(this.name, list)
  }
  filter(pred: (x: T) => boolean): T[] {
    return this.all().filter(pred)
  }
  clear(): void {
    storage.remove(this.name)
  }
}

// 使用示例
export const pendingAnswersTable = new LocalTable<PendingAnswer>('pending_answers')
```

### 4. TTS 跨端适配（`utils/tts.ts`）

不同端 TTS 实现完全不同，用条件编译：

```typescript
export interface TtsOptions {
  text: string
  accent?: 'uk' | 'us'
  rate?: number      // 0.5 ~ 1.5
  audioUrl?: string  // 如果有预录音频 URL，优先使用
}

export function speak(opts: TtsOptions): Promise<void> {
  // 有预录音频时，所有端都优先用音频播放
  if (opts.audioUrl) return playAudio(opts.audioUrl)
  
  // #ifdef MP-WEIXIN
  // 小程序没有本地 TTS，调用服务端音频 URL 或有道词典 API
  return playFromDict(opts.text, opts.accent || 'us')
  // #endif
  
  // #ifdef APP-PLUS
  return appSpeak(opts)
  // #endif
  
  // #ifdef H5
  return h5Speak(opts)
  // #endif
}

function playAudio(url: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const ctx = uni.createInnerAudioContext()
    ctx.src = url
    ctx.onEnded(() => { ctx.destroy(); resolve() })
    ctx.onError((e) => { ctx.destroy(); reject(e) })
    ctx.play()
  })
}

// 小程序：走有道词典音频 CDN（或自己的后端音频代理）
function playFromDict(word: string, accent: 'uk' | 'us'): Promise<void> {
  const type = accent === 'uk' ? 1 : 2
  const url = `https://dict.youdao.com/dictvoice?audio=${encodeURIComponent(word)}&type=${type}`
  return playAudio(url)
}

// #ifdef APP-PLUS
function appSpeak(opts: TtsOptions): Promise<void> {
  return new Promise((resolve, reject) => {
    // Android 原生 TextToSpeech via plus API
    // 需要在 nativeplugin 或用 plus.speech（部分机型可能受限）
    // 方案 A：plus.speech（语音合成 API，部分设备支持）
    // 方案 B：退回 playFromDict
    if ((plus as any).speech) {
      const language = opts.accent === 'uk' ? 'en-GB' : 'en-US'
      ;(plus as any).speech.speak({
        text: opts.text,
        language,
        rate: opts.rate || 1,
      }, resolve, reject)
    } else {
      playFromDict(opts.text, opts.accent || 'us').then(resolve).catch(reject)
    }
  })
}
// #endif

// #ifdef H5
function h5Speak(opts: TtsOptions): Promise<void> {
  return new Promise((resolve) => {
    if (typeof window === 'undefined' || !window.speechSynthesis) {
      return playFromDict(opts.text, opts.accent || 'us').then(resolve).catch(() => resolve())
    }
    const u = new SpeechSynthesisUtterance(opts.text)
    u.lang = opts.accent === 'uk' ? 'en-GB' : 'en-US'
    u.rate = opts.rate || 1
    u.onend = () => resolve()
    window.speechSynthesis.cancel()
    window.speechSynthesis.speak(u)
  })
}
// #endif
```

**注意**：小程序端有"业务域名"校验，调用有道词典音频 CDN 需要在微信小程序管理后台配置 `request` 和 `downloadFile` 合法域名（`dict.youdao.com`），否则只能在开发阶段用"不校验合法域名"。生产环境更稳妥的做法是**自己后端代理一层音频 URL**（`/api/v1/tts/audio?word=xxx&accent=uk`），服务端从有道/剑桥 CDN 拉回来后 304 透传。

### 5. 设备 ID

```typescript
// utils/device-id.ts
import { storage } from '@/utils/storage'

export function getDeviceId(): string {
  let id = storage.get<string>('device_id')
  if (!id) {
    id = generate()
    storage.set('device_id', id)
  }
  return id
}

function generate(): string {
  // 尽量用稳定的设备特征（尽管各端能拿到的东西不同）
  const sys = uni.getSystemInfoSync()
  const seed = `${sys.platform}-${sys.model || 'x'}-${Date.now()}-${Math.random()}`
  // 简单哈希
  let h = 0
  for (let i = 0; i < seed.length; i++) h = ((h << 5) - h + seed.charCodeAt(i)) | 0
  return `d_${Math.abs(h).toString(36)}_${Date.now().toString(36)}`
}
```

## 关键页面实现

### 1. 单词卡片组件

```vue
<!-- components/word-card/word-card.vue -->
<template>
  <view class="word-card" :class="{ flipped }" @click="handleFlip">
    <!-- 正面 -->
    <view v-if="!flipped" class="face front">
      <text class="emoji" v-if="word.emoji">{{ word.emoji }}</text>
      <image v-else-if="word.image_url" :src="word.image_url" class="image" mode="aspectFit" />
      <text class="word">{{ word.word }}</text>
      <view class="ipa-row">
        <text class="ipa">{{ preferredAccent === 'uk' ? word.ipa_uk : word.ipa_us }}</text>
        <view class="audio-btn" @click.stop="playAudio">🔊</view>
      </view>
      <text class="hint">点击查看释义</text>
    </view>
    <!-- 反面 -->
    <view v-else class="face back">
      <view class="section">
        <text class="label">English</text>
        <text class="content">{{ word.en_definition }}</text>
      </view>
      <view class="section">
        <text class="label">中文</text>
        <text class="content">{{ word.zh_definition }}</text>
      </view>
      <view class="section example" v-if="word.example_en">
        <text class="label">Example</text>
        <text class="content">"{{ word.example_en }}"</text>
        <text class="zh">{{ word.example_zh }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useSettingsStore } from '@/stores/settings'
import { speak } from '@/utils/tts'

const props = defineProps<{ word: any }>()
const emit = defineEmits<{ flip: [] }>()

const flipped = ref(false)
const settings = useSettingsStore()
const preferredAccent = computed(() => settings.preferredAccent)

function handleFlip() {
  if (flipped.value) return
  flipped.value = true
  emit('flip')
  if (settings.autoPlayAudio) playAudio()
}

function playAudio() {
  const audioUrl = preferredAccent.value === 'uk' ? props.word.audio_url_uk : props.word.audio_url_us
  speak({ text: props.word.word, accent: preferredAccent.value, audioUrl })
}

defineExpose({ reset: () => (flipped.value = false) })
</script>

<style lang="scss" scoped>
.word-card {
  width: 680rpx;
  min-height: 800rpx;
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.08);
  padding: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  
  .emoji { font-size: 180rpx; }
  .image { width: 400rpx; height: 300rpx; }
  .word { font-size: 72rpx; font-weight: 700; margin-top: 32rpx; }
  .ipa-row { display: flex; align-items: center; margin-top: 16rpx; gap: 16rpx; }
  .ipa { font-size: 36rpx; color: #606266; }
  .audio-btn { padding: 8rpx 20rpx; background: #f0f2f5; border-radius: 24rpx; font-size: 36rpx; }
  .hint { font-size: 28rpx; color: #909399; margin-top: 48rpx; }
  
  .back {
    width: 100%;
    gap: 32rpx;
    align-items: flex-start;
  }
  .section { width: 100%; }
  .label { font-size: 28rpx; color: #909399; margin-bottom: 12rpx; }
  .content { font-size: 34rpx; color: #303133; line-height: 1.6; }
  .zh { font-size: 30rpx; color: #606266; margin-top: 8rpx; display: block; }
}
</style>
```

### 2. 学习会话页

```vue
<!-- pages/study/session.vue -->
<template>
  <view class="session">
    <view class="header">
      <text class="counter">{{ currentIdx + 1 }} / {{ queue.length }}</text>
      <text class="correct">✓ {{ correctCount }}</text>
    </view>
    <view class="progress">
      <view class="progress-inner" :style="{ width: progress + '%' }"></view>
    </view>
    
    <word-card v-if="currentWord" ref="cardRef" :key="currentWord.id" :word="currentWord" @flip="isFlipped = true" />
    
    <view class="actions" v-if="isFlipped">
      <button class="btn wrong" @click="handleAnswer('wrong')">不认识</button>
      <button class="btn correct" @click="handleAnswer('correct')">认识 ✓</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import * as studyApi from '@/api/study'
import { useOfflineSync } from '@/composables/useOfflineSync'

const level = ref('')
const queue = ref<any[]>([])
const currentIdx = ref(0)
const isFlipped = ref(false)
const correctCount = ref(0)
const startTime = ref(Date.now())
const cardRef = ref<any>(null)

const { submitAnswer } = useOfflineSync()

const currentWord = computed(() => queue.value[currentIdx.value])
const progress = computed(() => queue.value.length ? Math.round(currentIdx.value / queue.value.length * 100) : 0)

onLoad((opts) => {
  level.value = (opts?.level as string) || 'CET4'
})

onMounted(async () => {
  const plan = await studyApi.getToday(level.value)
  queue.value = [...plan.review_words, ...plan.new_words]
  startTime.value = Date.now()
})

async function handleAnswer(result: 'correct' | 'wrong') {
  const word = currentWord.value
  if (result === 'correct') correctCount.value++
  
  await submitAnswer({
    word_id: word.id,
    level_code: word.level_code,
    result,
    mode: 'card',
    duration_ms: Date.now() - startTime.value,
    client_ts: new Date().toISOString(),
  })
  
  if (currentIdx.value + 1 >= queue.value.length) {
    uni.redirectTo({ url: `/pages/study/done?level=${level.value}&correct=${correctCount.value}&total=${queue.value.length}` })
  } else {
    currentIdx.value++
    isFlipped.value = false
    startTime.value = Date.now()
    cardRef.value?.reset()
  }
}
</script>
```

### 3. 离线同步 composable

```typescript
// composables/useOfflineSync.ts
import { ref, watch, onMounted } from 'vue'
import { pendingAnswersTable } from '@/utils/storage'
import * as studyApi from '@/api/study'
import * as syncApi from '@/api/sync'
import { getDeviceId } from '@/utils/device-id'

const online = ref(true)

// 监听网络状态
uni.onNetworkStatusChange((res) => {
  online.value = res.isConnected
  if (res.isConnected) flushQueue()
})

uni.getNetworkType({
  success: (res) => { online.value = res.networkType !== 'none' }
})

export function useOfflineSync() {
  async function submitAnswer(answer: any) {
    if (online.value) {
      try {
        return await studyApi.answer(answer)
      } catch (e) {
        pendingAnswersTable.add({ ...answer, synced: 0 })
        throw e
      }
    } else {
      pendingAnswersTable.add({ ...answer, synced: 0 })
      return { queued: true }
    }
  }
  
  return { submitAnswer, online }
}

async function flushQueue() {
  const pending = pendingAnswersTable.filter(x => !x.synced)
  if (pending.length === 0) return
  
  try {
    await syncApi.push({
      device_id: getDeviceId(),
      answers: pending.map(({ id, synced, ...rest }) => rest),
    })
    pending.forEach(p => pendingAnswersTable.remove(p.id))
  } catch (e) {
    console.error('flush failed', e)
  }
}
```

### 4. 微信小程序登录

```typescript
// api/auth.ts 中的微信登录辅助
export function wechatLogin(userInfo?: any): Promise<void> {
  return new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: async ({ code }) => {
        try {
          const data = await http.post<any>('/auth/login-wechat', {
            code,
            user_info: userInfo,
          })
          const userStore = useUserStore()
          userStore.setAuth(data)
          resolve()
        } catch (e) { reject(e) }
      },
      fail: reject,
    })
  })
}

// 页面中的触发
// <button open-type="getUserInfo" @getuserinfo="onGetUserInfo"> 微信登录 </button>
async function onGetUserInfo(e: any) {
  const userInfo = e.detail?.userInfo
  await wechatLogin(userInfo)
  uni.switchTab({ url: '/pages/dashboard/index' })
}
```

### 5. 遗忘曲线图表（uCharts）

```vue
<!-- components/ebbinghaus-chart/ebbinghaus-chart.vue -->
<template>
  <view class="chart-wrap">
    <qiun-data-charts
      type="mix"
      :chartData="chartData"
      :opts="opts"
      :canvas2d="true"
      canvasId="ebbinghausChart"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{
  reviews: Array<{ ts: string; result: string; stage_after: number }>
  firstLearnedAt: string
}>()

const chartData = ref<any>({ categories: [], series: [] })
const opts = ref({
  color: ['#1890FF', '#67C23A', '#F56C6C'],
  padding: [15, 10, 0, 15],
  xAxis: { disableGrid: true },
  yAxis: { gridType: 'dash', dashLength: 2, data: [{ min: 0, max: 1 }] },
  legend: { show: true, position: 'top' },
})

watch(() => props.reviews, () => { rebuildChart() }, { immediate: true, deep: true })

function rebuildChart() {
  const start = new Date(props.firstLearnedAt).getTime()
  const hoursFromStart = (ts: string) => (new Date(ts).getTime() - start) / 3600000
  
  // 理论曲线点（简化版）
  let S = 24
  let lastT = 0
  const theoretical: [number, number][] = []
  for (const r of props.reviews) {
    const t = hoursFromStart(r.ts)
    const steps = 10
    for (let i = 0; i <= steps; i++) {
      const ti = lastT + (t - lastT) * (i / steps)
      theoretical.push([ti, Math.exp(-(ti - lastT) / S)])
    }
    S *= r.result === 'correct' ? 2 : 0.5
    lastT = t
  }
  
  const dots = props.reviews.map(r => ({
    value: r.result === 'correct' ? 1 : 0.3,
    label: new Date(r.ts).toLocaleString(),
  }))
  
  chartData.value = {
    categories: theoretical.map(([t]) => `${t.toFixed(1)}h`),
    series: [
      { name: '理论曲线', type: 'line', data: theoretical.map(p => p[1]), smooth: true },
      { name: '复习点', type: 'point', data: dots.map(d => d.value) },
    ],
  }
}
</script>
```

> 需要安装 `@qiun/ucharts` 或 `qiun-data-charts` 插件（Uni-app 插件市场可直接导入）。

## 编译发布流程

### 微信小程序

1. 在 HBuilderX 中选 **发行 → 小程序-微信**，生成 `unpackage/dist/build/mp-weixin/` 目录
2. 打开微信开发者工具 → 导入该目录 → 预览/上传
3. 微信公众平台 → 版本管理 → 提交审核 → 发布

注意事项：
- `request` 合法域名必须配置后端 API 域名（必须 HTTPS）
- 音频播放域名单独配置在 `downloadFile` 合法域名
- 小程序包体积 ≤ 2MB（主包），超过要用分包加载

### Android APK

1. HBuilderX → **发行 → 原生 App-云打包**（DCloud 云服务）
2. 选择"公共测试证书"快速打包（生产需要自己的 keystore 签名）
3. 等 5~10 分钟生成 APK，下载安装

或本地打包（需要自建 Android Studio 环境，较繁琐，推荐云打包）。

生产建议：
- 使用自签名 keystore，**务必备份**（丢了无法更新应用）
- `targetSdkVersion` 跟进 Google Play 和国内应用市场最新要求（当前 34）
- APP 启动时请求更新：调用 `uni.getUpdateManager()`（仅小程序）或自建版本检查接口 `/api/v1/version/latest`，Android 端引导下载新 APK

## Uni-app 与 Web 端的代码共享策略

理论上 `src/api/`、`src/stores/`、部分 `utils/` 可以共享。实操建议：

1. 新建一个 `shared` 包（pnpm workspace 或 monorepo），把**纯业务逻辑**（API 类型定义、错误码、日期工具、艾宾浩斯常量）放进去
2. 不共享：`request.ts`（axios vs uni.request）、`storage.ts`（IndexedDB vs uni.storage）、`tts.ts`（Web Speech vs 原生）、UI 组件（虽然 Uni-app H5 能跑 Vue，但样式规范不同）
3. Pinia store 的**定义**可共享，但**持久化方式**不同（Web 端用 localStorage 插件，小程序用 uni.storage 插件）

MVP 阶段先各自独立实现，稳定后再抽共享包，避免早期抽象错误。

## 常见问题

1. **小程序不支持 dynamic import**：用条件编译代替
2. **Uni-app Pinia 必须显式安装** `@dcloudio/uni-app` 的 Pinia 插件
3. **rpx 单位**：Uni-app 布局用 rpx（750 设计稿），不要混用 px
4. **条件编译** `#ifdef` 必须写在注释里，TypeScript 不报错但构建时识别
5. **App 端 fixed 定位层级**：底部 tabBar 遮挡问题，页面容器加 `padding-bottom: var(--window-bottom)` 或 `uni.getSystemInfoSync().safeAreaInsets.bottom`
6. **iOS WKWebView 音频自动播放**：首次播放需要用户交互触发，此后可编程播放
