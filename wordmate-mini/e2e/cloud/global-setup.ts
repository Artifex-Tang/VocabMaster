import { execSync } from 'child_process'
import { existsSync, readFileSync } from 'fs'

export default async function globalSetup() {
  const email = `mini+${Date.now()}@vocab.local`
  const pwd = 'FixTest#2026'
  execSync(`python e2e/provision.py "${email}" "${pwd}" e2e/.auth.json`, { stdio: 'pipe' })
  if (!existsSync('e2e/.auth.json')) throw new Error('mini globalSetup: provision failed')
  const auth = readFileSync('e2e/.auth.json', 'utf-8')
  process.env.MINI_AUTH = auth
  console.log(`[mini globalSetup] provisioned ${email}`)
}
