import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 30000,
  retries: 0,
  use: {
    baseURL: 'http://localhost:3001',
    screenshot: 'on',
    trace: 'on-first-retry',
    browserName: 'chromium',
  },
  projects: [
    {
      name: 'Desktop Chrome',
      use: {
        viewport: { width: 1280, height: 800 },
      },
    },
  ],
})
