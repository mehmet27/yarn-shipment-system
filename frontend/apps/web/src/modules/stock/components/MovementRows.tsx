import { useTranslation } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { Select, Table, Text } from '@iplik/ui';
import { useState } from 'react';
import { MovementRow } from './MovementRow';

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
