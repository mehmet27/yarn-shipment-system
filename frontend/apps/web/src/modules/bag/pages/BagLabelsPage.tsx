import { useTranslation } from '@iplik/i18n';
import { Stack, Text, Title } from '@iplik/ui';
import { QueryState } from '@/modules/shipment';
import { LotPicker } from '../components/LotPicker';
import { useLockedLots } from '../hooks/useLockedLots';

/** Çuval üretimi ve etiket basımı (rapor Bölüm 10; eksik listesi 2.1, 2.2). */
export function BagLabelsPage() {
  const { t } = useTranslation();
  const query = useLockedLots();
  return (
    <Stack>
      <Title order={1} size="h2">
        {t('bag.title')}
      </Title>
      <Text c="dimmed">{t('bag.intro')}</Text>
      <QueryState query={query}>
        {(lots) => (lots.length === 0 ? <Text>{t('bag.noLots')}</Text> : <LotPicker lots={lots} />)}
      </QueryState>
    </Stack>
  );
}
