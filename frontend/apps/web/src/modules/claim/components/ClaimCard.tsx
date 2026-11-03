import type { QualityClaimView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Badge, Group, Paper, Stack, Text, Title } from '@iplik/ui';
import { InspectionItem } from './InspectionItem';
import { RequestInspectionForm } from './RequestInspectionForm';

/** Tek itiraz: uyuşmazlık no ve durumu, çuvallar, açıklama, fotoğraf sayısı ve numune/lab adımları. */
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
          {t('claim.card.bags')}: {claim.bagKeys.join(', ')}
        </Text>
        <Text size="sm" c="dimmed">
          {t('claim.card.photos', { count: claim.evidenceIds.length })} · {t('claim.card.openedAt')}
          : {formatDateTime(claim.openedAt)}
        </Text>
        {claim.resolution !== undefined && (
          <Text size="sm">
            {t('discrepancy.facts.resolution')}: {t(`discrepancy.resolution.${claim.resolution}`)}
            {claim.resolutionNote === undefined ? '' : ` · ${claim.resolutionNote}`}
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
