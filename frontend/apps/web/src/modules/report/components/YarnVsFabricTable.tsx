import type { YarnVsFabricRowView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Table } from '@iplik/ui';
import { formatFireRate, formatKg3 } from '../format';

const MIN_WIDTH = 820;

/** Fasoncu bazında giden iplik ↔ gelen kumaş tablosu. */
export function YarnVsFabricTable({ rows }: { rows: readonly YarnVsFabricRowView[] }) {
  const { t } = useTranslation();
  return (
    <Table.ScrollContainer minWidth={MIN_WIDTH}>
      <Table>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('report.parties.subcontractor')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.sent')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.returnedYarn')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.fabric')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.actualFireRate')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.shortage')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.overLimitCount')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.overLimitFire')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {rows.map((row) => (
            <Table.Tr key={row.subcontractorId}>
              <Table.Td>{row.subcontractorName}</Table.Td>
              <Table.Td ta="right">{formatKg3(row.sentKg)}</Table.Td>
              <Table.Td ta="right">{formatKg3(row.returnedYarnKg)}</Table.Td>
              <Table.Td ta="right">{formatKg3(row.fabricKg)}</Table.Td>
              <Table.Td ta="right">{formatFireRate(row.actualFireRate)}</Table.Td>
              <Table.Td ta="right">{formatKg3(row.shortageKg)}</Table.Td>
              <Table.Td ta="right">{row.overLimitShipmentCount}</Table.Td>
              <Table.Td ta="right">{formatKg3(row.overLimitFireKg)}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
