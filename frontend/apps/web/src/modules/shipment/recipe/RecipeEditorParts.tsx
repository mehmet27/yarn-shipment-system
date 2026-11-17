import { useTranslation } from '@iplik/i18n';
import { SectionCard, TextInput } from '@iplik/ui';

const MAX_REASON_LENGTH = 500;

/** Düzeltme gerekçesi kartı. */
export function ReasonCard({
  reason,
  onChange,
}: {
  reason: string;
  onChange: (value: string) => void;
}) {
  const { t } = useTranslation();
  return (
    <SectionCard title={t('recipeEdit.reasonTitle')} icon="edit" tone="brand">
      <TextInput
        label={t('recipeEdit.reason')}
        withAsterisk
        maxLength={MAX_REASON_LENGTH}
        value={reason}
        onChange={(event) => {
          onChange(event.currentTarget.value);
        }}
      />
    </SectionCard>
  );
}
