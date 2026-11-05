import { useTranslation } from '@iplik/i18n';
import { Paper, SimpleGrid, Text } from '@iplik/ui';

export type WeightSummaryProps = {
  /** Çıkış net kilosu; taraf gizliyse tire. */
  dispatchNet: string;
  deliveryNet: string;
  difference: string;
};

/** Çıkış → teslim → fark: asıl bakılan üç sayı, tablonun üstünde büyük gösterilir. */
export function WeightSummary({ dispatchNet, deliveryNet, difference }: WeightSummaryProps) {
  const { t } = useTranslation();
  const items = [
    {
      key: 'dispatch',
      label: `${t('shipment.file.dispatch')} ${t('shipment.file.net')}`,
      value: dispatchNet,
    },
    {
      key: 'delivery',
      label: `${t('shipment.file.delivery')} ${t('shipment.file.net')}`,
      value: deliveryNet,
    },
    { key: 'difference', label: t('shipment.file.difference'), value: difference },
  ];
  return (
    <SimpleGrid cols={3} spacing="sm" data-testid="weight-summary">
      {items.map((item) => (
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
  );
}
