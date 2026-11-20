import { useTranslation } from '@iplik/i18n';
import {
  Button,
  Group,
  ProgressSteps,
  Stack,
  StepList,
  Text,
  useDisclosure,
  useMediaQuery,
} from '@iplik/ui';
import { GUIDE_STEPS, STEP_ACTOR } from './guideSteps';

const WIDE_QUERY = '(min-width: 48em)';
/** Telefonda dokunma hedefi en az 44 px (FE-UI-07). */
const TOUCH_TARGET_PX = 44;

/**
 * Sevkiyatın altı adımı ve her adımı kimin yaptığı. Geniş ekranda yatay çubuk; telefonda yer kaplamasın diye
 * "kaçıncı adım" özeti ve isteyince açılan dikey liste.
 */
export function ShipmentSteps({ stepIndex }: { stepIndex: number }) {
  const { t } = useTranslation();
  const isWide = useMediaQuery(WIDE_QUERY, true, { getInitialValueInEffect: false });
  const [isOpen, { toggle }] = useDisclosure(false);
  const label = t('shipment.progress.steps');
  const steps = GUIDE_STEPS.map((step) => ({
    label: t(`shipment.guide.step.${step}`),
    caption: t(`users.role.${STEP_ACTOR[step]}`),
  }));
  if (isWide) {
    return (
      <ProgressSteps
        label={label}
        steps={steps.map((step) => step.label)}
        captions={steps.map((step) => step.caption)}
        current={stepIndex}
      />
    );
  }
  const currentStep = steps[stepIndex];
  return (
    <Stack gap="xs" data-testid="shipment-steps">
      <Group justify="space-between" gap="xs" wrap="nowrap">
        <Text size="sm" fw={600}>
          {currentStep === undefined
            ? t('shipment.guide.summaryDone', { total: steps.length })
            : t('shipment.guide.summary', {
                current: stepIndex + 1,
                total: steps.length,
                step: currentStep.label,
              })}
        </Text>
        <Button variant="subtle" mih={TOUCH_TARGET_PX} onClick={toggle} aria-expanded={isOpen}>
          {t(isOpen ? 'shipment.guide.hideSteps' : 'shipment.guide.showSteps')}
        </Button>
      </Group>
      {isOpen && <StepList label={label} steps={steps} current={stepIndex} />}
    </Stack>
  );
}
