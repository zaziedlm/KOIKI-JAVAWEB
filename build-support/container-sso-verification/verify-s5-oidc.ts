/** S5 exclusive transient helper: tokens in memory only, no business writes. */
import fs from 'node:fs';
import http from 'node:http';
import * as oidc from './frontend/node_modules/openid-client/build/index.js';
import {configure,begin,finish,ISSUER,CALLBACK} from './frontend/lib/oidc.ts';
const secret=fs.readFileSync('/run/secrets/client_a','utf8').trim();
const pending=new Map(),tokens=new Map();let rotationUsed=false,beforeUsed=false,afterUsed=false;
const json=(res:http.ServerResponse,status:number,body:unknown)=>{res.writeHead(status,{'Content-Type':'application/json','Cache-Control':'no-store'});res.end(JSON.stringify(body));};
let cached:Awaited<ReturnType<typeof configure>>|undefined;
const cfg=async()=>cached??=await configure('koiki-d3-a',secret);
const claims=(token:string)=>{const c=JSON.parse(Buffer.from(token.split('.')[1],'base64url').toString());return {iat:c.iat,exp:c.exp};};
const direct=async(token:string)=>(await fetch('http://backend:8080/api/v1/expense-requests/d3200000-0000-4000-8000-000000000099',{headers:{Authorization:'Bearer '+token},signal:AbortSignal.timeout(5000),redirect:'error'})).status;
const server=http.createServer(async(req,res)=>{
 try {
  const url=new URL(req.url!,CALLBACK);
  if(req.method==='GET'&&url.pathname==='/health'){const config=await cfg();const response=await fetch('http://keycloak:9000/health/ready',{signal:AbortSignal.timeout(5000)});return json(res,response.ok?200:503,{issuer:config.serverMetadata().issuer,status:response.ok?'UP':'DOWN'});}
  if(req.method==='GET'&&['/begin/expiry','/begin/rotation'].includes(url.pathname)){
   const name=url.pathname.split('/').at(-1);if(tokens.has(name))return json(res,400,{error:'already_used'});
   const request=await begin(await cfg(),'openid expense.apply');pending.set(request.state,{name,...request});res.writeHead(302,{Location:request.url.href,'Cache-Control':'no-store'});return res.end();
  }
  if(req.method==='GET'&&url.pathname==='/a/auth/callback'){
   const expected=pending.get(url.searchParams.get('state'));if(!expected)return json(res,400,{error:'invalid_state'});pending.delete(expected.state);
   const result=await finish(await cfg(),url,expected);tokens.set(expected.name,result);const clock=claims(result.access_token);
   return json(res,200,{case:expected.name,authenticated:true,...clock,target_epoch:clock.exp+65,now_epoch:Math.floor(Date.now()/1000),issuer:ISSUER});
  }
  if(req.method==='POST'&&url.pathname==='/rotation'&&!rotationUsed){rotationUsed=true;
   const old=tokens.get('rotation');if(!old)throw new Error();const next=await oidc.refreshTokenGrant(await cfg(),old.refresh_token);let refused=false;
   try{await oidc.refreshTokenGrant(await cfg(),old.refresh_token);}catch(error){refused=error instanceof oidc.ResponseBodyError&&error.error==='invalid_grant';}
   tokens.delete('rotation');return json(res,200,{rotated:next.refresh_token!==old.refresh_token,old_refresh_rejected:refused,old_token_exchange_attempts:2,purpose:'dedicated isolated session only'});
  }
  if(req.method==='POST'&&url.pathname==='/expiry-before'&&!beforeUsed){beforeUsed=true;return json(res,200,{status:await direct(tokens.get('expiry').access_token)});}
  if(req.method==='POST'&&url.pathname==='/expiry-after'&&!afterUsed){afterUsed=true;const token=tokens.get('expiry'),clock=claims(token.access_token),now=Math.floor(Date.now()/1000);if(now<clock.exp+65)return json(res,425,{error:'too_early'});return json(res,200,{status:await direct(token.access_token),...clock,observed_epoch:now,skew_seconds:60,margin_seconds:5,refreshes:0});}
  if(req.method==='POST'&&url.pathname==='/forget'){tokens.clear();pending.clear();cached=undefined;return json(res,200,{tokens_retained:0,pending_retained:0});}
  return json(res,404,{error:'not_found'});
 }catch{return json(res,503,{error:'helper_failed'});}
});
server.listen(13000,'0.0.0.0');process.on('SIGTERM',()=>{tokens.clear();pending.clear();server.close(()=>process.exit(0));});
