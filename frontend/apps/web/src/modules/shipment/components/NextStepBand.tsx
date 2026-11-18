import type { ShipmentStatus } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Box, Text } from '@iplik/ui';

/** Renk ipucu yalnızca kantar uyuşmazlığı ve iptalde; diğerleri nötr. */
const NEXT_STEP_TONE: Readonly<Partial<Record<ShipmentStatus, string>>> = {
  WEIGHT_MISMATCH: 'orange',
  CANCELLED: 'red',
};

/** Sıradaki adım kutusu: girintili, küçük soluk etiket ve yarı kalın cümle. */
export function NextStepBand({ status }: { status: ShipmentStatus }) {
  const { t } = useTranslation();
  const tone = NEXT_STEP_TONE[status];
  return (
    <Box
      px="md"
      py="sm"
      bg={tone === undefined ? 'var(--app-inset)' : `var(--mantine-color-${tone}-light)`}
      style={{
        border: '1px solid var(--app-line)',
        borderRadius: 'var(--mantine-radius-md)',
        ...(tone === undefined
          ? {}
          : { boxShadow: `inset 3px 0 0 var(--mantine-color-${tone}-filled)` }),
      }}
    >
      <Text
        fw={600}
        c={tone === undefined ? 'var(--app-text)' : `var(--mantine-color-${tone}-light-color)`}
      >
        {t('shipment.progress.current', { step: t(`shipment.nextStep.${status}`) })}
      </Text>
    </Box>
  );
}
