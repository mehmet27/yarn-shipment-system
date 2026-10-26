import type { ShipmentDetail } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Anchor, Button, Group, Paper, SimpleGrid, Stack, Text, Title } from '@iplik/ui';
import { Link, useParams } from 'react-router';
import { ArrivalDeclaration } from '../components/ArrivalDeclaration';
import { PickupConfirmation } from '../components/PickupConfirmation';
import { QueryState } from '../components/QueryState';
import { ShipmentStatusBadge } from '../components/ShipmentStatusBadge';
import { useShipment } from '../hooks/useShipment';

function findBagCount(shipment: ShipmentDetail): number | undefined {
  return 'dispatchBagCount' in shipment ? shipment.dispatchBagCount : undefined;
}

function findSealNo(shipment: ShipmentDetail): string | undefined {
  return 'dispatchSealNo' in shipment ? shipment.dispatchSealNo : undefined;
}

/** Şoför ekranı: yalnızca çuval sayısı ve mühür no (yetkilendirme Y1); kilo hiç gelmez. */
export function FieldShipmentPage() {
  const { t } = useTranslation();
  const { shipmentId = '' } = useParams();
  const query = useShipment(shipmentId);
  return (
    <Stack>
      <Anchor component={Link} to="/saha">
        {t('field.back')}
      </Anchor>
      <QueryState query={query}>
        {(shipment) => (
          <Stack>
            <Group justify="space-between">
              <Title order={3}>{shipment.shipmentNo}</Title>
              <ShipmentStatusBadge status={shipment.status} />
            </Group>
            <Text>{shipment.subcontractorTitle}</Text>
            <Paper withBorder p="md">
              <SimpleGrid cols={2}>
                <Text c="dimmed">{t('field.bagCount')}</Text>
                <Text fw={700} size="xl">
                  {findBagCount(shipment) ?? t('common.notRecorded')}
                </Text>
                <Text c="dimmed">{t('field.sealNo')}</Text>
                <Text fw={700} size="xl">
                  {findSealNo(shipment) ?? t('common.notRecorded')}
                </Text>
              </SimpleGrid>
            </Paper>
            {shipment.allowedActions.includes('CONFIRM_PICKUP') && (
              <PickupConfirmation shipmentId={shipment.shipmentId} />
            )}
            {shipment.allowedActions.includes('DECLARE_ARRIVAL') && (
              <ArrivalDeclaration shipmentId={shipment.shipmentId} />
            )}
            {shipment.allowedActions.includes('RECORD_DELIVERY') && (
              <Button size="xl" fullWidth component={Link} to="teslim" relative="path">
                {t('field.recordDelivery')}
              </Button>
            )}
          </Stack>
        )}
      </QueryState>
    </Stack>
  );
}
