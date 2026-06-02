'use strict';
/**
 * VocabMaster — 微信开发者工具自动化启动器
 * 移植自 FocusLab launch-devtools.js
 *
 * 流程: CLI islogin → 解析端口 → HTTP /v2/open → HTTP /v2/auto → WS 等待 → automator.connect
 * 优势: 不用 spawn 中文路径 cli.bat，通过 HTTP API 启动自动化
 */
const http = require('http')
const { execSync } = require('child_process')
const automator = require('miniprogram-automator')
const path = require('path')

const CLI = 'C:\\Program Files (x86)\\Tencent\\微信web开发者工具\\cli.bat'
const PROJECT_PATH = path.resolve(__dirname, '..', 'dist', 'dev', 'mp-weixin')
const AUTO_PORT = Number(process.env.WX_AUTO_PORT) || 60616

function runCli(args) {
  try { return execSync(`cmd /c "${CLI}" ${args} 2>&1`, { encoding: 'utf8', timeout: 15000 }) }
  catch (e) { return e.stdout || e.message }
}

function discoverPort() {
  const out = runCli('islogin')
  const m = out.match(/listening on http:\/\/127\.0\.0\.1:(\d+)/)
  if (m) return Number(m[1])
  throw new Error(`Cannot detect DevTools port.\nCLI output:\n${out}`)
}

function httpGet(url) {
  return new Promise((resolve, reject) => {
    http.get(url, { timeout: 10000 }, (res) => {
      let data = ''
      res.on('data', d => data += d)
      res.on('end', () => resolve({ status: res.statusCode, body: data }))
    }).on('error', reject)
  })
}

function waitForWs(port, timeout = 20000) {
  return new Promise((resolve, reject) => {
    const deadline = Date.now() + timeout
    function attempt() {
      const ws = new (require('ws'))(`ws://127.0.0.1:${port}`)
      ws.once('open', () => { ws.close(); resolve() })
      ws.once('error', () => {
        if (Date.now() < deadline) setTimeout(attempt, 500)
        else reject(new Error(`WS port ${port} not ready after ${timeout}ms`))
      })
    }
    attempt()
  })
}

async function launchMiniProgram() {
  console.log('[launch] Discovering DevTools port...')
  const httpPort = discoverPort()
  const BASE = `http://127.0.0.1:${httpPort}`
  console.log(`[launch] DevTools HTTP at ${BASE}`)

  const enc = encodeURIComponent(PROJECT_PATH)

  // Open project
  console.log('[launch] Opening project...')
  const openRes = await httpGet(`${BASE}/v2/open?projectpath=${enc}`)
  console.log(`[launch] Open: ${openRes.status}`)
  await new Promise(r => setTimeout(r, 3000))

  // Enable automation (reuse if already open)
  console.log(`[launch] Enabling automation on port ${AUTO_PORT}...`)
  let autoOk = false
  try {
    await waitForWs(AUTO_PORT, 2000)
    console.log(`[launch] Port ${AUTO_PORT} already open, reusing`)
    autoOk = true
  } catch (_) { /* port not open */ }

  if (!autoOk) {
    const autoRes = await httpGet(`${BASE}/v2/auto?project=${enc}&port=${AUTO_PORT}`)
    console.log(`[launch] Auto: ${autoRes.status} ${autoRes.body.substring(0, 100)}`)
    if (autoRes.status !== 200) throw new Error(`/v2/auto failed: ${autoRes.body}`)
  }

  // Wait for WS
  console.log('[launch] Waiting for automation WebSocket...')
  await waitForWs(AUTO_PORT, 20000)

  // Connect
  console.log('[launch] Connecting automator...')
  const mp = await automator.connect({ wsEndpoint: `ws://127.0.0.1:${AUTO_PORT}` })
  console.log('[launch] Connected!')
  return mp
}

module.exports = { launchMiniProgram, AUTO_PORT }
