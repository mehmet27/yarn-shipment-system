import { BagScanStep } from './BagScanStep';
import type { DeliveryStep } from './deliveryDraft';
import { DocumentStep } from './DocumentStep';
import { SealStep } from './SealStep';
import { SummaryStep } from './SummaryStep';
import type { StepProps } from './types';
import { WeighingStep } from './WeighingStep';

export type StepContentProps = StepProps & { step: DeliveryStep };

export function StepContent({ step, ...props }: StepContentProps) {
  switch (step) {
    case 'seal':
      return <SealStep {...props} />;
    case 'bags':
      return <BagScanStep {...props} />;
    case 'weighing':
      return <WeighingStep {...props} />;
    case 'document':
      return <DocumentStep {...props} />;
    case 'summary':
      return <SummaryStep draft={props.draft} />;
  }
}
