import { Stack } from '@iplik/ui';
import { QueryState } from '@/modules/shipment';
import { useState } from 'react';
import type { InvoicedFilter } from '../hooks/useFireSettlements';
import { useInvoiced } from '../hooks/useFireSettlements';
import { InvoicedExport } from './InvoicedExport';
import { InvoicedFilters } from './InvoicedFilters';
import { InvoicedList } from './InvoicedList';

const NO_FILTER: InvoicedFilter = { from: '', to: '', subcontractorId: undefined };

/** Faturalananlar sekmesi: girilmiş faturalar; tarih ve fasoncuya göre süzülür, Excel'e alınabilir. */
export function InvoicedTab() {
  const [filter, setFilter] = useState(NO_FILTER);
  const [page, setPage] = useState(1);
  const isRangeInvalid = filter.from !== '' && filter.to !== '' && filter.from > filter.to;
  const query = useInvoiced(filter, page, !isRangeInvalid);
  return (
    <Stack>
      <InvoicedFilters
        filter={filter}
        isRangeInvalid={isRangeInvalid}
        onChange={(next) => {
          setFilter(next);
          setPage(1);
        }}
      >
        <InvoicedExport
          filter={filter}
          totalKg={query.data?.totalKg ?? '0'}
          isDisabled={isRangeInvalid || query.data === undefined}
        />
      </InvoicedFilters>
      {!isRangeInvalid && (
        <QueryState query={query}>
          {(data) => <InvoicedList data={data} page={page} onPage={setPage} />}
        </QueryState>
      )}
    </Stack>
  );
}
