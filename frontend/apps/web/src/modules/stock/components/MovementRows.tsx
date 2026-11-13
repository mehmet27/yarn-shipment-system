import { formatDateTime, useTranslation } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { Badge, Select, Table, Text } from '@iplik/ui';
import { useState } from 'react';
import { formatSignedKg } from '@/modules/shipment';

const KIND_COLOR: Record<string, string> = {
  YARN_RECEIPT: 'green',
  PRODUCTION: 'orange',
  TO_SUBCONTRACTOR: 'blue',
};

function MovementRow({ move }: { move: CentralStock['movements'][number] }) {
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

/** Merkez stoğun son hareketleri; türe göre süzülür, giriş yeşil, çıkış kırmızı gösterilir. */
export function MovementRows({ movements }: { movements: CentralStock['movements'] }) {
  const { t } = useTranslation();
  const [kind, setKind] = useState<string | null>(null);
  if (movements.length === 0) return <Text c="dimmed">{t('stock.movementsEmpty')}</Text>;
  const kinds = [...new Set(movements.map((move) => move.kind))];
  const visible = kind === null ? movements : movements.filter((move) => move.kind === kind);
  return (
    <>
      <Select
        w={260}
        label={t('stock.kindFilter')}
        placeholder={t('stock.allKinds')}
        clearable
        data={kinds.map((value) => ({ value, label: t(`stock.kinds.${value}`) }))}
        value={kind}
        onChange={setKind}
      />
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
            {visible.map((move) => (
              <MovementRow key={move.id} move={move} />
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </>
  );
}
