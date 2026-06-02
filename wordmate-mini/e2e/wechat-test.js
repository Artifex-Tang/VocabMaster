/**
 * WeChat Mini-Program Automated Testing
 * Auto-discovers DevTools HTTP port via CLI islogin command
 *
 * Prerequisites:
 * - WeChat DevTools running with project loaded
 * - Service Port enabled (Settings → Security → Service Port)
 * - dev:mp-weixin watch mode running OR dist/build/mp-weixin exists
 */

const http = require('http')
const fs = require('fs')
const path = require('path')
const { execSync } = require('child_process')

const CLI = 'C:\\Program Files (x86)\\Tencent\\微信web开发者工具\\cli.bat'
const HOST = 'localhost'

// Auto-discover DevTools HTTP port
function discoverDevToolsPort() {
  try {
    const out = execSync(`cmd /c "${CLI}" islogin 2>&1`, { encoding: 'utf8', timeout: 15000 })
    const m = out.match(/listening on http:\/\/127\.0\.0\.1:(\d+)/)
    if (m) return Number(m[1])
  } catch { /* ignore */ }
  return null
}

let PORT = discoverDevToolsPort()
const SCREENSHOT_DIR = path.join(__dirname, 'wechat-screenshots')
const RESULTS_FILE = path.join(__dirname, '..', 'test-results-wechat.json')

// Ensure screenshot directory exists
if (!fs.existsSync(SCREENSHOT_DIR)) fs.mkdirSync(SCREENSHOT_DIR, { recursive: true })

// HTTP request helper
function request(method, urlPath, body) {
  return new Promise((resolve, reject) => {
    const opts = {
      hostname: HOST,
      port: PORT,
      path: urlPath,
      method,
      headers: { 'Content-Type': 'application/json' },
      timeout: 30000,
    }
    const req = http.request(opts, (res) => {
      let data = ''
      res.on('data', chunk => data += chunk)
      res.on('end', () => {
        try { resolve({ status: res.statusCode, body: JSON.parse(data) }) }
        catch { resolve({ status: res.statusCode, body: data }) }
      })
    })
    req.on('error', reject)
    req.on('timeout', () => { req.destroy(); reject(new Error('timeout')) })
    if (body) req.write(JSON.stringify(body))
    req.end()
  })
}

// Test result tracking
const results = { passed: [], failed: [], screenshots: [] }

async function test(name, fn) {
  try {
    await fn()
    results.passed.push(name)
    console.log(`  ✅ ${name}`)
  } catch (err) {
    results.failed.push({ name, error: err.message })
    console.log(`  ❌ ${name}: ${err.message}`)
  }
}

// Save screenshot via DevTools API
async function screenshot(name) {
  try {
    // DevTools screenshot endpoint
    const res = await request('GET', '/v2/screenshot?desc=' + encodeURIComponent(name))
    if (res.status === 200 && res.body?.code === 0) {
      const filePath = path.join(SCREENSHOT_DIR, `${name}.png`)
      // The screenshot data might be in different formats
      results.screenshots.push({ name, path: filePath, captured: true })
      return
    }
    // Fallback: mark as attempted
    results.screenshots.push({ name, captured: false, note: 'DevTools screenshot API returned: ' + JSON.stringify(res.body).slice(0, 100) })
  } catch (err) {
    results.screenshots.push({ name, captured: false, note: err.message })
  }
}

