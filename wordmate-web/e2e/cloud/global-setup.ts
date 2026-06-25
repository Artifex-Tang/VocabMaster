import { execSync } from 'child_process'
import { existsSync, readFileSync } from 'fs'

/**
 * Provision one real user on cloud prod (send-code -> Redis code -> register -> login),
 * then stash tokens into process.env so every worker can inject them into localStorage.
 */
export default async function globalSetup() {
  const email = `e2e+${Date.now()}@vocab.local`
  const pwd = 'FixTest#2026'
  execSync(`python e2e/provision.py "${email}" "${pwd}" e2e/.auth.json`, {
    stdio: 'pipe',
    cwd: process.cwd(),
  })
  if (!existsSync('e2e/.auth.json')) {
    throw new Error('globalSetup: provision produced no .auth.json')
  }
  const auth = readFileSync('e2e/.auth.json', 'utf-8')
  process.env.E2E_AUTH = auth
  console.log(`[globalSetup] provisioned ${email}`)
}
