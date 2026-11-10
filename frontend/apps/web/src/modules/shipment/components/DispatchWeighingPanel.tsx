import type { Weighing } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Paper, SimpleGrid, Stack, Table, Text } from '@iplik/ui';
import { formatKg } from '@iplik/units';
import { TareDeviationRow } from './TareDeviationRow';

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
    <Stack gap="md" maw={560}>
      <SimpleGrid cols={2} spacing="sm" data-testid="weight-summary">
        {summary.map((item) => (
          <Paper key={item.key} p={{ base: 'sm', sm: 'md' }} bg="gray.0" radius="md">
            <Text size="sm" c="dimmed">
              {item.label}
            </Text>
            <Text fz={{ base: 16, sm: 24 }} fw={700} lh={1.3}>
              {item.value}
            </Text>
          </Paper>
        ))}
      </SimpleGrid>
      <Table>
        <Table.Tbody>
          {WEIGHT_ROWS.map(({ field, labelKey }) => (
            <Table.Tr key={field}>
              <Table.Th scope="row">{t(labelKey)}</Table.Th>
              <Table.Td ta="right">{formatKg(weighing[field])}</Table.Td>
            </Table.Tr>
          ))}
          <TareDeviationRow weighing={weighing} />
        </Table.Tbody>
      </Table>
    </Stack>
  );
}
