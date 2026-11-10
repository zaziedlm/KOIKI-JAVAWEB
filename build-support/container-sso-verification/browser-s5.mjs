/** S5 real Chromium, finite host/runner rendezvous; secret values never leave memory. */
import fs from 'node:fs';
import {createHash} from 'node:crypto';
import {chromium} from './frontend/node_modules/playwright/index.mjs';
import {selectReplayCookies} from './cookie-replay.mjs';
const work=process.argv[2],origin='http://localhost:13000',password=fs.readFileSync(work+'/secrets/login_password','utf8').trim();
const hash=v=>createHash('sha256').update(v).digest('hex'),obs={worker:1,max_concurrent_contexts:3,statuses:{},checks:[]};
const jwt=/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/;
class HarnessAssertion extends Error{}
const check=(v,label)=>{if(!v)throw new HarnessAssertion(label);};let browser,stage='startup';
const subject='d3210000-0000-4000-8000-000000000001',otherSubject='d3210000-0000-4000-8000-000000000002';
async function point(name,data={}){stage=name;fs.writeFileSync(work+'/'+name+'.json',JSON.stringify(data)+'\n');const deadline=Date.now()+420000;let released=false;while(!fs.existsSync(work+'/'+name+'-go.json')){check(Date.now()<deadline,'Runner rendezvous timeout');
 if(process.env.D3_STOP_DIAGNOSTIC==='1'&&name==='restart-ready'&&!released&&fs.existsSync(work+'/release-api-go.json')){
  const contexts=browser.contexts(),before=await Promise.all(contexts.map(ctx=>ctx.cookies()));
  await Promise.all(contexts.map(ctx=>ctx.request.dispose()));
  check((await Promise.all(contexts.map(ctx=>ctx.cookies()))).every((values,index)=>JSON.stringify(values)===JSON.stringify(before[index])),'Diagnostic API dispose retains Browser cookies');
  released=true;fs.writeFileSync(work+'/api-connections-released.json',JSON.stringify({contexts:contexts.length,context_cookies_unchanged:true})+'\n');
 }
 await new Promise(r=>setTimeout(r,200));}console.log('RESUME '+name);}
