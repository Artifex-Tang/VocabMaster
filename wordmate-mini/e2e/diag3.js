const { launchMiniProgram } = require('./launch-devtools')
;(async()=>{
  const mp = await launchMiniProgram()
  console.log('connected, testing mp calls (10s each)...')
  for (const [name, fn] of [['systemInfo', ()=>mp.systemInfo()], ['currentPage', ()=>mp.currentPage()]]) {
    try { const r = await Promise.race([fn(), new Promise((_,r)=>setTimeout(()=>r(new Error('HANG')),10000))]); console.log(name,'OK', r.path||r.platform||r.system||'') }
    catch(e){ console.log(name,'FAIL:',e.message) }
  }
  process.exit(0)
})()
