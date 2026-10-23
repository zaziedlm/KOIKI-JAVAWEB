import { useEffect, useRef, useState } from 'react';
import { flushSync } from 'react-dom';
import type { FormEvent } from 'react';
import { ApiError, SessionApi, StaleResponse } from './api';
import type { Detail, Draft, Options, Page } from './types';

type Screen = { kind: 'list'; value: Page } | { kind: 'new'; value: Options } | { kind: 'detail'; value: Detail };
const statuses: Record<string, string> = { DRAFT: '下書き', SUBMITTED: '提出済み', APPROVED: '承認済み', REJECTED: '却下', RETURNED: '差戻し', SETTLED: '精算済み' };
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const money = (value: string) => BigInt(value).toLocaleString('ja-JP') + ' 円';
const updated = (value: string) => new Date(value).toLocaleString('ja-JP');

export default function App() {
  const api = useRef(new SessionApi()).current;
  const inFlight = useRef(false);
  const stopped = useRef(false);
  const [screen, setScreen] = useState<Screen | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('読み込んでいます…');
  const [login, setLogin] = useState(false);
  const [invalidFields, setInvalidFields] = useState<string[]>([]);

  function clear() { api.invalidate(); setScreen(null); setInvalidFields([]); }
  function failure(error: unknown, mutation = false) {
    if (error instanceof StaleResponse || (error instanceof DOMException && error.name === 'AbortError')) return;
    if (error instanceof ApiError) {
      switch (error.status) {
        case 401:
          clear(); stopped.current = true; setLogin(true);
          history.replaceState(null, '', '#/requests');
          setMessage('セッションが終了しました。再度ログインしてください。'); return;
        case 403:
          clear();
          if (error.problem.code === 'KOIKI-REF-CSRF-001') {
            setMessage('操作を確認できませんでした。再読込して最新の状態を確認し、必要な操作をやり直してください。');
          } else {
            stopped.current = true; setMessage('この操作を利用する権限がありません。');
          }
          return;
        case 404: clear(); setMessage('申請が見つかりません。一覧から確認してください。'); return;
        case 409: {
          clear();
          const generation = api.generation + 1;
          void load().then(ok => { if (ok && api.generation === generation) setMessage('申請の状態が変わりました。最新の内容を確認してから操作してください。'); });
          return;
        }
        case 400: case 422:
          setInvalidFields((error.problem.violations ?? []).map(violation => violation.field));
          setMessage('入力を確認してください。金額・日付・必須項目と、現在利用できる部門・費目を見直してください。'); return;
      }
    }
    if (mutation) clear();
    setMessage(mutation ? '操作結果を確認できません。自動再送は行いません。再読込して申請の状態を照合してください。'
      : 'データを取得できませんでした。再読込してください。');
  }

  async function load() {
    if (stopped.current || document.hidden) return false;
    clear(); setMessage('読み込んでいます…');
    const generation = api.generation;
    try {
      await api.request<void>('csrf');
      const path = location.hash.slice(1) || '/requests';
      let next: Screen;
      if (path === '/new') next = { kind: 'new', value: await api.request<Options>('v1/expense-options') };
      else if (/^\/requests(?:\?page=(0|[1-9][0-9]*))?$/.test(path)) {
        const page = Number(new URLSearchParams(path.split('?')[1]).get('page') ?? '0');
        if (!Number.isSafeInteger(page) || page > 2147483647) throw new ApiError(404);
        next = { kind: 'list', value: await api.request<Page>('v1/expense-requests?page=' + page) };
      }
      else {
        const id = path.startsWith('/requests/') ? path.slice('/requests/'.length) : '';
        if (!uuid.test(id)) throw new ApiError(404);
        next = { kind: 'detail', value: await api.request<Detail>('v1/expense-requests/' + id) };
      }
      if (generation !== api.generation || stopped.current) return false;
      setScreen(next); setMessage(''); return true;
    } catch (error) { if (generation === api.generation) failure(error); return false; }
  }

  useEffect(() => {
    const hidden = () => { if (document.hidden) { flushSync(() => { clear(); setMessage(''); }); } else void load(); };
    const leave = () => { flushSync(() => { clear(); setMessage(''); }); };
    const restore = () => { void load(); };
    window.addEventListener('hashchange', restore);
    window.addEventListener('pageshow', restore);
    window.addEventListener('pagehide', leave);
    document.addEventListener('visibilitychange', hidden);
    void load();
    return () => {
      api.invalidate();
      window.removeEventListener('hashchange', restore); window.removeEventListener('pageshow', restore);
      window.removeEventListener('pagehide', leave); document.removeEventListener('visibilitychange', hidden);
    };
  }, []);

  async function mutate(action: () => Promise<void>) {
    if (inFlight.current || stopped.current) return;
    inFlight.current = true; setBusy(true); setMessage('処理しています…');
    const generation = api.generation;
    try { await action(); }
    catch (error) { if (generation === api.generation) failure(error, true); }
    finally { inFlight.current = false; setBusy(false); }
  }

  function create(draft: Draft) {
    return mutate(async () => {
      const result = await api.request<{ expenseRequestId: string }>('v1/expense-requests', draft);
      location.hash = '/requests/' + result.expenseRequestId;
    });
  }
  function submit(detail: Detail) {
    return mutate(async () => {
      await api.request<void>('v1/expense-requests/' + detail.expenseRequestId + '/submit', { expectedVersion: detail.version });
      await load();
    });
  }
  async function logout() {
    if (inFlight.current || stopped.current) return;
    stopped.current = true; inFlight.current = true; setBusy(true); clear();
    history.replaceState(null, '', '#/requests'); setMessage('ログアウトしています…');
    try {
      await api.request<void>('logout', {});
      setMessage('ログアウトしました。');
    } catch {
      setMessage('画面のデータは破棄しましたが、サーバーの失効は確認できません。ログイン状態を確認してください。');
    } finally { inFlight.current = false; setBusy(false); setLogin(true); }
  }

  return <>
    <header className="topbar"><a className="brand" href="/">KOIKI <span>Reference</span></a><div className="header-links"><a href="/">MVCホーム</a><button className="quiet" disabled={busy || stopped.current} onClick={() => void logout()}>ログアウト</button></div></header>
    <main className="workspace">
      <div className="eyebrow">EXPENSE REQUESTS</div>
      <div className="page-heading"><div><h1>経費申請</h1><p>自分の申請を確認し、新しい経費を提出できます。</p></div></div>
      {!login && <nav aria-label="申請操作"><a href="#/requests" aria-current={screen?.kind === 'list' ? 'page' : undefined}>申請一覧</a><a href="#/new" aria-current={screen?.kind === 'new' ? 'page' : undefined}>新しい申請</a><button disabled={busy || stopped.current} onClick={() => void load()}>再読込</button></nav>}
      {message && <div className="notice" role="status" aria-live="polite">{message}</div>}
      {login && <a className="primary link-button" href="/login">ログイン画面へ</a>}
      {screen?.kind === 'list' && <section className="panel"><div className="section-heading"><h2>自分の申請</h2><span>全 {screen.value.totalElements} 件</span></div>
        {screen.value.content.length === 0 ? <p className="empty">申請はまだありません。「新しい申請」から作成してください。</p> : <div><p className="table-hint">表は横にスクロールして確認できます。</p><div className="table-scroll" tabIndex={0} role="region" aria-label="申請一覧（横スクロール）"><table><thead><tr><th>部門</th><th>申請金額</th><th>状態</th><th>更新日時</th><th>詳細</th></tr></thead><tbody>{screen.value.content.map(item => <tr key={item.expenseRequestId}><td>{item.department.name}</td><td className="amount">{money(item.claimedAmount)}</td><td><span className={'badge ' + item.status.toLowerCase()}>{statuses[item.status] ?? item.status}</span></td><td>{updated(item.updatedAt)}</td><td><a href={'#/requests/' + item.expenseRequestId}>詳細を見る</a></td></tr>)}</tbody></table></div></div>}
        <div className="pagination"><span>{screen.value.page + 1} ページ目</span>{screen.value.page > 0 && <a href={'#/requests?page=' + (screen.value.page - 1)}>前へ</a>}{screen.value.page < 2147483647 && BigInt(screen.value.totalElements) > BigInt(screen.value.page + 1) * BigInt(screen.value.size) && <a href={'#/requests?page=' + (screen.value.page + 1)}>次へ</a>}</div>
      </section>}
      {screen?.kind === 'new' && <DraftForm key={api.generation} options={screen.value} busy={busy} create={create} invalidFields={invalidFields} />}
      {screen?.kind === 'detail' && <section className="panel detail"><div className="section-heading"><h2>申請の詳細</h2><span className="badge">{statuses[screen.value.status] ?? screen.value.status}</span></div>
        <dl><div><dt>申請番号</dt><dd className="identifier">{screen.value.expenseRequestId}</dd></div><div><dt>部門</dt><dd>{screen.value.department.name}</dd></div><div><dt>申請金額</dt><dd className="total">{money(screen.value.claimedAmount)}</dd></div><div><dt>更新日時</dt><dd>{updated(screen.value.updatedAt)}</dd></div></dl>
        {screen.value.decisionReason && <p className="notice">{screen.value.decisionReason}</p>}
        <h3>明細</h3>{screen.value.lines.map(line => <article className="line" key={line.expenseLineId}><div><strong>{line.expenseCategory.name}</strong><span>{line.usageDate}</span></div><p>{line.description}</p><p className="muted">{line.purpose}</p><strong>{money(line.amount)}</strong></article>)}
        {screen.value.status === 'DRAFT' && <div className="actions"><p className="hint">内容を確認して提出してください。提出後の承認はMVC画面で行います。</p><button className="primary" disabled={busy} onClick={() => void submit(screen.value as Detail)}>この申請を提出</button></div>}
      </section>}
    </main><footer>KOIKI Reference · Session SPA</footer>
  </>;
}

