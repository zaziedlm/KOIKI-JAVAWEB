import {NextRequest,NextResponse} from 'next/server';
import {config,cookieOptions,FLOW,ORIGIN,rpAllowed,SESSION} from '../../../../lib/config';
import {csrfFor,opaque} from '../../../../lib/crypto';
import {BoundaryError,jsonBody,validateMutation} from '../../../../lib/csrf';
import {begin,CALLBACK,configure,finish,ISSUER} from '../../../../lib/oidc';
import {consumeTransaction,createSession,removeSession,saveTransaction,session} from '../../../../lib/store';
export const dynamic='force-dynamic';
const safeHeaders={'Cache-Control':'private, no-store, max-age=0','Pragma':'no-cache'};
const reply=(body:unknown,status=200)=>NextResponse.json(body,{status,headers:safeHeaders});
type Params={params:Promise<{rp:string;action:string}>};
export async function GET(request:NextRequest,{params}:Params) {
  const {rp,action}=await params;if(!rpAllowed(rp))return reply({error:'not_found'},404);
  if(action==='preflight') {
    const response=NextResponse.redirect(new URL('/a',ORIGIN),303);
    response.headers.set('Cache-Control',safeHeaders['Cache-Control']);
    response.cookies.set(FLOW,opaque(),{...cookieOptions,maxAge:300});return response;
  }
  if(action!=='callback')return reply({error:'not_found'},404);
  try {
    const state=request.nextUrl.searchParams.get('state'),binding=request.cookies.get(FLOW)?.value;
    if(!state||!binding)return reply({error:'invalid_auth_transaction'},400);
    const expected=await consumeTransaction(binding,state);if(!expected)return reply({error:'invalid_auth_transaction'},400);
    const cfg=await configure('koiki-d3-a',config().clientSecret);
    const callback=new URL(CALLBACK);callback.search=request.nextUrl.search;
    const tokens=await finish(cfg,callback,expected),claims=tokens.claims();
    if(!claims || claims.iss!==ISSUER || !claims.sub)return reply({error:'invalid_identity'},400);
    const id=await createSession({access_token:tokens.access_token,id_token:tokens.id_token,refresh_token:tokens.refresh_token,expires_in:tokens.expires_in},claims.iss,claims.sub);
    const response=NextResponse.redirect(new URL('/a',ORIGIN),303);
    response.headers.set('Cache-Control',safeHeaders['Cache-Control']);
    response.cookies.set(SESSION,id,{...cookieOptions,maxAge:3600});
    response.cookies.set(FLOW,'',{...cookieOptions,maxAge:0});return response;
  }catch{return reply({error:'authentication_failed'},400);}
}
export async function POST(request:NextRequest,{params}:Params) {
  const {rp,action}=await params;if(!rpAllowed(rp)||!['start','logout'].includes(action))return reply({error:'not_found'},404);
  try {
    const id=request.cookies.get(action==='start'?FLOW:SESSION)?.value;
    if(!id)return reply({error:'authentication_required'},401);
    validateMutation(request.headers.get('origin'),request.headers.get('content-type'),request.headers.get('x-csrf-token'),csrfFor(config().key,action==='start'?'login':'session',id));
    const body=await jsonBody(request);
    if(!body||typeof body!=='object'||Array.isArray(body)||Object.keys(body).length!==0)return reply({error:'invalid_body'},400);
    if(action==='start') {
      const cfg=await configure('koiki-d3-a',config().clientSecret),value=await begin(cfg,'openid expense.apply');
      await saveTransaction(id,{state:value.state,verifier:value.verifier,nonce:value.nonce});
      return reply({redirect:value.url.href});
    }
    if(!await session(id))return reply({error:'authentication_required'},401);
    await removeSession(id);const response=reply({redirect:'/a'});
    response.cookies.set(SESSION,'',{...cookieOptions,maxAge:0});response.cookies.set(FLOW,'',{...cookieOptions,maxAge:0});return response;
  }catch(error){return reply({error:'request_rejected'},error instanceof BoundaryError?error.status:503);}
}
