import type { Weighing } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Paper, SimpleGrid, Stack, StatCard, Table, Text } from '@iplik/ui';
import { formatKg } from '@iplik/units';

export type DispatchWeighingPanelProps = {
  weighing: Weighing | undefined;
};

type WeightField = 'grossKg' | 'tareKg' | 'netKg';
const WEIGHT_ROWS: readonly { field: WeightField; labelKey: string }[] = [
  { field: 'grossKg', labelKey: 'shipment.file.gross' },
  { field: 'tareKg', labelKey: 'shipment.file.tare' },
  { field: 'netKg', labelKey: 'shipment.file.net' },
];

/** Deponun kantarda girdiği çıkış tartısı: net kilo ve çuval sayısı büyük, ayrıntı tabloda. */
export function DispatchWeighingPanel({ weighing }: DispatchWeighingPanelProps) {
  const { t } = useTranslation();
  if (weighing === undefined) {
    return <Text c="dimmed">{t('shipment.file.dispatchNotRecorded')}</Text>;
  }
  const summary = [
    {
      key: 'net',
      label: `${t('shipment.file.dispatch')} ${t('shipment.file.net')}`,
      value: formatKg(weighing.netKg),
    },
    { key: 'bags', label: t('shipment.file.bagCount'), value: String(weighing.bagCount) },
  ];
  return (
    <Stack gap="md">
      <SimpleGrid cols={2} spacing="sm" data-testid="weight-summary">
        {summary.map((item) => (
          <StatCard key={item.key} label={item.label} value={item.value} />
        ))}
      </SimpleGrid>
      <Paper withBorder style={{ overflow: 'hidden' }}>
        <Table>
          <Table.Tbody>
            {WEIGHT_ROWS.map(({ field, labelKey }) => (
              <Table.Tr key={field}>
                <Table.Th scope="row">{t(labelKey)}</Table.Th>
                <Table.Td ta="right" style={{ whiteSpace: 'nowrap' }}>
                  {formatKg(weighing[field])}
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Paper>
    </Stack>
  );
}
