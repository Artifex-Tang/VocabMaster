import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e/cloud',
  globalSetup: './e2e/cloud/global-setup.ts',
  timeout: 60000,
  expect: { timeout: 10000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'e2e/.report' }]],
  use: {
    baseURL: process.env.MINI_BASE || 'http://localhost:3002',
    screenshot: 'on',
    trace: 'retain-on-failure',
    viewport: { width: 393, height: 852 },
    deviceScaleFactor: 3,
    isMobile: true,
    hasTouch: true,
    actionTimeout: 12000,
    navigationTimeout: 20000,
  },
})
