import type { FabricReceipt } from '@iplik/api-client/settlements';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Group, Paper, Stack, Text } from '@iplik/ui';
import { formatKgText } from './format';

export function FabricReceiptCard({ receipt }: { receipt: FabricReceipt }) {
  const { t } = useTranslation();
  const rolls =
    receipt.rollCount === undefined
      ? ''
      : ` · ${t('settlement.fabric.rolls', { count: receipt.rollCount })}`;
  return (
    <Paper withBorder p="md" radius="md" bg="var(--app-surface)" component="article">
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
          <Group key={yarn.yarnType} justify="space-between" wrap="nowrap">
            <Text size="sm">{yarn.yarnType}:</Text>
            <Text size="sm" ta="right" style={{ whiteSpace: 'nowrap' }}>
              {formatKgText(yarn.netKg)}
            </Text>
          </Group>
        ))}
      </Stack>
    </Paper>
  );
}
