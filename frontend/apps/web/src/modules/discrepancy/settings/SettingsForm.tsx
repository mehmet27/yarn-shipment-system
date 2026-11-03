import type { DiscrepancySettingsView } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Paper, SimpleGrid, Stack, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useUpdateDiscrepancySettings } from '../hooks/useDiscrepancySettings';
import type { DraftErrors, SettingsDraft } from './settingsDraft';
import { draftOf, MAX_HOURS, MIN_HOURS, validateDraft } from './settingsDraft';

const SERVER_ERRORS: Record<string, string> = {
  TOLERANCE_BANDS_INVALID: 'discrepancy.settings.errors.order',
  TIME_LIMIT_INVALID: 'discrepancy.settings.errors.hours',
};

function serverErrorKey(error: unknown): string {
  return SERVER_ERRORS[toProblem(error).code] ?? 'discrepancy.settings.failed';
}

/** Alanlar istemcide backend kurallarıyla denetlenir; sunucunun reddi de alan dışında gösterilir. */
/** Taslak alanlar; değişiklik "kaydedildi" bildirimini kaldırır. */
function useSettingsDraft(settings: DiscrepancySettingsView) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState<SettingsDraft>(() => draftOf(settings));
  const [errors, setErrors] = useState<DraftErrors>({});
  const [isSaved, setIsSaved] = useState(false);
  const field = (name: keyof SettingsDraft) => ({
    value: draft[name],
    error:
      errors[name] === undefined ? undefined : t(`discrepancy.settings.errors.${errors[name]}`),
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => {
      const { value } = event.currentTarget;
      setDraft((current) => ({ ...current, [name]: value }));
      setIsSaved(false);
    },
    withAsterisk: true,
  });
  return { draft, setErrors, isSaved, setIsSaved, field };
}

export function SettingsForm({ settings }: { settings: DiscrepancySettingsView }) {
  const { t } = useTranslation();
  const { draft, setErrors, isSaved, setIsSaved, field } = useSettingsDraft(settings);
  const mutation = useUpdateDiscrepancySettings();
  const submit = () => {
    const result = validateDraft(draft);
    setErrors('errors' in result ? result.errors : {});
    if ('errors' in result) return;
    mutation.mutate(result.request, {
      onSuccess: () => {
        setIsSaved(true);
      },
    });
  };
  return (
    <Paper withBorder p="md">
      <Stack>
        <SimpleGrid cols={{ base: 1, sm: 3 }}>
          <TextInput
            label={t('discrepancy.settings.greenMaxPct')}
            description={t('discrepancy.settings.greenHint')}
            inputMode="decimal"
            {...field('greenMaxPct')}
          />
          <TextInput
            label={t('discrepancy.settings.yellowMaxPct')}
            description={t('discrepancy.settings.yellowHint')}
            inputMode="decimal"
            {...field('yellowMaxPct')}
          />
          <TextInput
            label={t('discrepancy.settings.inTransitMaxHours')}
            description={t('discrepancy.settings.hoursHint', { min: MIN_HOURS, max: MAX_HOURS })}
            inputMode="numeric"
            {...field('inTransitMaxHours')}
          />
        </SimpleGrid>
        {mutation.isError && <Alert color="red">{t(serverErrorKey(mutation.error))}</Alert>}
        {isSaved && <Alert color="green">{t('discrepancy.settings.saved')}</Alert>}
        <Button loading={mutation.isPending} onClick={submit}>
          {t('discrepancy.settings.submit')}
        </Button>
      </Stack>
    </Paper>
  );
}
