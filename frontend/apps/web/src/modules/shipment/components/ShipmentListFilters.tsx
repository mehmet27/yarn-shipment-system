import { SHIPMENT_STATUSES } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Group, Paper, Select, TextInput } from '@iplik/ui';
import type { ReactNode } from 'react';
import type { ShipmentListFilter } from '../hooks/useShipmentListFilter';
import { ShipmentSearchBox } from './ShipmentSearchBox';

export type ShipmentListFiltersProps = {
  filter: ShipmentListFilter;
  /** Süzgeçlerin yanına konan düğme (Excel'e al). */
  children?: ReactNode;
};

/** Sevkiyat listesi süzgeçleri: arama metni, durum ve sevkiyat tarihi aralığı. Hepsi adrese yazılır. */
export function ShipmentListFilters({ filter, children }: ShipmentListFiltersProps) {
  const { t } = useTranslation();
  const statusOptions = SHIPMENT_STATUSES.map((status) => ({
    value: status,
    label: t(`shipment.status.${status}`),
  }));
  return (
    <Paper withBorder p="md">
      <Group align="flex-end" gap="md" wrap="wrap">
        <ShipmentSearchBox
          key={filter.search}
          label={t('shipment.list.search')}
          placeholder={t('shipment.list.searchPlaceholder')}
          initialValue={filter.search}
          onSearch={filter.setSearch}
        />
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
        <TextInput
          type="date"
          label={t('shipment.list.dateFrom')}
          value={filter.from}
          max={filter.to === '' ? undefined : filter.to}
          error={filter.hasRangeError ? t('shipment.list.dateRangeInvalid') : undefined}
          onChange={(event) => {
            filter.setRange({ from: event.currentTarget.value, to: filter.to });
          }}
        />
        <TextInput
          type="date"
          label={t('shipment.list.dateTo')}
          value={filter.to}
          min={filter.from === '' ? undefined : filter.from}
          onChange={(event) => {
            filter.setRange({ from: filter.from, to: event.currentTarget.value });
          }}
        />
        {children}
      </Group>
    </Paper>
  );
}
