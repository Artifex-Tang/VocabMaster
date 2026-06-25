import { defineConfig } from '@playwright/test'

// Cloud-prod E2E config. Run: npx playwright test --config=playwright.e2e.config.ts
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
    baseURL: process.env.E2E_BASE || 'http://60.205.145.132',
    screenshot: 'on',
    trace: 'retain-on-failure',
    viewport: { width: 1280, height: 800 },
    actionTimeout: 12000,
    navigationTimeout: 20000,
    ignoreHTTPSErrors: true,
  },
})
