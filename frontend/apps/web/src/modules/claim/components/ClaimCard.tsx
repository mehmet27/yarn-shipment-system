import type { QualityClaimView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { formatKg, isKilogram } from '@iplik/units';
import { Badge, Group, Paper, Stack, Text, Title } from '@iplik/ui';
import { EvidenceByIds } from '@/modules/evidence';
import { InspectionItem } from './InspectionItem';
import { RequestInspectionForm } from './RequestInspectionForm';

function formatClaimKg(value: string): string {
  const kg = isKilogram(value) ? value : undefined;
  return kg === undefined ? value : formatKg(kg);
}

/** Tek itiraz: uyuşmazlık no ve durumu, itiraz edilen kilo, açıklama, fotoğraf sayısı ve numune/lab adımları. */
export function ClaimCard({ claim }: { claim: QualityClaimView }) {
  const { t } = useTranslation();
  return (
    <Paper withBorder p="md" component="article" aria-label={claim.discrepancyNo}>
      <Stack gap="xs">
        <Group justify="space-between">
          <Title order={4}>{claim.discrepancyNo}</Title>
          <Badge variant="light">{t(`discrepancy.status.${claim.status}`)}</Badge>
        </Group>
        <Text>{claim.description}</Text>
        <Text size="sm">
          {t('claim.card.claimedKg')}: {formatClaimKg(claim.claimedKg)}
        </Text>
        <Text size="sm" c="dimmed">
          {t('claim.card.photos', { count: claim.evidenceIds.length })} · {t('claim.card.openedAt')}
          : {formatDateTime(claim.openedAt)}
        </Text>
        <EvidenceByIds evidenceIds={claim.evidenceIds} />
        {claim.resolution !== undefined && (
          <Text size="sm">
            {t('discrepancy.facts.resolution')}: {t(`discrepancy.resolution.${claim.resolution}`)}
            {claim.resolutionNote === undefined ? '' : ` · ${claim.resolutionNote}`}
          </Text>
        )}
        {claim.resolution === 'CLAIM_JUSTIFIED' && (
          <Text size="sm" c="dimmed">
            {t('claim.card.adjustedKg')}: {formatClaimKg(claim.claimedKg)}
          </Text>
        )}
        {claim.inspections.map((inspection) => (
          <InspectionItem
            key={inspection.id}
            claimId={claim.id}
            shipmentId={claim.shipmentId}
            inspection={inspection}
          />
        ))}
        {claim.allowedActions?.includes('REQUEST_INSPECTION') && (
          <RequestInspectionForm claimId={claim.id} />
        )}
      </Stack>
    </Paper>
  );
}
