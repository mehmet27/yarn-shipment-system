import type { FabricReceipt } from '@iplik/api-client/settlements';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Paper, Stack, Text } from '@iplik/ui';
import { formatKgText } from './format';

export function FabricReceiptCard({ receipt }: { receipt: FabricReceipt }) {
  const { t } = useTranslation();
  const rolls =
    receipt.rollCount === undefined
      ? ''
      : ` · ${t('settlement.fabric.rolls', { count: receipt.rollCount })}`;
  return (
    <Paper withBorder p="md" component="article">
      <Stack gap="xs">
        <Text fw={600}>
          {receipt.fabricType} · {formatKgText(receipt.netKg)}
        </Text>
        <Text size="sm" c="dimmed">
          {t('settlement.fabric.waybillNo')}: {receipt.waybillNo} ·{' '}
          {t('settlement.fabric.receivedAt')} {formatDateTime(receipt.receivedAt)}
          {rolls}
        </Text>
        {receipt.yarns.map((yarn) => (
          <Text key={yarn.yarnType} size="sm">
            {yarn.yarnType}: {formatKgText(yarn.netKg)}
          </Text>
        ))}
      </Stack>
    </Paper>
  );
}