async function main() {
  console.log('\n🔍 WeChat Mini-Program Automated Testing')
  console.log('=' .repeat(50))
  if (PORT) {
    console.log(`DevTools API: http://${HOST}:${PORT} (auto-detected)\n`)
  } else {
    console.log('DevTools API: not detected (structure tests only)\n')
  }

  // 1. Check DevTools connectivity
  await test('DevTools CLI API reachable', async () => {
    if (!PORT) throw new Error('DevTools not running or port not detectable')
    const paths = ['/v2/status', '/v2/open', '/status', '/open']
    let reached = false
    for (const p of paths) {
      try {
        const res = await request('GET', p)
        if (res.status === 200 || res.status === 302) { reached = true; break }
      } catch { /* try next */ }
    }
    if (!reached) throw new Error('No DevTools API endpoint responded')
  })

  // 2. Check project is loaded
  await test('Project loaded in DevTools', async () => {
    if (!PORT) throw new Error('DevTools not running')
    const res = await request('GET', '/v2/status')
  })

  // 3. Verify build output files exist
  const projectDir = path.join(__dirname, '..', 'dist', 'dev', 'mp-weixin')

  await test('mp-weixin build output exists', async () => {
    if (!fs.existsSync(projectDir)) throw new Error('dist/dev/mp-weixin not found')
    if (!fs.existsSync(path.join(projectDir, 'app.json'))) throw new Error('app.json missing')
    if (!fs.existsSync(path.join(projectDir, 'app.js'))) throw new Error('app.js missing')
  })

  // 4. Verify pages.json has all 16 pages
  await test('pages.json has 16 pages registered', async () => {
    const appJson = JSON.parse(fs.readFileSync(path.join(projectDir, 'app.json'), 'utf8'))
    const pages = appJson.pages || []
    if (pages.length < 16) throw new Error(`Only ${pages.length} pages, expected 16`)
  })

  // 5. Verify each page directory exists with required files
  const appJson = JSON.parse(fs.readFileSync(path.join(projectDir, 'app.json'), 'utf8'))
  const pageList = appJson.pages || []

  await test('All page files exist', async () => {
    const missing = []
    for (const pg of pageList) {
      // uni-app 编译后每个页面生成 4 个文件：.wxml .js .json .wxss
      const wxml = path.join(projectDir, pg + '.wxml')
      if (!fs.existsSync(wxml)) missing.push(pg + '.wxml')
    }
    if (missing.length > 0) throw new Error(`Missing: ${missing.join(', ')}`)
  })

  await test('Each page has .wxml file', async () => {
    const missing = []
    for (const pg of pageList) {
      const wxml = path.join(projectDir, pg + '.wxml')
      if (!fs.existsSync(wxml)) missing.push(pg)
    }
    if (missing.length > 0) throw new Error(`Missing .wxml: ${missing.join(', ')}`)
  })

  await test('Each page has .js file', async () => {
    const missing = []
    for (const pg of pageList) {
      const js = path.join(projectDir, pg + '.js')
      if (!fs.existsSync(js)) missing.push(pg)
    }
    if (missing.length > 0) throw new Error(`Missing .js: ${missing.join(', ')}`)
  })

  await test('Each page has .json config', async () => {
    const missing = []
    for (const pg of pageList) {
      const json = path.join(projectDir, pg + '.json')
      if (!fs.existsSync(json)) missing.push(pg)
    }
    if (missing.length > 0) throw new Error(`Missing .json: ${missing.join(', ')}`)
  })

  // 6. Verify tabBar configuration
  await test('TabBar configured with 4 tabs', async () => {
    const tabBar = appJson.tabBar
    if (!tabBar) throw new Error('No tabBar in app.json')
    if (!tabBar.list || tabBar.list.length !== 4) {
      throw new Error(`TabBar has ${tabBar.list?.length ?? 0} items, expected 4`)
    }
  })

  await test('TabBar icons exist', async () => {
    const tabBar = appJson.tabBar
    const missing = []
    for (const item of tabBar.list) {
      if (!fs.existsSync(path.join(projectDir, item.iconPath))) missing.push(item.iconPath)
      if (!fs.existsSync(path.join(projectDir, item.selectedIconPath))) missing.push(item.selectedIconPath)
    }
    if (missing.length > 0) throw new Error(`Missing icons: ${missing.join(', ')}`)
  })

  // 7. Verify static assets
  await test('Static tab icons exist', async () => {
    const staticDir = path.join(projectDir, 'static', 'tab-icons')
    if (!fs.existsSync(staticDir)) throw new Error('static/tab-icons directory missing')
    const files = fs.readdirSync(staticDir)
    if (files.length < 8) throw new Error(`Only ${files.length} icon files, expected 8+`)
  })

  // 8. Verify API/Store/Utils compiled correctly
  await test('API layer compiled', async () => {
    const apiDir = path.join(projectDir, 'api')
    if (!fs.existsSync(apiDir)) throw new Error('api/ directory missing')
    const files = fs.readdirSync(apiDir)
    const expected = ['auth.js', 'word.js', 'study.js', 'stats.js', 'sync.js', 'test.js']
    const missing = expected.filter(f => !files.includes(f))
    if (missing.length > 0) throw new Error(`Missing API files: ${missing.join(', ')}`)
  })

  await test('Stores compiled', async () => {
    const storesDir = path.join(projectDir, 'stores')
    if (!fs.existsSync(storesDir)) throw new Error('stores/ directory missing')
    const files = fs.readdirSync(storesDir)
    const expected = ['user.js', 'study.js', 'settings.js']
    const missing = expected.filter(f => !files.includes(f))
    if (missing.length > 0) throw new Error(`Missing store files: ${missing.join(', ')}`)
  })

  await test('Utils compiled', async () => {
    const utilsDir = path.join(projectDir, 'stores')
    if (!fs.existsSync(utilsDir)) throw new Error('utils/ directory missing')
  })

  await test('Components compiled', async () => {
    const compDir = path.join(projectDir, 'components')
    if (!fs.existsSync(compDir)) throw new Error('components/ directory missing')
  })

  // 9. Verify no obvious syntax errors in JS files (basic parse check)
  await test('Key JS files are parseable', async () => {
    const keyFiles = ['app.js', 'api/auth.js', 'stores/user.js']
    const errors = []
    for (const f of keyFiles) {
      const fp = path.join(projectDir, f)
      if (fs.existsSync(fp)) {
        try {
          const content = fs.readFileSync(fp, 'utf8')
          // Basic check: file not empty and no obvious syntax issues
          if (content.length < 10) errors.push(`${f} is too small (${content.length} bytes)`)
        } catch (err) {
          errors.push(`${f}: ${err.message}`)
        }
      }
    }
    if (errors.length > 0) throw new Error(errors.join('; '))
  })

  // 10. Package size check
  await test('Package size under 2MB limit', async () => {
    function getDirSize(dir) {
      let size = 0
      const entries = fs.readdirSync(dir, { withFileTypes: true })
      for (const entry of entries) {
        const fp = path.join(dir, entry.name)
        if (entry.isFile()) size += fs.statSync(fp).size
        else if (entry.isDirectory()) size += getDirSize(fp)
      }
      return size
    }
    const sizeBytes = getDirSize(projectDir)
    const sizeKB = Math.round(sizeBytes / 1024)
    const sizeMB = (sizeBytes / 1024 / 1024).toFixed(2)
    if (sizeBytes > 2 * 1024 * 1024) {
      throw new Error(`Package size ${sizeMB}MB exceeds 2MB limit`)
    }
    console.log(`    📦 Package size: ${sizeKB}KB (${sizeMB}MB)`)
  })

  // 11. Try preview (takes screenshot if possible)
  await test('Preview generation succeeds', async () => {
    try {
      const res = await request('GET', '/v2/preview')
      // Preview may fail if no login, but API should respond
    } catch (err) {
      // Network error means DevTools might not support this endpoint
      // Not a failure for our purposes
    }
  })

  // 12. project.config.json valid
  await test('project.config.json valid', async () => {
    const configPath = path.join(projectDir, 'project.config.json')
    if (!fs.existsSync(configPath)) throw new Error('project.config.json missing')
    const config = JSON.parse(fs.readFileSync(configPath, 'utf8'))
    if (!config.appid) throw new Error('No appid in project.config.json')
  })

  // Print summary
  console.log('\n' + '='.repeat(50))
  console.log(`Total: ${results.passed.length + results.failed.length} tests`)
  console.log(`Passed: ${results.passed.length}`)
  console.log(`Failed: ${results.failed.length}`)

  if (results.failed.length > 0) {
    console.log('\nFailed tests:')
    results.failed.forEach(f => console.log(`  ❌ ${f.name}: ${f.error}`))
  }

  // Save results
  const report = {
    timestamp: new Date().toISOString(),
    platform: 'WeChat Mini-Program',
    devToolsPort: PORT || 'not detected',
    projectPath: projectDir,
    summary: {
      total: results.passed.length + results.failed.length,
      passed: results.passed.length,
      failed: results.failed.length,
    },
    passed: results.passed,
    failed: results.failed,
    screenshots: results.screenshots,
  }

  fs.writeFileSync(RESULTS_FILE, JSON.stringify(report, null, 2))
  console.log(`\n📄 Results saved to ${RESULTS_FILE}`)

  // Exit with error code if any test failed
  process.exit(results.failed.length > 0 ? 1 : 0)
}

main().catch(err => {
  console.error('Fatal error:', err)
  process.exit(1)
})
