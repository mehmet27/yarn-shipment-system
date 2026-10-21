import type { AxiosRequestConfig } from 'axios';
import { httpClient } from './httpClient';

/** Orval mutator: üretilen her çağrı ortak gateway istemcisinden geçer (CSRF, X-Request-Id, Idempotency-Key). */
export async function customInstance<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await httpClient.request<T>(config);
  return response.data;
}
