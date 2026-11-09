/** Host-side single-worker Chromium. No trace/HAR/storageState or token output. */
import fs from 'node:fs';
import {chromium} from './frontend/node_modules/playwright/index.mjs';
const work = process.argv[2];
const password = fs.readFileSync(work + '/secrets/login_password', 'utf8').trim();
const fixtures = [['normal','applicant-a'],['second-user','applicant-b'],['disabled','disabled'],
  ['unprivileged','unprivileged'],['wrong-audience','applicant-a'],['no-scope','applicant-a']];
const observations=[];
let browser;
try {
  browser=await chromium.launch({headless:true});
  for (const [name,user] of fixtures) {
    const context=await browser.newContext();
    try {
      const page=await context.newPage();
      const sentCookieNames=new Set();
      const headersObserved=[];
      page.on('request',request=>{
        if(new URL(request.url()).hostname==='idp.localhost') {
          headersObserved.push(request.allHeaders().then(headers=>{
            for(const cookie of (headers.cookie||'').split(';')) {
              const name=cookie.trim().split('=')[0];
              if(name)sentCookieNames.add(name);
            }
          }));
        }
      });
      await page.goto('http://localhost:13000/begin/'+name,{timeout:20000});
      await page.locator('#username').fill(user);
      await page.locator('#password').fill(password);
      await page.locator('#kc-login').click();
      await page.waitForURL(url=>url.origin==='http://localhost:13000' && url.pathname==='/a/auth/callback',{timeout:20000});
      const content=JSON.parse(await page.locator('body').innerText());
      if(content.authenticated!==true)throw new Error('authentication_failed');
      const cookies=(await context.cookies()).filter(c=>c.domain==='idp.localhost').map(c=>({name:c.name,secure:c.secure,httpOnly:c.httpOnly,sameSite:c.sameSite,path:c.path}));
      await Promise.all(headersObserved);
      if(!sentCookieNames.has('AUTH_SESSION_ID'))throw new Error('IdP session cookie was not sent');
      observations.push({case:name,authenticated:true,keycloak_cookie_attributes:cookies,sent_cookie_names:[...sentCookieNames].sort()});
    } catch {
      throw new Error('Browser authentication failed for '+name+' (URL/credentials omitted)');
    } finally {await context.close();}
  }
  fs.writeFileSync(work+'/browser.json',JSON.stringify({browser:browser.version(),worker:1,max_concurrent_contexts:1,observations},null,2)+'\n');
  console.log('S2_BROWSER_PASS '+observations.length);
} catch(error) {
  console.error(error.message);
  process.exitCode=1;
} finally {if(browser)await browser.close();}
