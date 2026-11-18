import type { FireRowView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Table } from '@iplik/ui';
import { formatFireRate, formatKg3 } from '../format';

/** Kumaşı henüz gelmemiş sevkiyatta fire ve eksik hesaplanamaz; "%100 fire" göstermek yanıltır. */
const NOT_YET = '—';

const MIN_WIDTH_PLAIN = 860;
const MIN_WIDTH_STATUS = 1100;

export function FireTable({ rows, withStatus }: { rows: FireRowView[]; withStatus: boolean }) {
  const { t } = useTranslation();
  return (
    <Table.ScrollContainer minWidth={withStatus ? MIN_WIDTH_STATUS : MIN_WIDTH_PLAIN}>
      <Table>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('report.export.shipmentNo')}</Table.Th>
            <Table.Th>{t('report.parties.subcontractor')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.sent')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.returnedYarn')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.fabric')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.fireRate')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.expectedFabric')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.actualFireRate')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.shortage')}</Table.Th>
            {withStatus && (
              <>
                <Table.Th style={{ whiteSpace: 'nowrap' }}>{t('report.export.status')}</Table.Th>
                <Table.Th>{t('report.fire.invoiceNo')}</Table.Th>
              </>
            )}
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {rows.map((row) => {
            const isAwaiting = row.status === 'AWAITING_FABRIC';
            return (
              <Table.Tr key={row.shipmentId}>
                <Table.Td>{row.shipmentNo}</Table.Td>
                <Table.Td>{row.subcontractorName}</Table.Td>
                <Table.Td ta="right">{formatKg3(row.sentKg)}</Table.Td>
                <Table.Td ta="right">{formatKg3(row.returnedYarnKg)}</Table.Td>
                <Table.Td ta="right">{isAwaiting ? NOT_YET : formatKg3(row.fabricKg)}</Table.Td>
                <Table.Td ta="right">{formatFireRate(row.fireRate)}</Table.Td>
                <Table.Td ta="right">{formatKg3(row.expectedFabricKg)}</Table.Td>
                <Table.Td ta="right">
                  {isAwaiting ? NOT_YET : formatFireRate(row.actualFireRate)}
                </Table.Td>
                <Table.Td ta="right">{isAwaiting ? NOT_YET : formatKg3(row.shortageKg)}</Table.Td>
                {withStatus && (
                  <>
                    <Table.Td style={{ whiteSpace: 'nowrap' }}>
                      {t(`report.fire.status.${row.status}`)}
                    </Table.Td>
                    <Table.Td>{row.invoiceNo ?? ''}</Table.Td>
                  </>
                )}
              </Table.Tr>
            );
          })}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
