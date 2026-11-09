import test from 'node:test';
import assert from 'node:assert/strict';
import {configure, finish} from '../lib/oidc.ts';

test('local HTTP exception rejects a different issuer before network access', async()=> {
  await assert.rejects(()=>configure('koiki-d3-a','fixture','http://unexpected.example/realm'), /Unapproved local issuer/);
});
test('OIDC config rejects an unapproved client before network access', async()=> {
  await assert.rejects(()=>configure('unknown-client','fixture'), /Unapproved local issuer\/client/);
});
test('callback rejects a different origin before token exchange', async()=> {
  await assert.rejects(()=>finish(null,new URL('http://unexpected.example/a/auth/callback'),{}), /Callback mismatch/);
});
