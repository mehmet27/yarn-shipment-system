import type { DispatchBagView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { formatKgText, sumKg } from '@iplik/units';
import { Table, Text } from '@iplik/ui';

/** Kodsuz çuval listesi: sıra no + kg, altta çuval adedi ve toplam kg. */
export function ExitSlipBags({ bags }: { bags: readonly DispatchBagView[] }) {
  const { t } = useTranslation();
  if (bags.length === 0) return <Text c="dimmed">{t('shipment.exitSlip.noBags')}</Text>;
  return (
    <Table withTableBorder withColumnBorders data-testid="exit-slip-bags">
      <Table.Thead>
        <Table.Tr>
          <Table.Th w={120}>{t('shipment.exitSlip.seq')}</Table.Th>
          <Table.Th ta="right">{t('shipment.exitSlip.kg')}</Table.Th>
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {bags.map((bag) => (
          <Table.Tr key={bag.seqNo}>
            <Table.Td>{bag.seqNo}</Table.Td>
            <Table.Td ta="right">{formatKgText(bag.weightKg)}</Table.Td>
          </Table.Tr>
        ))}
        <Table.Tr>
          <Table.Th>{t('shipment.exitSlip.total', { count: bags.length })}</Table.Th>
          <Table.Th ta="right">{formatKgText(sumKg(bags.map((bag) => bag.weightKg)))}</Table.Th>
        </Table.Tr>
      </Table.Tbody>
    </Table>
  );
}
