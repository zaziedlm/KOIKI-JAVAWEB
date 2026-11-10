import {test} from 'node:test';
import assert from 'node:assert/strict';
import {selectReplayCookies} from './cookie-replay.mjs';

const helper = changes => ({name:'KC_AUTH_SESSION_HASH',domain:'idp.localhost',path:'/realms/koiki-d3/',expires:100,...changes});
test('only the expired helper at the exact expiry boundary is omitted', () => {
  const cookie=helper(),input=[cookie],selection=selectReplayCookies(input,100);
  assert.deepEqual(selection.expected,[]);assert.deepEqual(selection.expiredAuxiliary,[cookie]);
  assert.deepEqual(input,[cookie]);
});
test('valid and session helpers remain subject to exact comparison', () => {
  const cookies=[helper({expires:101}),helper({expires:-1})];
  assert.deepEqual(selectReplayCookies(cookies,100),{expected:cookies,expiredAuxiliary:[]});
});
test('mandatory Session and other IdP cookies are never excluded even if expired', () => {
  const cookies=[helper({name:'D3_A_SESSION'}),helper({name:'KEYCLOAK_SESSION'}),helper({name:'KEYCLOAK_IDENTITY'})];
  assert.deepEqual(selectReplayCookies(cookies,100),{expected:cookies,expiredAuxiliary:[]});
});
test('other scopes and invalid expiry values do not match the exception', () => {
  const cookies=[helper({domain:'other.localhost'}),helper({path:'/realms/other/'}),helper({expires:NaN}),helper({expires:-2})];
  assert.deepEqual(selectReplayCookies(cookies,100),{expected:cookies,expiredAuxiliary:[]});
  assert.equal(selectReplayCookies([helper({domain:'.idp.localhost'})],100).expiredAuxiliary.length,1);
});
test('an invalid comparison clock is rejected', () => {
  for(const time of [NaN,Infinity,-1])assert.throws(()=>selectReplayCookies([helper()],time),TypeError);
});
