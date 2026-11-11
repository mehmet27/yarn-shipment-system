import { useTranslation } from '@iplik/i18n';
import { Box, Stack, Text } from '@iplik/ui';

/** "Teslim Alan" imza alanı; altına araç plakası yazılır. */
export function ExitSlipSignatures({ plateNo }: { plateNo: string | undefined }) {
  const { t } = useTranslation();
  return (
    <Stack gap="xl" pt="xl" w={260}>
      <Text fw={600}>{t('shipment.exitSlip.receivedBy')}</Text>
      <Box style={{ borderBottom: '1px solid var(--mantine-color-gray-6)' }} />
      <Text size="sm">
        {t('shipment.exitSlip.plate')}: {plateNo ?? '—'}
      </Text>
    </Stack>
  );
}
