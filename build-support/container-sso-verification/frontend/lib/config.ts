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
export function rpAllowed(rp:string){return rp==='a';}
