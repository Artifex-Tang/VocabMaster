const http=require('http')
const automator=require('miniprogram-automator')
const path=require('path')
const CLI='C:\Program Files (x86)\Tencent\微信web开发者工具\cli.bat'
const PROJECT=path.resolve(__dirname,'..','dist','dev','mp-weixin')
const PORT=Number(process.env.WX_AUTO_PORT)||60617
function runCli(a){try{return require('child_process').execSync(`cmd /c "${CLI}" ${a} 2>&1`,{encoding:'utf-8',timeout:15000})}catch(e){return e.stdout||e.message}}
function httpGet(u){return new Promise((res,rej)=>{http.get(u,{timeout:10000},r=>{let d='';r.on('data',c=>d+=c);r.on('end',()=>res({status:r.statusCode,body:d}))}).on('error',rej)})}
;(async()=>{
  const out=runCli('islogin'); const m=out.match(/listening on http:\/\/127\.0\.0\.1:(\d+)/)
  if(!m){console.log('NO PORT. cli out:',out.slice(0,200));process.exit(1)}
  const hp=m[1]; const B=`http://127.0.0.1:${hp}`; console.log('http port',hp)
  const enc=encodeURIComponent(PROJECT)
  await httpGet(`${B}/v2/open?projectpath=${enc}`); console.log('opened')
  await new Promise(r=>setTimeout(r,4000))
  const ar=await httpGet(`${B}/v2/auto?project=${enc}&port=${PORT}`); console.log('auto',ar.status,ar.body.slice(0,80))
  // wait ws
  for(let i=0;i<40;i++){try{const ws=new (require('ws'))(`ws://127.0.0.1:${PORT}`);await new Promise((res,rej)=>{ws.once('open',()=>{ws.close();res()});ws.once('error',rej)});break}catch(e){await new Promise(r=>setTimeout(r,500))}}
  console.log('ws ready, connecting...')
  const mp=await automator.connect({wsEndpoint:`ws://127.0.0.1:${PORT}`})
  console.log('connected. systemInfo (15s timeout)...')
  try{const si=await Promise.race([mp.systemInfo(),new Promise((_,r)=>setTimeout(()=>r(new Error('SI TIMEOUT')),15000))]);console.log('systemInfo OK',si.platform||si.system)}catch(e){console.log('systemInfo FAIL:',e.message)}
  process.exit(0)
})()
