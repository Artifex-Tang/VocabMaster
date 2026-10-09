const { launchMiniProgram } = require('./launch-devtools')
;(async()=>{
  const mp = await launchMiniProgram()
  console.log('connected. systemInfo...')
  try { const si = await mp.systemInfo(); console.log('systemInfo ok, platform=', si.platform || si.system) } catch(e){ console.log('systemInfo FAIL:', e.message) }
  try { const p = await mp.currentPage(); console.log('currentPage path=', p.path) } catch(e){ console.log('currentPage FAIL:', e.message) }
  try { const p = await mp.reLaunch('/pages/auth/login'); console.log('reLaunch OK path=', p.path) } catch(e){ console.log('reLaunch FAIL:', e.message) }
  process.exit(0)
})()
