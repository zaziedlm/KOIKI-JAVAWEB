import {NextRequest,NextResponse} from 'next/server';
import {config,clientSecret,ORIGIN,rpAllowed,rpConfig,purpose} from '../../../../lib/config';
import {csrfFor,opaque} from '../../../../lib/crypto';
import {BoundaryError,jsonBody,validateMutation} from '../../../../lib/csrf';
import {begin,configure,finish,ISSUER,logoutUrl} from '../../../../lib/oidc';
import {consumeTransaction,createSession,removeSession,saveTransaction,session} from '../../../../lib/store';
export const dynamic='force-dynamic';
const safeHeaders={'Cache-Control':'private, no-store, max-age=0','Pragma':'no-cache'};
const reply=(body:unknown,status=200)=>NextResponse.json(body,{status,headers:safeHeaders});
type Params={params:Promise<{rp:string;action:string}>};
export async function GET(request:NextRequest,{params}:Params) {
  const {rp,action}=await params;if(!rpAllowed(rp))return reply({error:'not_found'},404);
  const c=rpConfig(rp);
  const home=()=>{const response=NextResponse.redirect(new URL('/'+rp,ORIGIN),303);response.headers.set('Cache-Control',safeHeaders['Cache-Control']);return response;};
  if(action==='preflight') {const response=home();response.cookies.set(c.flow,opaque(),{...c.options,maxAge:300});return response;}
  if(!['callback','logout-return'].includes(action))return reply({error:'not_found'},404);
  try {
    const logout=action==='logout-return',state=request.nextUrl.searchParams.get('state'),binding=request.cookies.get(logout?c.logout:c.flow)?.value;
    if(!state||!binding)return reply({error:'invalid_auth_transaction'},400);
    const expected=await consumeTransaction(binding,state,rp,logout?'logout':'auth');if(!expected)return reply({error:'invalid_auth_transaction'},400);
    if(logout){const response=home();response.cookies.set(c.logout,'',{...c.options,maxAge:0});return response;}
    const providerError=request.nextUrl.searchParams.get('error');
    if(providerError)return reply({error:['login_required','interaction_required','consent_required','account_selection_required'].includes(providerError)?providerError:'authentication_failed'},400);
    const cfg=await configure(c.clientId,clientSecret(rp));
    const callback=new URL(c.callback);callback.search=request.nextUrl.search;
    const tokens=await finish(cfg,callback,{state:expected.state,nonce:expected.nonce,verifier:expected.verifier}),claims=tokens.claims();
    if(!claims || claims.iss!==ISSUER || !claims.sub)return reply({error:'invalid_identity'},400);
    const id=await createSession({access_token:tokens.access_token,id_token:tokens.id_token,refresh_token:tokens.refresh_token,expires_in:tokens.expires_in},claims.iss,claims.sub,rp);
    const response=home();response.cookies.set(c.session,id,{...c.options,maxAge:3600});response.cookies.set(c.flow,'',{...c.options,maxAge:0});return response;
  }catch{return reply({error:'authentication_failed'},400);}
}
export async function POST(request:NextRequest,{params}:Params) {
  const {rp,action}=await params;if(!rpAllowed(rp)||!['start','silent','logout','sso-logout'].includes(action))return reply({error:'not_found'},404);
  const c=rpConfig(rp),login=action==='start'||action==='silent';
  try {
    const id=request.cookies.get(login?c.flow:c.session)?.value;
    if(!id)return reply({error:'authentication_required'},401);
    validateMutation(request.headers.get('origin'),request.headers.get('content-type'),request.headers.get('x-csrf-token'),csrfFor(config().key,purpose(login?'login':'session',rp),id));
    const body=await jsonBody(request);
    if(!body||typeof body!=='object'||Array.isArray(body)||Object.keys(body).length!==0)return reply({error:'invalid_body'},400);
    if(login) {
      const cfg=await configure(c.clientId,clientSecret(rp)),value=await begin(cfg,'openid expense.apply',action==='silent');
      await saveTransaction(id,{state:value.state,verifier:value.verifier,nonce:value.nonce},rp);return reply({redirect:value.url.href});
    }
    if(action==='sso-logout'&&!await session(id,rp))return reply({error:'authentication_required'},401);
    let target='/'+rp,logoutBinding;
    if(action==='sso-logout') {
      const cfg=await configure(c.clientId,clientSecret(rp)),state=opaque();logoutBinding=opaque();
      target=logoutUrl(cfg,state).href;await saveTransaction(logoutBinding,{state,kind:'logout'},rp);
    }
    const removed=await removeSession(id,rp);if(!removed)return reply({error:'authentication_required'},401);const response=reply({redirect:target});
    response.cookies.set(c.session,'',{...c.options,maxAge:0});response.cookies.set(c.flow,'',{...c.options,maxAge:0});
    if(logoutBinding)response.cookies.set(c.logout,logoutBinding,{...c.options,maxAge:300});return response;
  }catch(error){return reply({error:'request_rejected'},error instanceof BoundaryError?error.status:503);}
}
