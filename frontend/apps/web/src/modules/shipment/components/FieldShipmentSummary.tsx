import type { ShipmentDetail } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { PageHero, Paper, ProgressSteps, SimpleGrid, Stack, Text } from '@iplik/ui';
import { FieldValueText } from './FieldValueText';
import { PROGRESS_STATUSES, toProgressDone } from './progressSteps';
import { findBagCount } from './shipmentViews';
import { ShipmentStatusBadge } from './ShipmentStatusBadge';
import { useFieldText } from './useFieldText';

export type FieldShipmentSummaryProps = { shipment: ShipmentDetail };

/** Saha detayının üstü: başlık, süreç adımları ve çıkışta tartılan çuval sayısı. */
export function FieldShipmentSummary({ shipment }: FieldShipmentSummaryProps) {
  const { t } = useTranslation();
  const text = useFieldText();
  return (
    <Stack>
      <PageHero
        title={shipment.shipmentNo}
        subtitle={shipment.subcontractorTitle}
        actions={<ShipmentStatusBadge status={shipment.status} />}
      />
      {shipment.status === 'CANCELLED' ? null : (
        <Paper withBorder p="md" radius="md">
          <ProgressSteps
            label={t('shipment.progress.steps')}
            steps={PROGRESS_STATUSES.map((status) => t(`shipment.progress.step.${status}`))}
            current={toProgressDone(shipment.status) - 1}
          />
        </Paper>
      )}
      <Paper withBorder p="md" radius="md">
        <SimpleGrid cols={2}>
          <Text c="dimmed">{t('field.bagCount')}</Text>
          <FieldValueText field={findBagCount(shipment)} text={text} />
        </SimpleGrid>
      </Paper>
    </Stack>
  );
}
