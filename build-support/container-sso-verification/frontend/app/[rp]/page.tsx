import {cookies} from 'next/headers';
import {notFound,redirect} from 'next/navigation';
import {config,rpAllowed,rpConfig,purpose} from '../../lib/config';
import {csrfFor} from '../../lib/crypto';
import {session} from '../../lib/store';
import {api} from '../../lib/api-client';
import Panel from './panel';
export const dynamic='force-dynamic';
export const revalidate=0;
export default async function Page({params,searchParams}:{params:Promise<{rp:string}>;searchParams:Promise<{request?:string}>}) {
  const {rp}=await params;if(!rpAllowed(rp))notFound();const c=rpConfig(rp);
  const jar=await cookies(),current=await session(jar.get(c.session)?.value,rp);
  if(!current && !jar.get(c.flow))redirect('/'+rp+'/auth/preflight');
  let initialDetail=null;
  const id=(await searchParams).request;
  if(current && id){try{const r=await api(current.tokens.access_token,'detail',id);if(r.status===200)initialDetail={expenseRequestId:r.body.expenseRequestId,status:r.body.status,claimedAmount:r.body.claimedAmount,version:r.body.version};}catch{/* no provider data in errors */}}
  return <Panel rp={rp} csrf={current?.csrf??csrfFor(config().key,purpose('login',rp),jar.get(c.flow)!.value)} authenticated={Boolean(current)} subject={current?.subject??''} initialDetail={initialDetail}/>;
}
