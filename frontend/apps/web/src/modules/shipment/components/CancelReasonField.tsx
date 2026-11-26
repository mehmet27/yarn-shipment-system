import { useTranslation } from '@iplik/i18n';
import { TextInput } from '@iplik/ui';

const MAX_REASON_LENGTH = 500;

export function CancelReasonField({
  value,
  onChange,
}: {
  value: string;
  onChange: (next: string) => void;
}) {
  const { t } = useTranslation();
  return (
    <TextInput
      label={t('shipment.cancel.reason')}
      withAsterisk
      maxLength={MAX_REASON_LENGTH}
      value={value}
      onChange={(event) => {
        onChange(event.currentTarget.value);
      }}
    />
  );
}
