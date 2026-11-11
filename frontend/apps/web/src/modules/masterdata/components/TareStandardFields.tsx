import { useTranslation } from '@iplik/i18n';
import { SimpleGrid, TextInput } from '@iplik/ui';
import type { TareStandardDraft, TareStandardErrors } from '../tareStandardDraft';

type Props = {
  draft: TareStandardDraft;
  errors: TareStandardErrors;
  onChange: (patch: Partial<TareStandardDraft>) => void;
};

export function TareStandardFields({ draft, errors, onChange }: Props) {
  const { t } = useTranslation();
  return (
    <SimpleGrid cols={{ base: 1, sm: 2 }}>
      <TextInput
        label={t('masterData.tareStandard.standardTareKg')}
        withAsterisk
        inputMode="decimal"
        value={draft.standardTareKg}
        onChange={(event) => {
          onChange({ standardTareKg: event.currentTarget.value });
        }}
        error={errors.standardTareKg ? t('masterData.tareStandard.invalidKg') : undefined}
      />
      <TextInput
        label={t('masterData.tareStandard.maxDeviationPct')}
        withAsterisk
        inputMode="decimal"
        value={draft.maxDeviationPct}
        onChange={(event) => {
          onChange({ maxDeviationPct: event.currentTarget.value });
        }}
        error={errors.maxDeviationPct ? t('masterData.tareStandard.invalidPct') : undefined}
      />
    </SimpleGrid>
  );
}
