/** One exchange attempt. A durable pending marker must precede this call. */
export type Tokens={access_token:string;refresh_token?:string;id_token?:string;expires_in?:number};
export function lifetimeValid(row:{idle_expires_at:Date;absolute_expires_at:Date},now=Date.now()) {
  return row.idle_expires_at.getTime()>now&&row.absolute_expires_at.getTime()>now;
}
export async function renew(current:Tokens,exchange:(token:string)=>Promise<Tokens>,commit:(tokens:Tokens)=>Promise<boolean>,invalidate:()=>Promise<void>) {
  try {
    if(!current.refresh_token)throw new Error('No refresh credential');
    const next=await exchange(current.refresh_token);
    if(!next.access_token||!next.refresh_token||next.refresh_token===current.refresh_token||!next.expires_in||next.expires_in<10)throw new Error('Invalid rotation');
    const tokens={...next,id_token:next.id_token??current.id_token};
    return await commit(tokens)?tokens:null;
  }catch{await invalidate();return null;}
}
