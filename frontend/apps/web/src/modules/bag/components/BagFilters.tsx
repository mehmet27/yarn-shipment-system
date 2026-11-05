import { useTranslation } from '@iplik/i18n';
import { Button, Group, SegmentedControl } from '@iplik/ui';
import { BAG_FILTERS } from './bagFilter';
import type { BagFilter } from './bagFilter';

export type BagFiltersProps = {
  filter: BagFilter;
  onFilter: (filter: BagFilter) => void;
  onSelectUnprinted: () => void;
};

/** Çuval listesinin durum süzgeci ve "basılmamışları seç" kısayolu. */
export function BagFilters({ filter, onFilter, onSelectUnprinted }: BagFiltersProps) {
  const { t } = useTranslation();
  return (
    <Group justify="space-between" align="flex-end">
      <SegmentedControl
        size="xs"
        aria-label={t('bag.filter.label')}
        data={BAG_FILTERS.map((value) => ({ value, label: t(`bag.filter.${value}`) }))}
        value={filter}
        onChange={(value) => {
          onFilter(BAG_FILTERS.find((item) => item === value) ?? 'ALL');
        }}
      />
      <Button variant="default" onClick={onSelectUnprinted}>
        {t('bag.selectUnprinted')}
      </Button>
    </Group>
  );
}
