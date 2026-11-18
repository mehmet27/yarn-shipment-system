import type { ShipmentStatus } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Text } from '@iplik/ui';

/** Sıradaki adım şeridinin rengi: olağan akış marka renginde, biten yeşil, karar bekleyen turuncu, iptal kırmızı. */
const NEXT_STEP_TONE: Readonly<Record<ShipmentStatus, string>> = {
  DRAFT: 'gray',
  AWAITING_DELIVERY: 'yellow',
  COMPLETED: 'green',
  WEIGHT_MISMATCH: 'orange',
  CANCELLED: 'red',
};

/** Sıradaki adım şeridi: ekranın "şimdi ne olacak" cevabı; kartı boydan boya keser. */
export function NextStepBand({ status }: { status: ShipmentStatus }) {
  const { t } = useTranslation();
  const tone = NEXT_STEP_TONE[status];
  return (
    <Text
      fw={600}
      px="md"
      py="sm"
      c={tone === 'gray' ? 'var(--app-text)' : `var(--mantine-color-${tone}-light-color)`}
      bg={tone === 'gray' ? 'var(--app-inset)' : `var(--mantine-color-${tone}-light)`}
      style={{
        borderBlock: '1px solid var(--app-line)',
        boxShadow: `inset 3px 0 0 var(--mantine-color-${tone}-filled)`,
      }}
    >
      {t('shipment.progress.current', { step: t(`shipment.nextStep.${status}`) })}
    </Text>
  );
}
