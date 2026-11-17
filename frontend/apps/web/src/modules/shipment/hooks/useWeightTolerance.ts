import { getShipmentSettings } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';
import { FIRE_RATE_SETTING_PATH } from './useFireRateDefault';

/** Kantar toleransı (kg metni); ayar gelene dek ve hata durumunda undefined — karşılaştırma tolerans sıfır sayar. */
export function useWeightTolerance(): string | undefined {
  const query = useQuery({
    queryKey: [FIRE_RATE_SETTING_PATH],
    queryFn: getShipmentSettings,
    staleTime: Infinity,
  });
  return query.data?.weightToleranceKg;
}
