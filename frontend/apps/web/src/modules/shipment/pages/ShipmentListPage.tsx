import { SHIPMENT_STATUSES } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button, EmptyState, PageHero, Select, Stack } from '@iplik/ui';
import { Can } from '@/modules/auth';
import { Link } from 'react-router';
import { ListPagination, QueryState } from '@/modules/shared';
import { ShipmentTable } from '../components/ShipmentTable';
import { useShipmentListFilter, PAGE_SIZE } from '../hooks/useShipmentListFilter';
import { useShipments } from '../hooks/useShipments';

export function ShipmentListPage() {
  const { t } = useTranslation();
  const filter = useShipmentListFilter();
  const query = useShipments({
    page: filter.page - 1,
    size: PAGE_SIZE,
    ...statusParam(filter.status),
  });
  const statusOptions = SHIPMENT_STATUSES.map((status) => ({
    value: status,
    label: t(`shipment.status.${status}`),
  }));

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
      <Select
        aria-label={t('shipment.list.statusFilter')}
        placeholder={t('shipment.list.allStatuses')}
        data={statusOptions}
        value={filter.status ?? null}
        onChange={(value) => {
          filter.setStatus(SHIPMENT_STATUSES.find((status) => status === value));
        }}
        clearable
        maw={280}
      />
      <QueryState query={query}>
        {(page) =>
          page.items.length === 0 ? (
            <EmptyState title={t('shipment.list.empty')} />
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

/** exactOptionalPropertyTypes: filtre yoksa alan hiç gönderilmez. */
function statusParam(status: ReturnType<typeof useShipmentListFilter>['status']) {
  return status === undefined ? {} : { status: [status] };
}
