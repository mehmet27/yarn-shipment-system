import type { RecipeLineView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Stack, Text, useMediaQuery } from '@iplik/ui';
import { fireRateToPctInput, formatKgText, formatPct, sumKg } from '@iplik/units';
import { RecipeLineCards } from './RecipeLineCards';
import { RecipeTable } from './RecipeTable';

export type RecipePanelProps = {
  lines: readonly RecipeLineView[];
  /** Kesir dizesi ("0.0400" = %4). */
  fireRate: string | undefined;
};

const DESKTOP_QUERY = '(min-width: 48em)';

/** Müdürün yazdığı reçete: satırlar, toplam net kg ve sevkiyatta saklanan fire oranı; dar ekranda kart listesi. */
export function RecipePanel({ lines, fireRate }: RecipePanelProps) {
  const { t } = useTranslation();
  const isDesktop = useMediaQuery(DESKTOP_QUERY, true, { getInitialValueInEffect: false });
  if (lines.length === 0) return <Text c="dimmed">{t('shipment.file.recipe.empty')}</Text>;
  const total = formatKgText(sumKg(lines.map((line) => line.netKg)));
  const View = isDesktop ? RecipeTable : RecipeLineCards;
  return (
    <Stack gap="sm" data-testid="recipe-panel">
      <View lines={lines} total={total} />
      {fireRate !== undefined && (
        <Text fw={600}>
          {t('shipment.file.recipe.fireRate', {
            rate: formatPct(fireRateToPctInput(fireRate).replace(',', '.')),
          })}
        </Text>
      )}
    </Stack>
  );
}
