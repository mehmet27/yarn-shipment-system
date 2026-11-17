import { useTranslation } from '@iplik/i18n';
import { Menu, Text } from '@iplik/ui';
import { Link } from 'react-router';
import type { usePendingWeightDecisions } from '@/modules/shipment';

type PendingShipments = ReturnType<typeof usePendingWeightDecisions>;

/** Kantar uyuşmazlığında müdürün kararını bekleyen sevkiyatlar; her biri sevkiyat dosyasına gider. */
export function WeightDecisionItems({ shipments }: { shipments: PendingShipments }) {
  const { t } = useTranslation();
  if (shipments.length === 0) return null;
  return (
    <>
      <Menu.Label>{t('notifications.weightTitle')}</Menu.Label>
      {shipments.map((item) => (
        <Menu.Item key={item.shipmentId} component={Link} to={`/panel/sevkiyat/${item.shipmentId}`}>
          <Text size="sm" fw={600}>
            {item.shipmentNo}
          </Text>
          <Text size="xs" c="dimmed">
            {item.subcontractorTitle}
          </Text>
        </Menu.Item>
      ))}
    </>
  );
}
