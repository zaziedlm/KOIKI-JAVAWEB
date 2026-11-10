/** Finite S4 browser contrast. No trace/HAR/storageState/provider URL export. */
import fs from 'node:fs';
import {createHash,randomBytes} from 'node:crypto';
import {chromium} from './frontend/node_modules/playwright/index.mjs';
const work=process.argv[2],origin='http://localhost:13000';
const password=fs.readFileSync(work+'/secrets/login_password','utf8').trim();
const hash=v=>createHash('sha256').update(v).digest('hex'),random=()=>randomBytes(32).toString('base64url');
const obs={worker:1,max_concurrent_contexts:2,statuses:{},checks:[],cookies:[]};
class HarnessAssertion extends Error {}
function check(v,label){if(!v)throw new HarnessAssertion(label);}
function step(label){console.log(label);obs.checks.push(label);}
let browser;
try {
  browser=await chromium.launch({headless:true});const context=await browser.newContext(),page=await context.newPage();
  const cookie=async(name,ctx=context)=>(await ctx.cookies()).find(c=>c.name===name);
  const call=(path,method='GET',body,headers={},ctx=context)=>ctx.request.fetch(origin+path,{method,headers:{Origin:origin,'Content-Type':'application/json',...headers},data:body,maxRedirects:0,failOnStatusCode:false});
  const status=async(label,path,expected,method='GET',body,headers={},ctx=context)=>{const r=await call(path,method,body,headers,ctx);obs.statuses[label]=r.status();check(r.status()===expected,label);return r;};
  const csrf=async(p=page)=>p.getByTestId('csrf').textContent();
  const home=async(rp,p=page)=>{await p.goto(origin+'/'+rp);await p.getByRole('button',{name:'ログイン',exact:true}).waitFor();};
  const silent=async(rp,p=page)=>{await p.getByRole('button',{name:'SSOでログイン',exact:true}).click();await p.waitForURL(u=>u.origin===origin&&u.pathname==='/'+rp&&!u.search,{timeout:20000});await p.getByRole('button',{name:'ログアウト',exact:true}).waitFor();};
  const dummy='/api/expense-requests/d3200000-0000-4000-8000-000000000099';
  await home('a');await page.getByRole('button',{name:'ログイン',exact:true}).click();await page.locator('#username').fill('applicant-a');await page.locator('#password').fill(password);await page.locator('#kc-login').click();
  await page.waitForURL(u=>u.origin===origin&&u.pathname==='/a'&&!u.search,{timeout:20000});await page.getByRole('button',{name:'ログアウト',exact:true}).waitFor();
  const a=await cookie('D3_A_SESSION'),aCsrf=await csrf();check(a,'A Cookie');obs.a_first_session_hash=hash(a.value);step('A interactive login');
  await home('b');check(!await cookie('D3_B_SESSION'),'B must initially have no Session');check(await page.getByTestId('subject').textContent()==='','B anonymous SSR');
  await status('b_before_sso','/b'+dummy,401);
  await silent('b');const b=await cookie('D3_B_SESSION');check(b&&b.value!==a.value,'Independent B Cookie');check((await cookie('D3_A_SESSION')).value===a.value,'A unchanged');
  check(await page.getByTestId('subject').textContent()==='d3210000-0000-4000-8000-000000000001','B same IdP identity');
  obs.b_session_hash=hash(b.value);obs.same_idp_subject=true;obs.b_credentials_entered=0;
  for(const c of [a,b]){check(c.httpOnly&&!c.secure&&c.sameSite==='Lax'&&c.path===(c.name==='D3_A_SESSION'?'/a':'/b'),'Cookie attributes');obs.cookies.push({name:c.name,path:c.path,httpOnly:c.httpOnly,secure:c.secure,sameSite:c.sameSite});check(!await page.evaluate(name=>document.cookie.includes(name),c.name),'HttpOnly');}
  const html=await page.content();check(!html.includes(a.value)&&!html.includes(b.value)&&!/eyJ[\w-]+\.[\w-]+\.[\w-]+/.test(html),'HTML no secret');
  await context.addCookies([{...b,value:a.value}]);await status('a_cookie_as_b','/b'+dummy,401);await context.addCookies([b]);
  await status('b_own_cookie','/b'+dummy,404);
  await status('a_csrf_on_b','/b/auth/logout',403,'POST',{}, {'X-CSRF-Token':aCsrf});
  fs.writeFileSync(work+'/await-independent.json',JSON.stringify({a_hash:hash(a.value),b_hash:hash(b.value)})+'\n');
  const deadline=Date.now()+30000;while(!fs.existsSync(work+'/independent-go.json')){check(Date.now()<deadline,'DB coordination');await new Promise(r=>setTimeout(r,200));}
  step('A/B independent encrypted Sessions and Cookie confusion rejected');
  const cold=await browser.newContext(),coldPage=await cold.newPage();await home('b',coldPage);
  const coldDone=coldPage.waitForResponse(r=>new URL(r.url()).pathname==='/b/auth/callback');await coldPage.getByRole('button',{name:'SSOでログイン',exact:true}).click();
  const denied=await coldDone;check(denied.status()===400,'Cold silent rejected');obs.cold_error=(await denied.json()).error;check(obs.cold_error==='login_required','Cold login_required');check(!await cookie('D3_B_SESSION',cold),'Cold no B Session');
  await home('a',coldPage);const flow=await cookie('D3_A_FLOW',cold),coldCsrf=await csrf(coldPage);
  const pending=await call('/a/auth/start','POST',{}, {'X-CSRF-Token':coldCsrf},cold);check(pending.status()===200,'Pending A start');const pendingURL=new URL((await pending.json()).redirect),state=pendingURL.searchParams.get('state');
  obs.pending_a_state_hash=hash(state);await cold.addCookies([{...flow,name:'D3_B_FLOW',path:'/b'}]);
  await status('a_transaction_as_b','/b/auth/callback?state='+encodeURIComponent(state)+'&code=invalid',400,'GET',undefined,{},cold);
  check(!await cookie('D3_B_SESSION',cold),'Cross transaction no Session');await cold.close();step('Cold prompt none and cross client transaction rejection');
  await page.goto(origin+'/a');await page.getByRole('button',{name:'ログアウト',exact:true}).click();await page.getByRole('button',{name:'ログイン',exact:true}).waitFor();
  check(!await cookie('D3_A_SESSION'),'Local A Cookie deleted');check((await cookie('D3_B_SESSION')).value===b.value,'B survives A local logout');await status('a_after_local_logout','/a'+dummy,401);await status('b_after_a_local_logout','/b'+dummy,404);
  await silent('a');check((await cookie('D3_A_SESSION')).value!==a.value,'A fresh SSO Session');obs.local_logout={a_session_removed:true,b_session_preserved:true,idp_session_preserved:true};step('Local logout scope and A silent reauthentication');
  const token=await csrf();await status('unapproved_logout_return','/a/auth/sso-logout',400,'POST',{returnTo:'https://untrusted.example'},{'X-CSRF-Token':token});
  await status('sso_logout_missing_csrf','/a/auth/sso-logout',403,'POST',{});
  step('SSO logout input boundaries');
  const logout=await call('/a/auth/sso-logout','POST',{}, {'X-CSRF-Token':token});check(logout.status()===200,'SSO logout initiation');const end=new URL((await logout.json()).redirect),logoutCookie=await cookie('D3_A_LOGOUT');
  check(end.searchParams.get('post_logout_redirect_uri')===origin+'/a/auth/logout-return'&&!end.searchParams.has('id_token_hint'),'Fixed logout URL without JWT');
  const logoutState=end.searchParams.get('state'),returnURL=origin+'/a/auth/logout-return?state='+encodeURIComponent(logoutState);check(logoutCookie,'Logout binding Cookie');
  await status('logout_wrong_state','/a/auth/logout-return?state='+random(),400);
  await context.addCookies([{...logoutCookie,name:'D3_B_LOGOUT',path:'/b'}]);await status('logout_wrong_client','/b/auth/logout-return?state='+encodeURIComponent(logoutState),400);
  await context.addCookies([{...logoutCookie,name:'D3_A_FLOW'}]);await status('logout_state_as_auth','/a/auth/callback?state='+encodeURIComponent(logoutState)+'&code=invalid',400);
  const invalidEnd=new URL(end);invalidEnd.searchParams.set('post_logout_redirect_uri','https://untrusted.example');const invalid=await page.goto(invalidEnd.href);check(invalid.status()===400,'IdP rejects unregistered return');obs.statuses.idp_unregistered_return=invalid.status();
  step('SSO logout URL/state and fixed return boundaries');
  await page.goto(end.href);await page.locator('#kc-logout').waitFor();await page.locator('#kc-logout').click();
  await page.waitForURL(u=>u.origin===origin&&u.pathname==='/a'&&!u.search,{timeout:20000});await page.getByRole('button',{name:'ログイン',exact:true}).waitFor();
  check(!await cookie('D3_A_LOGOUT'),'Logout binding cleared');await context.addCookies([logoutCookie]);const replay=await context.request.get(returnURL,{maxRedirects:0});check(replay.status()===400,'Logout return replay');obs.statuses.logout_return_replay=replay.status();
  check((await cookie('D3_B_SESSION')).value===b.value,'Existing B Cookie after SSO logout');await status('b_jwt_after_sso_logout','/b'+dummy,404);
  const noSSO=page.waitForResponse(r=>new URL(r.url()).pathname==='/a/auth/callback');await page.getByRole('button',{name:'SSOでログイン',exact:true}).click();const noSession=await noSSO;check(noSession.status()===400&&(await noSession.json()).error==='login_required','New SSO must fail after logout');check(!await cookie('D3_A_SESSION'),'No A Session after logout');
  obs.sso_logout={a_session_removed:true,new_sso_error:'login_required',existing_b_session_preserved:true,existing_b_jwt_accepted_at_observation:true,backchannel_logout:false};step('SSO logout state replay and new silent SSO rejection; B existing JWT still accepted');
  fs.writeFileSync(work+'/browser.json',JSON.stringify({browser:browser.version(),...obs},null,2)+'\n');await context.close();console.log('S4_BROWSER_PASS');
}catch(error){obs.failure=error instanceof HarnessAssertion?error.message:'Browser operation failed after '+obs.checks.at(-1);fs.writeFileSync(work+'/browser.json',JSON.stringify(obs,null,2)+'\n');console.error('S4 browser check failed: '+obs.failure+'; credentials and provider URLs omitted');process.exitCode=1;}
finally{if(browser)await browser.close();}
