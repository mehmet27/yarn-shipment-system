import { useTranslation } from '@iplik/i18n';
import { Group, Paper, Select, TextInput } from '@iplik/ui';
import { useMasterOptions } from '@/modules/shipment';
import type { ReactNode } from 'react';
import type { InvoicedFilter } from '../hooks/useFireSettlements';

/** Faturalananlar süzgeci: tarih aralığı ve fasoncu; yanına dışa aktarma düğmesi konur. */
export function InvoicedFilters({
  filter,
  isRangeInvalid,
  onChange,
  children,
}: {
  filter: InvoicedFilter;
  isRangeInvalid: boolean;
  onChange: (next: InvoicedFilter) => void;
  children: ReactNode;
}) {
  const { t } = useTranslation();
  const subcontractors = useMasterOptions('subcontractor', '');
  const { from, to, subcontractorId } = filter;
  return (
    <Paper withBorder p="md">
      <Group align="flex-end" gap="md" wrap="wrap">
        <TextInput
          type="date"
          label={t('settlement.invoiced.from')}
          value={from}
          max={to === '' ? undefined : to}
          onChange={(event) => {
            onChange({ ...filter, from: event.currentTarget.value });
          }}
        />
        <TextInput
          type="date"
          label={t('settlement.invoiced.to')}
          value={to}
          min={from === '' ? undefined : from}
          error={isRangeInvalid ? t('settlement.invoiced.rangeInvalid') : undefined}
          onChange={(event) => {
            onChange({ ...filter, to: event.currentTarget.value });
          }}
        />
        <Select
          label={t('settlement.invoiced.subcontractor')}
          placeholder={t('settlement.invoiced.allSubcontractors')}
          data={subcontractors.data ?? []}
          value={subcontractorId ?? null}
          clearable
          onChange={(value) => {
            onChange({ ...filter, subcontractorId: value ?? undefined });
          }}
        />
        {children}
      </Group>
    </Paper>
  );
}
