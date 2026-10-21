import { classifyProblem, toProblem } from '@iplik/api-client';
import type { QueryKey } from '@tanstack/react-query';
import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';
import { CURRENT_USER_QUERY_KEY } from '@/modules/auth';

const MAX_RETRY_COUNT = 2;

/** Yalnızca geçici hatalar tekrar denenir; 4xx yanıtlar tekrar denense de değişmez. */
function shouldRetry(failureCount: number, error: unknown): boolean {
  const kind = classifyProblem(toProblem(error));
  return failureCount < MAX_RETRY_COUNT && (kind === 'unavailable' || kind === 'unexpected');
}

/**
 * Oturum açıldıktan sonra süresi dolarsa herhangi bir istek 401 döner. Kullanıcı sorgusu yenilenir;
 * o da 401 alırsa RequireAuth girişe yönlendirir.
 */
function handleSessionExpiry(client: () => QueryClient) {
  return (error: unknown, queryKey?: QueryKey) => {
    // Kullanıcı sorgusunun kendi 401'i döngü kurmasın: onu RequireAuth karşılar.
    const isUserQuery = queryKey?.[0] === CURRENT_USER_QUERY_KEY[0];
    if (!isUserQuery && classifyProblem(toProblem(error)) === 'unauthenticated') {
      void client().invalidateQueries({ queryKey: CURRENT_USER_QUERY_KEY });
    }
  };
}

export function createQueryClient(): QueryClient {
  const client: QueryClient = new QueryClient({
    queryCache: new QueryCache({
      onError: (error, query) => {
        handleSessionExpiry(() => client)(error, query.queryKey);
      },
    }),
    mutationCache: new MutationCache({
      onError: (error) => {
        handleSessionExpiry(() => client)(error);
      },
    }),
    defaultOptions: {
      queries: { retry: shouldRetry, refetchOnWindowFocus: false },
    },
  });
  return client;
}
