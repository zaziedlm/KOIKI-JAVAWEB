/** Shared local-fixture OIDC boundary. Only this exact issuer permits HTTP. */
import * as oidc from 'openid-client';

export const ISSUER = 'http://idp.localhost:18080/realms/koiki-d3';
export const CALLBACK = 'http://localhost:13000/a/auth/callback';

export async function configure(clientId: string, secret: string, issuer = ISSUER) {
  if (issuer !== ISSUER || !['koiki-d3-a', 'koiki-d3-b', 'koiki-d3-wrong-audience'].includes(clientId)) {
    throw new Error('Unapproved local issuer/client');
  }
  const config = await oidc.discovery(new URL(issuer), clientId, secret, undefined,
    {execute: [oidc.allowInsecureRequests, oidc.enableNonRepudiationChecks], timeout: 5});
  if (config.serverMetadata().issuer !== ISSUER) throw new Error('Issuer mismatch');
  return config;
}

export async function begin(config: oidc.Configuration, scope: string, silent=false) {
  const verifier = oidc.randomPKCECodeVerifier();
  const state = oidc.randomState();
  const nonce = oidc.randomNonce();
  const url = oidc.buildAuthorizationUrl(config, {redirect_uri: callbackFor(config.clientMetadata().client_id), ...(silent?{prompt:'none'}:{}), response_type: 'code',
    scope, state, nonce, code_challenge: await oidc.calculatePKCECodeChallenge(verifier), code_challenge_method: 'S256'});
  return {url, verifier, state, nonce};
}

export async function finish(config: oidc.Configuration, currentUrl: URL,
  expected: {verifier: string; state: string; nonce: string}) {
  if (![CALLBACK,CALLBACK.replace('/a/','/b/')].includes(currentUrl.origin + currentUrl.pathname)) throw new Error('Callback mismatch');
  if(currentUrl.origin+currentUrl.pathname!==callbackFor(config.clientMetadata().client_id))throw new Error('Callback client mismatch');
  const tokens = await oidc.authorizationCodeGrant(config, currentUrl, {pkceCodeVerifier: expected.verifier,
    expectedState: expected.state, expectedNonce: expected.nonce, idTokenExpected: true});
  if (!oidc.getJwksCache(config)) throw new Error('ID Token signature JWKS was not used');
  return tokens;
}

export function callbackFor(clientId:string) {
  if(!['koiki-d3-a','koiki-d3-b','koiki-d3-wrong-audience'].includes(clientId))throw new Error('Unapproved client');
  return CALLBACK.replace('/a/',clientId==='koiki-d3-b'?'/b/':'/a/');
}
export function logoutUrl(config:oidc.Configuration,state:string) {
  const clientId=config.clientMetadata().client_id;
  return oidc.buildEndSessionUrl(config,{client_id:clientId,post_logout_redirect_uri:callbackFor(clientId).replace('/callback','/logout-return'),state});
}

export async function refresh(config:oidc.Configuration,token:string,issuer:string,subject:string) {
  const result=await oidc.refreshTokenGrant(config,token);
  const claims=result.claims();
  if(claims&&(claims.iss!==issuer||claims.sub!==subject))throw new Error('Refresh identity mismatch');
  return {access_token:result.access_token,refresh_token:result.refresh_token,id_token:result.id_token,expires_in:result.expires_in};
}
