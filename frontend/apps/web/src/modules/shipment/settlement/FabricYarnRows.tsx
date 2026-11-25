import type { FabricYarnOption } from '@iplik/api-client/fabric';
import { useTranslation } from '@iplik/i18n';
import { Stack, Text } from '@iplik/ui';
import { ratioKey, ratioTotal } from './fabricRatios';
import { FabricYarnRow } from './FabricYarnRow';

export type FabricYarnRowsProps = {
  /** Fasoncudaki tüm iplikler; her biri için bir oran satırı. */
  options: readonly FabricYarnOption[];
  ratios: Record<string, string>;
  onChange: (key: string, value: string) => void;
};

const FULL = 100;

/** Fasoncudaki her iplik için bir oran kutusu ve canlı toplam; toplam tam 100 olmalı. */
export function FabricYarnRows({ options, ratios, onChange }: FabricYarnRowsProps) {
  const { t } = useTranslation();
  const total = ratioTotal(ratios);
  const isFull = total === FULL;
  return (
    <Stack gap="xs">
      <Text fw={500}>{t('settlement.fabric.distribution')}</Text>
      <Text size="sm" c="dimmed">
        {t('settlement.fabric.distributionHint')}
      </Text>
      {options.length === 0 && (
        <Text size="sm" c="dimmed">
          {t('settlement.fabric.noYarnHeld')}
        </Text>
      )}
      {options.map((option) => (
        <FabricYarnRow
          key={ratioKey(option)}
          option={option}
          value={ratios[ratioKey(option)] ?? ''}
          onChange={(value) => {
            onChange(ratioKey(option), value);
          }}
        />
      ))}
      <Text size="sm" fw={600} c={isFull ? 'green' : 'red'} data-testid="fabric-ratio-total">
        {t(isFull ? 'settlement.fabric.ratioTotalOk' : 'settlement.fabric.ratioTotal', {
          total: String(total).replace('.', ','),
        })}
      </Text>
    </Stack>
  );
}
