import axios from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';
import { randomId } from './randomId';

const MUTATING_METHODS = new Set(['post', 'put', 'patch', 'delete']);

/**
 * Okuma isteği bu sürede yanıt almazsa kesilir; ekran sonsuza dek "yükleniyor" kalmaz, hata ve "yeniden dene" görünür.
 * Yazma isteklerine (ör. zayıf bağlantıda büyük fotoğraf yükleme) süre sınırı konmaz.
 */
export const READ_TIMEOUT_MS = 30_000;

/**
 * Çerez oturumu: token tarayıcıya inmez (FE-SEC-01). Oturum çerezi
 * `withCredentials` ile, CSRF değeri XSRF-TOKEN çerezinden X-XSRF-TOKEN başlığıyla gider.
 */
export const httpClient = axios.create({
  baseURL: '/',
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  // Dizi parametreleri tekrarlanır: ?status=A&status=B (OpenAPI explode: true).
  paramsSerializer: { indexes: null },
});

function addTraceHeaders(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  config.headers.set('X-Request-Id', randomId());
  const method = (config.method ?? 'get').toLowerCase();
  if (method === 'get' && (config.timeout ?? 0) === 0) {
    config.timeout = READ_TIMEOUT_MS;
  }
  // FE-API-07: offline yeniden denemede aynı anahtar korunur; çağıran verdiyse ona dokunulmaz.
  if (MUTATING_METHODS.has(method) && !config.headers.has('Idempotency-Key')) {
    config.headers.set('Idempotency-Key', randomId());
  }
  return config;
}

httpClient.interceptors.request.use(addTraceHeaders);
