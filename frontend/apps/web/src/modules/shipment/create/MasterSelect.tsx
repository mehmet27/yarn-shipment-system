import { Select, useDebouncedValue } from '@iplik/ui';
import { useState } from 'react';
import type { MasterKind, MasterOption } from '../hooks/useMasterOptions';
import { useMasterOptions } from '../hooks/useMasterOptions';

const SEARCH_DEBOUNCE_MS = 300;

export type MasterSelectProps = {
  kind: MasterKind;
  label: string;
  value: MasterOption | undefined;
  onChange: (option: MasterOption | undefined) => void;
  isRequired?: boolean;
  subcontractorId?: string;
};

/** Aranabilir ana veri seçici; seçili kayıt, arama sonucundan düşse de listede kalır. */
export function MasterSelect({
  kind,
  label,
  value,
  onChange,
  isRequired = false,
  subcontractorId,
}: MasterSelectProps) {
  const [search, setSearch] = useState('');
  const [q] = useDebouncedValue(search, SEARCH_DEBOUNCE_MS);
  const query = useMasterOptions(kind, q, subcontractorId);
  const fetched = query.data ?? [];
  const options =
    value === undefined || fetched.some((option) => option.value === value.value)
      ? fetched
      : [value, ...fetched];
  return (
    <Select
      label={label}
      data={options}
      value={value?.value ?? null}
      onChange={(next) => {
        onChange(options.find((option) => option.value === next));
      }}
      searchable
      clearable={!isRequired}
      withAsterisk={isRequired}
      onSearchChange={setSearch}
      nothingFoundMessage="—"
      comboboxProps={{ withinPortal: true }}
    />
  );
}
