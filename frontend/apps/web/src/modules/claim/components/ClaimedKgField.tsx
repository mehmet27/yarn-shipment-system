import type { Kilogram } from '@iplik/units';
import { formatKg } from '@iplik/units';
import { useTranslation } from '@iplik/i18n';
import { TextInput } from '@iplik/ui';

export type ClaimedKgFieldProps = {
  value: string;
  onChange: (value: string) => void;
  isInvalid: boolean;
  /** Sevkiyatın net kilosu; biliniyorsa ipucu ve aşım hatası gerçek sınırı yazar. */
  limit: Kilogram | undefined;
  isOverLimit: boolean;
};

/** İtiraz edilen kilo girişi (Türkçe ondalık virgülü kabul edilir). */
export function ClaimedKgField(props: ClaimedKgFieldProps) {
  const { t } = useTranslation();
  const { value, onChange, limit, isInvalid, isOverLimit } = props;
  let error: string | undefined;
  if (isInvalid) error = t('claim.open.claimedKgInvalid');
  else if (isOverLimit && limit !== undefined) {
    error = t('claim.open.claimedKgTooMuch', { kg: formatKg(limit) });
  }
  return (
    <TextInput
      label={t('claim.open.claimedKg')}
      description={
        limit === undefined
          ? t('claim.open.claimedKgHint')
          : t('claim.open.claimedKgLimit', { kg: formatKg(limit) })
      }
      inputMode="decimal"
      value={value}
      onChange={(event) => {
        onChange(event.currentTarget.value);
      }}
      error={error}
      withAsterisk
    />
  );
}
