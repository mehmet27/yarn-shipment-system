import type { WeightCheckView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, SimpleGrid, Stack, Text } from '@iplik/ui';
import { formatKgText, formatSignedKg } from '@iplik/units';

/** Kantar uyuşmazlığı: beklenen, tartılan ve fark kg; depo nedeni varsa yanında. */
export function WeightCheckPanel({ check }: { check: WeightCheckView }) {
  const { t } = useTranslation();
  const items = [
    { key: 'expected', value: formatKgText(check.expectedKg) },
    { key: 'measured', value: formatKgText(check.measuredKg) },
    { key: 'diff', value: formatSignedKg(check.diffKg) },
  ];
  return (
    <Alert color="orange" title={t('shipment.file.weightCheck.title')} data-testid="weight-check">
      <Stack gap="sm">
        <Text size="sm">{t('shipment.file.weightCheck.blocked')}</Text>
        <SimpleGrid cols={{ base: 1, xs: 3 }} spacing="sm">
          {items.map((item) => (
            <Stack key={item.key} gap={2}>
              <Text size="sm" c="dimmed">
                {t(`shipment.file.weightCheck.${item.key}`)}
              </Text>
              <Text fw={700}>{item.value}</Text>
            </Stack>
          ))}
        </SimpleGrid>
        {check.shortfallReason !== undefined && (
          <Text size="sm">
            {t('shipment.file.weightCheck.shortfallReason')}: {check.shortfallReason}
          </Text>
        )}
      </Stack>
    </Alert>
  );
}
