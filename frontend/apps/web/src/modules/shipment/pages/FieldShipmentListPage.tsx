import { useTranslation } from '@iplik/i18n';
import { Stack, Text, Title } from '@iplik/ui';
import { QueryState } from '../components/QueryState';
import { ShipmentCard } from '../components/ShipmentCard';
import { useShipments } from '../hooks/useShipments';

const FIELD_PAGE_SIZE = 50;

/** Backend kapsamı uygular: şoför yalnızca kendisine atanan sevkiyatları alır. */
export function FieldShipmentListPage() {
  const { t } = useTranslation();
  const query = useShipments({ page: 0, size: FIELD_PAGE_SIZE });
  return (
    <Stack>
      <Title order={3}>{t('field.myShipments')}</Title>
      <QueryState query={query}>
        {(page) =>
          page.items.length === 0 ? (
            <Text c="dimmed">{t('field.empty')}</Text>
          ) : (
            <Stack gap="sm">
              {page.items.map((shipment) => (
                <ShipmentCard
                  key={shipment.shipmentId}
                  shipment={shipment}
                  to={`/saha/sevkiyat/${shipment.shipmentId}`}
                />
              ))}
            </Stack>
          )
        }
      </QueryState>
    </Stack>
  );
}
