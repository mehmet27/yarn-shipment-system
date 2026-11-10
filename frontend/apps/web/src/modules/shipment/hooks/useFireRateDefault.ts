import { getShipmentSettings } from '@iplik/api-client';
import { fireRateToPctInput } from '@iplik/units';
import { useQuery } from '@tanstack/react-query';

export const FIRE_RATE_SETTING_PATH = '/api/v1/settings/shipment';
/** Ayar okunamazsa kullanılan varsayılan fire yüzdesi (analiz 5.1). */
export const FALLBACK_FIRE_RATE_PCT = '4';

/** Varsayılan fire oranı yüzde metni olarak ("4"); ayar gelene dek ve hata durumunda %4. */
export function useFireRateDefault(): string {
  const query = useQuery({
    queryKey: [FIRE_RATE_SETTING_PATH],
    queryFn: getShipmentSettings,
    staleTime: Infinity,
  });
  return query.data === undefined
    ? FALLBACK_FIRE_RATE_PCT
    : fireRateToPctInput(query.data.defaultFireRate);
}
