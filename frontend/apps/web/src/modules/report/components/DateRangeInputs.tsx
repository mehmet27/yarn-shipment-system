import { useTranslation } from '@iplik/i18n';
import { Group, TextInput } from '@iplik/ui';

export type DateRange = { from: string; to: string };

export type DateRangeInputsProps = {
  value: DateRange;
  onChange: (value: DateRange) => void;
  hasError: boolean;
};

/** Özel tarih aralığı; ikisi de doluysa hazır dönemin yerine geçer. */
export function DateRangeInputs({ value, onChange, hasError }: DateRangeInputsProps) {
  const { t } = useTranslation();
  return (
    <Group gap="xs" align="flex-start">
      <TextInput
        type="date"
        aria-label={t('report.range.from')}
        value={value.from}
        max={value.to === '' ? undefined : value.to}
        error={hasError ? t('report.range.invalid') : undefined}
        onChange={(event) => {
          onChange({ ...value, from: event.currentTarget.value });
        }}
      />
      <TextInput
        type="date"
        aria-label={t('report.range.to')}
        value={value.to}
        min={value.from === '' ? undefined : value.from}
        onChange={(event) => {
          onChange({ ...value, to: event.currentTarget.value });
        }}
      />
    </Group>
  );
}

/** İki tarih de doluysa ve başlangıç bitişten sonra değilse geçerli özel aralık. */
export function isValidRange(range: DateRange): boolean {
  return range.from !== '' && range.to !== '' && range.from <= range.to;
}
