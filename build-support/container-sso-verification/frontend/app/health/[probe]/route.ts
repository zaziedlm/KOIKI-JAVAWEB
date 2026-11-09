import {NextResponse} from 'next/server';
import {config} from '../../../lib/config';
import {configure} from '../../../lib/oidc';
import {pool} from '../../../lib/store';
export const dynamic='force-dynamic';
export async function GET(_request:Request,{params}:{params:Promise<{probe:string}>}) {
  const {probe}=await params;
  if(!['live','ready'].includes(probe))return NextResponse.json({error:'not_found'},{status:404});
  let status='UP';
  if(probe==='ready')try {
    const cfg=await configure('koiki-d3-a',config().clientSecret);
    for(const url of ['http://backend:8080/actuator/health/readiness','http://keycloak:9000/health/ready',cfg.serverMetadata().jwks_uri!]) {
      const r=await fetch(url,{cache:'no-store',signal:AbortSignal.timeout(5000)});if(!r.ok)throw new Error();
    }
    await pool().query('SELECT 1');
  }catch{status='DOWN';}
  return NextResponse.json({status},{status:status==='UP'?200:503,headers:{'Cache-Control':'no-store'}});
}
