import type { ShipmentClaimsView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { EmptyState, Stack, Text } from '@iplik/ui';
import { ClaimCard } from './ClaimCard';
import { ClaimOpenSection } from './ClaimOpenSection';

/** İtiraz süresi, yeni itiraz bölümü ve açılmış itirazlar. */
export function ShipmentClaims({
  shipmentId,
  view,
}: {
  shipmentId: string;
  view: ShipmentClaimsView;
}) {
  const { t } = useTranslation();
  const deadline = formatDateTime(view.deadlineAt);
  return (
    <Stack>
      <Text c="dimmed" size="sm">
        {t(view.windowOpen ? 'claim.window.open' : 'claim.window.closed', { deadline })}
      </Text>
      <ClaimOpenSection shipmentId={shipmentId} view={view} />
      {view.claims.length === 0 && <EmptyState title={t('claim.empty')} />}
      {view.claims.map((claim) => (
        <ClaimCard key={claim.id} claim={claim} />
      ))}
    </Stack>
  );
}
