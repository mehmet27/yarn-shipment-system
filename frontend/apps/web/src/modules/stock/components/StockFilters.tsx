import { useTranslation } from '@iplik/i18n';
import { Group, SegmentedControl, TextInput } from '@iplik/ui';

export type SortKey = 'name' | 'kg';

const isSortKey = (value: string): value is SortKey => value === 'name' || value === 'kg';

type StockFiltersProps = {
  search: string;
  onSearch: (value: string) => void;
  sort: SortKey;
  onSort: (value: SortKey) => void;
};

/** Stok tablosu için iplik cinsi arama kutusu ve sıralama seçimi. */
export function StockFilters({ search, onSearch, sort, onSort }: StockFiltersProps) {
  const { t } = useTranslation();
  return (
    <Group align="flex-end">
      <TextInput
        label={t('stock.search')}
        placeholder={t('stock.searchPlaceholder')}
        value={search}
        onChange={(event) => {
          onSearch(event.currentTarget.value);
        }}
      />
      <SegmentedControl
        aria-label={t('stock.sort.label')}
        value={sort}
        onChange={(value) => {
          if (isSortKey(value)) onSort(value);
        }}
        data={[
          { value: 'name', label: t('stock.sort.name') },
          { value: 'kg', label: t('stock.sort.kg') },
        ]}
      />
    </Group>
  );
}
