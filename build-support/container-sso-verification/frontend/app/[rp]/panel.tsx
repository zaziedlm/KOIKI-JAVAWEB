'use client';
import {useState} from 'react';
type Detail={expenseRequestId:string;status:string;claimedAmount:number;version:number};
export default function Panel({csrf,authenticated,subject,initialDetail}:{csrf:string;authenticated:boolean;subject:string;initialDetail:Detail|null}) {
  const [detail,setDetail]=useState(initialDetail),[message,setMessage]=useState(''),[busy,setBusy]=useState(false);
  async function request(path:string,body:unknown) {
    const r=await fetch(path,{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-Token':csrf},body:JSON.stringify(body),cache:'no-store'});
    const value=await r.json();if(!r.ok)throw new Error('操作を完了できませんでした。再ログインして確認してください。');return value;
  }
  async function auth(action:string){setBusy(true);try{const value=await request('/a/auth/'+action,{});window.location.assign(value.redirect);}catch{setMessage('認証操作を完了できませんでした。');setBusy(false);}}
  async function create(event:React.FormEvent<HTMLFormElement>){event.preventDefault();setBusy(true);try{
    const form=new FormData(event.currentTarget),amount=Number(form.get('amount'));
    const value=await request('/a/api/expense-requests',{departmentId:'d3200000-0000-4000-8000-000000000021',claimedAmount:amount,
      lines:[{expenseCategoryId:'d3200000-0000-4000-8000-000000000022',usageDate:new Date().toISOString().slice(0,10),
        description:String(form.get('description')),purpose:String(form.get('purpose')),amount}]});
    const r=await fetch('/a/api/expense-requests/'+value.expenseRequestId,{cache:'no-store'});if(!r.ok)throw new Error();
    setDetail(await r.json());window.history.replaceState(null,'','/a?request='+value.expenseRequestId);setMessage('下書きを保存しました。');
  }catch{setMessage('保存できませんでした。状態を確認してください。');}finally{setBusy(false);}}
  async function submit(){if(!detail)return;setBusy(true);try{
    const r=await fetch('/a/api/expense-requests/'+detail.expenseRequestId+'/submit',{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-Token':csrf},body:JSON.stringify({expectedVersion:detail.version}),cache:'no-store'});
    if(r.status!==204)throw new Error();const updated=await fetch('/a/api/expense-requests/'+detail.expenseRequestId,{cache:'no-store'});setDetail(await updated.json());setMessage('提出しました。');
  }catch{setMessage('提出結果を確認してください。同じ操作は自動再送しません。');}finally{setBusy(false);}}
  return <main><h1>経費申請</h1><span hidden data-testid="subject">{subject}</span><span hidden data-testid="csrf">{csrf}</span>
    {!authenticated?<><p>ログインして申請を作成してください。</p><button disabled={busy} onClick={()=>auth('start')}>ログイン</button></>:
    <><p>ログイン済み</p><button disabled={busy} onClick={()=>auth('logout')}>ログアウト</button>
      {!detail?<form onSubmit={create}><p><label>金額（円） <input name="amount" type="number" min="1" defaultValue="1200" required/></label></p>
        <p><label>内容 <input name="description" defaultValue="交通費" required maxLength={200}/></label></p>
        <p><label>目的 <input name="purpose" defaultValue="訪問" required maxLength={500}/></label></p><button disabled={busy}>下書きを保存</button></form>:
      <section><h2>本人の申請</h2><p data-testid="status">{detail.status}</p><p>{detail.claimedAmount.toLocaleString()}円</p>
        {detail.status==='DRAFT'&&<button disabled={busy} onClick={submit}>この申請を提出</button>}</section>}</>}
    <p role="status">{message}</p></main>;
}
