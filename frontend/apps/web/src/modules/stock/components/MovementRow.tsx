import { formatDateTime, useTranslation } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { Badge, Table, Text } from '@iplik/ui';
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
      <Table.Td>{formatDateTime(move.occurredAt)}</Table.Td>
      <Table.Td>
        <Badge variant="light" color={KIND_COLOR[move.kind] ?? 'gray'}>
          {t(`stock.kinds.${move.kind}`)}
        </Badge>
      </Table.Td>
      <Table.Td>{move.yarnType}</Table.Td>
      <Table.Td>
        <Text span c={move.netKg.startsWith('-') ? 'red.7' : 'green.8'} fw={600}>
          {formatSignedKg(move.netKg)}
        </Text>
      </Table.Td>
      <Table.Td>{move.note ?? ''}</Table.Td>
    </Table.Tr>
  );
}
