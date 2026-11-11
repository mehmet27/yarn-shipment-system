import type { OpeningImportRow } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Table } from '@iplik/ui';
import { formatKg3 } from '../format';

const MIN_WIDTH = 420;

export function OpeningPreview({ rows }: { rows: OpeningImportRow[] }) {
  const { t } = useTranslation();
  return (
    <Table.ScrollContainer minWidth={MIN_WIDTH}>
      <Table>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('report.parties.subcontractor')}</Table.Th>
            <Table.Th>{t('report.matrix.yarnType')}</Table.Th>
            <Table.Th>{t('report.opening.netKg')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {rows.map((row, index) => (
            <Table.Tr key={`${String(index)}-${row.subcontractorId}-${row.yarnType}`}>
              <Table.Td>{row.subcontractorName}</Table.Td>
              <Table.Td>{row.yarnType}</Table.Td>
              <Table.Td>{formatKg3(row.netKg)}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
