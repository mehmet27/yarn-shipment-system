import { EvidenceUpload } from '@/modules/evidence';
import { useTranslation } from '@iplik/i18n';
import { Anchor, Button, Group, Paper, SimpleGrid, Stack, Text, Title } from '@iplik/ui';
import { Link, useParams } from 'react-router';
import { ArrivalDeclaration } from '../components/ArrivalDeclaration';
import { PickupConfirmation } from '../components/PickupConfirmation';
import { QueryState } from '../components/QueryState';
import { findBagCount, findSealNo } from '../components/shipmentViews';
import { ShipmentStatusBadge } from '../components/ShipmentStatusBadge';
import { useFieldText } from '../components/useFieldText';
import { useShipment } from '../hooks/useShipment';

/** Şoför ekranı: yalnızca çuval sayısı ve mühür no (yetkilendirme Y1); kilo hiç gelmez. */
export function FieldShipmentPage() {
  const { t } = useTranslation();
  const { shipmentId = '' } = useParams();
  const query = useShipment(shipmentId);
  const text = useFieldText();
  return (
    <Stack>
      <Anchor component={Link} to="/saha">
        {t('field.back')}
      </Anchor>
      <QueryState query={query}>
        {(shipment) => (
          <Stack>
            <Group justify="space-between">
              <Title order={1} size="h3">
                {shipment.shipmentNo}
              </Title>
              <ShipmentStatusBadge status={shipment.status} />
            </Group>
            <Text>{shipment.subcontractorTitle}</Text>
            <Paper withBorder p="md">
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
            {shipment.allowedActions.includes('CONFIRM_PICKUP') && (
              <PickupConfirmation shipmentId={shipment.shipmentId} />
            )}
            {shipment.allowedActions.includes('DECLARE_ARRIVAL') && (
              <ArrivalDeclaration shipmentId={shipment.shipmentId} />
            )}
            <EvidenceUpload shipmentId={shipment.shipmentId} />
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