const call=(ctx,path,method='GET',body,csrf)=>ctx.request.fetch(origin+path,{method,headers:{Origin:origin,'Content-Type':'application/json',...(process.env.D3_STOP_DIAGNOSTIC==='1'?{'X-D3-Diagnostic-Client':'api'}:{}),...(csrf?{'X-CSRF-Token':csrf}:{})},data:body,maxRedirects:0,failOnStatusCode:false,timeout:30000});
const cookie=async ctx=>(await ctx.cookies()).find(c=>c.name==='D3_A_SESSION');
const csrf=async page=>page.getByTestId('csrf').textContent();
const identity=async(page,wanted)=>check(await page.getByTestId('subject').textContent()===wanted,'SSR user identity');
async function login(page,user,wanted){await page.goto(origin+'/a');const logged=page.getByRole('button',{name:'ログアウト',exact:true});if(!await logged.isVisible()){await page.getByRole('button',{name:'ログイン',exact:true}).click();await Promise.race([page.locator('#username').waitFor(),logged.waitFor()]);if(await page.locator('#username').isVisible()){await page.locator('#username').fill(user);await page.locator('#password').fill(password);await page.locator('#kc-login').click();}await logged.waitFor();}await identity(page,wanted);}
async function create(page,amount,purpose){await page.goto(origin+'/a');await page.getByLabel('金額（円）').fill(String(amount));await page.getByLabel('内容',{exact:true}).fill('S5 browser fixture');await page.getByLabel('目的',{exact:true}).fill(purpose);const done=page.waitForResponse(r=>r.request().method()==='POST'&&r.url()===origin+'/a/api/expense-requests');await page.getByRole('button',{name:'下書きを保存',exact:true}).click();const response=await done;check(response.status()===201,'Browser create status');const id=(await response.json()).expenseRequestId;await page.getByRole('button',{name:'この申請を提出',exact:true}).waitFor();return id;}
try{
 browser=await chromium.launch({headless:true});
 if(process.env.D3_STOP_DIAGNOSTIC==='1'){
  const originalContext=browser.newContext.bind(browser);
  browser.newContext=options=>originalContext({...options,extraHTTPHeaders:{...options?.extraHTTPHeaders,'X-D3-Diagnostic-Client':'page'}});
 }
 let a=await browser.newContext(),b=await browser.newContext(),page=await a.newPage(),other=await b.newPage();
 await login(page,'applicant-a',subject);obs.fixture_request_id=await create(page,1200,'S5 persistence witness');const fixture='/a/api/expense-requests/'+obs.fixture_request_id;
 const initial=await page.reload();check(initial.headers()['cache-control'].includes('no-store'),'A SSR no-store');
 await login(other,'applicant-b',otherSubject);check(!await other.content().then(s=>s.includes(obs.fixture_request_id)),'Other SSR contains no A request');
 const sidA=await cookie(a),sidB=await cookie(b);check(sidA&&sidB&&sidA.value!==sidB.value,'Separate user Session Cookies');obs.a_hash=hash(sidA.value);obs.b_hash=hash(sidB.value);
 for(const [ctx,p,wanted] of [[a,page,subject],[b,other,otherSubject]]){const html=await p.content();check(!jwt.test(html)&&!html.includes(sidA.value)&&!html.includes(sidB.value),'SSR no token/Session leak');const response=await ctx.request.get(p.url(),{headers:{RSC:'1',...(process.env.D3_STOP_DIAGNOSTIC==='1'?{'X-D3-Diagnostic-Client':'api-rsc'}:{})}});const text=await response.text();check(response.status()===200&&response.headers()['cache-control'].includes('no-store')&&response.headers()['content-type'].includes('text/x-component'),'RSC no-store');check(text.includes(wanted)&&!text.includes(wanted===subject?otherSubject:subject)&&!jwt.test(text)&&!text.includes(sidA.value)&&!text.includes(sidB.value),'RSC user isolation');}
 const own=await call(a,fixture),denied=await call(b,fixture);check(own.status()===200&&denied.status()===404&&denied.headers()['cache-control'].includes('no-store'),'API owner isolation');obs.statuses.own_detail=own.status();obs.statuses.other_detail=denied.status();
 await point('parallel-ready',{a_hash:obs.a_hash,b_hash:obs.b_hash});
 const parallel=await Promise.all([call(a,fixture),call(a,fixture)]);obs.parallel_statuses=parallel.map(r=>r.status());check(obs.parallel_statuses.every(s=>s===200),'Parallel requests after real refresh');await point('parallel-done');obs.checks.push('two users SSR/RSC/API/cache and real parallel refresh');
 // Third independent Session for refresh/logout competition; main business witness is preserved.
 const race=await browser.newContext(),racePage=await race.newPage();await login(racePage,'applicant-a',subject);const raceCookie=await cookie(race),raceCsrf=await csrf(racePage);obs.race_hash=hash(raceCookie.value);
 await point('race-ready',{session_hash:obs.race_hash});const pending=call(race,fixture);await point('race-started');
 const logout=await call(race,'/a/auth/logout','POST',{},raceCsrf);check(logout.status()===200,'Logout while refresh pending');await point('race-logout-done');const raceResult=await pending;check(raceResult.status()===401,'Refresh cannot resurrect logout');obs.statuses.refresh_logout_race=raceResult.status();check(!await cookie(race),'Race Cookie deleted');await race.close();await point('race-done');
 await point('idp-stop-ready');
 const live=await call(a,'/health/live'),ready=await call(a,'/health/ready');check(live.status()===200&&ready.status()===503,'IdP failure liveness/readiness split');
 const existing=await call(b,'/a/api/expense-requests/d3200000-0000-4000-8000-000000000099');check(existing.status()===404,'Existing JWT route during IdP outage');obs.statuses.idp_down_existing_jwt=existing.status();
 const expired=await call(a,fixture);check(expired.status()===401,'Refresh fails closed during IdP outage');obs.statuses.idp_down_refresh=expired.status();
 const cold=await browser.newContext(),coldPage=await cold.newPage();await coldPage.goto(origin+'/a');const start=await call(cold,'/a/auth/start','POST',{},await csrf(coldPage));check(start.status()===503,'Login dependency fails during IdP outage');obs.statuses.idp_down_login=start.status();await cold.close();await point('idp-stop-done');
 await login(page,'applicant-a',subject);obs.a_recovered_hash=hash((await cookie(a)).value);await point('backend-stop-ready');
 const payload={departmentId:'d3200000-0000-4000-8000-000000000021',claimedAmount:999,lines:[{expenseCategoryId:'d3200000-0000-4000-8000-000000000022',usageDate:new Date().toISOString().slice(0,10),description:'Must not be retried',purpose:'S5 backend unavailable negative',amount:999}]};
 const failedPost=await call(a,'/a/api/expense-requests','POST',payload,await csrf(page));check([503,504].includes(failedPost.status()),'Backend outage bounded failure');obs.statuses.backend_down_post=failedPost.status();obs.backend_down_business_post_attempts=1;await point('backend-stop-done');
 await point('db-stop-ready');const dbReady=await call(a,'/health/ready'),dbLive=await call(a,'/health/live'),dbApi=await call(a,fixture);check(dbReady.status()===503&&dbLive.status()===200&&dbApi.status()===503,'DB failure liveness/readiness/API');obs.statuses.db_down_ready=dbReady.status();obs.statuses.db_down_live=dbLive.status();obs.statuses.db_down_api=dbApi.status();await point('db-stop-done');
 stage='DB recovery owner API';const recovered=await call(a,fixture);obs.statuses.db_recovered_owner_api=recovered.status();check(recovered.status()===200,'DB recovered owner API status');
 stage='DB recovery SSR';await page.goto(origin+'/a?request='+obs.fixture_request_id);await page.getByRole('button',{name:'ログアウト',exact:true}).waitFor();await identity(page,subject);check(await page.getByTestId('status').textContent()==='DRAFT','Recovery retained business state');
 stage='new independent pending witness login after DB recovery';let uncertain=await browser.newContext(),uncertainPage=await uncertain.newPage();await login(uncertainPage,'applicant-a',subject);obs.pending_recovery_hash=hash((await cookie(uncertain)).value);
 // Quiescent restart, not a drain test. Cookie values never leave this process.
 stage='restart browser quiescence';const restartContexts=[a,b,uncertain],restartPages=[page,other,uncertainPage];
 const cookieCapturedAt=Date.now()/1000;
 const cookiesBefore=await Promise.all(restartContexts.map(ctx=>ctx.cookies()));
 const cookieSignature=values=>hash(JSON.stringify(values.map(({name,value,domain,path,expires,httpOnly,secure,sameSite,partitionKey})=>({name,value,domain,path,expires,httpOnly,secure,sameSite,partitionKey})).sort((left,right)=>JSON.stringify(left).localeCompare(JSON.stringify(right)))));
 const restartIdentity={a_hash:hash((await cookie(a)).value),b_hash:hash((await cookie(b)).value),pending_hash:obs.pending_recovery_hash};
 if(process.env.D3_STOP_DIAGNOSTIC==='1'){
  // Keep the original blank-page condition for the separate diagnostic control.
  await Promise.all(restartPages.map(p=>p.goto('about:blank')));
  await Promise.all(restartPages.map(p=>p.waitForLoadState('networkidle',{timeout:10000})));
  check(restartPages.every(p=>p.url()==='about:blank'),'All restart pages blank');
  const cookiesAfter=await Promise.all(restartContexts.map(ctx=>ctx.cookies()));
  check(cookiesBefore.every((values,index)=>cookieSignature(values)===cookieSignature(cookiesAfter[index])),'Restart context cookies retained');
  obs.restart_quiescence={pages:restartPages.length,all_about_blank:true,networkidle:true,context_cookies_unchanged:true};
 }else{
  const closing=browser;await closing.close();check(!closing.isConnected(),'Browser disconnected before SIGTERM');browser=undefined;
  obs.restart_quiescence={contexts:3,browser_closed:true,cookies_memory_only:true,cookie_file_written:false};
 }
 await point('restart-ready',{...restartIdentity,quiescence:obs.restart_quiescence});
 if(process.env.D3_STOP_DIAGNOSTIC!=='1'){
  browser=await chromium.launch({headless:true});a=await browser.newContext();b=await browser.newContext();uncertain=await browser.newContext();
  const cookieInjectionAt=Date.now()/1000;
  const selections=cookiesBefore.map(values=>selectReplayCookies(values,cookieInjectionAt));
  const restored=[a,b,uncertain];await Promise.all(restored.map((ctx,index)=>ctx.addCookies(selections[index].expected)));
  const restoredCookies=await Promise.all(restored.map(ctx=>ctx.cookies()));
  const cookieObservedAt=Date.now()/1000;
  const expiryState=(c,time)=>c.expires===-1?'session':Number.isFinite(c.expires)?(c.expires<=time?'expired':'valid'):'unknown';
  const expiryBucket=(c,time)=>c.expires===-1?'session':!Number.isFinite(c.expires)?'unknown':c.expires<=time?'expired':c.expires-time<=5?'0-5s':c.expires-time<=30?'5-30s':c.expires-time<=120?'30-120s':'>120s';
  const cookieRole=c=>/^D3_[AB]_SESSION$/.test(c.name)?'BFF_SESSION':/^D3_[AB]_FLOW$/.test(c.name)?'BFF_AUTH_FLOW':/^D3_[AB]_LOGOUT$/.test(c.name)?'BFF_LOGOUT':c.domain.replace(/^\./,'')==='idp.localhost'?({AUTH_SESSION_ID:'IDP_AUTH_SESSION',KC_AUTH_SESSION_HASH:'IDP_AUTH_SESSION_HASH',KC_RESTART:'IDP_AUTH_RESTART',KEYCLOAK_IDENTITY:'IDP_IDENTITY',KEYCLOAK_SESSION:'IDP_SSO_SESSION'}[c.name]??'IDP_OTHER'):'OTHER';
  obs.restart_cookie_selection=selections.map((selection,index)=>({context:index,captured_count:cookiesBefore[index].length,expected_count:selection.expected.length,
   excluded_expired_auth_hash:selection.expiredAuxiliary.length,
   excluded_not_restored:selection.expiredAuxiliary.every(expired=>!restoredCookies[index].some(actual=>actual.name===expired.name&&actual.domain===expired.domain&&actual.path===expired.path))}));
  // Preserve only counts and changed field names if the strict witness fails.
  // Cookie names, scopes, values and timestamps stay in this process.
  obs.restart_cookie_comparison=restoredCookies.map((values,index)=>{
   const expected=cookiesBefore[index],key=c=>JSON.stringify([c.name,c.domain,c.path]);
   const missing=expected.filter(c=>!values.some(value=>key(value)===key(c)));
   const unexpected=values.filter(c=>!expected.some(value=>key(value)===key(c)));
   const changed=new Set();
   for(const before of expected){const after=values.find(c=>key(c)===key(before));if(after)for(const field of ['value','expires','httpOnly','secure','sameSite','partitionKey'])if(before[field]!==after[field])changed.add(field);}
   return {context:index,expected_count:expected.length,actual_count:values.length,missing_count:missing.length,unexpected_count:unexpected.length,changed_fields:[...changed].sort(),
    missing:missing.map(c=>({role:cookieRole(c),at_capture:expiryState(c,cookieCapturedAt),remaining_at_capture:expiryBucket(c,cookieCapturedAt),before_injection:expiryState(c,cookieInjectionAt),after_read:expiryState(c,cookieObservedAt),http_only:c.httpOnly,secure:c.secure,same_site:c.sameSite}))};
  });
  obs.restart_required_bff_sessions=restoredCookies.map((values,index)=>{
   const expected=cookiesBefore[index].find(c=>c.name==='D3_A_SESSION');
   const actual=expected&&values.find(c=>c.name===expected.name&&c.domain===expected.domain&&c.path===expected.path);
   const wanted=[restartIdentity.a_hash,restartIdentity.b_hash,restartIdentity.pending_hash][index];
   return {context:index,present:Boolean(actual),same_session_id:Boolean(actual)&&hash(actual.value)===wanted,
    all_attributes_match:Boolean(actual)&&cookieSignature([expected])===cookieSignature([actual]),valid_at_restore:Boolean(actual)&&expiryState(actual,cookieObservedAt)!=='expired'};
  });
  check(obs.restart_cookie_selection.every(value=>value.excluded_not_restored),'Expired auxiliary Cookie must not be revived');
  check(restoredCookies.every((values,index)=>cookieSignature(values)===cookieSignature(selections[index].expected)),'Retained cookie values and attributes match');
  check(obs.restart_required_bff_sessions.every(value=>value.present&&value.same_session_id&&value.all_attributes_match&&value.valid_at_restore),'All required BFF Sessions retained exactly');
  check(hash((await cookie(a)).value)===restartIdentity.a_hash&&hash((await cookie(b)).value)===restartIdentity.b_hash&&hash((await cookie(uncertain)).value)===restartIdentity.pending_hash,'Restored same BFF Session IDs');
  for(const values of [...cookiesBefore,...restoredCookies,...selections.flatMap(selection=>[selection.expected,selection.expiredAuxiliary])])values.length=0;
  page=await a.newPage();other=await b.newPage();uncertainPage=await uncertain.newPage();
  obs.restart_browser_restore={new_browser:true,contexts:3,cookie_values_and_attributes_match:true,same_bff_session_ids:true,cookie_file_written:false,expired_auth_hash_not_revived:true};
  await point('browser-restored',obs.restart_browser_restore);
 }
 const invalidPending=await call(uncertain,fixture);check(invalidPending.status()===401,'Durable pending marker requires reauthentication after restart');obs.statuses.pending_after_restart=invalidPending.status();await uncertain.close();await point('pending-recovery-done');
 await page.goto(origin+'/a?request='+obs.fixture_request_id);await page.getByTestId('status').filter({hasText:'DRAFT'}).waitFor();await identity(page,subject);await other.goto(origin+'/a');await identity(other,otherSubject);obs.checks.push('same volume/key restart preserves business/Audit/Session/realm and decryption');
 await point('restart-done');obs.smoke_request_id=await create(page,2300,'S5 final smoke separate request');
 const submitted=page.waitForResponse(r=>r.request().method()==='POST'&&r.url()===origin+'/a/api/expense-requests/'+obs.smoke_request_id+'/submit');await page.getByRole('button',{name:'この申請を提出',exact:true}).click();check((await submitted).status()===204,'Final submit');await page.getByTestId('status').filter({hasText:'SUBMITTED'}).waitFor();const ssr=await page.reload();check(ssr.headers()['cache-control'].includes('no-store'),'Final SSR no-store');
 await page.goto(origin+'/b');await page.getByRole('button',{name:'SSOでログイン',exact:true}).waitFor();check(!(await a.cookies()).some(c=>c.name==='D3_B_SESSION'),'Final B initially anonymous');await page.getByRole('button',{name:'SSOでログイン',exact:true}).click();await page.getByRole('button',{name:'ログアウト',exact:true}).waitFor();await identity(page,subject);const rpB=(await a.cookies()).find(c=>c.name==='D3_B_SESSION');check(rpB&&rpB.value!==(await cookie(a)).value,'Final independent B Session');obs.smoke_b_hash=hash(rpB.value);
 let bundles=0;for(const src of await page.locator('script[src]').evaluateAll(s=>s.map(x=>x.src))){const response=await a.request.get(src);check(!jwt.test(await response.text()),'Client bundle JWT exposure');bundles++;}obs.checked_client_bundles=bundles;check(bundles>0,'Client bundles observed');
 stage='SSO logout and remaining B refresh';await page.goto(origin+'/a');const ssoLogout=await call(a,'/a/auth/sso-logout','POST',{},await csrf(page));check(ssoLogout.status()===200,'SSO logout for remaining B refresh');const end=new URL((await ssoLogout.json()).redirect);check(!end.searchParams.has('id_token_hint'),'SSO logout without browser token');await page.goto(end.href);await page.locator('#kc-logout').click();await page.getByRole('button',{name:'ログイン',exact:true}).waitFor();
 const remaining=await call(a,'/b/api/expense-requests/'+obs.smoke_request_id);check(remaining.status()===200,'Existing B JWT after SSO logout');obs.statuses.sso_logout_existing_b_jwt=remaining.status();
 await point('sso-refresh-ready',{b_hash:obs.smoke_b_hash});const deniedRefresh=await call(a,'/b/api/expense-requests/'+obs.smoke_request_id);check(deniedRefresh.status()===401,'B refresh rejected after IdP logout');obs.statuses.sso_logout_b_refresh=deniedRefresh.status();
 await page.goto(origin+'/b');const deniedSSO=page.waitForResponse(r=>new URL(r.url()).pathname==='/b/auth/callback');await page.getByRole('button',{name:'SSOでログイン',exact:true}).click();const noSession=await deniedSSO;check(noSession.status()===400&&(await noSession.json()).error==='login_required','B silent login after IdP logout');obs.statuses.sso_logout_b_silent=400;await point('sso-refresh-done');
 obs.checks.push('final artifact browser create/detail/submit and A to B SSO');fs.writeFileSync(work+'/browser.json',JSON.stringify({browser:browser.version(),...obs},null,2)+'\n');await a.close();await b.close();console.log('S5_BROWSER_PASS');
}catch(error){obs.failure=error instanceof HarnessAssertion?error.message:'Browser operation failed at '+stage;fs.writeFileSync(work+'/browser.json',JSON.stringify(obs,null,2)+'\n');console.error('S5 browser failed: '+obs.failure+'; credentials/provider URLs omitted');process.exitCode=1;}
finally{if(browser)await browser.close();}
