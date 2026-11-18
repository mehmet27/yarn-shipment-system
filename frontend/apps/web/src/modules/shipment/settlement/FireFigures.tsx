import type { FireSettlement } from '@iplik/api-client/settlements';
import { useTranslation } from '@iplik/i18n';
import { SimpleGrid, Stack, Text } from '@iplik/ui';
import { FireDetails } from './FireDetails';
import { FireFact } from './FireFact';
import { formatFireRate, formatKgText, formatPercentText } from './format';

/**
 * Fire dosyası rakamları: önce düz cümle ve sonuç (gelmesi gereken, faturalanacak, gerçekleşen fire, anlaşılan oran),
 * hesabın girdileri (gönderilen, dönen, itiraz edilen, gelen kumaş, fire hesabına esas iplik) "Hesap ayrıntıları" altında.
 */
export function FireFigures({ settlement }: { settlement: FireSettlement }) {
  const { t } = useTranslation();
  return (
    <Stack>
      <Text>
        {t('settlement.fire.summary', {
          sent: formatKgText(settlement.sentKg),
          fabric: formatKgText(settlement.fabricKg),
          expected: formatKgText(settlement.expectedFabricKg),
        })}
      </Text>
      <SimpleGrid cols={{ base: 1, xs: 2, md: 4 }}>
        <FireFact
          label={t('settlement.fire.expected')}
          value={formatKgText(settlement.expectedFabricKg)}
        />
        <FireFact
          label={t('settlement.fire.faulty')}
          value={formatKgText(settlement.remainingKg)}
        />
        <FireFact
          label={t('settlement.fire.actual')}
          value={formatPercentText(settlement.actualFirePercent)}
        />
        <FireFact label={t('settlement.fire.rate')} value={formatFireRate(settlement.fireRate)} />
      </SimpleGrid>
      {settlement.invoices.length > 0 && (
        <Stack gap={2} data-testid="fire-invoices">
          <Text size="sm" fw={600}>
            {t('settlement.fire.invoicedList', { kg: formatKgText(settlement.invoicedKg) })}
          </Text>
          {settlement.invoices.map((entry) => (
            <Text key={entry.invoiceNo} size="sm" c="dimmed">
              {t('settlement.fire.invoiceLine', {
                no: entry.invoiceNo,
                date: entry.invoicedOn,
                kg: formatKgText(entry.kg),
              })}
            </Text>
          ))}
        </Stack>
      )}
      <FireDetails settlement={settlement} />
    </Stack>
  );
}
