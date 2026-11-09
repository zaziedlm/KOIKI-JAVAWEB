import {createCipheriv,createDecipheriv,createHash,createHmac,randomBytes,timingSafeEqual} from 'node:crypto';
export const opaque=()=>randomBytes(32).toString('base64url');
export const hash=(value:string)=>createHash('sha256').update(value).digest('hex');
export function equal(a:string,b:string) {
  const x=Buffer.from(a),y=Buffer.from(b);
  return x.length===y.length && timingSafeEqual(x,y);
}
export function keyFrom(value:string) {
  const key=Buffer.from(value,'base64');
  if(key.length!==32 || key.toString('base64')!==value)throw new Error('Invalid encryption key');
  return key;
}
export const csrfFor=(key:Buffer,purpose:string,id:string)=>createHmac('sha256',key).update(purpose+':'+id).digest('base64url');
export function seal(key:Buffer,value:unknown,aad:string) {
  const iv=randomBytes(12),cipher=createCipheriv('aes-256-gcm',key,iv);
  cipher.setAAD(Buffer.from(aad));
  const payload=Buffer.concat([cipher.update(JSON.stringify(value),'utf8'),cipher.final()]);
  return {payload,iv,tag:cipher.getAuthTag()};
}
export function open(key:Buffer,row:{payload:Buffer;iv:Buffer;tag:Buffer},aad:string) {
  const cipher=createDecipheriv('aes-256-gcm',key,row.iv);cipher.setAAD(Buffer.from(aad));cipher.setAuthTag(row.tag);
  return JSON.parse(Buffer.concat([cipher.update(row.payload),cipher.final()]).toString('utf8'));
}
