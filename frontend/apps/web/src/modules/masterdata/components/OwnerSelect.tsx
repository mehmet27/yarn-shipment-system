import { useTranslation } from '@iplik/i18n';
import { Select } from '@iplik/ui';
import { useRecords } from '../hooks/useRecords';

const OWNER_PAGE_SIZE = 100;

export type OwnerSelectProps = {
  value: string | undefined;
  onChange: (subcontractorId: string | undefined) => void;
};

/** Şoförün bağlı olduğu fasoncu; boş bırakılırsa şoför kendi şoförüdür. */
export function OwnerSelect({ value, onChange }: OwnerSelectProps) {
  const { t } = useTranslation();
  const query = useRecords('subcontractor', {
    q: '',
    page: 0,
    size: OWNER_PAGE_SIZE,
    isShowingInactive: false,
  });
  const data = (query.data?.rows ?? []).map((row) => ({ value: row.id, label: row.label }));
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
