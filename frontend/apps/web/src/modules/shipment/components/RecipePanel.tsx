import type { RecipeLineView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Stack, Table, Text } from '@iplik/ui';
import { fireRateToPctInput, formatKgText, formatPct, sumKg } from '@iplik/units';

export type RecipePanelProps = {
  lines: readonly RecipeLineView[];
  /** Kesir dizesi ("0.0400" = %4). */
  fireRate: string | undefined;
};

function yarnLabel(line: RecipeLineView): string {
  return line.yarnTypeText ?? line.yarnTypeId ?? '—';
}

/** Müdürün yazdığı reçete: satırlar, toplam net kg ve sevkiyatta saklanan fire oranı. */
export function RecipePanel({ lines, fireRate }: RecipePanelProps) {
  const { t } = useTranslation();
  if (lines.length === 0) return <Text c="dimmed">{t('shipment.file.recipe.empty')}</Text>;
  const total = sumKg(lines.map((line) => line.netKg));
  return (
    <Stack gap="sm" data-testid="recipe-panel">
      <Table.ScrollContainer minWidth={0}>
        <Table>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('shipment.file.recipe.yarnType')}</Table.Th>
              <Table.Th>{t('shipment.file.recipe.lot')}</Table.Th>
              <Table.Th ta="right">{t('shipment.file.recipe.netKg')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {lines.map((line, index) => (
              <Table.Tr key={`${yarnLabel(line)}-${String(index)}`}>
                <Table.Td>{yarnLabel(line)}</Table.Td>
                <Table.Td>{line.lotId ?? '—'}</Table.Td>
                <Table.Td ta="right">{formatKgText(line.netKg)}</Table.Td>
              </Table.Tr>
            ))}
            <Table.Tr>
              <Table.Th colSpan={2}>{t('shipment.file.recipe.total')}</Table.Th>
              <Table.Th ta="right">{formatKgText(total)}</Table.Th>
            </Table.Tr>
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
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
