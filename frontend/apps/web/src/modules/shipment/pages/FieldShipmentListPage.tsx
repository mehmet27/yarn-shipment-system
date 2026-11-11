import { useTranslation } from '@iplik/i18n';
import { EmptyState, PageHero, Stack } from '@iplik/ui';
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
      <PageHero title={t('field.myShipments')} />
      <QueryState query={query}>
        {(page) =>
          page.items.length === 0 ? (
            <EmptyState title={t('field.empty')} />
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
