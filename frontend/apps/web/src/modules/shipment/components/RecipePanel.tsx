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

/** Sipariş formu ayrıntıları: LYC, en türü, iplik uzunluğu, puss-fine ve fiyat; girilmeyenler atlanır. */
function specText(line: RecipeLineView, t: (key: string) => string): string {
  const parts = [
    line.lycra,
    line.widthType === undefined ? undefined : t(`create.recipe.widthTypes.${line.widthType}`),
    line.yarnLength,
    line.pussFine,
    line.unitPrice === undefined ? undefined : `${line.unitPrice} TL/kg`,
  ].filter((part): part is string => part !== undefined && part !== '');
  return parts.length === 0 ? '—' : parts.join(' · ');
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
              <Table.Th>{t('shipment.file.recipe.fabricType')}</Table.Th>
              <Table.Th>{t('shipment.file.recipe.spec')}</Table.Th>
              <Table.Th>{t('shipment.file.recipe.lot')}</Table.Th>
              <Table.Th ta="right">{t('shipment.file.recipe.netKg')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {lines.map((line, index) => (
              <Table.Tr key={`${yarnLabel(line)}-${String(index)}`}>
                <Table.Td>{yarnLabel(line)}</Table.Td>
                <Table.Td>{line.fabricType ?? '—'}</Table.Td>
                <Table.Td>{specText(line, t)}</Table.Td>
                <Table.Td>{line.lotId ?? '—'}</Table.Td>
                <Table.Td ta="right">{formatKgText(line.netKg)}</Table.Td>
              </Table.Tr>
            ))}
            <Table.Tr>
              <Table.Th colSpan={4}>{t('shipment.file.recipe.total')}</Table.Th>
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
