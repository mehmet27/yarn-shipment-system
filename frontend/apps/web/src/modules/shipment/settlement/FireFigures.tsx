import type { FireSettlement } from '@iplik/api-client/settlements';
import { useTranslation } from '@iplik/i18n';
import { SimpleGrid } from '@iplik/ui';
import { FireFact } from './FireFact';
import { formatFireRate, formatKgText, formatPercentText } from './format';

/** G, D, I, K, T, B, hatalı kilo, gerçekleşen fire ve oran (analiz Bölüm 8.1). */
export function FireFigures({ settlement }: { settlement: FireSettlement }) {
  const { t } = useTranslation();
  return (
    <SimpleGrid cols={{ base: 1, xs: 2, md: 3 }}>
      <FireFact label={t('settlement.fire.sent')} value={formatKgText(settlement.sentKg)} />
      <FireFact label={t('settlement.fire.returned')} value={formatKgText(settlement.returnedKg)} />
      <FireFact label={t('settlement.fire.claim')} value={formatKgText(settlement.claimKg)} />
      <FireFact label={t('settlement.fire.fabric')} value={formatKgText(settlement.fabricKg)} />
      <FireFact label={t('settlement.fire.base')} value={formatKgText(settlement.baseKg)} />
      <FireFact
        label={t('settlement.fire.expected')}
        value={formatKgText(settlement.expectedFabricKg)}
      />
      <FireFact label={t('settlement.fire.faulty')} value={formatKgText(settlement.faultyKg)} />
      <FireFact
        label={t('settlement.fire.actual')}
        value={formatPercentText(settlement.actualFirePercent)}
      />
      <FireFact label={t('settlement.fire.rate')} value={formatFireRate(settlement.fireRate)} />
    </SimpleGrid>
  );
}
