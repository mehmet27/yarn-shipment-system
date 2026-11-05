import type { BrandingSaveRequest } from '@iplik/api-client/branding';
import { saveBranding } from '@iplik/api-client/branding';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { saveCachedBranding } from './brandingCache';
import { BRANDING_QUERY_KEY } from './useBranding';

/** Kayıttan dönen ayar önbelleğe yazılır; tema ve üst bar sayfa yenilenmeden değişir. */
export function useSaveBranding() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: BrandingSaveRequest) => saveBranding(request),
    onSuccess: (branding) => {
      saveCachedBranding(branding);
      queryClient.setQueryData(BRANDING_QUERY_KEY, branding);
    },
  });
}
