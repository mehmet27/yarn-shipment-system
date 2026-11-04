import { useTranslation } from '@iplik/i18n';
import { Select } from '@iplik/ui';
import type { AccountKind } from '../staffRoles';
import { ACCOUNT_KINDS, asAccountKind } from '../staffRoles';

export type AccountKindSelectProps = {
  value: AccountKind | undefined;
  onChange: (kind: AccountKind | undefined) => void;
  error?: string | undefined;
};

/** Hesap açarken rol seçimi: fabrika içi roller ve kayda bağlı şoför/fasoncu. */
export function AccountKindSelect({ value, onChange, error }: AccountKindSelectProps) {
  const { t } = useTranslation();
  return (
    <Select
      label={t('users.field.roles')}
      data={ACCOUNT_KINDS.map((kind) => ({ value: kind, label: t(`users.role.${kind}`) }))}
      value={value ?? null}
      onChange={(next) => {
        onChange(asAccountKind(next));
      }}
      allowDeselect={false}
      withAsterisk
      error={error}
    />
  );
}
