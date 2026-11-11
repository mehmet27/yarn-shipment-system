import { useTranslation } from '@iplik/i18n';
import { TextInput } from '@iplik/ui';

type ClaimedKgFieldProps = { value: string; onChange: (value: string) => void; isInvalid: boolean };

/** İtiraz edilen kilo girişi (Türkçe ondalık virgülü kabul edilir). */
export function ClaimedKgField({ value, onChange, isInvalid }: ClaimedKgFieldProps) {
  const { t } = useTranslation();
  return (
    <TextInput
      label={t('claim.open.claimedKg')}
      description={t('claim.open.claimedKgHint')}
      inputMode="decimal"
      value={value}
      onChange={(event) => {
        onChange(event.currentTarget.value);
      }}
      error={isInvalid ? t('claim.open.claimedKgInvalid') : undefined}
      withAsterisk
    />
  );
}
