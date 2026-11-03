import { useTranslation } from '@iplik/i18n';
import { Stack, Title } from '@iplik/ui';
import { useShipmentClaims } from '../hooks/useClaims';
import { ClaimCard } from './ClaimCard';

export type ClaimOfDiscrepancyProps = { shipmentId: string; discrepancyId: string };

/** Kalite uyuşmazlığının itiraz dosyası: açıklama, çuvallar ve numune/lab adımları. */
export function ClaimOfDiscrepancy({ shipmentId, discrepancyId }: ClaimOfDiscrepancyProps) {
  const { t } = useTranslation();
  const query = useShipmentClaims(shipmentId);
  const claim = query.data?.claims.find((item) => item.discrepancyId === discrepancyId);
  if (claim === undefined) return null;
  return (
    <Stack gap="xs">
      <Title order={4}>{t('claim.ofDiscrepancy')}</Title>
      <ClaimCard claim={claim} />
    </Stack>
  );
}
