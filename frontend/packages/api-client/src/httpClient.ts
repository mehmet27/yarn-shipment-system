import axios from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';

const MUTATING_METHODS = new Set(['post', 'put', 'patch', 'delete']);

/**
 * Gateway BFF oturumu: token tarayıcıya inmez (FE-SEC-01). Oturum çerezi
 * `withCredentials` ile, CSRF değeri XSRF-TOKEN çerezinden X-XSRF-TOKEN başlığıyla gider.
 */
export const httpClient = axios.create({
  baseURL: '/',
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

function addTraceHeaders(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  config.headers.set('X-Request-Id', crypto.randomUUID());
  const method = (config.method ?? 'get').toLowerCase();
  // FE-API-07: offline yeniden denemede aynı anahtar korunur; çağıran verdiyse ona dokunulmaz.
  if (MUTATING_METHODS.has(method) && !config.headers.has('Idempotency-Key')) {
    config.headers.set('Idempotency-Key', crypto.randomUUID());
  }
  return config;
}

httpClient.interceptors.request.use(addTraceHeaders);
