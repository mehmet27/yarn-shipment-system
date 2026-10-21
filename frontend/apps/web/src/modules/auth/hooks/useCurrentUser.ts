import { getCurrentUser, ME_PATH } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';

export const CURRENT_USER_QUERY_KEY = [ME_PATH] as const;

/** Kullanıcı ve izinler açılışta bir kez alınır; oturum süresince önbellekte kalır. */
export function useCurrentUser() {
  return useQuery({
    queryKey: CURRENT_USER_QUERY_KEY,
    queryFn: getCurrentUser,
    staleTime: Infinity,
  });
}
