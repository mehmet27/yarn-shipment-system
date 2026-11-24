import { useTranslation } from '@iplik/i18n';
import { Button, PageHero, Stack } from '@iplik/ui';
import { Can } from '@/modules/auth';
import { Link } from 'react-router';
import { ListPagination, QueryState } from '@/modules/shared';
import { ShipmentListEmpty } from '../components/ShipmentListEmpty';
import { ShipmentListFilters } from '../components/ShipmentListFilters';
import { ShipmentTable } from '../components/ShipmentTable';
import { PAGE_SIZE, toListParams, useShipmentListFilter } from '../hooks/useShipmentListFilter';
import { useShipments } from '../hooks/useShipments';

export function ShipmentListPage() {
  const { t } = useTranslation();
  const filter = useShipmentListFilter();
  const query = useShipments({ page: filter.page - 1, size: PAGE_SIZE, ...toListParams(filter) });

  return (
    <Stack>
      <PageHero
        title={t('shipment.list.title')}
        actions={
          <Can permission="SHIPMENT_CREATE">
            <Button component={Link} to="/panel/sevkiyat/yeni">
              {t('create.open')}
            </Button>
          </Can>
        }
      />
      <ShipmentListFilters filter={filter} />
      <QueryState query={query}>
        {(page) =>
          page.items.length === 0 ? (
            <ShipmentListEmpty
              isFiltered={filter.isFiltered}
              isOnlyStatus={filter.search === '' && filter.from === '' && filter.to === ''}
              onShowAll={filter.clear}
            />
          ) : (
            <Stack>
              <ShipmentTable shipments={page.items} />
              <ListPagination
                total={page.page.totalPages}
                value={filter.page}
                onChange={filter.setPage}
              />
            </Stack>
          )
        }
      </QueryState>
    </Stack>
  );
}
