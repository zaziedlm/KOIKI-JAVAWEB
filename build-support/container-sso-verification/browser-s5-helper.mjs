import fs from 'node:fs';
import {chromium} from './frontend/node_modules/playwright/index.mjs';
const work=process.argv[2],origin='http://localhost:13000',password=fs.readFileSync(work+'/secrets/login_password','utf8').trim();
const check=(value,label)=>{if(!value)throw new Error(label);};let browser;
try{
 browser=await chromium.launch({headless:true});const observations={browser:browser.version(),worker:1,max_concurrent_contexts:1};let expiry;
 for(const name of ['expiry','rotation']){const context=await browser.newContext(),page=await context.newPage();await page.goto(origin+'/begin/'+name);await page.locator('#username').fill('applicant-a');await page.locator('#password').fill(password);await page.locator('#kc-login').click();await page.waitForURL(u=>u.origin===origin&&u.pathname==='/a/auth/callback');const result=JSON.parse(await page.locator('body').textContent());check(result.authenticated&&result.exp-result.iat===60,'Real token lifetime');if(name==='expiry')expiry=result;await context.close();}
 const context=await browser.newContext();const post=async path=>{const r=await context.request.post(origin+path);check(r.status()===200,'Helper response');return r.json();};
 observations.rotation=await post('/rotation');check(observations.rotation.rotated&&observations.rotation.old_refresh_rejected,'Rotation rejection');
 observations.before=await post('/expiry-before');check(observations.before.status===404,'Unexpired direct JWT');
 console.log('JWT expiry wait started; target derived from exp plus effective 60-second skew and 5-second margin');
 while(Date.now()<expiry.target_epoch*1000+250)await new Promise(resolve=>setTimeout(resolve,Math.min(1000,expiry.target_epoch*1000+250-Date.now())));
 observations.after=await post('/expiry-after');check(observations.after.status===401&&observations.after.refreshes===0,'Expired direct JWT');observations.clock=expiry;observations.forgotten=await post('/forget');
 fs.writeFileSync(work+'/helper.json',JSON.stringify(observations,null,2)+'\n');console.log('S5_HELPER_PASS');await context.close();
}catch{console.error('S5 helper browser failed; all provider URLs and inputs omitted');process.exitCode=1;}
finally{if(browser)await browser.close();}
