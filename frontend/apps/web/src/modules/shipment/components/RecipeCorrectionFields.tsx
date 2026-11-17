import type { RecipeLineView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Stack, Text, TextInput } from '@iplik/ui';
import { formatKgText } from '@iplik/units';
import type { CorrectionDraft } from './recipeCorrection';

export type RecipeCorrectionFieldsProps = {
  lines: readonly RecipeLineView[];
  draft: CorrectionDraft;
  measuredKg: string;
  /** Yazılan toplam ile tartılan net farkı ("0.000" eşit); satırlar geçersizse undefined. */
  difference: string | undefined;
  onChange: (lineNo: number, value: string) => void;
};

/** Müdür: iplik listesindeki her satırın kilosunu yeniden yazar; toplam tartılan net'e eşit olmalıdır. */
export function RecipeCorrectionFields({
  lines,
  draft,
  measuredKg,
  difference,
  onChange,
}: RecipeCorrectionFieldsProps) {
  const { t } = useTranslation();
  const isEqual = difference === '0.000';
  return (
    <Stack gap="xs" data-testid="recipe-correction">
      <Text size="sm" c="dimmed">
        {t('shipment.weightDecision.recipe.hint', { net: formatKgText(measuredKg) })}
      </Text>
      {lines.map((line) => (
        <TextInput
          key={line.lineNo}
          inputMode="decimal"
          rightSection="kg"
          label={
            line.yarnTypeName ??
            line.yarnTypeText ??
            t('shipment.weightDecision.recipe.line', { no: line.lineNo })
          }
          value={draft[line.lineNo] ?? ''}
          onChange={(event) => {
            onChange(line.lineNo, event.currentTarget.value);
          }}
        />
      ))}
      <Text size="sm" c={isEqual ? 'green' : 'red'} data-testid="recipe-correction-status">
        {isEqual
          ? t('shipment.weightDecision.recipe.equal')
          : t('shipment.weightDecision.recipe.differs')}
      </Text>
    </Stack>
  );
}
