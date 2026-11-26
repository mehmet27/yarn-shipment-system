import { useTranslation } from '@iplik/i18n';
import { Select } from '@iplik/ui';
import { useOwnerChoices } from '../hooks/useAllChoices';

export type OwnerSelectProps = {
  value: string | undefined;
  onChange: (subcontractorId: string | undefined) => void;
};

/** Şoförün bağlı olduğu fasoncu; boş bırakılırsa şoför kendi şoförüdür. */
export function OwnerSelect({ value, onChange }: OwnerSelectProps) {
  const { t } = useTranslation();
  const data = useOwnerChoices().data ?? [];
  return (
    <Select
      label={t('masterData.owner')}
      description={t('masterData.ownerHint')}
      data={data}
      value={value ?? null}
      onChange={(next) => {
        onChange(next ?? undefined);
      }}
      searchable
      clearable
    />
  );
}
