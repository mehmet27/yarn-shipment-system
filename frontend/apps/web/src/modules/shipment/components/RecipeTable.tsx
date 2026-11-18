import type { RecipeLineView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { formatKgText } from '@iplik/units';
import { Paper, Table } from '@iplik/ui';
import { specText, yarnLabel } from './recipeText';

const MIN_TABLE_WIDTH = 560;
const NO_WRAP = { whiteSpace: 'nowrap' } as const;

/** Geniş ekranda reçete tablosu: sayı sütunu sağa yaslı, toplam altta. */
export function RecipeTable({ lines, total }: { lines: readonly RecipeLineView[]; total: string }) {
  const { t } = useTranslation();
  return (
    <Paper withBorder style={{ overflow: 'hidden' }}>
      <Table.ScrollContainer minWidth={MIN_TABLE_WIDTH}>
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
                <Table.Td>{line.lotNo ?? line.lotId ?? '—'}</Table.Td>
                <Table.Td ta="right" style={NO_WRAP}>
                  {formatKgText(line.netKg)}
                </Table.Td>
              </Table.Tr>
            ))}
            <Table.Tr>
              <Table.Th colSpan={4}>{t('shipment.file.recipe.total')}</Table.Th>
              <Table.Th ta="right" style={NO_WRAP}>
                {total}
              </Table.Th>
            </Table.Tr>
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  );
}
