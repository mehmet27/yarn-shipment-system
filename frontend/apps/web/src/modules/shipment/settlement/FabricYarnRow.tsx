import type { FabricYarnOption } from '@iplik/api-client/fabric';
import { useTranslation } from '@iplik/i18n';
import { Group, Stack, Text, TextInput } from '@iplik/ui';
import { formatKgText } from './format';

const NUMBER_STYLES = { input: { textAlign: 'right' } } as const;
const ROW_STYLE = { border: '1px solid var(--app-line)', borderRadius: 'var(--mantine-radius-md)' };

function yarnTitle(option: FabricYarnOption, lotText: string) {
  return option.lotNo === undefined ? option.yarnType : `${option.yarnType} · ${lotText}`;
}

type RowProps = {
  option: FabricYarnOption;
  value: string;
  onChange: (value: string) => void;
};

/** Bir iplik: adı, partisi, tedarikçisi, elde kalan kilo ve oran kutusu. */
export function FabricYarnRow({ option, value, onChange }: RowProps) {
  const { t } = useTranslation();
  const lotText = t('settlement.fabric.lotNo', { lot: option.lotNo ?? '' });
  const details = [
    option.supplierName,
    t('settlement.fabric.heldHere', { kg: formatKgText(option.ownKg) }),
    t('settlement.fabric.heldTotal', { kg: formatKgText(option.totalKg) }),
  ].filter((part) => part !== undefined && part !== '');
  return (
    <Group p="sm" bg="var(--app-inset)" justify="space-between" wrap="nowrap" style={ROW_STYLE}>
      <Stack gap={2} style={{ minWidth: 0 }}>
        <Text fw={500}>{yarnTitle(option, lotText)}</Text>
        <Text size="xs" c="dimmed">
          {details.join(' · ')}
        </Text>
      </Stack>
      <TextInput
        aria-label={`${t('settlement.fabric.ratio')}: ${yarnTitle(option, option.lotNo ?? '')}`}
        inputMode="decimal"
        styles={NUMBER_STYLES}
        w={110}
        rightSection="%"
        value={value}
        onChange={(event) => {
          onChange(event.currentTarget.value);
        }}
      />
    </Group>
  );
}
