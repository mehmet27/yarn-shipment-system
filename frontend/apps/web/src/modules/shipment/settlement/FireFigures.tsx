import type { FireSettlement } from '@iplik/api-client/settlements';
import { useTranslation } from '@iplik/i18n';
import { Box, SimpleGrid, Stack, Text } from '@iplik/ui';
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
      <Box component="details">
        <Text component="summary" size="sm" c="dimmed" style={{ cursor: 'pointer' }}>
          {t('settlement.fire.details')}
        </Text>
        <SimpleGrid cols={{ base: 1, xs: 2, md: 3 }} mt="xs">
          <FireFact label={t('settlement.fire.sent')} value={formatKgText(settlement.sentKg)} />
          <FireFact
            label={t('settlement.fire.returned')}
            value={formatKgText(settlement.returnedKg)}
          />
          <FireFact label={t('settlement.fire.claim')} value={formatKgText(settlement.claimKg)} />
          <FireFact label={t('settlement.fire.fabric')} value={formatKgText(settlement.fabricKg)} />
          <FireFact label={t('settlement.fire.base')} value={formatKgText(settlement.baseKg)} />
        </SimpleGrid>
      </Box>
    </Stack>
  );
}
