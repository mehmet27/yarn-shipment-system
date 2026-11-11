import { useTranslation } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { Stack, Title } from '@iplik/ui';
import { MovementRows } from './MovementRows';
import { StockRows } from './StockRows';

/** Merkezde iplik cinsi başına eldeki kilo ve son hareketler. */
export function StockTable({ stock }: { stock: CentralStock }) {
  const { t } = useTranslation();
  return (
    <Stack>
      <StockRows stock={stock} />
      <Title order={2} size="h3">
        {t('stock.movements')}
      </Title>
      <MovementRows movements={stock.movements} />
    </Stack>
  );
}
