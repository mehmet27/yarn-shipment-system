import type { DeliveryDraft } from './deliveryDraft';

export type StepProps = {
  draft: DeliveryDraft;
  onChange: (patch: Partial<DeliveryDraft>) => void;
};
