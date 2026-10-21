import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Stack, Stepper } from '@iplik/ui';
import { useState } from 'react';
import { useSubmitDelivery } from '../hooks/useSubmitDelivery';
import type { DeliveryDraft, DraftError } from './deliveryDraft';
import { DELIVERY_STEPS, EMPTY_DRAFT, toDeliveryRequest, validateStep } from './deliveryDraft';
import { StepContent } from './StepContent';

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
  const code = mutation.isError ? toProblem(mutation.error).code : undefined;
  const failure =
    code === 'INVALID_SHIPMENT_STATUS' ? 'delivery.invalidStatus' : 'delivery.submitFailed';
  return (
    <Stack>
      <Stepper active={active} size="xs">
        {DELIVERY_STEPS.map((item) => (
          <Stepper.Step key={item} label={t(`delivery.steps.${item}`)} />
        ))}
      </Stepper>
      <StepContent
        step={step}
        draft={draft}
        onChange={(patch) => {
          setDraft({ ...draft, ...patch });
        }}
      />
      {error !== undefined && <Alert color="red">{t(`delivery.errors.${error}`)}</Alert>}
      {code !== undefined && <Alert color="red">{t(failure)}</Alert>}
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
