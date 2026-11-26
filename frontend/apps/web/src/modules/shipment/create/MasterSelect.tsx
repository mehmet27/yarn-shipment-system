import { useTranslation } from '@iplik/i18n';
import { Select, useDebouncedValue } from '@iplik/ui';
import { useState } from 'react';
import type { MasterKind, MasterOption } from '../hooks/useMasterOptions';
import { CENTRAL_GROUP, OPTION_PAGE_SIZE, useMasterOptions } from '../hooks/useMasterOptions';

const SEARCH_DEBOUNCE_MS = 300;

export type MasterSelectProps = {
  kind: MasterKind;
  label: string;
  value: MasterOption | undefined;
  onChange: (option: MasterOption | undefined) => void;
  isRequired?: boolean;
  error?: string;
  subcontractorId?: string;
  /** Parti seçicisinde yalnızca bu ipliğin partileri listelenir. */
  productId?: string;
  /** Parti seçicisinde yalnızca bu tedarikçinin partileri listelenir. */
  supplierId?: string;
};

type Item = { value: string; label: string };
type SelectData = readonly Item[] | readonly { group: string; items: Item[] }[];

/** Merkez grubu önce, diğerleri Türkçe alfabetik. */
function compareGroups(a: string, b: string): number {
  if (a === b) return 0;
  if (a === CENTRAL_GROUP) return -1;
  if (b === CENTRAL_GROUP) return 1;
  return a.localeCompare(b, 'tr');
}

function groupTitle(title: string, centralTitle: string, ownTitle: string): string {
  if (title === CENTRAL_GROUP) return centralTitle;
  return title === '' ? ownTitle : title;
}

/** Gruplu seçenek varsa başlıklar altında (merkez önce), yoksa düz liste döner. */
function groupOptions(
  options: readonly MasterOption[],
  centralTitle: string,
  ownTitle: string,
): SelectData {
  if (!options.some((option) => option.group !== undefined)) {
    return options.map(({ value, label }) => ({ value, label }));
  }
  const titles = [...new Set(options.map((option) => option.group ?? ''))].sort(compareGroups);
  return titles.map((title) => ({
    group: groupTitle(title, centralTitle, ownTitle),
    items: options
      .filter((option) => (option.group ?? '') === title)
      .map(({ value, label }) => ({ value, label })),
  }));
}

/** Aranabilir ana veri seçici; seçili kayıt, arama sonucundan düşse de listede kalır. */
export function MasterSelect({
  kind,
  label,
  value,
  onChange,
  isRequired = false,
  error,
  subcontractorId,
  productId,
  supplierId,
}: MasterSelectProps) {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const [q] = useDebouncedValue(search, SEARCH_DEBOUNCE_MS);
  const query = useMasterOptions(kind, q, { subcontractorId, productId, supplierId });
  const fetched = query.data ?? [];
  const options =
    value === undefined || fetched.some((option) => option.value === value.value)
      ? fetched
      : [value, ...fetched];
  const data = groupOptions(options, t('create.centralGroup'), t('create.ownGroup'));
  return (
    <Select
      label={label}
      placeholder={t('create.selectPlaceholder')}
      error={error}
      data={data}
      value={value?.value ?? null}
      onChange={(next) => {
        onChange(options.find((option) => option.value === next));
      }}
      searchable
      clearable={!isRequired}
      withAsterisk={isRequired}
      onSearchChange={setSearch}
      nothingFoundMessage="—"
      {...(fetched.length >= OPTION_PAGE_SIZE ? { description: t('create.moreOptions') } : {})}
      comboboxProps={{ withinPortal: true, hideDetached: false }}
    />
  );
}
