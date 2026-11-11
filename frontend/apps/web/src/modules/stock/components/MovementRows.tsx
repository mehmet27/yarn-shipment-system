import { formatDateTime, useTranslation } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { Table, Text } from '@iplik/ui';
import { formatSignedKg } from '@/modules/shipment';

/** Merkez stoğun son hareketleri. */
export function MovementRows({ movements }: { movements: CentralStock['movements'] }) {
  const { t } = useTranslation();
  if (movements.length === 0) return <Text c="dimmed">{t('stock.movementsEmpty')}</Text>;
  return (
    <Table.ScrollContainer minWidth={480}>
      <Table striped aria-label={t('stock.movements')}>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('stock.date')}</Table.Th>
            <Table.Th>{t('stock.kind')}</Table.Th>
            <Table.Th>{t('stock.yarnType')}</Table.Th>
            <Table.Th>{t('stock.kg')}</Table.Th>
            <Table.Th>{t('stock.note')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {movements.map((move) => (
            <Table.Tr key={move.id}>
              <Table.Td>{formatDateTime(move.occurredAt)}</Table.Td>
              <Table.Td>{t(`stock.kinds.${move.kind}`)}</Table.Td>
              <Table.Td>{move.yarnType}</Table.Td>
              <Table.Td>{formatSignedKg(move.netKg)}</Table.Td>
              <Table.Td>{move.note ?? ''}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
