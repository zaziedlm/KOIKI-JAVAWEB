/** S3 host Chromium, one worker/context. Secret values stay in process memory. */
import fs from 'node:fs';
import {createHash,randomBytes} from 'node:crypto';
import {chromium} from './frontend/node_modules/playwright/index.mjs';
const work=process.argv[2],origin='http://localhost:13000';
const password=fs.readFileSync(work+'/secrets/login_password','utf8').trim();
const hash=v=>createHash('sha256').update(v).digest('hex');
const observations={checks:[],statuses:{},worker:1,max_concurrent_contexts:1};
const jwt=/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/;
function require(condition,message){if(!condition)throw new Error(message);}
const record=(name,status)=>{observations.statuses[name]=status;};
let browser;
try {
  browser=await chromium.launch({headless:true});const context=await browser.newContext();const page=await context.newPage();
  let callbackURL,flowCookie;const errors=[];page.on('pageerror',()=>errors.push('pageerror'));
  page.on('request',req=>{const u=new URL(req.url());if(u.origin===origin&&u.pathname==='/a/auth/callback')callbackURL=req.url();});
  await page.goto(origin+'/a');await page.getByRole('button',{name:'ログイン',exact:true}).waitFor();
  let csrf=await page.getByTestId('csrf').textContent();
  const call=async(path,method='GET',body,headers={})=>context.request.fetch(origin+path,{method,headers:{Origin:origin,'Content-Type':'application/json',...headers},data:body,failOnStatusCode:false,maxRedirects:0});
  const rejectReturn=await call('/a/auth/start','POST',{returnTo:'https://untrusted.example'},{'X-CSRF-Token':csrf});require(rejectReturn.status()===400,'Unapproved return parameter');record('open_redirect_input',rejectReturn.status());
  const missingStart=await call('/a/auth/start','POST',{});require(missingStart.status()===403,'Login CSRF missing');record('login_missing_csrf',missingStart.status());
  flowCookie=(await context.cookies()).find(c=>c.name==='D3_A_FLOW');
  await page.getByRole('button',{name:'ログイン',exact:true}).click();
  await page.locator('#username').waitFor();
  const invalidState=await call('/a/auth/callback?state='+randomBytes(32).toString('base64url')+'&code=invalid');
  require(invalidState.status()===400,'State mismatch');record('state_mismatch',invalidState.status());
  await page.locator('#username').fill('applicant-a');await page.locator('#password').fill(password);await page.locator('#kc-login').click();
  await page.waitForURL(u=>u.origin===origin&&u.pathname==='/a'&&!u.search,{timeout:20000});
  await page.getByRole('button',{name:'下書きを保存',exact:true}).waitFor();
  const cookie=(await context.cookies()).find(c=>c.name==='D3_A_SESSION');require(cookie&&cookie.httpOnly&&cookie.sameSite==='Lax'&&!cookie.secure&&cookie.path==='/a','Session Cookie flags');
  require(!await page.evaluate(()=>document.cookie.includes('D3_A_SESSION')),'HttpOnly visibility');
  require((await page.getByTestId('subject').textContent())==='d3210000-0000-4000-8000-000000000001','SSR identity');
  observations.cookie={name:cookie.name,httpOnly:cookie.httpOnly,secure:cookie.secure,sameSite:cookie.sameSite,path:cookie.path};
  observations.first_session_id_hash=hash(cookie.value);
  observations.checks.push('browser-login-SSR-identity-cookie');
  require(callbackURL&&flowCookie,'Callback observation');
  await context.addCookies([flowCookie]);const replay=await context.request.get(callbackURL,{maxRedirects:0});require(replay.status()===400,'Callback replay');record('callback_replay',replay.status());
  await context.clearCookies({name:'D3_A_FLOW'});
  csrf=await page.getByTestId('csrf').textContent();
  const payload={departmentId:'d3200000-0000-4000-8000-000000000021',claimedAmount:1200,lines:[{expenseCategoryId:'d3200000-0000-4000-8000-000000000022',usageDate:new Date().toISOString().slice(0,10),description:'Negative fixture',purpose:'Must not mutate',amount:1200}]};
  for(const [name,headers,expected] of [['missing_csrf',{},403],['wrong_csrf',{'X-CSRF-Token':'wrong'},403],['wrong_origin',{'X-CSRF-Token':csrf,Origin:'http://untrusted.example'},403],['content_type',{'X-CSRF-Token':csrf,'Content-Type':'text/plain'},415]]) {
    const response=await call('/a/api/expense-requests','POST',payload,headers);require(response.status()===expected,name);record(name,response.status());
  }
  const oversized=await call('/a/api/expense-requests','POST','x'.repeat(65537),{'X-CSRF-Token':csrf});require(oversized.status()===413,'Body limit');record('body_limit',oversized.status());
  const arbitrary=await call('/a/api/proxy?url=http://untrusted.example');require(arbitrary.status()===404,'Fixed API allowlist');record('proxy_target',arbitrary.status());
  const other=await call('/b');require(other.status()===404,'Client B excluded');record('rp_b',other.status());
  await page.getByLabel('金額（円）').fill('1200');await page.getByLabel('内容',{exact:true}).fill('S3 browser travel');await page.getByLabel('目的',{exact:true}).fill('S3 verification');
  const created=page.waitForResponse(r=>r.request().method()==='POST'&&r.url()===origin+'/a/api/expense-requests');
  await page.getByRole('button',{name:'下書きを保存',exact:true}).click();require((await created).status()===201,'Create status');
  await page.getByRole('button',{name:'この申請を提出',exact:true}).waitFor();
  const id=new URL(page.url()).searchParams.get('request');require(id,'Created ID');observations.request_id=id;
  const submitted=page.waitForResponse(r=>r.request().method()==='POST'&&r.url()===origin+'/a/api/expense-requests/'+id+'/submit');
  await page.getByRole('button',{name:'この申請を提出',exact:true}).click();require((await submitted).status()===204,'Submit status');await page.getByTestId('status').filter({hasText:'SUBMITTED'}).waitFor();
  const ssr=await page.reload();require(ssr.headers()['cache-control'].includes('no-store'),'SSR no-store');
  await page.getByTestId('status').filter({hasText:'SUBMITTED'}).waitFor();
  const html=await page.content();require(!jwt.test(html)&&!html.includes(cookie.value),'HTML secret leak');
  const rsc=await context.request.get(page.url(),{headers:{RSC:'1'}});const rscText=await rsc.text();require(rsc.status()===200&&rsc.headers()['cache-control'].includes('no-store')&&!jwt.test(rscText)&&!rscText.includes(cookie.value),'RSC no-store/secret');
  observations.rsc_content_type=rsc.headers()['content-type'];
  const detail=await call('/a/api/expense-requests/'+id);require(detail.status()===200&&detail.headers()['cache-control'].includes('no-store'),'API no-store');
  let bundles=0;for(const src of await page.locator('script[src]').evaluateAll(s=>s.map(x=>x.src))) {
    const response=await context.request.get(src);require(!jwt.test(await response.text()),'Bundle secret leak');bundles++;
  }observations.checked_client_bundles=bundles;
  await page.screenshot({path:work+'/spa.png',fullPage:true});
  observations.checks.push('browser-create-detail-submit-reload','state-replay-CSRF-Origin-body-proxy-boundaries','HTML-RSC-API-no-store-and-no-token');
  csrf=await page.getByTestId('csrf').textContent();
  const badLogout=await call('/a/auth/logout','POST',{});require(badLogout.status()===403,'Logout CSRF');record('logout_missing_csrf',badLogout.status());
  await page.getByRole('button',{name:'ログアウト',exact:true}).click();await page.getByRole('button',{name:'ログイン',exact:true}).waitFor();
  require(!(await context.cookies()).some(c=>c.name==='D3_A_SESSION'),'Logout Cookie deleted');
  const denied=await call('/a/api/expense-requests/'+id);require(denied.status()===401,'Logout API denied');record('after_logout_api',denied.status());
  observations.checks.push('local-logout-cookie-and-session-API-denied');
  // New authentication for a separate, finite expiry fixture. No second business write.
  await page.getByRole('button',{name:'ログイン',exact:true}).click();
  await page.waitForURL(u=>u.origin===origin&&u.pathname==='/a'&&!u.search,{timeout:20000});
  await page.getByRole('button',{name:'下書きを保存',exact:true}).waitFor();
  const expiryCookie=(await context.cookies()).find(c=>c.name==='D3_A_SESSION');require(expiryCookie.value!==cookie.value,'Fresh Session after login');
  observations.expiry_session_id_hash=hash(expiryCookie.value);
  fs.writeFileSync(work+'/await-expiry.json',JSON.stringify({session_id_hash:observations.expiry_session_id_hash})+'\n');
  const deadline=Date.now()+30000;while(!fs.existsSync(work+'/expiry-go.json')) {if(Date.now()>deadline)throw new Error('Expiry coordination timeout');await new Promise(r=>setTimeout(r,200));}
  const expired=await call('/a/api/expense-requests/'+id);require(expired.status()===401,'Expired Session rejected');record('expired_session_api',expired.status());
  require(errors.length===0,'Browser page error');observations.checks.push('idle-expiry-real-DB-rejection');
  fs.writeFileSync(work+'/browser.json',JSON.stringify({browser:browser.version(),...observations},null,2)+'\n');
  console.log('S3_BROWSER_PASS');await context.close();
}catch{console.error('S3 browser check failed (provider URLs, inputs and credentials omitted)');process.exitCode=1;}
finally{if(browser)await browser.close();}
