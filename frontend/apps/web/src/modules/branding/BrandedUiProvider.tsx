import { UiProvider } from '@iplik/ui';
import type { ReactNode } from 'react';
import { useBranding } from './useBranding';

export type BrandedUiProviderProps = { children: ReactNode };

/** Temayı kurulumun ana renginden üretir; renk gelene kadar varsayılan tema geçerlidir. */
export function BrandedUiProvider({ children }: BrandedUiProviderProps) {
  const branding = useBranding();
  return <UiProvider brandColor={branding.data?.primaryColor}>{children}</UiProvider>;
}
