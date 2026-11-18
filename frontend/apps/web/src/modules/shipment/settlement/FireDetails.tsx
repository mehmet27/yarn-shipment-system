import type { FireSettlement } from '@iplik/api-client/settlements';
import { useTranslation } from '@iplik/i18n';
import { Box, SimpleGrid, Text } from '@iplik/ui';
import { FireFact } from './FireFact';
import { formatKgText } from './format';

/** "Hesap ayrıntıları": hesabın girdileri (gönderilen, dönen, itiraz edilen, gelen kumaş, fire hesabına esas iplik). */
export function FireDetails({ settlement }: { settlement: FireSettlement }) {
  const { t } = useTranslation();
  return (
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
        {Number(settlement.claimRemainingKg) > 0 && (
          <FireFact
            label={t('settlement.fire.claimToInvoice')}
            value={formatKgText(settlement.claimRemainingKg)}
          />
        )}
        <FireFact label={t('settlement.fire.fabric')} value={formatKgText(settlement.fabricKg)} />
        <FireFact label={t('settlement.fire.base')} value={formatKgText(settlement.baseKg)} />
      </SimpleGrid>
    </Box>
  );
}
