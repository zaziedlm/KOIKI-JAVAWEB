// Tooling policy: only the observed, expired IdP authentication helper is omitted.
export function selectReplayCookies(cookies, nowSeconds) {
  if (!Number.isFinite(nowSeconds) || nowSeconds < 0) throw new TypeError('Invalid replay clock');
  const expected = [], expiredAuxiliary = [];
  for (const cookie of cookies) {
    const eligible = cookie.name === 'KC_AUTH_SESSION_HASH'
      && cookie.domain.replace(/^\./, '') === 'idp.localhost'
      && ['/realms/koiki-d3', '/realms/koiki-d3/'].includes(cookie.path)
      && Number.isFinite(cookie.expires) && cookie.expires >= 0 && cookie.expires <= nowSeconds;
    (eligible ? expiredAuxiliary : expected).push(cookie);
  }
  return {expected, expiredAuxiliary};
}
