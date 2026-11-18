import { formatDateTime, useTranslation } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { Table, Text } from '@iplik/ui';
import { DotBadge } from '@iplik/ui';
import { formatSignedKg } from '@/modules/shipment';

const KIND_COLOR: Record<string, string> = {
  YARN_RECEIPT: 'green',
  PRODUCTION: 'orange',
  TO_SUBCONTRACTOR: 'blue',
};

/** Hareket satırı: tür rozeti, giriş yeşil / çıkış kırmızı kilo. */
export function MovementRow({ move }: { move: CentralStock['movements'][number] }) {
  const { t } = useTranslation();
  return (
    <Table.Tr>
      <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatDateTime(move.occurredAt)}</Table.Td>
      <Table.Td style={{ whiteSpace: 'nowrap' }}>
        <DotBadge color={KIND_COLOR[move.kind] ?? 'gray'}>{t(`stock.kinds.${move.kind}`)}</DotBadge>
      </Table.Td>
      <Table.Td>{move.yarnType}</Table.Td>
      <Table.Td>{move.receipt?.supplierName ?? ''}</Table.Td>
      <Table.Td>{move.receipt?.lotNo ?? ''}</Table.Td>
      <Table.Td>{move.receipt?.waybillNo ?? ''}</Table.Td>
      <Table.Td ta="right">
        <Text
          span
          c={
            move.netKg.startsWith('-')
              ? 'var(--mantine-color-red-light-color)'
              : 'var(--mantine-color-green-light-color)'
          }
          fw={600}
        >
          {formatSignedKg(move.netKg)}
        </Text>
      </Table.Td>
      <Table.Td>{move.note ?? ''}</Table.Td>
    </Table.Tr>
  );
}
