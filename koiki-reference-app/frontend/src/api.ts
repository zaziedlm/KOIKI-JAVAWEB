import type { Problem } from './types';

export class ApiError extends Error {
  constructor(readonly status: number, readonly problem: Problem = {}) { super('Request failed'); }
}
export class StaleResponse extends Error {}

// Only the CSRF cookie is read here. Session credentials stay with the browser.
function csrfToken(): string {
  const value = document.cookie.split(';').map(part => part.trim())
    .find(part => part.startsWith('XSRF-TOKEN='));
  return value ? decodeURIComponent(value.slice('XSRF-TOKEN='.length)) : '';
}

export class SessionApi {
  generation = 0;
  private reads = new AbortController();
  invalidate(): void {
    this.generation++;
    this.reads.abort();
    this.reads = new AbortController();
  }
  async request<T>(path: string, body?: unknown): Promise<T> {
    const generation = this.generation;
    const mutation = body !== undefined;
    const headers: Record<string, string> = { Accept: 'application/json, application/problem+json' };
    if (mutation) {
      headers['Content-Type'] = 'application/json';
      headers['X-XSRF-TOKEN'] = csrfToken();
    }
    const response = await fetch('/spa-api/' + path, {
      method: mutation ? 'POST' : 'GET', credentials: 'same-origin', cache: 'no-store', redirect: 'error',
      headers, body: mutation ? JSON.stringify(body) : undefined,
      signal: mutation ? AbortSignal.timeout(15000) : AbortSignal.any([this.reads.signal, AbortSignal.timeout(15000)])
    });
    if (generation !== this.generation) throw new StaleResponse();
    if (!response.ok) {
      let problem: Problem = {};
      if (response.headers.get('content-type')?.includes('json')) {
        try { problem = await response.json() as Problem; } catch { /* Use the status-only fallback. */ }
      }
      throw new ApiError(response.status, problem);
    }
    if (response.status === 204) return undefined as T;
    if (!response.headers.get('content-type')?.includes('application/json')) throw new ApiError(502);
    const result = await response.json() as T;
    if (generation !== this.generation) throw new StaleResponse();
    return result;
  }
}
