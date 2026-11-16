import type { StockShortfall } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, List, Stack, Text } from '@iplik/ui';
import { formatKgText } from '@iplik/units';

type Props = {
  shortfalls: readonly StockShortfall[];
  loading: boolean;
  onConfirm: () => void;
};

/**
 * Merkez iplik stoğu yetmiyor: eksik kilolar gösterilir. Doğru yol önce depoda "Merkeze iplik girişi" yapmaktır;
 * müdür yine de tamamlarsa eksik kilo kadar sistem girişi yazılır ve stok eksiye düşmez.
 */
export function StockShortfallAlert({ shortfalls, loading, onConfirm }: Props) {
  const { t } = useTranslation();
  return (
    <Alert
      color="yellow"
      title={t('shipment.complete.shortfall.title')}
      data-testid="stock-shortfall"
    >
      <Stack gap="xs">
        <List size="sm">
          {shortfalls.map((item) => (
            <List.Item key={item.yarnType}>
              {t('shipment.complete.shortfall.item', {
                yarn: item.yarnType,
                kg: formatKgText(item.missingKg),
              })}
            </List.Item>
          ))}
        </List>
        <Text size="sm">{t('shipment.complete.shortfall.advice')}</Text>
        <Button w="fit-content" color="yellow" loading={loading} onClick={onConfirm}>
          {t('shipment.complete.shortfall.confirm')}
        </Button>
      </Stack>
    </Alert>
  );
}
