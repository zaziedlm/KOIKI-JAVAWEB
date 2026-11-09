/** S2 transient helper. Real tokens stay in process memory and are never returned. */
import http from 'node:http';
import fs from 'node:fs';
import {ISSUER, CALLBACK, configure, begin, finish} from './frontend/lib/oidc.ts';

const secret = (name: string) => fs.readFileSync('/run/secrets/' + name, 'utf8').trim();
const cases = new Set(['normal', 'second-user', 'disabled', 'unprivileged', 'wrong-audience', 'no-scope']);
const pending = new Map();
const tokens = new Map();
const completed = new Map();
let targetId: string | undefined;
let exercised = false;
let denialsDone = false;
const auth = new Map();
const json = (res: http.ServerResponse, status: number, value: unknown) => {
  res.writeHead(status, {'Content-Type': 'application/json', 'Cache-Control': 'no-store'});
  res.end(JSON.stringify(value));
};
const decode = (jwt: string) => JSON.parse(Buffer.from(jwt.split('.')[1], 'base64url').toString());
const publicClaims = (jwt: string) => {
  const c = decode(jwt);
  return Object.fromEntries(['iss', 'sub', 'aud', 'scope', 'token_use', 'koiki_user_id', 'iat', 'exp'].filter(k => k in c).map(k => [k, c[k]]));
};
async function config(wrong: boolean) {
  const id = wrong ? 'koiki-d3-wrong-audience' : 'koiki-d3-a';
  if (!auth.has(id)) auth.set(id, await configure(id, secret(wrong ? 'client_wrong' : 'client_a')));
  return auth.get(id);
}
async function localGet(url: string) {
  const r = await fetch(url, {signal: AbortSignal.timeout(5000)});
  if (!r.ok) throw new Error('Dependency unavailable');
  return r.json();
}
async function api(token: string, method: string, path: string, body?: unknown) {
  if (!/^\/api\/v1\/expense-requests(?:\/[a-f0-9-]{36}(?:\/submit)?)?$/.test(path)) throw new Error('Unapproved API path');
  const r = await fetch('http://backend:8080' + path, {method, redirect: 'error',
    headers: {Authorization: 'Bearer ' + token, 'Content-Type': 'application/json'},
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(5000)});
  const text = await r.text();
  return {status: r.status, body: text ? JSON.parse(text) : null};
}
const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url!, CALLBACK);
    if (req.method === 'GET' && url.pathname === '/health') {
      const ready = await localGet('http://keycloak:9000/health/ready');
      const cfg = await config(false);
      const discovery = cfg.serverMetadata();
      const jwks = await localGet(discovery.jwks_uri!);
      return json(res, 200, {status: 'UP', keycloak: ready.status, issuer: discovery.issuer,
        jwks_uri: discovery.jwks_uri, jwks_key_count: jwks.keys.length});
    }
    if (req.method === 'GET' && url.pathname.startsWith('/begin/')) {
      const name = url.pathname.slice('/begin/'.length);
      if (!cases.has(name) || completed.has(name)) return json(res, 400, {error: 'unknown_or_used_case'});
      const cfg = await config(name === 'wrong-audience');
      const request = await begin(cfg, name === 'no-scope' ? 'openid' : 'openid expense.apply');
      pending.set(request.state, {name, cfg, ...request});
      res.writeHead(302, {Location: request.url.href, 'Cache-Control': 'no-store'});return res.end();
    }
    if (req.method === 'GET' && url.pathname === '/a/auth/callback') {
      const state = url.searchParams.get('state');
      const expected = pending.get(state);
      if (!expected) return json(res, 400, {error: 'callback_state_rejected'});
      pending.delete(state);
      try {
        const result = await finish(expected.cfg, url, expected);
        tokens.set(expected.name, result);
        completed.set(expected.name, {case: expected.name, success: true, id_token_signature: 'JWKS/library verified',
          access_claims: publicClaims(result.access_token), id_claims: publicClaims(result.id_token!)});
        return json(res, 200, {case: expected.name, authenticated: true});
      } catch {
        completed.set(expected.name, {case: expected.name, success: false, error: 'OIDC_validation_failed'});
        return json(res, 400, {error: 'OIDC_validation_failed'});
      }
    }
    if (req.method === 'GET' && url.pathname === '/results') {
      return json(res, 200, {completed: [...completed.values()], pending: pending.size});
    }
    if (req.method === 'POST' && url.pathname === '/exercise' && !exercised) {
      exercised = true; // Never retry an uncertain business POST.
      if ([...cases].some(c => !tokens.has(c))) throw new Error('Tokens not ready');
      const normal = tokens.get('normal');
      const create = await api(normal.access_token, 'POST', '/api/v1/expense-requests', {
        departmentId: 'd3200000-0000-4000-8000-000000000021', claimedAmount: 1200,
        lines: [{expenseCategoryId: 'd3200000-0000-4000-8000-000000000022',
          usageDate: new Date().toISOString().slice(0,10), description: 'S2 real IdP train', purpose: 'S2 API verification', amount: 1200}]});
      if (create.status !== 201) return json(res, 200, {failed: 'create', status: create.status, code: create.body?.code});
      targetId = create.body.expenseRequestId;
      const path = '/api/v1/expense-requests/' + targetId;
      const detail = await api(normal.access_token, 'GET', path);
      if (detail.status !== 200) return json(res, 200, {failed: 'detail', status: detail.status});
      const submit = await api(normal.access_token, 'POST', path + '/submit', {expectedVersion: detail.body.version});
      return json(res, 200, {request_id: targetId, create: create.status, detail: detail.status, submit: submit.status});
    }
    if (req.method === 'POST' && url.pathname === '/denials' && targetId && !denialsDone) {
      denialsDone = true;
      const path = '/api/v1/expense-requests/' + targetId;
      const denied = [];
      const payload = {departmentId: 'd3200000-0000-4000-8000-000000000021', claimedAmount: 1200,
        lines: [{expenseCategoryId: 'd3200000-0000-4000-8000-000000000022', usageDate: new Date().toISOString().slice(0,10),
          description: 'Must be denied', purpose: 'No business mutation', amount: 1200}]};
      for (const name of ['wrong-audience', 'no-scope', 'disabled', 'unprivileged', 'id-token']) {
        const token = name === 'id-token' ? tokens.get('normal').id_token : tokens.get(name).access_token;
        const r = await api(token, 'GET', path);
        const post = await api(token, 'POST', '/api/v1/expense-requests', payload);
        denied.push({case: name, status: r.status, post_status: post.status});
      }
      const other = await api(tokens.get('second-user').access_token, 'GET', path);
      denied.push({case: 'second-user', status: other.status});
      return json(res, 200, {denied});
    }
    if (req.method === 'POST' && url.pathname === '/forget') {
      tokens.clear(); pending.clear(); auth.clear();
      return json(res, 200, {tokens_retained: tokens.size, transactions_retained: pending.size});
    }
    return json(res, 404, {error: 'not_found'});
  } catch {
    // No exception dump: provider responses can contain tokens/authorization codes.
    return json(res, 503, {error: 'S2_helper_operation_failed'});
  }
});
server.listen(13000, '0.0.0.0');
process.on('SIGTERM', () => { tokens.clear();pending.clear();auth.clear();server.close(() => process.exit(0)); });
