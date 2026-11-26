import { listProducts } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Select } from '@iplik/ui';
import { useQuery } from '@tanstack/react-query';
import { MASTER_DATA_PATH, OPTION_PAGE_SIZE } from '../hooks/useMasterOptions';

const ELASTANE_SEARCH = ['elastan', 'likra', 'lycra'];

/** Tanımlar'daki elastan ipliklerinin numaraları ("20 denye" gibi). */
async function elastaneCounts(): Promise<string[]> {
  const pages = await Promise.all(
    ELASTANE_SEARCH.map((q) => listProducts({ q, size: OPTION_PAGE_SIZE })),
  );
  const counts = pages.flatMap((page) => page.items.map((item) => item.yarnCount ?? ''));
  return [...new Set(counts.filter((count) => count !== ''))].sort((a, b) =>
    a.localeCompare(b, 'tr'),
  );
}

/** LYC: elastan ipliklerinin tanımlı numaralarından seçilir. */
export function LycraSelect({
  value,
  onChange,
}: {
  value: string;
  onChange: (next: string) => void;
}) {
  const { t } = useTranslation();
  const counts = useQuery({
    queryKey: [MASTER_DATA_PATH, 'product', 'elastane'],
    queryFn: elastaneCounts,
  });
  const options = counts.data ?? [];
  const data = value === '' || options.includes(value) ? options : [value, ...options];
  return (
    <Select
      label={t('create.recipe.lycra')}
      placeholder={t('create.selectPlaceholder')}
      data={data}
      value={value === '' ? null : value}
      onChange={(next) => {
        onChange(next ?? '');
      }}
      clearable
      nothingFoundMessage="—"
      comboboxProps={{ withinPortal: true, hideDetached: false }}
    />
  );
}
