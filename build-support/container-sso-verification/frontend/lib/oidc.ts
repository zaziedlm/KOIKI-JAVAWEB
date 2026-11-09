/** Shared local-fixture OIDC boundary. Only this exact issuer permits HTTP. */
import * as oidc from 'openid-client';

export const ISSUER = 'http://idp.localhost:18080/realms/koiki-d3';
export const CALLBACK = 'http://localhost:13000/a/auth/callback';

export async function configure(clientId: string, secret: string, issuer = ISSUER) {
  if (issuer !== ISSUER || !['koiki-d3-a', 'koiki-d3-wrong-audience'].includes(clientId)) {
    throw new Error('Unapproved local issuer/client');
  }
  const config = await oidc.discovery(new URL(issuer), clientId, secret, undefined,
    {execute: [oidc.allowInsecureRequests, oidc.enableNonRepudiationChecks], timeout: 5});
  if (config.serverMetadata().issuer !== ISSUER) throw new Error('Issuer mismatch');
  return config;
}

export async function begin(config: oidc.Configuration, scope: string) {
  const verifier = oidc.randomPKCECodeVerifier();
  const state = oidc.randomState();
  const nonce = oidc.randomNonce();
  const url = oidc.buildAuthorizationUrl(config, {redirect_uri: CALLBACK, response_type: 'code',
    scope, state, nonce, code_challenge: await oidc.calculatePKCECodeChallenge(verifier), code_challenge_method: 'S256'});
  return {url, verifier, state, nonce};
}

export async function finish(config: oidc.Configuration, currentUrl: URL,
  expected: {verifier: string; state: string; nonce: string}) {
  if (currentUrl.origin + currentUrl.pathname !== CALLBACK) throw new Error('Callback mismatch');
  const tokens = await oidc.authorizationCodeGrant(config, currentUrl, {pkceCodeVerifier: expected.verifier,
    expectedState: expected.state, expectedNonce: expected.nonce, idTokenExpected: true});
  if (!oidc.getJwksCache(config)) throw new Error('ID Token signature JWKS was not used');
  return tokens;
}
