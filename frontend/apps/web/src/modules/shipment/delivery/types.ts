import type { DeliveryDraft } from './deliveryDraft';

export type StepProps = {
  shipmentId: string;
  draft: DeliveryDraft;
  onChange: (patch: Partial<DeliveryDraft>) => void;
};