function DraftForm({ options, busy, create, invalidFields }: { options: Options; busy: boolean; create: (value: Draft) => Promise<void>; invalidFields: string[] }) {
  const [error, setError] = useState('');
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const amount = String(form.get('amount') ?? '');
    if (!/^[1-9][0-9]{0,18}$/.test(amount) || BigInt(amount) > 9223372036854775807n) { setError('金額は1円以上の整数で入力してください。'); return; }
    const departmentId = String(form.get('departmentId'));
    await create({ departmentId, claimedAmount: amount, lines: [{ expenseCategoryId: String(form.get('expenseCategoryId')), usageDate: String(form.get('usageDate')), description: String(form.get('description')), purpose: String(form.get('purpose')), amount }] });
  }
  const labels: Record<string, string> = { departmentId: '部門', claimedAmount: '金額', 'lines[0].amount': '金額', 'lines[0].expenseCategoryId': '費目', 'lines[0].usageDate': '利用日', 'lines[0].description': '内容', 'lines[0].purpose': '目的' };
  const unavailable = options.departments.length === 0 || options.expenseCategories.length === 0;
  return <section className="panel"><div className="section-heading"><h2>新しい申請</h2><span>1明細の経費申請</span></div>
    {unavailable ? <p className="notice">利用できる部門・費目がありません。管理者へ確認してください。</p> : <form onSubmit={event => void save(event)}>
      <fieldset disabled={busy}><div className="form-grid"><label>部門<select name="departmentId" required>{options.departments.map(option => <option key={option.id} value={option.id}>{option.name}</option>)}</select></label><label>費目<select name="expenseCategoryId" required>{options.expenseCategories.map(option => <option key={option.id} value={option.id}>{option.name}</option>)}</select></label><label>利用日<input name="usageDate" type="date" required defaultValue={new Date().toISOString().slice(0, 10)} max={new Date().toISOString().slice(0, 10)} /></label><label>金額（円）<input name="amount" type="text" inputMode="numeric" pattern="[1-9][0-9]{0,18}" maxLength={19} required placeholder="例：1200" /></label></div><label>内容<input name="description" maxLength={200} required placeholder="例：訪問先への交通費" /></label><label>目的<textarea name="purpose" maxLength={500} required rows={3} placeholder="経費の利用目的" /></label>{error && <p role="alert">{error}</p>}{invalidFields.length > 0 && <ul role="alert">{Array.from(new Set(invalidFields.map(field => labels[field] ?? '入力内容'))).map(label => <li key={label}>{label}を確認してください。</li>)}</ul>}<div className="form-footer"><span className="hint">下書きを保存した後、詳細画面から提出できます。</span><button className="primary" type="submit">下書きを保存</button></div></fieldset>
    </form>}
  </section>;
}
