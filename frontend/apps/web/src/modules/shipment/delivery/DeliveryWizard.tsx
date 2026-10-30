import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Stack } from '@iplik/ui';
import { useState } from 'react';
import { useSubmitDelivery } from '../hooks/useSubmitDelivery';
import type { DeliveryDraft, DraftError } from './deliveryDraft';
import { DELIVERY_STEPS, EMPTY_DRAFT, toDeliveryRequest, validateStep } from './deliveryDraft';
import { deliveryFailureKey } from './deliveryErrors';
import { StepContent } from './StepContent';
import { WizardProgress } from './WizardProgress';

export type DeliveryWizardProps = { shipmentId: string; onSubmitted: () => void };

/** Kör teslim sihirbazı (mimari doküman, saha akışı): her adım ileri geçmeden doğrulanır. */
export function DeliveryWizard({ shipmentId, onSubmitted }: DeliveryWizardProps) {
  const { t } = useTranslation();
  const [active, setActive] = useState(0);
  const [draft, setDraft] = useState<DeliveryDraft>(EMPTY_DRAFT);
  const [error, setError] = useState<DraftError>();
  const mutation = useSubmitDelivery(shipmentId);
  const step = DELIVERY_STEPS[active] ?? 'summary';
  const next = () => {
    const stepError = validateStep(step, draft);
    setError(stepError);
    if (stepError === undefined) setActive(active + 1);
  };
  const submit = () => {
    const request = toDeliveryRequest(draft);
    if (request !== undefined) mutation.mutate(request, { onSuccess: onSubmitted });
  };
  return (
    <Stack>
      <WizardProgress active={active} />
      <StepContent
        step={step}
        shipmentId={shipmentId}
        draft={draft}
        onChange={(patch) => {
          setDraft({ ...draft, ...patch });
          setError(undefined);
        }}
      />
      {error !== undefined && <Alert color="red">{t(`delivery.errors.${error}`)}</Alert>}
      {mutation.isError && <Alert color="red">{t(deliveryFailureKey(mutation.error))}</Alert>}
      <Group grow>
        <Button
          size="lg"
          variant="default"
          disabled={active === 0}
          onClick={() => {
            setError(undefined);
            setActive(active - 1);
          }}
        >
          {t('delivery.back')}
        </Button>
        {step === 'summary' ? (
          <Button size="lg" loading={mutation.isPending} onClick={submit}>
            {t('delivery.submit')}
          </Button>
        ) : (
          <Button size="lg" onClick={next}>
            {t('delivery.next')}
          </Button>
        )}
      </Group>
    </Stack>
  );
}
