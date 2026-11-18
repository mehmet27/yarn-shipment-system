import { useTranslation } from '@iplik/i18n';
import { SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';
import { parseKg, sumKg } from '@iplik/units';
import { formatKgText } from './format';
import type { InvoiceLineDraft } from './settlementDraft';

const NUMBER_STYLES = { input: { textAlign: 'right' } } as const;

export type InvoiceYarnRowsProps = {
  lines: readonly InvoiceLineDraft[];
  /** İplik anahtarı → ekranda görünen ad. */
  labelOf: (yarnType: string) => string;
  onChange: (index: number, netKg: string) => void;
};

/** Faturada iplik başına kilo satırları ve bu faturanın toplamı. */
export function InvoiceYarnRows({ lines, labelOf, onChange }: InvoiceYarnRowsProps) {
  const { t } = useTranslation();
  const total = sumKg(lines.map((line) => parseKg(line.netKg) ?? '0.000'));
  return (
    <Stack gap="xs">
      <Text size="sm" c="dimmed">
        {t('settlement.fire.invoiceLinesHint')}
      </Text>
      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        {lines.map((line, index) => (
          <TextInput
            key={line.yarnType}
            label={t('settlement.fire.invoiceYarnKg', { yarn: labelOf(line.yarnType) })}
            inputMode="decimal"
            styles={NUMBER_STYLES}
            value={line.netKg}
            onChange={(event) => {
              onChange(index, event.currentTarget.value);
            }}
          />
        ))}
      </SimpleGrid>
      <Text size="sm" data-testid="invoice-total">
        {t('settlement.fire.invoiceTotal', { kg: formatKgText(total) })}
      </Text>
    </Stack>
  );
}
