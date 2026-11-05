import type { BrandingView } from '@iplik/api-client/branding';
import { getBranding } from '@iplik/api-client/branding';
import { useQuery } from '@tanstack/react-query';
import { loadCachedBranding, saveCachedBranding } from './brandingCache';

export const BRANDING_QUERY_KEY = ['branding'] as const;

const STALE_TIME_MS = 300_000;

async function fetchBranding(): Promise<BrandingView> {
  const branding = await getBranding();
  saveCachedBranding(branding);
  return branding;
}

/** Kurulumun marka ayarı; yanıt gelene kadar son bilinen ayar (yoksa varsayılan tema) kullanılır. */
export function useBranding() {
  return useQuery({
    queryKey: BRANDING_QUERY_KEY,
    queryFn: fetchBranding,
    placeholderData: loadCachedBranding,
    staleTime: STALE_TIME_MS,
  });
}
