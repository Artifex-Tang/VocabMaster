<template>
  <view v-if="visible" class="privacy-mask" catchtouchmove="noop">
    <view class="privacy-box">
      <view class="privacy-title">隐私保护提示</view>
      <view class="privacy-desc">
        感谢使用 VocabMaster。继续操作前请阅读并同意
        <text class="privacy-link" @click="openContract">《用户隐私保护指引》</text>。
        我们仅收集必要的账号信息与学习记录，用于提供背单词服务，不会共享给第三方。
      </view>
      <view class="privacy-btns">
        <button class="btn-reject" @click="onReject">拒绝</button>
        <button
          id="agree-btn"
          class="btn-agree"
          open-type="agreePrivacyAuthorization"
          @agreeprivacyauthorization="onAgree"
        >
          同意并继续
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { usePrivacyStore } from '@/stores/privacy'
import { storeToRefs } from 'pinia'

const store = usePrivacyStore()
const { visible } = storeToRefs(store)

function noop() {}

function openContract() {
  // #ifdef MP-WEIXIN
  const g = globalThis as unknown as {
    openPrivacyContract?: (opts: { success?: () => void; fail?: () => void }) => void
  }
  g.openPrivacyContract?.({})
  // #endif
}

function onAgree() {
  store.agree()
}

function onReject() {
  store.disagree()
  uni.showToast({ title: '需同意后才能使用相关功能', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.privacy-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 64rpx;
}

.privacy-box {
  width: 100%;
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx 40rpx 32rpx;
}

.privacy-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1f2937;
  text-align: center;
  margin-bottom: 24rpx;
}

.privacy-desc {
  font-size: 28rpx;
  color: #4b5563;
  line-height: 1.7;
  margin-bottom: 40rpx;
}

.privacy-link {
  color: #1890ff;
}

.privacy-btns {
  display: flex;
  gap: 24rpx;
}

.btn-reject,
.btn-agree {
  flex: 1;
  height: 88rpx;
  border-radius: 20rpx;
  font-size: 30rpx;
  border: none;
  margin: 0;
  padding: 0;

  &::after {
    border: none;
  }
}

.btn-reject {
  background: #f3f4f6;
  color: #6b7280;
}

.btn-agree {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: #fff;
}
</style>
