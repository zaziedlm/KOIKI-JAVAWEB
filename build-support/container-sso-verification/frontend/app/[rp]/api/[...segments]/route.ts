import {NextRequest,NextResponse} from 'next/server';
import {api,apiPath} from '../../../../lib/api-client';
import {config,rpAllowed,rpConfig,purpose} from '../../../../lib/config';
import {csrfFor} from '../../../../lib/crypto';
import {BoundaryError,jsonBody,validateMutation} from '../../../../lib/csrf';
import {session} from '../../../../lib/store';
export const dynamic='force-dynamic';
const reply=(body:unknown,status=200)=>new NextResponse(status===204?null:JSON.stringify(body),{status,headers:{'Content-Type':'application/json','Cache-Control':'private, no-store, max-age=0','Pragma':'no-cache'}});
type Params={params:Promise<{rp:string;segments:string[]}>};
async function handle(request:NextRequest,{params}:Params) {
  const {rp,segments}=await params;if(!rpAllowed(rp))return reply({error:'not_found'},404);
  let operation:string,id:string|undefined;
  if(request.method==='POST' && segments.length===1 && segments[0]==='expense-requests')operation='create';
  else if(request.method==='GET' && segments.length===2 && segments[0]==='expense-requests'){operation='detail';id=segments[1];}
  else if(request.method==='POST' && segments.length===3 && segments[0]==='expense-requests' && segments[2]==='submit'){operation='submit';id=segments[1];}
  else return reply({error:'not_found'},404);
  try{apiPath(operation,id);}catch{return reply({error:'not_found'},404);}
  try {
    const sid=request.cookies.get(rpConfig(rp).session)?.value;if(!sid)return reply({error:'authentication_required'},401);
    if(request.method==='POST')validateMutation(request.headers.get('origin'),request.headers.get('content-type'),request.headers.get('x-csrf-token'),csrfFor(config().key,purpose('session',rp),sid));
    const current=await session(sid,rp);if(!current)return reply({error:'authentication_required'},401);
    let body;
    if(request.method==='POST') {
      body=await jsonBody(request);
      const allowed=operation==='submit'?['expectedVersion']:['departmentId','claimedAmount','lines'];
      if(!body||typeof body!=='object'||Array.isArray(body)||Object.keys(body).some(k=>!allowed.includes(k)))return reply({error:'invalid_body'},400);
    }
    const response=await api(current.tokens.access_token,operation,id,body);
    return reply(response.body,response.status);
  }catch(error){return reply({error:'request_failed'},error instanceof BoundaryError?error.status:503);}
}
export const GET=handle;
export const POST=handle;
