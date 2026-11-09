export function apiPath(operation:string,id?:string) {
  if(operation==='create' && !id)return '/api/v1/expense-requests';
  if(!id || !/^[a-f0-9]{8}-[a-f0-9]{4}-[1-5][a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/i.test(id))throw new Error('Invalid API selector');
  if(operation==='detail')return '/api/v1/expense-requests/'+id;
  if(operation==='submit')return '/api/v1/expense-requests/'+id+'/submit';
  throw new Error('Invalid API selector');
}
export async function api(token:string,operation:string,id?:string,body?:unknown) {
  const response=await fetch('http://backend:8080'+apiPath(operation,id),{method:operation==='detail'?'GET':'POST',
    headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},
    body:body===undefined?undefined:JSON.stringify(body),cache:'no-store',redirect:'error',signal:AbortSignal.timeout(5000)});
  const content=await response.text();return {status:response.status,body:content?JSON.parse(content):null};
}
