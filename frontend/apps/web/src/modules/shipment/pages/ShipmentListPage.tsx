import { SHIPMENT_STATUSES } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button, Group, Pagination, Select, Stack, Text, Title } from '@iplik/ui';
import { Can } from '@/modules/auth';
import { Link } from 'react-router';
import { QueryState } from '../components/QueryState';
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
      <Group justify="space-between">
        <Title order={1} size="h2">
          {t('shipment.list.title')}
        </Title>
        <Can permission="SHIPMENT_CREATE">
          <Button component={Link} to="/panel/sevkiyat/yeni">
            {t('create.open')}
          </Button>
        </Can>
        <Select
          label={t('shipment.list.statusFilter')}
          placeholder={t('shipment.list.allStatuses')}
          data={statusOptions}
          value={filter.status ?? null}
          onChange={(value) => {
            filter.setStatus(SHIPMENT_STATUSES.find((status) => status === value));
          }}
          clearable
        />
      </Group>
      <QueryState query={query}>
        {(page) =>
          page.items.length === 0 ? (
            <Text c="dimmed">{t('shipment.list.empty')}</Text>
          ) : (
            <Stack>
              <ShipmentTable shipments={page.items} />
              <Pagination
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
