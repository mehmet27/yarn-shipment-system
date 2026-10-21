import { SEAL_CONDITIONS } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { SegmentedControl, Stack, Text, TextInput } from '@iplik/ui';
import type { StepProps } from './types';

/** Mühür durumu ve üzerindeki numara; çıkış mühür numarası gösterilmez (SEC-05). */
export function SealStep({ draft, onChange }: StepProps) {
  const { t } = useTranslation();
  return (
    <Stack>
      <Text fw={500}>{t('delivery.seal.condition')}</Text>
      <SegmentedControl
        fullWidth
        aria-label={t('delivery.seal.condition')}
        value={draft.sealCondition ?? ''}
        data={SEAL_CONDITIONS.map((value) => ({ value, label: t(`delivery.seal.${value}`) }))}
        onChange={(value) => {
          const sealCondition = SEAL_CONDITIONS.find((condition) => condition === value);
          onChange({ sealCondition, ...(sealCondition === 'MISSING' ? { sealNo: '' } : {}) });
        }}
      />
      {draft.sealCondition !== 'MISSING' && (
        <TextInput
          size="lg"
          label={t('delivery.seal.number')}
          value={draft.sealNo}
          onChange={(event) => {
            onChange({ sealNo: event.currentTarget.value });
          }}
        />
      )}
    </Stack>
  );
}
