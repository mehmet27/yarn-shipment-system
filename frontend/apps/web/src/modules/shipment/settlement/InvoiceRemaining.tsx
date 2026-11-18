import type { FireSettlement } from '@iplik/api-client/settlements';
import { useTranslation } from '@iplik/i18n';
import { Alert, Stack, Text } from '@iplik/ui';
import { formatKgText } from './format';

/** Faturalanmamış kilo; içinde kalite itirazından gelen kilo varsa müdüre ayrıca bilgi verilir. */
export function InvoiceRemaining({ settlement }: { settlement: FireSettlement }) {
  const { t } = useTranslation();
  return (
    <Stack gap="xs">
      <Text data-testid="invoice-remaining">
        {t('settlement.fire.invoiceRemaining', { kg: formatKgText(settlement.remainingKg) })}
      </Text>
      {Number(settlement.claimRemainingKg) > 0 && (
        <Alert color="blue" data-testid="invoice-claim-note">
          {t('settlement.fire.invoiceClaimNote', { kg: formatKgText(settlement.claimRemainingKg) })}
        </Alert>
      )}
    </Stack>
  );
}
