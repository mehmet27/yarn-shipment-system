import type { WeightDecisionKind } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { SegmentedControl, Stack, Text } from '@iplik/ui';

const DECISIONS: readonly WeightDecisionKind[] = ['CANCEL', 'ADD_REMOVE_BAGS', 'UPDATE_RECIPE'];

export type DecisionPickerProps = {
  decision: WeightDecisionKind;
  onChange: (decision: WeightDecisionKind) => void;
};

/** Kantar uyuşmazlığı kararını seçtirir; seçilenin ne yaptığını altına yazar. */
export function DecisionPicker({ decision, onChange }: DecisionPickerProps) {
  const { t } = useTranslation();
  return (
    <Stack gap="xs">
      <SegmentedControl
        fullWidth
        orientation="vertical"
        aria-label={t('shipment.weightDecision.decision')}
        value={decision}
        data={DECISIONS.map((value) => ({
          value,
          label: t(`shipment.weightDecision.options.${value}`),
        }))}
        onChange={(value) => {
          onChange(DECISIONS.find((candidate) => candidate === value) ?? decision);
        }}
      />
      <Text size="sm" c="dimmed">
        {t(`shipment.weightDecision.hints.${decision}`)}
      </Text>
    </Stack>
  );
}
