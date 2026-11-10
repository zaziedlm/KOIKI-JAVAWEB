import {equal} from './crypto.ts';
export const ORIGIN='http://localhost:13000';
export class BoundaryError extends Error {status:number;constructor(status:number){super('Request rejected');this.status=status;}}
export function validateMutation(origin:string|null,contentType:string|null,given:string|null,expected:string) {
  if(origin!==ORIGIN || !given || !equal(given,expected))throw new BoundaryError(403);
  if(contentType?.split(';')[0].trim().toLowerCase()!=='application/json')throw new BoundaryError(415);
}
export async function jsonBody(request:Request) {
  const reader=request.body?.getReader();if(!reader)throw new BoundaryError(400);
  const chunks:Uint8Array[]=[];let total=0;
  for(;;){const {done,value}=await reader.read();if(done)break;total+=value.length;
    if(total>65536){await reader.cancel();throw new BoundaryError(413);}chunks.push(value);}
  try{return JSON.parse(Buffer.concat(chunks).toString('utf8'));}catch{throw new BoundaryError(400);}
}
