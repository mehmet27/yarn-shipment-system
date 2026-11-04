import { useTranslation } from '@iplik/i18n';
import { Select } from '@iplik/ui';
import { useRecords } from '../hooks/useRecords';

const OWNER_PAGE_SIZE = 100;

export type AccountOwnerSelectProps = {
  kind: 'driver' | 'subcontractor';
  value: string | undefined;
  onChange: (recordId: string | undefined) => void;
  error?: string | undefined;
};

/** Giriş hesabının bağlanacağı etkin şoför ya da fasoncu kaydı; hesap o kişinin sevkiyatlarını görür. */
export function AccountOwnerSelect({ kind, value, onChange, error }: AccountOwnerSelectProps) {
  const { t } = useTranslation();
  const query = useRecords(kind, {
    q: '',
    page: 0,
    size: OWNER_PAGE_SIZE,
    isShowingInactive: false,
  });
  const data = (query.data?.rows ?? []).map((row) => ({ value: row.id, label: row.label }));
  return (
    <Select
      label={t(`users.field.record.${kind}`)}
      description={t('users.field.recordHint')}
      data={data}
      value={value ?? null}
      onChange={(next) => {
        onChange(next ?? undefined);
      }}
      searchable
      withAsterisk
      error={error}
      nothingFoundMessage={t('masterData.empty')}
    />
  );
}
