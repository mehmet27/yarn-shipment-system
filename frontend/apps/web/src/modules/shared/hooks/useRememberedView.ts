import { useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router';
import { useCurrentUser } from '@/modules/auth';
import { loadView, saveView } from '../viewMemory';

/** Sayfa numarası hatırlanmaz; adreste varsa bağlantı bilinçli verilmiş sayılır ve son görünüm geri yüklenmez. */
const PAGE_KEY = 'sayfa';

function pickKeys(params: URLSearchParams, keys: readonly string[]): string {
  const picked = new URLSearchParams();
  keys.forEach((key) => {
    const value = params.get(key);
    if (value !== null) picked.set(key, value);
  });
  return picked.toString();
}

function mergeSearch(current: URLSearchParams, saved: string): URLSearchParams {
  const next = new URLSearchParams(current);
  new URLSearchParams(saved).forEach((value, key) => {
    next.set(key, value);
  });
  return next;
}

/**
 * Ekranın görünümü (sekme, dönem, süzgeç) adreste durur; bu hook kişinin son seçimini hatırlar. Ekran süzgeçsiz
 * açılırsa (menüden) son seçim adrese geri yazılır; adres bir süzgeç ya da sayfa taşıyorsa (paylaşılan bağlantı,
 * ana sayfadaki "Tümünü gör") olduğu gibi kalır. `keys` modül sabiti olmalıdır.
 */
export function useRememberedView(screen: string, keys: readonly string[]): void {
  const userId = useCurrentUser().data?.userId;
  const [searchParams, setSearchParams] = useSearchParams();
  const current = pickKeys(searchParams, keys);
  const hasPage = searchParams.has(PAGE_KEY);
  const hasStarted = useRef(false);

  useEffect(() => {
    if (userId === undefined) return;
    const isFirstRun = !hasStarted.current;
    hasStarted.current = true;
    if (isFirstRun && current === '') {
      const saved = hasPage ? null : loadView(userId, screen);
      if (saved !== null) {
        setSearchParams((now) => mergeSearch(now, saved), { replace: true });
      }
      return;
    }
    saveView(userId, screen, current);
  }, [userId, screen, current, hasPage, setSearchParams]);
}
