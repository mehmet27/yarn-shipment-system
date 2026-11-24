import { listShipments, SHIPMENTS_PATH } from '@iplik/api-client';
import { useQueryClient } from '@tanstack/react-query';

/** Tek sonuç mu diye bakmak için iki kayıt yeter. */
const PROBE_SIZE = 2;

function listPath(text: string): string {
  return `/panel/sevkiyat?${new URLSearchParams({ ara: text }).toString()}`;
}

/**
 * Hızlı aramanın gideceği adres: arama tek sevkiyat buluyorsa o sevkiyatın dosyası, değilse (birden çok, hiç ya da
 * sunucu hatası) aynı aramayla sevkiyat listesi. Liste hatayı ve boş durumu kendi gösterir.
 */
export function useQuickSearchTarget(): (text: string) => Promise<string> {
  const queryClient = useQueryClient();
  return async (text) => {
    const params = { search: text, size: PROBE_SIZE };
    try {
      const page = await queryClient.query({
        queryKey: [SHIPMENTS_PATH, params],
        queryFn: () => listShipments(params),
      });
      const [only] = page.items;
      return page.page.totalElements === 1 && only !== undefined
        ? `/panel/sevkiyat/${only.shipmentId}`
        : listPath(text);
    } catch {
      return listPath(text);
    }
  };
}
