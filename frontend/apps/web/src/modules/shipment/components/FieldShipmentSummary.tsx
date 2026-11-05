import type { ShipmentDetail } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { PageHero, Paper, ProgressSteps, SimpleGrid, Stack, Text } from '@iplik/ui';
import { toProgressDone } from './progressSteps';
import { findBagCount, findSealNo } from './shipmentViews';
import { ShipmentStatusBadge } from './ShipmentStatusBadge';
import { useFieldText } from './useFieldText';

const STEP_STATUSES = [
  'DRAFT',
  'LOADED',
  'IN_TRANSIT',
  'AWAITING_DELIVERY',
  'CHECKING',
  'COMPLETED',
] as const;

export type FieldShipmentSummaryProps = { shipment: ShipmentDetail };

/** Saha detayının üstü: başlık, süreç adımları ve çuval/mühür bilgisi (kilo yok, Y1). */
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
            steps={STEP_STATUSES.map((status) => t(`shipment.progress.step.${status}`))}
            current={toProgressDone(shipment.status) - 1}
          />
        </Paper>
      )}
      <Paper withBorder p="md" radius="md">
        <SimpleGrid cols={2}>
          <Text c="dimmed">{t('field.bagCount')}</Text>
          <Text fw={700} size="xl">
            {text(findBagCount(shipment))}
          </Text>
          <Text c="dimmed">{t('field.sealNo')}</Text>
          <Text fw={700} size="xl">
            {text(findSealNo(shipment))}
          </Text>
        </SimpleGrid>
      </Paper>
    </Stack>
  );
}
