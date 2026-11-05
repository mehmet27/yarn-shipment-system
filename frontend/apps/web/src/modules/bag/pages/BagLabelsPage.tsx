import { useTranslation } from '@iplik/i18n';
import { PageHero, Stack, Text } from '@iplik/ui';
import { QueryState } from '@/modules/shipment';
import { LotPicker } from '../components/LotPicker';
import { useLockedLots } from '../hooks/useLockedLots';

/** Çuval üretimi ve etiket basımı (rapor Bölüm 10; eksik listesi 2.1, 2.2). */
export function BagLabelsPage() {
  const { t } = useTranslation();
  const query = useLockedLots();
  return (
    <Stack>
      <PageHero title={t('bag.title')} subtitle={t('bag.intro')} />
      <QueryState query={query}>
        {(lots) => (lots.length === 0 ? <Text>{t('bag.noLots')}</Text> : <LotPicker lots={lots} />)}
      </QueryState>
    </Stack>
  );
}
