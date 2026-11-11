import type { FireSettlement } from '@iplik/api-client/settlements';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Text } from '@iplik/ui';

/** Kapanmış dosyada fatura bilgisi ya da kapandı notu; düzenleme yoktur. */
export function FireClosedNote({ settlement }: { settlement: FireSettlement }) {
  const { t } = useTranslation();
  if (settlement.status !== 'INVOICED') {
    return <Text c="dimmed">{t('settlement.fire.closedNote')}</Text>;
  }
  const date = settlement.invoicedAt === undefined ? '' : formatDateTime(settlement.invoicedAt);
  const note = settlement.invoiceNote === undefined ? '' : ` · ${settlement.invoiceNote}`;
  return (
    <Text>
      {t('settlement.fire.invoiceInfo', { no: settlement.invoiceNo ?? '', date })}
      {note}
    </Text>
  );
}
