import fs from 'node:fs';
import {keyFrom} from './crypto.ts';
export const ORIGIN='http://localhost:13000';
export const SESSION='D3_A_SESSION';
export const FLOW='D3_A_FLOW';
export const cookieOptions={httpOnly:true,sameSite:'lax' as const,secure:false,path:'/a'};
let cached:ReturnType<typeof read>|undefined;
function read() {
  if(process.env.APP_ORIGIN!==ORIGIN || process.env.BACKEND_URL!=='http://backend:8080')throw new Error('Invalid fixed runtime origin');
  const key=keyFrom(fs.readFileSync('/run/secrets/bff_key','utf8').trim());
  return {key,keyId:'d3-s3-key-1',clientSecret:fs.readFileSync('/run/secrets/client_a','utf8').trim(),
    databasePassword:fs.readFileSync('/run/secrets/koiki_bff','utf8').trim()};
}
export function config(){return cached??=read();}
export function rpAllowed(rp:string){return rp==='a'||(rp==='b'&&process.env.BFF_RPS==='a,b');}
export function rpConfig(rp:string) {
  if(!rpAllowed(rp))throw new Error('Unapproved RP');
  return {clientId:'koiki-d3-'+rp,session:'D3_'+rp.toUpperCase()+'_SESSION',flow:'D3_'+rp.toUpperCase()+'_FLOW',
    logout:'D3_'+rp.toUpperCase()+'_LOGOUT',options:{...cookieOptions,path:'/'+rp},
    callback:ORIGIN+'/'+rp+'/auth/callback',postLogout:ORIGIN+'/'+rp+'/auth/logout-return'};
}
export function clientSecret(rp:string){rpConfig(rp);return fs.readFileSync('/run/secrets/client_'+rp,'utf8').trim();}
export function purpose(kind:string,rp:string){rpConfig(rp);return rp==='a'?kind:kind+':'+rp;}
