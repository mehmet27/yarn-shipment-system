import { useTranslation } from '@iplik/i18n';
import { Stack, Stepper, Text } from '@iplik/ui';
import { DELIVERY_STEPS } from './deliveryDraft';

export type WizardProgressProps = { active: number };

/** Adım noktaları 360 px'e tek satırda sığar; adın adı noktaların altında yazılır (FE-UI-07). */
export function WizardProgress({ active }: WizardProgressProps) {
  const { t } = useTranslation();
  const step = DELIVERY_STEPS[active] ?? 'summary';
  return (
    <Stack gap="xs">
      <Stepper active={active} size="xs" wrap={false}>
        {DELIVERY_STEPS.map((item) => (
          <Stepper.Step key={item} aria-label={t(`delivery.steps.${item}`)} />
        ))}
      </Stepper>
      <Text fw={700}>
        {t('delivery.stepOf', {
          current: active + 1,
          total: DELIVERY_STEPS.length,
          name: t(`delivery.steps.${step}`),
        })}
      </Text>
    </Stack>
  );
}
