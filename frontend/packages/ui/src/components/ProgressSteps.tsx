import { Group } from '@mantine/core';
import { ProgressStep } from './ProgressStep';

export type ProgressStepsProps = {
  steps: readonly string[];
  /** Geçerli adımın sırası (0'dan başlar). */
  current: number;
  /** Liste için erişilebilir ad. */
  label: string;
};

/** Sevkiyat süreci adım çubuğu: biten, geçerli ve bekleyen adımları ayırır. */
export function ProgressSteps({ steps, current, label }: ProgressStepsProps) {
  return (
    <Group component="ol" gap={0} wrap="nowrap" align="flex-start" aria-label={label} p={0} m={0}>
      {steps.map((step, index) => (
        <ProgressStep key={step} label={step} index={index} current={current} />
      ))}
    </Group>
  );
}
