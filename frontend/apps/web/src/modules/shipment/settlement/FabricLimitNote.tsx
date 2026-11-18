import { useTranslation } from '@iplik/i18n';
import { Text } from '@iplik/ui';
import { formatKgText } from './format';
import type { FabricLimits } from './useFabricLimits';

/** Tek cümle: fasoncuya giden iplik, şimdiye kadar gelen kumaş ve girilebilecek kalan kilo. Fire dosyası okunamadıysa görünmez. */
export function FabricLimitNote({ limits }: { limits: FabricLimits }) {
  const { t } = useTranslation();
  const { sentKg, receivedKg, invoicedKg, remainingKg } = limits;
  if (sentKg === undefined || receivedKg === undefined || remainingKg === undefined) return null;
  const hasInvoice = Number(invoicedKg ?? '0') > 0;
  return (
    <Text size="sm" data-testid="fabric-limit-note">
      {t(hasInvoice ? 'settlement.fabric.limitNoteInvoiced' : 'settlement.fabric.limitNote', {
        sent: formatKgText(sentKg),
        received: formatKgText(receivedKg),
        invoiced: formatKgText(invoicedKg ?? '0'),
        remaining: formatKgText(remainingKg),
      })}
    </Text>
  );
}
