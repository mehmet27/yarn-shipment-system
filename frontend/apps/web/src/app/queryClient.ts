import { classifyProblem, toProblem } from '@iplik/api-client';
import { QueryClient } from '@tanstack/react-query';

const MAX_RETRY_COUNT = 2;

/** Yalnızca geçici hatalar tekrar denenir; 4xx yanıtlar tekrar denense de değişmez. */
function shouldRetry(failureCount: number, error: unknown): boolean {
  const kind = classifyProblem(toProblem(error));
  return failureCount < MAX_RETRY_COUNT && (kind === 'unavailable' || kind === 'unexpected');
}

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: shouldRetry, refetchOnWindowFocus: false },
    },
  });
}
