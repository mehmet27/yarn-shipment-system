import type { InspectionView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Badge, Group, Paper, Stack, Text } from '@iplik/ui';
import { InspectionResultForm } from './InspectionResultForm';

export type InspectionItemProps = {
  claimId: string;
  shipmentId: string;
  inspection: InspectionView;
};

/** Numune / lab talebi: talep notu, sonuç ya da bekliyor bilgisi; sonuç formu `allowedActions` ile. */
export function InspectionItem({ claimId, shipmentId, inspection }: InspectionItemProps) {
  const { t } = useTranslation();
  const isPending = inspection.result === undefined;
  return (
    <Paper withBorder p="sm">
      <Stack gap="xs">
        <Group justify="space-between">
          <Text fw={600}>{t(`claim.inspection.kind.${inspection.kind}`)}</Text>
          <Badge variant="light" color={isPending ? 'yellow' : 'green'}>
            {t(isPending ? 'claim.inspection.pending' : 'claim.inspection.done')}
          </Badge>
        </Group>
        {inspection.requestNote !== undefined && <Text size="sm">{inspection.requestNote}</Text>}
        <Text size="sm" c="dimmed">
          {t('claim.inspection.requestedAt')}: {formatDateTime(inspection.requestedAt)}
        </Text>
        {inspection.result !== undefined && <Text>{inspection.result}</Text>}
        {inspection.resultEvidenceId !== undefined && (
          <Text size="sm" c="dimmed">
            {t('claim.inspection.documentAttached')}
          </Text>
        )}
        {inspection.allowedActions?.includes('RECORD_RESULT') && (
          <InspectionResultForm claimId={claimId} shipmentId={shipmentId} inspection={inspection} />
        )}
      </Stack>
    </Paper>
  );
}
