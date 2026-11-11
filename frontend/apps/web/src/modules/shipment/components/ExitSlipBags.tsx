import type { DispatchBagView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { formatKgText, sumKg } from '@iplik/units';
import { Table, Text } from '@iplik/ui';
import { Fragment } from 'react';

const ROWS = 20;
const GROUPS = 5;
const GROUP_IDS = ['a', 'b', 'c', 'd', 'e'] as const;

/** Çuvalları 20'şerli gruplara böler; her sayfada 5 grup (100 çuval) yan yana durur. */
function toBlocks(bags: readonly DispatchBagView[]): DispatchBagView[][][] {
  const blocks: DispatchBagView[][][] = [];
  for (let start = 0; start < bags.length; start += ROWS * GROUPS) {
    blocks.push(GROUP_IDS.map((_, g) => bags.slice(start + g * ROWS, start + (g + 1) * ROWS)));
  }
  return blocks;
}

/** Kodsuz çuval listesi (Gencer fişi gibi): 5 sütun × 20 satır, sütun "Top" toplamları, Genel Toplam. */
export function ExitSlipBags({ bags }: { bags: readonly DispatchBagView[] }) {
  const { t } = useTranslation();
  if (bags.length === 0) return <Text c="dimmed">{t('shipment.exitSlip.noBags')}</Text>;
  const total = formatKgText(sumKg(bags.map((bag) => bag.weightKg)));
  return (
    <>
      {toBlocks(bags).map((groups) => (
        <Table
          key={groups[0]?.[0]?.seqNo}
          withTableBorder
          withColumnBorders
          fz="xs"
          data-testid="exit-slip-bags"
        >
          <Table.Thead>
            <Table.Tr>
              {GROUP_IDS.map((id) => (
                <Fragment key={id}>
                  <Table.Th>{t('shipment.exitSlip.seq')}</Table.Th>
                  <Table.Th ta="right">{t('shipment.exitSlip.kg')}</Table.Th>
                </Fragment>
              ))}
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {Array.from({ length: ROWS }, (_, r) => (
              <Table.Tr key={groups[0]?.[0] === undefined ? r : groups[0][0].seqNo + r}>
                {groups.map((group, g) => (
                  <Fragment key={GROUP_IDS[g]}>
                    <Table.Td>{group[r]?.seqNo}</Table.Td>
                    <Table.Td ta="right">
                      {group[r] === undefined ? '' : formatKgText(group[r].weightKg)}
                    </Table.Td>
                  </Fragment>
                ))}
              </Table.Tr>
            ))}
            <Table.Tr>
              {groups.map((group, g) => (
                <Fragment key={GROUP_IDS[g]}>
                  <Table.Th>{t('shipment.exitSlip.top', { count: group.length })}</Table.Th>
                  <Table.Th ta="right">
                    {formatKgText(sumKg(group.map((bag) => bag.weightKg)))}
                  </Table.Th>
                </Fragment>
              ))}
            </Table.Tr>
          </Table.Tbody>
        </Table>
      ))}
      <Text ta="right" fw={700}>
        {t('shipment.exitSlip.grandTotal')}:{' '}
        {t('shipment.exitSlip.bagCount', { count: bags.length })} ~ {total}
      </Text>
    </>
  );
}
